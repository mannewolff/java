import { useEffect, useRef, useState } from 'react';
import type { ChangeEvent } from 'react';
import {
  Box,
  Button,
  CircularProgress,
  Paper,
  Stack,
  Tab,
  Tabs,
  TextField,
  Typography,
} from '@mui/material';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import DownloadIcon from '@mui/icons-material/Download';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { oneLight } from 'react-syntax-highlighter/dist/esm/styles/prism';
import type { Components } from 'react-markdown';

import { ApiError } from '../../api/client';
import { convertMarkdownToPdf } from '../../api/markdownToPdf';
import { useNotify } from '../../notify/NotifyProvider';

const MAX_CHARS = 1_000_000;

type ActiveTab = 'preview' | 'pdf';

function errorMessage(err: unknown): string {
  if (err instanceof ApiError) return err.message;
  if (err instanceof Error) return err.message;
  return 'Unbekannter Fehler';
}

const markdownComponents: Components = {
  code({ className, children, ...props }) {
    const match = /language-(\w+)/.exec(className ?? '');
    const isInline = !match;
    if (isInline) {
      return (
        <Box
          component="code"
          sx={{
            backgroundColor: 'action.hover',
            borderRadius: 0.5,
            px: 0.5,
            fontFamily: 'monospace',
            fontSize: '0.875em',
          }}
          {...(props as object)}
        >
          {children}
        </Box>
      );
    }
    return (
      <SyntaxHighlighter
        style={oneLight}
        language={match[1]}
        PreTag="div"
      >
        {String(children).replace(/\n$/, '')}
      </SyntaxHighlighter>
    );
  },
};

