# Homework Plan — denden_homework

> 這份文件是**給後續 AI agent / 開發者重複閱讀的計畫書**。
> 內容涵蓋兩部分作業:Part 1（OCPI 時序圖,已完成)、Part 2(會員系統 RESTful API,進行中)。
> 更新時請維持「決策已鎖定 / 待辦 / 已完成」的標記,讓接手者能快速判斷現況。

---

## 0. 專案背景

- **技術棧**:Spring Boot 3.5.x, Java 17, Maven(用 `./mvnw`)。
- **Package root**:`com.bryant.denden_homework`。
- **資料庫**:本機開發用 H2(in-memory);正式/部署用 PostgreSQL(driver 已在 pom)。
- **既有依賴**:Spring Web / Data JPA / Security / Validation / Mail、springdoc-openapi(Swagger)、Lombok、H2、PostgreSQL。
- 詳細命令與慣例見 `CLAUDE.md`。

---

## Part 1 — OCPI 2.2.1 時序圖(✅ 已完成)

**題目**:以 OCPI 2.2.1 為概念,畫一張從「啟動充電 → 結束充電 → 收到帳單」的時序圖(User → SCSP → EMSP → CPO),標註主要 API 呼叫與回應,可忽略 token 認證。

**產出**:
- `docs/ocpi-charging-sequence.md` — mermaid 時序圖 + 需求對照表 + 設計重點。

**已通過「超級 PM」審查**:Pass-with-fixes,7 個必備事件全覆蓋。已修正 CDR 章節引用(§8 → §10)。

**核心觀念(接手者必懂)**:
1. **協定分層**:`SCSP↔EMSP` = eMSP 自家 App API(非 OCPI);`EMSP↔CPO` = OCPI;`CPO↔充電樁` = OCPP。
2. **Commands 非同步雙回應(§13)**:CPO 先同步回 `CommandResponse{ACCEPTED}`(只代表受理),稍後再非同步 `POST` `CommandResult` 到 eMSP 的 `response_url`;eMSP 對此只回空的 ack(`status_code 1000`)。
3. **Sessions(§9)/ CDRs(§10)由 CPO 主動推**:Session 用 `PUT /sessions/{country_code}/{party_id}/{id}`(PENDING→ACTIVE→COMPLETED);CDR 用 `POST /cdrs`(回 201 + Location),CDR 不可變更。
4. OCPI 回應信封(§4.1.7):`{data, status_code, status_message, timestamp}`,`1000` = 成功。

> 註:先前有一套示範用的 OCPI **eMSP 接收端**程式碼(SessionController/CdrController/Session/Cdr/OcpiResponse + 測試),屬 Part 1 延伸練習,**已於 2026-06-15 全數移除**,讓本 repo 聚焦在 Part 2 會員系統。Part 1 的時序圖文件 `docs/ocpi-charging-sequence.md` 仍保留(那是 Part 1 的交付物)。

---

## Part 2 — 會員系統 RESTful API(🚧 進行中)

### 題目需求
- 註冊以 **Email 為帳號**,需寄**開通信**作為開通帳號的必要步驟。
- 登入用**帳號+密碼**,並加入 **Email 兩階段認證(2FA)**。
- 提供「查詢自己帳號**最後登入時間**」的 API(**非本人無法查詢**)。
- Email 寄送用 **Mailjet**(免費額度)。
- 部署到雲端免費額度(**GCP Cloud Run**)。
- 提供 **Swagger 或 Postman** 測試文件。
- 提供 **GitHub 連結**。

### 已鎖定的技術決策
| 項目 | 決策 | 理由 |
|---|---|---|
| 認證機制 | **JWT(無狀態)** | Cloud Run 會縮到 0、可能多實例,無狀態最適合 |
| 資料庫(雲端) | **Neon(免費託管 Postgres)** | Cloud Run 無狀態,H2 記憶體會掉資料 |
| Email 服務 | **Mailjet API** | 題目指定,免費額度可真寄 |
| 部署平台 | **GCP Cloud Run**(容器化) | 免費額度大、縮到 0、有官方 Cloud Run MCP 可協助部署 |
| API 文件 | **springdoc Swagger UI** | 依賴已在 pom |
| 密鑰管理 | 全走**環境變數**(本機 `application-dev.properties` 不進 git;雲端用 Cloud Run env var) | 不可寫死金鑰 |

### 目標架構
```
[使用者] → Cloud Run (Spring Boot + JWT) → Neon (PostgreSQL)
                    │
                    └→ Mailjet API (開通信 / 登入 OTP)
```

### API 設計
```
# 註冊 + Email 開通
POST /api/auth/register        {email, password}
     → 建立帳號 status=PENDING、產生 activation token、寄開通信
GET  /api/auth/verify?token=xxx
     → 驗證 token → 帳號轉 ACTIVE

# 登入 + Email 兩階段認證
POST /api/auth/login           {email, password}
     → 驗證帳密 + 帳號已開通 → 產生 OTP 寄 Email → 回 {challengeId}
POST /api/auth/login/verify    {challengeId, otp}
     → 驗證 OTP → 發 JWT → 記錄 lastLoginAt

# 查詢最後登入時間(僅本人)
GET  /api/users/me/last-login  (需帶 JWT)
     → 從 JWT 解出身分,只回「自己」的 lastLoginAt
```

