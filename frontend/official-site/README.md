# official-site

Argo 官網前端（Vue 3 + Vite + TypeScript）。

```
npm install
npm run dev        # 開發，預設 http://localhost:5173
npm run test:unit  # 單元測試
npm run build      # 型別檢查並打包
```

開發時 `/api` 會轉發到 `http://localhost:8080`，請先啟動後端。
正式環境可用 `VITE_API_BASE` 指定後端位址。
