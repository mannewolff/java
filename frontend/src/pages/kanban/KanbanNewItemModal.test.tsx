import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import KanbanNewItemModal from './KanbanNewItemModal';
import type { KanbanEpic } from '../../api/kanban';
import { getKanbanEpics } from '../../api/kanban';

vi.mock('../../api/kanban', async () => {
  const actual = await vi.importActual<typeof import('../../api/kanban')>('../../api/kanban');
  return { ...actual, getKanbanEpics: vi.fn() };
});

const TEMPLATE = '## Kontext\n\n## Aufgabe\n\n## Akzeptanzkriterium\n\n## Abhängigkeiten\n';

function epic(overrides: Partial<KanbanEpic> = {}): KanbanEpic {
  return {
    id: 7,
    number: 3,
    title: '10-Tage Workshop',
    body: '',
    type: 'EPIC',
    shortcode: null,
    progress: { done: 0, total: 0 },
    ...overrides,
  };
}

describe('KanbanNewItemModal', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getKanbanEpics).mockResolvedValue([]);
  });
  afterEach(() => cleanup());

  it('öffnet mit leerem Titel und vorbefüllter Vorlage im Beschreibungsfeld', () => {
    render(<KanbanNewItemModal open onClose={vi.fn()} onSubmit={vi.fn()} />);

    expect(screen.getByLabelText('Titel')).toHaveValue('');
    expect(screen.getByLabelText('Beschreibung')).toHaveValue(TEMPLATE);
  });

  it('Anlegen ist disabled, solange der Titel leer ist', () => {
    render(<KanbanNewItemModal open onClose={vi.fn()} onSubmit={vi.fn()} />);

    expect(screen.getByRole('button', { name: 'Anlegen' })).toBeDisabled();
  });

  it('Anlegen ruft onSubmit als normales Item (Typ ITEM, kein Epic)', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<KanbanNewItemModal open onClose={vi.fn()} onSubmit={onSubmit} />);

    const user = userEvent.setup();
    await user.type(screen.getByLabelText('Titel'), '  Neues Item  ');
    await user.click(screen.getByRole('button', { name: 'Anlegen' }));

    expect(onSubmit).toHaveBeenCalledWith('Neues Item', TEMPLATE, 'ITEM', null, null);
  });

  it('blendet bei Typ=Epic die Epic-Auswahl aus und legt ein Epic ohne Parent an', async () => {
    vi.mocked(getKanbanEpics).mockResolvedValue([epic()]);
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<KanbanNewItemModal open onClose={vi.fn()} onSubmit={onSubmit} />);

    const user = userEvent.setup();
    await user.selectOptions(screen.getByLabelText('Typ'), 'EPIC');

    expect(screen.queryByLabelText('Epic')).not.toBeInTheDocument();

    await user.type(screen.getByLabelText('Titel'), 'Mein Epic');
    await user.click(screen.getByRole('button', { name: 'Anlegen' }));

    expect(onSubmit).toHaveBeenCalledWith('Mein Epic', TEMPLATE, 'EPIC', null, null);
  });

  it('sendet ein eingegebenes Kürzel beim Anlegen eines Epics (#329)', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<KanbanNewItemModal open onClose={vi.fn()} onSubmit={onSubmit} />);

    const user = userEvent.setup();
    await user.selectOptions(screen.getByLabelText('Typ'), 'EPIC');
    await user.type(screen.getByLabelText('Titel'), 'Workshop');
    await user.type(screen.getByLabelText('Kürzel'), 'ITB');
    await user.click(screen.getByRole('button', { name: 'Anlegen' }));

    expect(onSubmit).toHaveBeenCalledWith('Workshop', TEMPLATE, 'EPIC', null, 'ITB');
  });

  it('sendet parentId, wenn ein Epic ausgewählt wurde', async () => {
    vi.mocked(getKanbanEpics).mockResolvedValue([epic({ id: 7, title: 'Workshop' })]);
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<KanbanNewItemModal open onClose={vi.fn()} onSubmit={onSubmit} />);

    const user = userEvent.setup();
    await waitFor(() => expect(getKanbanEpics).toHaveBeenCalled());
    // Native Select: die Option erscheint erst, nachdem die Epics geladen sind.
    await screen.findByRole('option', { name: /Workshop/ });
    await user.selectOptions(screen.getByLabelText('Epic'), '7');

    await user.type(screen.getByLabelText('Titel'), 'Story');
    await user.click(screen.getByRole('button', { name: 'Anlegen' }));

    expect(onSubmit).toHaveBeenCalledWith('Story', TEMPLATE, 'ITEM', 7, null);
  });

  it('übernimmt ein vorbelegtes Epic (defaultParentId)', async () => {
    vi.mocked(getKanbanEpics).mockResolvedValue([epic({ id: 7, title: 'Workshop' })]);
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<KanbanNewItemModal open onClose={vi.fn()} onSubmit={onSubmit} defaultParentId={7} />);

    const user = userEvent.setup();
    await waitFor(() => expect(getKanbanEpics).toHaveBeenCalled());
    await user.type(screen.getByLabelText('Titel'), 'Story');
    await user.click(screen.getByRole('button', { name: 'Anlegen' }));

    expect(onSubmit).toHaveBeenCalledWith('Story', TEMPLATE, 'ITEM', 7, null);
  });

  it('Abbrechen ruft onClose, ohne zu speichern', async () => {
    const onClose = vi.fn();
    const onSubmit = vi.fn();
    render(<KanbanNewItemModal open onClose={onClose} onSubmit={onSubmit} />);

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Abbrechen' }));

    expect(onClose).toHaveBeenCalledTimes(1);
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('setzt Titel und Vorlage bei jedem erneuten Öffnen zurück', () => {
    const { rerender } = render(
      <KanbanNewItemModal open={false} onClose={vi.fn()} onSubmit={vi.fn()} />,
    );
    rerender(<KanbanNewItemModal open onClose={vi.fn()} onSubmit={vi.fn()} />);

    expect(screen.getByLabelText('Titel')).toHaveValue('');
    expect(screen.getByLabelText('Beschreibung')).toHaveValue(TEMPLATE);
  });
});
