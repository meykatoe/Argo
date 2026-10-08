# 共用資源

三個前端（官網、業務後台、運維後台）共用的檔案，各專案透過 Vite 設定取用。

- `public/`：網站圖示與 manifest，三個專案的 `publicDir` 都指向這裡。
- `@brand` 別名指向 `public/`，程式中以 `import logo from '@brand/favicon.svg'` 取得標誌。
