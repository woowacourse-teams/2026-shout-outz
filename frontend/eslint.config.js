import { defineConfig, globalIgnores } from 'eslint/config';
import js from '@eslint/js';
import tseslint from 'typescript-eslint';
import reactHooks from 'eslint-plugin-react-hooks';
import importX from 'eslint-plugin-import-x';
import { createTypeScriptImportResolver } from 'eslint-import-resolver-typescript';
import prettierConfig from 'eslint-config-prettier';
import globals from 'globals';

export default defineConfig(
	globalIgnores(['dist', 'node_modules']),
	js.configs.recommended,
	tseslint.configs.recommended,
	reactHooks.configs.flat['recommended-latest'],
	importX.flatConfigs.recommended,
	importX.flatConfigs.typescript,
	{
		settings: {
			'import-x/resolver-next': [createTypeScriptImportResolver()],
		},
		rules: {
			'import-x/no-named-as-default': 'off',
			'import-x/no-named-as-default-member': 'off',
			'no-restricted-imports': [
				'error',
				{
					patterns: [
						{
							regex: '^\\.\\./',
							message: "상위 폴더로의 상대 경로 import는 금지합니다. '@/' 절대 경로를 사용하세요.",
						},
					],
				},
			],
		},
	},
	{
		files: ['src/**/*.{ts,tsx}'],
		languageOptions: {
			globals: globals.browser,
		},
	},
	{
		files: ['*.config.{js,ts}'],
		languageOptions: {
			globals: globals.node,
		},
	},
	prettierConfig,
);
