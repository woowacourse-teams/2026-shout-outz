# 🚀 Welcome to your new awesome project!

This project has been created using **create-webpack-app**.

Switch to the Node version pinned in `.nvmrc`:

```bash
nvm use
```

Install dependencies with the lockfile:

```bash
npm ci
```

Start the dev server:

```bash
npm run dev
```

Bundle your application:

```bash
npm run build
```

## Scripts

| Script                            | Description                  |
| --------------------------------- | ---------------------------- |
| `npm run dev`                     | Start the webpack dev server |
| `npm run build`                   | Production bundle            |
| `npm run build:dev`               | Development bundle           |
| `npm run watch`                   | Rebuild on file changes      |
| `npm run lint` / `lint:fix`       | Run ESLint                   |
| `npm run format` / `format:check` | Run Prettier                 |

> `engine-strict=true` is set, so `npm ci` fails outright on the wrong Node version.
> Run `nvm install` once if you do not have the pinned version yet.

> This project uses **npm**. `yarn.lock` and `pnpm-lock.yaml` are git-ignored — do not use other package managers.
