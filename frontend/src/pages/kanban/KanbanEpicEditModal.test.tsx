import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import KanbanEpicEditModal from './KanbanEpicEditModal';
import type { KanbanEpic } from '../../api/kanban';

function epic(overrides: Partial<KanbanEpic> = {}): KanbanEpic {
  return {
    id: 7,
    number: 3,
    title: 'Workshop A',
    body: 'Alte Beschreibung',
    type: 'EPIC',
    shortcode: null,
    progress: { done: 0, total: 0 },
    ...overrides,
  };
}

describe('KanbanEpicEditModal', () => {
  afterEach(() => cleanup());

  it('befüllt die Felder aus dem übergebenen Epic vor', () => {
    render(
      <KanbanEpicEditModal open epic={epic({ shortcode: 'WSA' })} onClose={vi.fn()} onSubmit={vi.fn()} />,
    );

    expect(screen.getByLabelText('Titel')).toHaveValue('Workshop A');
    expect(screen.getByLabelText('Beschreibung')).toHaveValue('Alte Beschreibung');
    expect(screen.getByLabelText('Kürzel')).toHaveValue('WSA');
  });

  it('rendert das Beschreibungsfeld mit fester Starthöhe (kein Autosize, #338)', () => {
    render(<KanbanEpicEditModal open epic={epic()} onClose={vi.fn()} onSubmit={vi.fn()} />);

    expect(screen.getByLabelText('Beschreibung')).toHaveAttribute('rows', '8');
  });

  it('sendet Titel, Body und Kürzel beim Speichern', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<KanbanEpicEditModal open epic={epic()} onClose={vi.fn()} onSubmit={onSubmit} />);

    const user = userEvent.setup();
    await user.clear(screen.getByLabelText('Titel'));
    await user.type(screen.getByLabelText('Titel'), 'Workshop B');
    await user.type(screen.getByLabelText('Kürzel'), 'WSB');
    await user.click(screen.getByRole('button', { name: 'Speichern' }));

    expect(onSubmit).toHaveBeenCalledWith('Workshop B', 'Alte Beschreibung', 'WSB');
  });

  it('sendet null als Kürzel, wenn das Feld leer bleibt', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<KanbanEpicEditModal open epic={epic()} onClose={vi.fn()} onSubmit={onSubmit} />);

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Speichern' }));

    expect(onSubmit).toHaveBeenCalledWith('Workshop A', 'Alte Beschreibung', null);
  });

  it('Speichern ist disabled, solange der Titel leer ist', async () => {
    render(<KanbanEpicEditModal open epic={epic()} onClose={vi.fn()} onSubmit={vi.fn()} />);

    const user = userEvent.setup();
    await user.clear(screen.getByLabelText('Titel'));

    expect(screen.getByRole('button', { name: 'Speichern' })).toBeDisabled();
  });

  it('Abbrechen ruft onClose, ohne zu speichern', async () => {
    const onClose = vi.fn();
    const onSubmit = vi.fn();
    render(<KanbanEpicEditModal open epic={epic()} onClose={onClose} onSubmit={onSubmit} />);

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Abbrechen' }));

    expect(onClose).toHaveBeenCalledTimes(1);
    expect(onSubmit).not.toHaveBeenCalled();
  });
});
