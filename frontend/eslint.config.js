import js from '@eslint/js';
import tsEslint from 'typescript-eslint';
import reactHooks from 'eslint-plugin-react-hooks';
import jsxA11y from 'eslint-plugin-jsx-a11y';

export default tsEslint.config(
  js.configs.recommended,
  ...tsEslint.configs.recommended,
  {
    files: ['src/**/*.{ts,tsx}'],
    plugins: {
      'react-hooks': reactHooks,
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      '@typescript-eslint/no-explicit-any': 'error',
      // Experimentelle Regel in react-hooks v7: ablehnen von useEffect(() => { void load(); }).
      // Das ist das Standard-Datenfetch-Pattern in diesem Projekt.
      'react-hooks/set-state-in-effect': 'off',
    },
  },
  {
    files: ['src/**/*.tsx'],
    plugins: {
      'jsx-a11y': jsxA11y,
    },
    rules: {
      ...jsxA11y.configs.recommended.rules,
      // autoFocus auf Dialog-Inputs ist ARIA-konform; pauschales Verbot ist kontraproduktiv.
      'jsx-a11y/no-autofocus': 'off',
    },
  },
  {
    ignores: ['dist/**', 'node_modules/**'],
  },
);
