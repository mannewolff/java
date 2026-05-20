import { api } from './client';

export interface Book {
  id: number;
  title: string;
  author: string;
  isbn: string | null;
}

export interface BookRequest {
  title: string;
  author: string;
  isbn?: string;
}

export const listBooks = () => api.get<Book[]>('/books');
export const getBook = (id: number) => api.get<Book>(`/books/${id}`);
export const createBook = (req: BookRequest) => api.post<Book>('/books', req);
export const updateBook = (id: number, req: BookRequest) =>
  api.put<Book>(`/books/${id}`, req);
export const deleteBook = (id: number) => api.del(`/books/${id}`);
