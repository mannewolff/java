import { ApiError, authedFetch, type ApiErrorBody } from './client';

const MD_TO_PDF_PATH = '/api/tools/md-to-pdf';

async function safeJson(response: Response): Promise<ApiErrorBody | null> {
  try {
    return (await response.json()) as ApiErrorBody;
  } catch {
    return null;
  }
}

/**
 * Konvertiert Markdown-Text zu PDF via `POST /api/tools/md-to-pdf` (#27).
 * Liefert das PDF als Blob (für In-App-Preview und Download).
 */
export async function convertMarkdownToPdf(markdown: string): Promise<Blob> {
  const response = await authedFetch(MD_TO_PDF_PATH, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ markdown }),
  });

  if (!response.ok) {
    const errorBody = await safeJson(response);
    const message = errorBody?.message ?? `${response.status} ${response.statusText}`;
    throw new ApiError(response.status, message, errorBody);
  }

  return response.blob();
}
