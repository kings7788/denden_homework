# WebSocket 練習題目

> 一份循序漸進的 WebSocket 練習清單(Spring Boot)。每題標注「學到的核心概念」與難度,挑一個開始即可。

## 背景:WebSocket 是什麼

HTTP 是「請求–回應」單向開合;WebSocket 在一次握手(handshake)後建立**持久的雙向連線**,伺服器可以**主動推**訊息給前端,適合即時場景(聊天、通知、即時資料)。

Spring Boot 兩種做法:
- **原生 `WebSocketHandler`**:低階,自己處理訊息字串。適合簡單場景。
- **STOMP + SockJS**:高階,有「訂閱 / 主題(topic)」概念,像訊息佇列。適合多房間 / 通知。

| | 原生 WebSocketHandler | STOMP + SockJS |
|---|---|---|
| 風格 | 低階,手動處理訊息 | 高階,訂閱/主題 |
| 適合 | Echo、簡單聊天 | 多房間、通知推播 |
| 前端 | 瀏覽器原生 `WebSocket` | `stomp.js` + `sockjs` |

---

## 練習題目(由淺到深)

### 1. Echo 伺服器 ⭐
- **做什麼**:前端送什麼,伺服器原樣回傳。
- **學到**:連線生命週期(`afterConnectionEstablished` / `handleTextMessage` / `afterConnectionClosed`)、握手流程。
- **驗收**:用瀏覽器原生 `WebSocket` 或 `wscat` 連上,送 "hi" 收到 "hi"。

### 2. 即時聊天室 ⭐⭐
- **做什麼**:所有連線者共用一個房間,任何人發言會**廣播**給全部人。
- **學到**:廣播(broadcast)、維護連線清單(`Set<WebSocketSession>`)、加入/離開通知、線上人數。
- **延伸**:暱稱、訊息時間戳。

### 3. 多房間聊天(像 Discord 頻道) ⭐⭐⭐
- **做什麼**:可建立/加入不同房間,訊息只送到該房間。
- **學到**:STOMP 的 `topic` 訂閱機制、`@MessageMapping` / `@SendTo`、依房間分群發送。
- **延伸**:房間成員列表、私訊。

### 4. 即時通知推播(可接現有會員系統)⭐⭐⭐
- **做什麼**:登入後,伺服器在事件發生時**主動推播給「特定使用者」**(非廣播)。
- **學到**:後端主動推送、`convertAndSendToUser`、**用 JWT 驗證 WebSocket 連線**(握手時驗 token)。
- **為什麼推薦**:能把 WebSocket 整進這個專案的 JWT 認證,面試很加分。

### 5. 協作白板 / 共筆編輯器 ⭐⭐⭐⭐
- **做什麼**:多人同時畫圖 / 編輯,變動即時同步。
- **學到**:高頻訊息處理、狀態同步、衝突處理、訊息節流(throttle / debounce)。

### 6. 即時股價 / 儀表板 ⭐⭐⭐
- **做什麼**:伺服器定時把(模擬)資料推給訂閱者。
- **學到**:伺服器定時推送(`@Scheduled` + 推播)、依 symbol 訂閱、**斷線重連**。

---

## 建議練習路徑

1. **#1 Echo** → 打通連線生命週期(基礎)
2. **#2 聊天室** → 廣播 + 連線管理
3. **#4 通知推播** → 主動推 + 針對特定使用者 + 接 JWT(整進真實系統)

打穩 1、2 的基礎後,#4 最有「實戰感」。#3、#5、#6 視興趣再挑。

---

## 技術備忘

- 依賴:`spring-boot-starter-websocket`
- 設定:`@EnableWebSocket`(原生)或 `@EnableWebSocketMessageBroker`(STOMP)
- 測試工具:瀏覽器 DevTools Console 的 `new WebSocket(...)`、`wscat`、或簡單的測試 HTML 頁
- 驗證重點:連線建立、訊息收發、斷線處理、(進階)認證與重連
