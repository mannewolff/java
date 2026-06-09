import { ApiError, authedFetch, type ApiErrorBody } from './client';

const RASTER_TO_PNG_PATH = '/api/tools/raster-to-png';

async function safeJson(response: Response): Promise<ApiErrorBody | null> {
  try {
    return (await response.json()) as ApiErrorBody;
  } catch {
    return null;
  }
}

export interface RasterToPngOptions {
  /** Zielbreite in Pixeln. Wenn `undefined`, bleibt die Originalbreite erhalten. */
  width?: number;
  /** Zielhöhe in Pixeln. Wenn `undefined`, bleibt die Originalhöhe erhalten. */
  height?: number;
}

export interface RasterToPngResponse {
  blob: Blob;
  contentType: string;
}

/**
 * Konvertiert ein JPEG oder PNG zu PNG via `POST /api/tools/raster-to-png`.
 * Width/Height optional; ohne Angabe bleibt die Originalgröße erhalten.
 */
export async function convertRasterToPng(
  file: File,
  options: RasterToPngOptions = {},
): Promise<RasterToPngResponse> {
  const body = new FormData();
  body.append('file', file);
  if (options.width != null) body.append('width', options.width.toString());
  if (options.height != null) body.append('height', options.height.toString());

  const response = await authedFetch(RASTER_TO_PNG_PATH, { method: 'POST', body });

  if (!response.ok) {
    const errorBody = await safeJson(response);
    const message = errorBody?.message ?? `${response.status} ${response.statusText}`;
    throw new ApiError(response.status, message, errorBody);
  }

  return {
    blob: await response.blob(),
    contentType: response.headers.get('Content-Type') ?? 'image/png',
  };
}