export default function MarkdownToPdfPage(): JSX.Element {
  const [markdown, setMarkdown] = useState('');
  const [pdfUrl, setPdfUrl] = useState<string | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);
  const [activeTab, setActiveTab] = useState<ActiveTab>('preview');
  const notify = useNotify();
  const inputRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    return () => {
      if (pdfUrl) URL.revokeObjectURL(pdfUrl);
    };
  }, [pdfUrl]);

  const handleFile = (event: ChangeEvent<HTMLInputElement>): void => {
    const file = event.target.files?.[0];
    if (!file) return;
    if (file.size > MAX_CHARS) {
      notify.error('Datei zu groß (max 1 MB).');
      return;
    }
    const reader = new FileReader();
    reader.onload = () => setMarkdown(String(reader.result ?? ''));
    reader.onerror = () => notify.error('Datei konnte nicht gelesen werden.');
    reader.readAsText(file);
    event.target.value = '';
  };

  const handleConvert = async (): Promise<void> => {
    if (markdown.trim() === '') return;
    setIsProcessing(true);
    try {
      const blob = await convertMarkdownToPdf(markdown);
      if (pdfUrl) URL.revokeObjectURL(pdfUrl);
      setPdfUrl(URL.createObjectURL(blob));
      setActiveTab('pdf');
    } catch (err) {
      notify.error(errorMessage(err));
    } finally {
      setIsProcessing(false);
    }
  };

  const canConvert = markdown.trim() !== '' && !isProcessing;

  return (
    <>
      <Typography variant="h4" sx={{ mb: 1 }}>
        Markdown zu PDF
      </Typography>
      <Typography color="text.secondary" sx={{ mb: 3 }}>
        Markdown-Datei hochladen oder Text direkt eingeben, dann ein PDF erzeugen, in der Vorschau
        ansehen und herunterladen.
      </Typography>

      <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} alignItems="stretch">
        <Paper sx={{ p: 2, flex: 1, display: 'flex', flexDirection: 'column' }}>
          <Stack direction="row" spacing={2} alignItems="center" sx={{ mb: 2 }}>
            <Button
              variant="outlined"
              startIcon={<CloudUploadIcon />}
              onClick={() => inputRef.current?.click()}
            >
              Markdown-Datei
            </Button>
            <input
              ref={inputRef}
              type="file"
              accept=".md,.markdown,text/markdown,text/plain"
              onChange={handleFile}
              hidden
              aria-label="Markdown-Datei auswählen"
            />
            <Button
              variant="contained"
              onClick={handleConvert}
              disabled={!canConvert}
              startIcon={
                isProcessing ? (
                  <CircularProgress size={16} color="inherit" />
                ) : (
                  <PictureAsPdfIcon />
                )
              }
            >
              PDF erzeugen
            </Button>
            <Typography variant="caption" color="text.secondary">
              .md / .markdown, max 1 MB
            </Typography>
          </Stack>
          <TextField
            label="Markdown"
            value={markdown}
            onChange={(e) => setMarkdown(e.target.value)}
            multiline
            minRows={16}
            fullWidth
            placeholder={'# Überschrift\n\nText, **fett**, Listen, Tabellen …'}
            inputProps={{ 'aria-label': 'Markdown', maxLength: MAX_CHARS }}
            sx={{ flex: 1 }}
          />
          {pdfUrl && (
            <Stack direction="row" spacing={2} sx={{ mt: 2 }}>
              <Button
                variant="outlined"
                startIcon={<DownloadIcon />}
                component="a"
                href={pdfUrl}
                download="document.pdf"
              >
                PDF herunterladen
              </Button>
            </Stack>
          )}
        </Paper>

        <Paper sx={{ p: 2, flex: 1, display: 'flex', flexDirection: 'column', minHeight: 480 }}>
          <Tabs
            value={activeTab}
            onChange={(_, val: ActiveTab) => setActiveTab(val)}
            sx={{ mb: 2, borderBottom: 1, borderColor: 'divider' }}
          >
            <Tab label="Vorschau" value="preview" />
            <Tab label="PDF" value="pdf" disabled={pdfUrl === null} />
          </Tabs>

          {activeTab === 'preview' && (
            <Box
              sx={{
                flex: 1,
                overflow: 'auto',
                '& h1, & h2, & h3, & h4, & h5, & h6': { mt: 2, mb: 1, fontWeight: 'bold' },
                '& h1': { fontSize: '2em' },
                '& h2': { fontSize: '1.5em' },
                '& h3': { fontSize: '1.25em' },
                '& p': { mb: 1 },
                '& ul, & ol': { pl: 3, mb: 1 },
                '& table': { borderCollapse: 'collapse', width: '100%', mb: 1 },
                '& th, & td': { border: 1, borderColor: 'divider', p: 0.75 },
                '& th': { backgroundColor: 'action.hover', fontWeight: 'bold' },
                '& blockquote': {
                  borderLeft: 4,
                  borderColor: 'primary.light',
                  pl: 2,
                  color: 'text.secondary',
                  my: 1,
                },
              }}
            >
              {markdown.trim() === '' ? (
                <Box
                  sx={{
                    height: '100%',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: 'text.secondary',
                    textAlign: 'center',
                  }}
                >
                  <Typography>Markdown links eingeben, hier erscheint die Vorschau.</Typography>
                </Box>
              ) : (
                <ReactMarkdown remarkPlugins={[remarkGfm]} components={markdownComponents}>
                  {markdown}
                </ReactMarkdown>
              )}
            </Box>
          )}

          {activeTab === 'pdf' && (
            <>
              {pdfUrl ? (
                <Box
                  component="iframe"
                  title="PDF-Vorschau"
                  src={pdfUrl}
                  sx={{
                    flex: 1,
                    width: '100%',
                    border: 1,
                    borderColor: 'divider',
                    borderRadius: 1,
                  }}
                />
              ) : (
                <Box
                  sx={{
                    flex: 1,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: 'text.secondary',
                    textAlign: 'center',
                  }}
                >
                  <Typography>Noch kein PDF. Links Markdown eingeben und „PDF erzeugen".</Typography>
                </Box>
              )}
            </>
          )}
        </Paper>
      </Stack>
    </>
  );
}
