import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import MarkdownToPdfPage from './MarkdownToPdfPage';
import { NotifyProvider } from '../../notify/NotifyProvider';
import * as api from '../../api/markdownToPdf';

function renderPage(): ReturnType<typeof render> {
  return render(
    <NotifyProvider>
      <MarkdownToPdfPage />
    </NotifyProvider>,
  );
}

describe('MarkdownToPdfPage', () => {
  beforeEach(() => {
    vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:fake-pdf');
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('disables "PDF erzeugen" until markdown is entered', async () => {
    renderPage();
    const button = screen.getByRole('button', { name: /PDF erzeugen/i });
    expect(button).toBeDisabled();

    await userEvent.type(screen.getByLabelText('Markdown'), '# Hallo');
    expect(button).toBeEnabled();
  });

  it('shows markdown preview tab as active by default', () => {
    renderPage();
    const previewTab = screen.getByRole('tab', { name: /Vorschau/i });
    expect(previewTab).toHaveAttribute('aria-selected', 'true');
  });

  it('renders markdown input as HTML in preview tab', async () => {
    renderPage();
    await userEvent.type(screen.getByLabelText('Markdown'), '# Mein Titel');
    const heading = await screen.findByRole('heading', { level: 1, name: /Mein Titel/i });
    expect(heading).toBeInTheDocument();
  });

  it('disables PDF tab when no PDF has been generated', () => {
    renderPage();
    const pdfTab = screen.getByRole('tab', { name: /PDF/i });
    expect(pdfTab).toHaveClass('Mui-disabled');
  });

  it('converts markdown, switches to PDF tab automatically, shows iframe and download', async () => {
    const blob = new Blob([new Uint8Array([1, 2, 3])], { type: 'application/pdf' });
    const spy = vi.spyOn(api, 'convertMarkdownToPdf').mockResolvedValue(blob);

    renderPage();
    await userEvent.type(screen.getByLabelText('Markdown'), '# Titel');
    await userEvent.click(screen.getByRole('button', { name: /PDF erzeugen/i }));

    await waitFor(() => {
      expect(screen.getByTitle('PDF-Vorschau')).toBeInTheDocument();
    });
    expect(spy).toHaveBeenCalledWith('# Titel');
    expect(screen.getByRole('link', { name: /PDF herunterladen/i })).toHaveAttribute(
      'href',
      'blob:fake-pdf',
    );
    const pdfTab = screen.getByRole('tab', { name: /PDF/i });
    expect(pdfTab).toHaveAttribute('aria-selected', 'true');
  });

  it('shows no PDF iframe when conversion fails', async () => {
    vi.spyOn(api, 'convertMarkdownToPdf').mockRejectedValue(new Error('boom'));

    renderPage();
    await userEvent.type(screen.getByLabelText('Markdown'), '# Titel');
    await userEvent.click(screen.getByRole('button', { name: /PDF erzeugen/i }));

    await waitFor(() => {
      expect(screen.queryByTitle('PDF-Vorschau')).not.toBeInTheDocument();
    });
  });
});
