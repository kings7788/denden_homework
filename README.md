# Member System API

會員註冊 / 登入 / 查詢系統 — Spring Boot 後端實作題。

## 線上 Demo(GCP Cloud Run)

- API base：<https://denden-homework-813602138003.asia-southeast1.run.app>
- Swagger UI：<https://denden-homework-813602138003.asia-southeast1.run.app/swagger-ui/index.html>

資料庫使用 Neon PostgreSQL,Email 經 Gmail SMTP 寄送。

以 Email 為帳號註冊並寄送開通信、登入採「帳密 + Email OTP」兩階段認證、並提供「查詢自己最後登入時間」的受保護 API(非本人無法查詢)。

## 功能對應需求

| 需求 | 實作 |
|---|---|
| 註冊以 Email 為帳號,寄開通信 | `POST /api/auth/register` → 建立 PENDING 帳號 + 寄開通信;`GET /api/auth/verify` 開通 |
| 登入帳密 + Email 兩階段認證 | `POST /api/auth/login`(驗帳密 → 寄 OTP) + `POST /api/auth/login/verify`(驗 OTP → 發 JWT) |
| 查自己最後登入時間,非本人不可查 | `GET /api/users/me/last-login`(身分取自 JWT,結構上無法查他人) |
| Email 寄送服務 | Gmail SMTP(可切換 Mailjet);介面抽象 `EmailService` |
| Swagger / Postman 測試文件 | Swagger UI `/swagger-ui/index.html`,OpenAPI `/v3/api-docs` |

## 技術棧

Java 17 · Spring Boot 3.5 · Spring Security + JWT(jjwt)· Spring Data JPA · H2(開發)/ PostgreSQL(部署)· springdoc-openapi · Lombok · Maven

## API 一覽

```
POST /api/auth/register       {email, password}            -> 201 建立帳號(PENDING)+ 寄開通信
GET  /api/auth/verify?token=  ...                           -> 200 帳號開通(ACTIVE)
POST /api/auth/login          {email, password}            -> 200 {challenge_id} + 寄 Email OTP
POST /api/auth/login/verify   {challenge_id, otp}          -> 200 {access_token, token_type, last_login_at}
GET  /api/users/me/last-login (Authorization: Bearer JWT)  -> 200 {email, last_login_at}
```

JSON 一律使用 snake_case。

## 設定檔 / Profile

| Profile | 資料庫 | Email | 用途 |
|---|---|---|---|
| `default` | H2 記憶體 | log(只印 log) | 跑測試、快速啟動 |
| `dev` | H2 記憶體 | Gmail SMTP(真寄) | 本機完整測試 |
| `prod` | PostgreSQL | 依環境變數 | 雲端部署(Cloud Run + Neon) |

機密設定（Email 金鑰、JWT secret、DB 連線）一律走環境變數或 git-ignored 的 `application-dev.properties`，**不進版控**。

## 本機執行

```bash
# 1. 純本機(email 只印 log,不需任何金鑰)
./mvnw spring-boot:run

# 2. 完整流程(真的寄開通信 / OTP)— 需先建立 src/main/resources/application-dev.properties
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

`application-dev.properties` 範例(此檔已被 .gitignore 忽略):
```properties
app.email.provider=gmail
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=YOUR_GMAIL@gmail.com
spring.mail.password=YOUR_GMAIL_APP_PASSWORD
app.email.from=YOUR_GMAIL@gmail.com
```

開啟 Swagger UI:<http://localhost:8080/swagger-ui/index.html>

## 測試

```bash
./mvnw test
```

涵蓋註冊/開通、重複註冊、輸入驗證、兩階段登入、JWT 授權、越權防護等。

## 部署(GCP Cloud Run + Neon PostgreSQL)

用 `prod` profile,並設定環境變數:

```
DB_URL=jdbc:postgresql://<neon-host>/neondb?sslmode=require
DB_USERNAME=...   DB_PASSWORD=...
JWT_SECRET=<長隨機字串>
EMAIL_PROVIDER=gmail
MAIL_USERNAME=...  MAIL_PASSWORD=<App Password>  MAIL_FROM=...
APP_BASE_URL=https://<your-cloud-run-url>
```
