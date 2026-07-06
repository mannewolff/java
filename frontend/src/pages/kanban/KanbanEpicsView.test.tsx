import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import KanbanEpicsView from './KanbanEpicsView';
import type { KanbanEpic } from '../../api/kanban';

function epic(overrides: Partial<KanbanEpic> = {}): KanbanEpic {
  return {
    id: 7,
    number: 3,
    title: '10-Tage Workshop',
    body: '## Kontext\nBeschreibung',
    type: 'EPIC',
    progress: { done: 1, total: 4 },
    ...overrides,
  };
}

describe('KanbanEpicsView', () => {
  afterEach(() => cleanup());

  it('zeigt einen Leerzustand, wenn keine Epics existieren', () => {
    render(<KanbanEpicsView epics={[]} onOpen={vi.fn()} />);
    expect(screen.getByText('Noch keine Epics')).toBeInTheDocument();
  });

  it('zeigt je Epic Kürzel, Titel und Fortschritt done/total', () => {
    render(<KanbanEpicsView epics={[epic()]} onOpen={vi.fn()} />);

    expect(screen.getByText('10-Tage Workshop')).toBeInTheDocument();
    expect(screen.getByText('1/4 Stories fertig')).toBeInTheDocument();
    // Kürzel „1WI" aus „10-Tage Workshop" → „1W" (zwei Wörter) ... prüfe die tatsächliche Ableitung.
    expect(screen.getByText('1W')).toBeInTheDocument();
  });

  it('öffnet ein Epic bei Klick', async () => {
    const onOpen = vi.fn();
    render(<KanbanEpicsView epics={[epic({ id: 9, title: 'Alpha Beta' })]} onOpen={onOpen} />);

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Epic öffnen: Alpha Beta' }));

    expect(onOpen).toHaveBeenCalledWith(expect.objectContaining({ id: 9 }));
  });
});
