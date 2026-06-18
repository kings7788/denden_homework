# Denden Frontend (Vue 3 + Vite)

前端,與後端分離。後端是同一個 repo 根目錄的 Spring Boot 專案。

## 開發

```bash
npm install
npm run dev      # http://localhost:5173
```
Vite dev server 會把 `/api`、`/ws-stomp` 代理到後端 `http://localhost:8080`,
所以開發時不用處理 CORS。請先啟動後端(`./mvnw spring-boot:run`)。

## 環境變數

| 變數 | 用途 |
|---|---|
| `VITE_WS_URL` | 正式環境的 WebSocket(SockJS)網址,例如 `https://<cloud-run-url>/ws-stomp`。dev 留空即可(走 proxy)。 |

## 建置 / 部署(Firebase Hosting)

```bash
# 1. 建置成靜態檔 -> frontend/dist
VITE_WS_URL=https://<your-cloud-run-url>/ws-stomp npm run build

# 2. 從 repo 根目錄部署(firebase.json 指向 frontend/dist)
cd ..
firebase login          # 第一次
firebase init hosting   # 第一次,選現有 GCP 專案
firebase deploy --only hosting
```

部署後記得在後端 Cloud Run 設定 `CORS_ALLOWED_ORIGINS=https://<firebase-hosting-url>`。

## 結構
```
frontend/
├── index.html
├── vite.config.js        # dev proxy + global polyfill
└── src/
    ├── main.js
    ├── App.vue
    └── components/StompChat.vue   # STOMP 聊天室(SockJS + @stomp/stompjs)
```
