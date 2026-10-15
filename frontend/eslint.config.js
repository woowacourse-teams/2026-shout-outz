import { defineConfig, globalIgnores } from 'eslint/config';
import js from '@eslint/js';
import tseslint from 'typescript-eslint';
import reactHooks from 'eslint-plugin-react-hooks';
import prettierConfig from 'eslint-config-prettier';
import globals from 'globals';

export default defineConfig(
  globalIgnores(['dist', 'node_modules', 'ssg/.build', 'src/routeTree.gen.ts']),
  js.configs.recommended,
  tseslint.configs.recommended,
  reactHooks.configs.flat['recommended-latest'],
  {
    files: ['src/**/*.{ts,tsx}'],
    rules: {
      'no-restricted-imports': [
        'error',
        {
          patterns: [
            {
              regex: '^\\.\\.?/',
              message: "상대 경로 import는 금지합니다. '@/' 절대 경로를 사용하세요.",
            },
          ],
        },
      ],
    },
  },
  {
    // 초기값으로 이후 값을 알 수 없는 useState는 제네릭으로 타입을 적는다.
    // null로 시작하면 다른 값을, 빈 배열로 시작하면 원소를 넣을 수 없게 추론된다.
    files: ['src/**/*.{ts,tsx}'],
    rules: {
      'no-restricted-syntax': [
        'error',
        {
          selector: [
            "CallExpression[callee.name='useState']:not([typeArguments])",
            "CallExpression[callee.property.name='useState']:not([typeArguments])",
          ]
            .flatMap((call) => [
              `${call}:not([arguments.0])`,
              `${call}[arguments.0.type='Literal'][arguments.0.raw='null']`,
              `${call}[arguments.0.type='Identifier'][arguments.0.name='undefined']`,
              `${call}[arguments.0.type='ArrayExpression']:not([arguments.0.elements.0])`,
              `${call}[arguments.0.type='ObjectExpression']:not([arguments.0.properties.0])`,
            ])
            .join(', '),
          message:
            '초기값으로 타입을 알 수 없는 useState는 제네릭을 적으세요. 예: useState<User | null>(null)',
        },
      ],
    },
  },
  {
    // 객체 모양은 interface로 쓴다. `&` 보정은 충돌을 never로 숨기지만 extends는 컴파일 에러로 드러낸다.
    files: ['**/*.{ts,tsx}'],
    rules: {
      '@typescript-eslint/consistent-type-definitions': ['error', 'interface'],
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
  {
    files: ['**/*.test.{ts,tsx}', 'jest.setup.ts'],
    languageOptions: {
      globals: globals.jest,
    },
  },
  prettierConfig,
);
