import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  IconButton,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import { ApiError } from '../api/client';
import { Book, createBook, deleteBook, listBooks } from '../api/books';

interface FormState {
  title: string;
  author: string;
  isbn: string;
}

const emptyForm: FormState = { title: '', author: '', isbn: '' };

export default function BooksPage() {
  const [books, setBooks] = useState<Book[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [form, setForm] = useState<FormState>(emptyForm);
  const [submitting, setSubmitting] = useState(false);

  const reload = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await listBooks();
      setBooks(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unbekannter Fehler');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void reload();
  }, [reload]);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    setFieldErrors({});
    try {
      await createBook({
        title: form.title,
        author: form.author,
        isbn: form.isbn || undefined,
      });
      setForm(emptyForm);
      await reload();
    } catch (err) {
      if (err instanceof ApiError && err.body?.fieldErrors) {
        setFieldErrors(err.body.fieldErrors);
      }
      setError(err instanceof Error ? err.message : 'Unbekannter Fehler');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id: number) => {
    setError(null);
    try {
      await deleteBook(id);
      await reload();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unbekannter Fehler');
    }
  };

  return (
    <>
      <Typography variant="h4" gutterBottom>
        Bücher
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      <Paper sx={{ p: 2, mb: 3 }} component="form" onSubmit={handleSubmit}>
        <Typography variant="h6" gutterBottom>
          Neues Buch anlegen
        </Typography>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
          <TextField
            label="Titel"
            value={form.title}
            onChange={(e) => setForm({ ...form, title: e.target.value })}
            error={Boolean(fieldErrors.title)}
            helperText={fieldErrors.title}
            required
            fullWidth
          />
          <TextField
            label="Autor"
            value={form.author}
            onChange={(e) => setForm({ ...form, author: e.target.value })}
            error={Boolean(fieldErrors.author)}
            helperText={fieldErrors.author}
            required
            fullWidth
          />
          <TextField
            label="ISBN"
            value={form.isbn}
            onChange={(e) => setForm({ ...form, isbn: e.target.value })}
            error={Boolean(fieldErrors.isbn)}
            helperText={fieldErrors.isbn}
            fullWidth
          />
          <Button type="submit" variant="contained" disabled={submitting}>
            Anlegen
          </Button>
        </Stack>
      </Paper>

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
          <CircularProgress />
        </Box>
      ) : (
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>ID</TableCell>
                <TableCell>Titel</TableCell>
                <TableCell>Autor</TableCell>
                <TableCell>ISBN</TableCell>
                <TableCell align="right">Aktion</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {books.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={5} align="center">
                    Keine Bücher vorhanden.
                  </TableCell>
                </TableRow>
              ) : (
                books.map((book) => (
                  <TableRow key={book.id}>
                    <TableCell>{book.id}</TableCell>
                    <TableCell>{book.title}</TableCell>
                    <TableCell>{book.author}</TableCell>
                    <TableCell>{book.isbn ?? '—'}</TableCell>
                    <TableCell align="right">
                      <IconButton
                        aria-label="löschen"
                        onClick={() => void handleDelete(book.id)}
                      >
                        <DeleteIcon />
                      </IconButton>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </>
  );
}
