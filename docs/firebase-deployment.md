# 前端部署教學(Firebase Hosting)

> 前端 Vue 部署到 **Firebase Hosting**,後端 Spring Boot 在 **Cloud Run**,前後端分離。
> 線上:前端 <https://push-app-9c05e.web.app> · 後端 <https://denden-homework-813602138003.asia-southeast1.run.app>

---

## 1. 架構總覽

```
瀏覽器
  │  載入網頁(HTML/JS/CSS)
  ▼
Firebase Hosting(全球 CDN,只放「靜態檔」)
  │  前端 JS 發出 API / WebSocket 請求
  ▼
Cloud Run(Spring Boot 後端) ── Neon PostgreSQL / Gmail SMTP
```

關鍵觀念:**Firebase Hosting 只負責「送出靜態檔案」**(打包好的 HTML/JS/CSS),它**不執行任何後端程式**。所有動態邏輯(註冊、登入、聊天)都是前端 JS 從瀏覽器去呼叫 Cloud Run 後端。

| | Firebase Hosting | Cloud Run |
|---|---|---|
| 放什麼 | 靜態檔(`frontend/dist`) | Java 後端程式 |
| 做什麼 | 透過 CDN 快速送網頁給瀏覽器 | 處理 API / WebSocket / DB |
| 跨網域 | 不同網域 → 需要 CORS(已設定) | `CORS_ALLOWED_ORIGINS` 放行前端網址 |

---

## 2. Firebase Hosting 原理

### 它怎麼運作
1. `npm run build` 把 Vue 專案編譯成**純靜態檔**(`frontend/dist/`:一個 `index.html` + 打包後的 JS/CSS)。
2. `firebase deploy` 把 `dist/` 的檔案上傳到 Google 的 **全球 CDN**。
3. CDN 把檔案快取到世界各地節點,使用者就近下載 → 載入快。
4. 每次 deploy 是**原子性發布(atomic release)**:整包換上去,使用者不會看到部署到一半的破畫面;也保留歷史版本可隨時回滾。

### 設定檔 `firebase.json`(在 repo 根目錄)
```json
{
  "hosting": {
    "public": "frontend/dist",          // 要上傳哪個資料夾
    "ignore": ["firebase.json", "**/.*", "**/node_modules/**"],
    "rewrites": [
      { "source": "**", "destination": "/index.html" }   // SPA 路由:所有路徑都回 index.html
    ]
  }
}
```
- **`public`**:指向打包輸出。
- **`rewrites` 那行很重要**:Vue 是單頁應用(SPA),路由在前端。沒這行的話,直接開 `/some/path` 會 404;有了它,任何路徑都回 `index.html`,讓 Vue 接管路由。

### `.firebaserc`(綁定專案)
`firebase use --add` 產生的,記錄「這個資料夾對應哪個 Firebase 專案」(`push-app-9c05e`)。

### 環境變數怎麼進到前端
- `frontend/.env.production` 裡的 `VITE_API_BASE` / `VITE_WS_URL` 會在 **`npm run build` 時被「寫死」進打包後的 JS**。
- 所以前端的「環境變數」是**建置時**決定的,不是執行時。改了要**重新 build + deploy** 才生效。

---

## 3. 更新前端後,如何重新部署

每次改完前端程式碼:

```bash
cd frontend
npm run build      # 重新打包(自動帶入 .env.production)
cd ..
firebase deploy --only hosting
```

就這兩步。deploy 完幾秒內全球生效(CDN 會更新)。

> 後端(Cloud Run)沒改的話**不用動**。前後端可各自獨立部署 —— 這就是前後端分離的好處。

