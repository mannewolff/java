import { ApiError, type ApiErrorBody } from './client';

const CROP_PATH = '/api/tools/crop-og';
const PALETTE_PATH = '/api/tools/palette';

async function safeJson(response: Response): Promise<ApiErrorBody | null> {
  try {
    return (await response.json()) as ApiErrorBody;
  } catch {
    return null;
  }
}

async function checkOk(response: Response): Promise<void> {
  if (response.ok) return;
  const body = await safeJson(response);
  const message = body?.message ?? `${response.status} ${response.statusText}`;
  throw new ApiError(response.status, message, body);
}

export interface PaletteResponse {
  colors: string[];
}

export interface CropOptions {
  xOffset?: number;
  width?: number;
  height?: number;
  quality?: number;
}

export async function cropOg(
  file: File,
  yOffset: number,
  options: CropOptions = {},
): Promise<Blob> {
  const body = new FormData();
  body.append('file', file);
  body.append('y_offset', yOffset.toString());
  body.append('x_offset', (options.xOffset ?? 0.5).toString());
  body.append('quality', (options.quality ?? 88).toString());
  if (options.width !== undefined) body.append('width', options.width.toString());
  if (options.height !== undefined) body.append('height', options.height.toString());

  const response = await fetch(CROP_PATH, { method: 'POST', body });
  await checkOk(response);
  return await response.blob();
}

export async function extractPalette(
  file: File,
  count = 6,
): Promise<string[]> {
  const body = new FormData();
  body.append('file', file);
  body.append('count', count.toString());

  const response = await fetch(PALETTE_PATH, { method: 'POST', body });
  await checkOk(response);
  const json = (await response.json()) as PaletteResponse;
  return json.colors;
}
