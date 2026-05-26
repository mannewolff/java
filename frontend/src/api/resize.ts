import { ApiError, authedFetch, type ApiErrorBody } from './client';

const RESIZE_PATH = '/api/tools/resize';

async function safeJson(response: Response): Promise<ApiErrorBody | null> {
  try {
    return (await response.json()) as ApiErrorBody;
  } catch {
    return null;
  }
}

export type OutputFormat = 'auto' | 'png' | 'jpeg' | 'webp';

export interface ResizeOptions {
  outputFormat?: OutputFormat;
  quality?: number;
}

export interface ResizeResponse {
  blob: Blob;
  contentType: string;
}

export async function resizeImage(
  file: File,
  width: number,
  height: number,
  options: ResizeOptions = {},
): Promise<ResizeResponse> {
  const body = new FormData();
  body.append('file', file);
  body.append('width', width.toString());
  body.append('height', height.toString());
  body.append('output_format', options.outputFormat ?? 'auto');
  body.append('quality', (options.quality ?? 90).toString());

  const response = await authedFetch(RESIZE_PATH, { method: 'POST', body });

  if (!response.ok) {
    const errorBody = await safeJson(response);
    const message = errorBody?.message ?? `${response.status} ${response.statusText}`;
    throw new ApiError(response.status, message, errorBody);
  }

  return {
    blob: await response.blob(),
    contentType: response.headers.get('Content-Type') ?? 'application/octet-stream',
  };
}