### 回滾(部署壞了想退回上一版)
```bash
firebase hosting:clone push-app-9c05e:live push-app-9c05e:live   # 或用 Console
```
更簡單:到 [Firebase Console → Hosting](https://console.firebase.google.com) → 發布紀錄 → 對舊版本按「回復(Rollback)」。每次部署都有版本,可一鍵回滾。

---

## 4. CI/CD(自動化部署)

目前是「手動 build + deploy」。要自動化,最常見是 **GitHub Actions**:推到 GitHub 就自動部署。

### 一次性設定
```bash
firebase init hosting:github
```
它會:
- 引導你授權 GitHub repo
- 自動產生 `.github/workflows/*.yml`
- 自動把部署金鑰存進 GitHub Secrets

### 產生的效果
- **推到 `main`** → 自動 build + deploy 到正式站。
- **開 PR** → 自動部署到一個**預覽網址(preview channel)**,像 `https://push-app-9c05e--pr-12-xxxx.web.app`,讓你 merge 前先看效果。

### 範例 workflow(概念)
```yaml
# .github/workflows/firebase-hosting.yml
on:
  push:
    branches: [main]
jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: 20 }
      - run: cd frontend && npm ci && npm run build
      - uses: FirebaseExtended/action-hosting-deploy@v0
        with:
          repoToken: ${{ secrets.GITHUB_TOKEN }}
          firebaseServiceAccount: ${{ secrets.FIREBASE_SERVICE_ACCOUNT }}
          projectId: push-app-9c05e
          channelId: live
```

> 注意:CI 裡 build 也要有 `VITE_API_BASE` / `VITE_WS_URL`。因為 `.env.production` 已進 git,CI build 會自動讀到,不必另設。

---

## 5. 排錯(常見問題)

| 症狀 | 原因 | 解法 |
|---|---|---|
| **打開網頁一片空白** | SPA rewrites 沒設、或 build 路徑錯 | 確認 `firebase.json` 有 `rewrites → /index.html`;確認 `public` 指向 `frontend/dist` |
| **API 被 CORS 擋(Failed to fetch)** | 後端沒放行前端網域 | 後端設 `CORS_ALLOWED_ORIGINS=https://push-app-9c05e.web.app`(已設) |
| **API 打到 Firebase 自己(404)** | 前端用相對路徑 `/api` 但正式環境前後端不同網域 | 確認 `.env.production` 的 `VITE_API_BASE` 指向 Cloud Run,並**重新 build** |
| **WebSocket 連不上** | `VITE_WS_URL` 沒設或用了 `ws://` | 用 `https://...run.app/ws-stomp`(SockJS 用 http/https),**重新 build** |
| **改了東西沒生效** | 前端環境變數是 build 時寫死的;或 CDN/瀏覽器快取 | 重新 `npm run build` + `firebase deploy`;強制重整(Cmd+Shift+R) |
| **deploy 後網址不是 web.app** | site id 不同 | 用實際網址更新後端 CORS |

### 排錯三步驟
1. **開瀏覽器 DevTools → Console / Network**:看是哪個請求失敗、錯誤訊息是什麼(CORS?404?連線?)。
2. **分層確認**:前端載得到嗎(Firebase 200)?後端活著嗎(`curl 後端/v3/api-docs`)?CORS 對嗎(看回應有沒有 `access-control-allow-origin`)?
3. **確認 build 有帶到正確網址**:`grep -r "run.app" frontend/dist/assets/*.js`(應該看到 Cloud Run 網址被打包進去)。

---

## 6. 維護

- **回滾**:Firebase Console → Hosting → Rollback(每次部署留版本)。
- **預覽部署**:`firebase hosting:channel:deploy preview`(不影響正式站,給一個暫時網址測試)。
- **自訂網域**:Firebase Console → Hosting → 新增自訂網域(會引導設 DNS + 自動發 SSL)。設好後記得把新網域也加進後端 `CORS_ALLOWED_ORIGINS`。
- **費用**:Firebase Hosting 免費額度足夠個人/作業(每月 10GB 流量、360MB/日)。Cloud Run 也有免費額度。基本不會扣到錢。
- **監控**:Firebase Console → Hosting 看流量;Cloud Run Console 看後端 log/錯誤。

---

## 速查:完整重新部署(前端)
```bash
cd frontend && npm run build && cd ..
firebase deploy --only hosting
```

## 速查:更新後端 CORS(換網域時)
```bash
gcloud run services update denden-homework --region asia-southeast1 \
  --update-env-vars "^@^CORS_ALLOWED_ORIGINS=https://新網域"
```