> 🔒 **「非本人無法查詢」的實作關鍵**:此 API **不收任何 userId 參數**,身分一律從 JWT 解出,天生無法查到別人。(面試考點:用 token 身分而非 URL 參數防越權。)

### 資料模型
```
User:
  id, email (unique), password_hash,
  status (PENDING / ACTIVE),
  last_login_at, created_at

VerificationToken:
  id, user_id, token,
  type (ACTIVATION / LOGIN_OTP),
  expires_at, used (boolean)
```

### 安全 / 流程要點(接手者必做)
- 密碼用 **BCrypt** 雜湊儲存(專案已有 `BCryptPasswordEncoder` bean)。
- activation token 與 login OTP 都要有**過期時間**與**用過即失效(used)**。
- 未開通(PENDING)帳號**不可登入**。
- Security 設定:`/api/auth/**` 與 Swagger 路徑**公開**;`/api/users/**` 需 **JWT 驗證**。
- ⚠️ **既有衝突處理**:目前 `entity/User`、`config/DataSeeder`、`config/SecurityConfig` 是 Part 1 練習用的(權限全 permitAll、seeder 塞假會員)。Part 2 開始時**需先重構**:會員模型/權限規則要改成本題需求,避免兩套打架。

### 實作里程碑(依序進行)
1. **資料模型 + 註冊 API**(本機,純邏輯,不寄信)← **下一步,不需任何金鑰**
2. **接 Mailjet**:寄開通信 → 完成 `verify` 開通流程
3. **登入兩階段**:帳密 → 寄 OTP → 驗證 → 發 JWT → 記錄 `lastLoginAt`
4. **最後登入時間查詢 API**(JWT 防越權)
5. **Swagger 文件**整理
6. **容器化(Dockerfile)+ 部署 Cloud Run + 接 Neon**
7. **push GitHub + 寫 README**

### 待使用者提供的帳號 / 金鑰(只有使用者能申請)
| # | 申請什麼 | 拿到什麼 | 需要時機 |
|---|---|---|---|
| 1 | Mailjet 帳號 | API Key + Secret Key + 驗證寄件人 email | 里程碑 2 |
| 2 | Neon 帳號 | Postgres 連線字串 | 里程碑 6(或更早接) |
| 3 | GCP 帳號 | 啟用 Cloud Run、建專案 | 里程碑 6 |
| 4 | GitHub repo | 空 repo URL | 里程碑 7 |

---

## 雲端 MCP server 現況(參考)
- **GCP**:官方 **Cloud Run MCP**(`GoogleCloudPlatform/cloud-run-mcp`)可直接從對話部署 app 到 Cloud Run。本題選 GCP 部分原因即此。
- **AWS**:**AWS MCP Server**(2026/5 GA)+ awslabs 套件,偏管理/查文件。
- MCP 非部署必要條件,沒有它也能用 `gcloud` / `aws` CLI 部署。

---

## 目前進度快照
- [x] Part 1:OCPI 時序圖完成並通過審查(文件 docs/ocpi-charging-sequence.md)
- [x] Part 1 延伸 OCPI 接收端程式碼:**已移除**(與會員系統無關)
- [x] Part 2 里程碑 1:資料模型 + 註冊/開通 API(本機,7 測試通過)
- [x] Part 2 里程碑 2:接 email 寄開通信 —— **完成,Gmail SMTP 真寄成功(HTTP 201)**
- [x] Part 2 里程碑 3:登入兩階段 + JWT + 最後登入時間查詢(13 測試通過)
- [x] Part 2 里程碑 4:Swagger 文件(OpenAPI metadata + Bearer JWT 授權 + 端點註解,已驗證)
- [x] Part 2 里程碑 5:部署 —— **已部署到 Cloud Run,線上 API 運作正常**
  - URL: https://denden-homework-813602138003.asia-southeast1.run.app(Swagger: /swagger-ui/index.html)
  - GCP 專案 push-app-9c05e、region asia-southeast1、service denden-homework
  - 環境變數:SPRING_PROFILES_ACTIVE=prod + DB_*/JWT_SECRET/EMAIL_PROVIDER=gmail/MAIL_*/APP_BASE_URL
  - 踩雷:新專案需授予 compute 預設 SA `roles/cloudbuild.builds.builder` 才能用 --source 建置
- [x] Part 2 里程碑 6:push GitHub + README —— https://github.com/kings7788/denden_homework (public)

