import { ApiError, authedFetch, type ApiErrorBody } from './client';

const SVG_TO_PNG_PATH = '/api/tools/svg-to-png';

async function safeJson(response: Response): Promise<ApiErrorBody | null> {
  try {
    return (await response.json()) as ApiErrorBody;
  } catch {
    return null;
  }
}

export interface SvgToPngOptions {
  /** Zielbreite in Pixeln. Wenn `undefined`, nimmt das Backend die SVG-eigene Breite. */
  width?: number;
  /** Zielhöhe in Pixeln. Wenn `undefined`, nimmt das Backend die SVG-eigene Höhe. */
  height?: number;
  /** `"transparent"` oder ein `#rrggbb`-Hex (case-insensitive). Default `"transparent"`. */
  background?: string;
}

export interface SvgToPngResponse {
  blob: Blob;
  contentType: string;
}

/**
 * Konvertiert ein SVG zu PNG via `POST /api/tools/svg-to-png`. Width/Height optional;
 * Background bleibt transparent wenn weggelassen oder explizit "transparent".
 */
export async function convertSvgToPng(
  file: File,
  options: SvgToPngOptions = {},
): Promise<SvgToPngResponse> {
  const body = new FormData();
  body.append('file', file);
  if (options.width != null) body.append('width', options.width.toString());
  if (options.height != null) body.append('height', options.height.toString());
  if (options.background != null) body.append('background', options.background);

  const response = await authedFetch(SVG_TO_PNG_PATH, { method: 'POST', body });

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
