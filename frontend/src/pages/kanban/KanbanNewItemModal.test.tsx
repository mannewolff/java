import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import KanbanNewItemModal from './KanbanNewItemModal';

const TEMPLATE = '## Kontext\n\n## Aufgabe\n\n## Akzeptanzkriterium\n\n## Abhängigkeiten\n';

describe('KanbanNewItemModal', () => {
  beforeEach(() => vi.clearAllMocks());
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

  it('Anlegen ruft onSubmit mit getrimmtem Titel und der (ggf. bearbeiteten) Beschreibung', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<KanbanNewItemModal open onClose={vi.fn()} onSubmit={onSubmit} />);

    const user = userEvent.setup();
    await user.type(screen.getByLabelText('Titel'), '  Neues Item  ');
    await user.click(screen.getByRole('button', { name: 'Anlegen' }));

    expect(onSubmit).toHaveBeenCalledWith('Neues Item', TEMPLATE);
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