### 里程碑 5 現況(資料庫)
- `application-prod.properties`(已 commit,**全用環境變數,無密碼**):Postgres datasource + 關閉 H2 console + email/JWT 走 env。
- profile 分工:`default`=H2+log(測試)/ `dev`=H2+gmail(本機真寄)/ `prod`=Neon Postgres+env。
- Neon 實測:HikariPool 連線成功、Hibernate 自動建表、註冊寫入後跨重啟仍在(409 證實)。
- 部署 Cloud Run 需設定的環境變數:`DB_URL`(jdbc:postgresql://<neon-host>/neondb?sslmode=require)、`DB_USERNAME`、`DB_PASSWORD`、`JWT_SECRET`、`EMAIL_PROVIDER=gmail`、`MAIL_USERNAME`、`MAIL_PASSWORD`、`MAIL_FROM`、`APP_BASE_URL`。
- ⚠️ Neon 連線字串的 `channel_binding=require` 在 JDBC 用 `sslmode=require` 即可(已驗證可連)。
- 📌 測試時在 Neon 留了一筆 `neon-test@example.com`,如需乾淨 DB 可自行刪除。

### 里程碑 3 已建立 / 變更(接手者參考)
- 依賴:`pom.xml` 加 jjwt 0.12.6(api/impl/jackson)。
- `security/JwtService`(HS256,subject=email)、`security/JwtAuthFilter`(讀 Bearer → 設 SecurityContext)。
- `SecurityConfig` 重寫:無狀態、CSRF 關閉、`/api/auth/**`+swagger+h2+ocpi 公開、`/api/users/**` 需 JWT、未驗證回 401、掛上 JwtAuthFilter。
- `entity/VerificationToken` 加 `code` 欄位(放 LOGIN_OTP 的 6 碼)。
- `AuthService` 加 `login`(發 6 碼 OTP、回 challengeId)/ `verifyLogin`(驗 OTP → 設 lastLoginAt)。
- DTO:`LoginRequest`、`LoginVerifyRequest`。
- `AuthController` 加 `POST /api/auth/login`、`POST /api/auth/login/verify`(回 JWT)。
- `controller/UserController`:`GET /api/users/me/last-login`(身分取自 JWT,天生防越權)。
- `application.properties` 加 `app.jwt.secret`(可用 env `JWT_SECRET` 覆寫)/ `app.jwt.expiration-ms`。
- 測試:`LoginFlowTest`(6 案例)。
- ⚠️ **JSON 全用 snake_case**(全域 Jackson 設定):登入第二階段請求欄位是 `challenge_id`(非 challengeId);回應為 `access_token` / `token_type` / `last_login_at`。

### Part 2 完整 API(目前狀態)
```
POST /api/auth/register       {email,password}                -> 201 PENDING + 寄開通信
GET  /api/auth/verify?token=  ...                             -> 200 ACTIVE
POST /api/auth/login          {email,password}                -> 200 {challenge_id} + 寄 OTP
POST /api/auth/login/verify   {challenge_id,otp}              -> 200 {access_token, token_type, last_login_at}
GET  /api/users/me/last-login (Authorization: Bearer <jwt>)   -> 200 {email, last_login_at}
```

### 里程碑 2 現況(email)
- 供應商可切換,用 `app.email.provider` 選:`log`(預設,只印 log)/ `gmail`(已驗證可用)/ `mailjet`(備用)。
- 三個實作:`LogEmailService`、`GmailSmtpEmailService`(JavaMailSender + smtp.gmail.com:587)、`MailjetEmailService`(Send API v3.1)。各自 `@ConditionalOnProperty` 切換。
- 金鑰放 **git-ignored 的 `src/main/resources/application-dev.properties`**;用 `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` 啟動。
- ⚠️ **Mailjet 新帳號被封鎖**(mj-0001,需開客服工單)→ 暫時改用 Gmail SMTP。程式碼都在,Mailjet 解封後改 `app.email.provider=mailjet` 即可。
- ⚠️ **寄信用的 Gmail 帳號是 `kobe86170@gmail.com`**(用 App Password),非 CLAUDE.md context 提到的 kings86170。GCP 封 port 25 但允許 587,故 Gmail SMTP 部署到 Cloud Run 也可用。

### 里程碑 1 已建立的檔案(接手者參考)
- `entity/User`(重構為會員:email/password/status/lastLoginAt/createdAt)、`UserStatus`、`VerificationToken`、`TokenType`
- `repository/UserRepository`(findByEmail/existsByEmail)、`VerificationTokenRepository`
- `dto/RegisterRequest`(@Email + 密碼長度驗證)
- `service/AuthService`(register/verify)、`service/EmailService` + `LogEmailService`(里程碑 2 換成 Mailjet)
- `controller/AuthController`(`POST /api/auth/register`、`GET /api/auth/verify`)
- `exception/GlobalExceptionHandler`(409/400/驗證錯誤)
- `SecurityConfig`:CSRF 已對 `/api/**` 關閉(REST API)
- 測試:`AuthRegistrationTest`(5 案例)
- ⚠️ 已移除 Part 1 的 `config/DataSeeder`(與會員模型衝突)
- 📌 里程碑 2 換 email 實作:新增一個 **bean 名為 `mailjetEmailService`** 的 `EmailService`,`LogEmailService` 會自動退讓(`@ConditionalOnMissingBean`)
