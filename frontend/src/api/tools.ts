import { ApiError, authedFetch, type ApiErrorBody } from './client';

const REMOVE_BG_PATH = '/api/tools/remove-background';

async function safeJson(response: Response): Promise<ApiErrorBody | null> {
  try {
    return (await response.json()) as ApiErrorBody;
  } catch {
    return null;
  }
}

/**
 * Sends an image file to the Spring backend (which proxies to python-tools)
 * and returns the resulting transparent-background PNG as a Blob.
 */
export async function removeBackground(file: File): Promise<Blob> {
  const body = new FormData();
  body.append('file', file);

  const response = await authedFetch(REMOVE_BG_PATH, {
    method: 'POST',
    body,
  });

  if (!response.ok) {
    const errorBody = await safeJson(response);
    const message = errorBody?.message ?? `${response.status} ${response.statusText}`;
    throw new ApiError(response.status, message, errorBody);
  }

  return await response.blob();
}
