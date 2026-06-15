# 從 Swagger 到部署上 GCP — 完整過程與心路歷程

> 這份文件記錄「會員系統」怎麼一步步做到:① 設定 Swagger ② 寫 Dockerfile ③ 部署到 GCP Cloud Run。
> 每段都附「**做了什麼 / 為什麼這樣做 / 遇到什麼錯 / 怎麼解**」,給你理解背後的決策,而不只是抄指令。

---

## Part 1 — 設定 Swagger(API 文件)

### 做了什麼
1. **依賴早就有了**:`pom.xml` 裡的 `springdoc-openapi-starter-webmvc-ui`。它會自動掃描你的 `@RestController`,產生兩個東西:
   - `/v3/api-docs`(機器讀的 OpenAPI JSON 規格)
   - `/swagger-ui/index.html`(人看的互動式網頁)
2. **讓 Security 放行這些路徑**(否則被擋會看不到):在 `SecurityConfig` 的 `permitAll` 加入 `/swagger-ui/**`、`/swagger-ui.html`、`/v3/api-docs/**`。
3. **建立 `OpenApiConfig`**:設定文件標題、說明,以及**最關鍵的 Bearer JWT 授權機制**:
   ```java
   .components(new Components().addSecuritySchemes("bearerAuth",
       new SecurityScheme().type(HTTP).scheme("bearer").bearerFormat("JWT")));
   ```
4. **在 Controller 加註解**讓文件更清楚:
   - `@Tag`(分組)、`@Operation`(每支 API 的說明)
   - 受保護的端點加 `@SecurityRequirement(name = "bearerAuth")` → Swagger 上會出現「🔒」並能帶 token
5. **DTO 加 `@Schema(example = "...")`** → Swagger「Try it out」會自動預填範例值。

### 為什麼這樣做
- **為什麼用 springdoc 而不是手寫文件**:它從程式碼自動生成,程式改了文件就跟著改,不會過時。
- **為什麼要特別設 `bearerAuth`**:預設 Swagger 不知道你的 API 需要 JWT。設了之後,右上角會出現「Authorize」按鈕,貼上 token 就能直接測試受保護的 `/api/users/**`,不用自己組 header。

### 怎麼驗證成功
```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/v3/api-docs        # 期望 200
curl -s http://localhost:8080/v3/api-docs | grep -o '"title":"[^"]*"'           # 看到標題
```
結果:5 個端點都收錄、`bearerAuth` 安全機制出現在規格裡。

### 一個容易忽略的細節
專案全域用 **snake_case**(`spring.jackson.property-naming-strategy=SNAKE_CASE`),所以 DTO 欄位 `challengeId` 在 JSON / Swagger 裡會顯示成 `challenge_id`。springdoc 會自動跟著 Jackson 的命名策略走,所以文件和實際請求是一致的。

---

## Part 2 — 寫 Dockerfile(把 app 包成容器)

### 最終的 Dockerfile(多階段建置)
```dockerfile
# --- 建置階段:用 Maven 編譯出 jar ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -e -B dependency:go-offline   # 先抓依賴(可被快取,加速重建)
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# --- 執行階段:只用輕量 JRE 跑 jar ---
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT} -jar app.jar"]
```

### 每個決策的理由
| 決策 | 為什麼 |
|---|---|
| **多階段(build + runtime 分開)** | 建置階段有整套 Maven + JDK(很肥);最終映像只留 JRE + jar,**體積小、啟動快、攻擊面小** |
| **用 `mvn` 而不是 `./mvnw`** | 專案的 `.mvn/wrapper/maven-wrapper.jar` 被 `.gitignore` 忽略了,雲端建置時不會上傳這個 jar,`./mvnw` 會失敗。直接用 maven 官方映像的 `mvn` 最穩 |
| **先 `dependency:go-offline` 再 copy src** | Docker 分層快取:只要 `pom.xml` 沒變,依賴層就重用,改 code 重建時不用重抓依賴 |
| **`-DskipTests`** | 映像建置階段不應跑測試(測試需要資料庫等環境);測試在 CI / 本機跑 |
| **`COPY target/*.jar`(萬用字元)** | 不寫死 jar 檔名,版本號變了也不會壞 |
| **`-Dserver.port=${PORT}`** | **Cloud Run 會用環境變數 `PORT` 告訴容器要監聽哪個埠**(預設 8080)。Spring Boot 讀 `server.port`,所以把 `PORT` 映過去 |

### 還加了 `.dockerignore`
排除 `target/`、`.git/`、`application-dev.properties`(金鑰!)等,讓建置 context 乾淨、也避免把機密帶進映像。

### 遇到的錯 ①:本機 Docker daemon 沒開
```
DOCKER_DAEMON_DOWN
```
**心路歷程**:本來想先在本機 `docker build` 驗證 Dockerfile,但 Docker Desktop 沒開。
**怎麼解**:**不需要本機 Docker** —— 後面用 Cloud Run 的 `--source` 部署時,Google Cloud Build 會在雲端幫你建置。所以直接跳過本機建置,讓雲端建。

---

## Part 3 — 部署到 GCP Cloud Run

### 為什麼選 Cloud Run
- **Serverless**:不用管伺服器,沒人用會縮到 0(不扣錢),有人來自動開。
- **免費額度大**,適合作業。
- **支援直接從原始碼建置**(`--source`),不必自己推 Docker 映像。

