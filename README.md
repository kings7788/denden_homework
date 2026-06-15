# Member System API

會員註冊 / 登入 / 查詢系統 — Spring Boot 後端實作題。

以 Email 為帳號註冊並寄送開通信、登入採「帳密 + Email OTP」兩階段認證、並提供「查詢自己最後登入時間」的受保護 API(非本人無法查詢)。

## 線上 Demo(GCP Cloud Run)

- API base：<https://denden-homework-813602138003.asia-southeast1.run.app>
- Swagger UI：<https://denden-homework-813602138003.asia-southeast1.run.app/swagger-ui/index.html>

資料庫使用 Neon PostgreSQL,Email 經 Gmail SMTP 寄送。

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

## 基本操作

**啟動**
```bash
./mvnw spring-boot:run
```
開啟 Swagger UI:<http://localhost:8080/swagger-ui/index.html>

**使用流程**
1. `POST /api/auth/register` — 用 Email 註冊 → 收開通信
2. `GET /api/auth/verify?token=...` — 點開通信連結啟用帳號
3. `POST /api/auth/login` — 輸入帳密 → 收 Email OTP,取得 `challenge_id`
4. `POST /api/auth/login/verify` — 送 `challenge_id` + `otp` → 取得 `access_token`
5. 在 Swagger 右上 **Authorize** 貼上 `access_token`,即可呼叫
   `GET /api/users/me/last-login` 查詢自己的最後登入時間

JSON 一律使用 snake_case。
