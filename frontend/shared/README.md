# 共用資源

三個前端（官網、業務後台、運維後台）共用的檔案。各專案是獨立的 npm 專案，靠 Vite 別名與 TypeScript 路徑取用，沒有另外的建置步驟。

- `public/`：網站圖示與 manifest，三個專案的 `publicDir` 都指向這裡。`@brand` 別名指向它，例如 `import logo from '@brand/favicon.svg'`。
- `src/`：兩個後台共用的程式碼，別名是 `@shared`。
  - `components/`：`StaffApp`（登入狀態）、`LoginForm`、`AppShell`、`AppSidebar`、`PageHost`。
  - `api.ts`：`request`、`ApiError`、`Result`。
  - `utils/`：選單、角色名稱、錯誤文字（`createErrorTexts`）。
  - `config.ts`：`StaffConfig`，兩個後台的差異（名稱、頁面、API、錯誤文字）都由它傳入。
  - `assets/main.css`：後台共用樣式。
- `src/__tests__/`：共用程式碼的測試，由業務後台的 vitest 一併執行。

共用程式碼裡的 `vue`、`vue-router` 由各專案自己的 `node_modules` 解析（`resolve.dedupe` 與 tsconfig `paths`）。共用程式碼不可使用 `@/`，那是各專案自己的 `src`。
