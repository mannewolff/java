import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import KanbanSettingsDrawer from './KanbanSettingsDrawer';

describe('KanbanSettingsDrawer', () => {
  afterEach(() => cleanup());

  it('zeigt die aktuelle Retention beim Öffnen', () => {
    render(
      <KanbanSettingsDrawer
        open
        currentRetentionDays={14}
        onClose={vi.fn()}
        onSubmit={vi.fn()}
      />,
    );

    expect(screen.getByText(/Done-Items nach/)).toHaveTextContent('14');
  });

  it('ruft onSubmit mit der Retention auf, ohne showArchived-Parameter (#283)', async () => {
    const onSubmit = vi.fn();
    render(
      <KanbanSettingsDrawer
        open
        currentRetentionDays={5}
        onClose={vi.fn()}
        onSubmit={onSubmit}
      />,
    );

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Übernehmen' }));

    expect(onSubmit).toHaveBeenCalledWith(5);
  });

  it('zeigt keinen Archiv-Toggle mehr (#283 — Archiv nur über Listen-Filter)', () => {
    render(
      <KanbanSettingsDrawer
        open
        currentRetentionDays={5}
        onClose={vi.fn()}
        onSubmit={vi.fn()}
      />,
    );

    expect(screen.queryByLabelText('Archivierte Items anzeigen')).not.toBeInTheDocument();
    expect(screen.queryByText('Archivierte Items anzeigen')).not.toBeInTheDocument();
  });

  it('Abbrechen ruft onClose ohne zu speichern', async () => {
    const onClose = vi.fn();
    const onSubmit = vi.fn();
    render(
      <KanbanSettingsDrawer
        open
        currentRetentionDays={5}
        onClose={onClose}
        onSubmit={onSubmit}
      />,
    );

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Abbrechen' }));

    expect(onClose).toHaveBeenCalledTimes(1);
    expect(onSubmit).not.toHaveBeenCalled();
  });
});