### 步驟總覽
```bash
# 0. (使用者手動)登入 + 選專案 + 確認帳單
gcloud auth login
gcloud config set project push-app-9c05e

# 1. 啟用需要的 API
gcloud services enable run.googleapis.com cloudbuild.googleapis.com artifactregistry.googleapis.com

# 2. 部署(從 Dockerfile 雲端建置 + 部署)
gcloud run deploy denden-homework \
  --source . \
  --region asia-southeast1 \
  --allow-unauthenticated \
  --port 8080 --memory 512Mi --max-instances 2 \
  --set-env-vars "^|^SPRING_PROFILES_ACTIVE=prod|DB_URL=...|DB_USERNAME=...|DB_PASSWORD=...|JWT_SECRET=...|EMAIL_PROVIDER=gmail|MAIL_USERNAME=...|MAIL_PASSWORD=...|MAIL_FROM=..."
```

### 幾個關鍵決策
| 決策 | 為什麼 |
|---|---|
| **region = `asia-southeast1`(新加坡)** | Neon 資料庫在 AWS `ap-southeast-1`(也是新加坡),app 跟 DB 同區延遲最低 |
| **`--allow-unauthenticated`** | 這是要對外公開的 API,允許匿名存取(應用層自己用 JWT 做權限) |
| **環境變數用 `^\|^` 自訂分隔符** | `gcloud --set-env-vars` 預設用逗號分隔,但 `DB_URL` 裡有 `=`(`sslmode=require`)等特殊字元。用 `^\|^` 把分隔符改成 `\|`,避免被誤解析 |
| **JWT 密鑰用 `openssl rand -hex 32`** | 產生 64 字元的隨機十六進位字串(夠長、純英數、不含會干擾指令的特殊字元) |
| **機密全走環境變數** | 程式碼/Git 裡完全沒有密碼;`application-prod.properties` 只寫 `${DB_URL}` 這種佔位符 |

### 遇到的錯 ②:Git push 認證失敗
```
fatal: could not read Username for 'https://github.com': Device not configured
```
**心路歷程**:`gh repo create --push` 建 repo 時能推(gh 自己處理認證),但後續 `git push` 卻失敗 —— 因為 git 本身沒設定要用 gh 的憑證。
**怎麼解**:
```bash
gh auth setup-git   # 讓 git 改用 gh 的登入憑證
git push origin main
```

### 遇到的錯 ③(最關鍵):Cloud Build 權限不足
第一次部署,映像都還沒開始建就失敗:
```
ERROR: (gcloud.run.deploy) PERMISSION_DENIED: Build failed because the default
service account is missing required IAM permissions.
... IAM permission denied for service account
813602138003-compute@developer.gserviceaccount.com
```
**心路歷程**:這是 **GCP 近期改版的坑** —— 現在 Cloud Build 改用「**Compute 預設服務帳號**」來執行建置,但**新專案的這個帳號預設沒有建置所需權限**(讀取上傳的原始碼、寫 Artifact Registry 等)。所以不是我指令錯,是專案少了一個角色授權。
**怎麼解**:授予那個服務帳號 `cloudbuild.builds.builder` 角色(這個角色把建置需要的權限一次包好):
```bash
gcloud projects add-iam-policy-binding push-app-9c05e \
  --member="serviceAccount:813602138003-compute@developer.gserviceaccount.com" \
  --role="roles/cloudbuild.builds.builder" \
  --condition=None
```
> 小提醒:IAM 授權要幾十秒才生效,所以我加了 `sleep 30` 再重試部署。

重試後就成功了:
```
Service [denden-homework] revision [denden-homework-00001-xm7] has been deployed...
Service URL: https://denden-homework-813602138003.asia-southeast1.run.app
```

### 「先有雞還是先有蛋」:APP_BASE_URL 的兩步處理
開通信裡的連結需要 app 的公開網址(`APP_BASE_URL`),但**部署完才知道網址**。所以分兩步:
1. 先部署(`APP_BASE_URL` 暫時是預設的 localhost)。
2. 拿到網址後,**只更新這一個環境變數**(不重建映像):
   ```bash
   gcloud run services update denden-homework --region asia-southeast1 \
     --update-env-vars "APP_BASE_URL=https://denden-homework-813602138003.asia-southeast1.run.app"
   ```
   `update --update-env-vars` 只改指定的變數,其他(JWT_SECRET 等)會保留。

### 部署後驗證(線上實測)
```bash
URL=https://denden-homework-813602138003.asia-southeast1.run.app
curl -s -o /dev/null -w "%{http_code}\n" "$URL/v3/api-docs"                # 200
curl -X POST "$URL/api/auth/register" -H "Content-Type: application/json" \
     -d '{"email":"x@example.com","password":"password123"}'              # 201
curl -s -o /dev/null -w "%{http_code}\n" "$URL/api/users/me/last-login"    # 401(無 token)
```
`register` 回 **201** 同時證明了三件事:**Cloud Run 連得到 Neon、寫得進資料、而且透過 Gmail SMTP(587 埠)真的把信寄出去了**(GCP 封鎖 25 埠但放行 587)。

---

## 整體心路歷程總結

1. **Swagger**:用既有的 springdoc 自動生成,重點是把 **JWT 授權機制**接進文件,讓受保護端點能在網頁上直接測。
2. **Dockerfile**:選**多階段建置**讓映像小;關鍵坑是 **`mvnw` 的 wrapper jar 被 gitignore**,所以改用官方 maven 映像的 `mvn`;並把 **Cloud Run 的 `PORT`** 正確接到 Spring。
3. **GCP 部署**:用 Cloud Run `--source` 讓雲端建置(本機免裝 Docker);最大的坑是**新專案的 Cloud Build 服務帳號權限**,補上 `cloudbuild.builds.builder` 角色就通;最後處理 `APP_BASE_URL` 的「先有雞還是先有蛋」用「部署→再更新環境變數」兩步解決。

核心原則貫穿全程:**所有機密走環境變數,程式碼與 Git 裡永遠不出現密碼。**
