# 06. Tiến độ hàng tuần (Weekly Progress)

## Tuần 1 (Bắt đầu từ 13/07/2026)

### Nhật ký Ngày 1 (13/07/2026) - Tuần 1

**Đã hoàn thành:**
- [x] Khởi tạo cấu trúc dự án chuẩn (`backend/`, `frontend/`, `database/`, `docs/`, `diagrams/`, `postman/`, `README.md`, `.gitignore`, `.env`).
- [x] Viết tài liệu mô tả tổng quan đề tài học thuật ([01-project-overview.md](./01-project-overview.md)).
- [x] Xác định các tác nhân và cơ chế phân quyền RBAC ([07-actors-and-roles.md](./07-actors-and-roles.md)).
- [x] Chốt phạm vi sản phẩm khả dụng tối thiểu MVP theo phương pháp MoSCoW ([02-mvp-scope.md](./02-mvp-scope.md)).
- [x] Định nghĩa vai trò của AI Engine và thiết kế luồng dữ liệu Hybrid ([03-functional-requirements.md](./03-functional-requirements.md)).
- [x] Xây dựng bộ quy tắc nghiệp vụ hệ thống làm cơ sở phát triển backend ([05-business-rules.md](./05-business-rules.md)).
- [x] Hoàn thành và commit bộ tài liệu Ngày 1.
  - Commit: `8cd5db6`
  - Message: `docs: finalize Day 1 project specifications`

**Chưa hoàn thành / Kế hoạch cho các ngày tiếp theo của Tuần 1:**
- [ ] Nghiên cứu sâu về tài liệu API của LLM (OpenAI/Gemini) và thư viện LangChain4j cho Java.
- [ ] Tìm hiểu cơ chế cấu hình Spring Security 6.x + JWT và viết thử code mẫu (Boilerplate) cho phần Auth.
- [ ] Khảo sát các thư viện UI Component của React (MUI, Ant Design) và thiết lập cấu trúc thư mục frontend nháp.
- [ ] Chuẩn bị danh sách thô 30-50 bài tập gym gốc (Master Data) để chuẩn bị nạp vào Database ở tuần sau.

**Vấn đề gặp phải:**
- Cần kiểm soát chặt chẽ thiết kế prompt và định dạng phản hồi JSON từ AI Engine để đảm bảo Backend có thể parse dữ liệu ổn định và hậu kiểm an toàn.

**Quyết định đã chốt:**
- **Phát triển ứng dụng thực tế**: Hệ thống tập trung quản lý phòng gym và hoạt động tập luyện; AI đóng vai trò công cụ hỗ trợ tạo đề xuất phù hợp dựa trên dữ liệu đã được Backend kiểm duyệt.
- **Không tự huấn luyện mô hình**: Trong phạm vi MVP 9 tuần, dự án sẽ sử dụng các LLM API thương mại sẵn có thông qua Prompt Engineering thay vì tự train/fine-tune model để đảm bảo tính khả thi.
- **Phát triển cuốn chiếu**: Ưu tiên hoàn thiện luồng nghiệp vụ khép kín giữa Admin và Member trước, tích hợp phân hệ PT ở giai đoạn sau.

---

### Phiên làm việc bổ sung (13/07/2026 — Phản biện & Hoàn thiện tài liệu)

**Mô tả:** Tiến hành các phiên phản biện toàn diện (24 lỗi logic, 15 thiếu sót, cùng các mâu thuẫn giữa các file) đối với bộ tài liệu Ngày 1 và thực hiện hoàn thiện.

**Đã hoàn thành:**
- [x] **[P0]** Tinh chỉnh văn phong học thuật [01-project-overview.md](./01-project-overview.md): loại bỏ ngôn từ khẳng định thái quá (toàn diện → trong phạm vi đề tài, tối ưu → phù hợp), xác nhận PT là Should-have và không chặn luồng MVP.
- [x] **[P0]** Cập nhật [07-actors-and-roles.md](./07-actors-and-roles.md): Làm rõ vai trò PT là Should-have và Member nhận đề xuất AI trực tiếp. Khi đăng nhập, `UserDetailsService` kiểm tra `AccountStatus`; trên các endpoint yêu cầu xác thực, `AccountStatusGuard` hoặc Method Security kiểm tra trạng thái `ACTIVE`, `LOCKED`, `DISABLED`, trong khi `JwtAuthenticationFilter` xác thực kỹ thuật token và nạp identity/roles nhưng không quyết định trạng thái tài khoản. Đảm bảo hết hạn subscription không khóa tài khoản người dùng mà chỉ chặn các chức năng yêu cầu gói ACTIVE. Sửa đổi Dashboard từ "doanh thu" thành "tổng giá trị subscription đã xác nhận mô phỏng".
- [x] **[P0]** Cập nhật [02-mvp-scope.md](./02-mvp-scope.md): Thêm `activityLevel` Enum, trường dinh dưỡng, luồng Subscription, chuyển 1RM sang Should-have; đồng bộ metadata Exercise gồm `movementPattern`, `targetBodyRegions`, `equipmentRequired`, `contraindicationTags` và `isActive`.
- [x] **[P0]** Cập nhật [03-functional-requirements.md](./03-functional-requirements.md): Đồng bộ giới hạn Planned và Actual; xóa hoàn toàn calorie/macro khỏi output schema của AI; làm rõ Backend merge chỉ số dinh dưỡng, whitelist, metadata Exercise và điều kiện thiết bị `containsAll`.
- [x] **[P0]** Cập nhật [05-business-rules.md](./05-business-rules.md): Chuẩn hóa email; tách BR-09A (Planned) và BR-09B (Actual); bổ sung BR-09C xác nhận Backend sở hữu số liệu dinh dưỡng; cấu hình whitelist `whitelist.containsAll(responseExerciseIds)` và chính sách mật khẩu không tự ý trim dữ liệu nhập.
- [x] **[P1]** Tạo mới và hiệu chỉnh [04-non-functional-requirements.md](./04-non-functional-requirements.md): Đặc tả hiệu năng, TimeLimiter tổng 30 giây, timeout mỗi attempt 15 giây, retry tối đa 1 lần, không log password/JWT/API key và Docker Compose V2 `docker compose up --build`.

---

### Nhật ký Ngày 2 (14/07/2026) - Tuần 1

**Đã hoàn thành:**
- [x] Tạo và hoàn thiện đặc tả yêu cầu chức năng chi tiết [08-functional-requirements-detail.md](./08-functional-requirements-detail.md) theo mã quy ước `FR-xx` đồng bộ cho 44 chức năng.
- [x] Tạo [09-use-case-specification.md](./09-use-case-specification.md) đặc tả chi tiết 10 Use Cases cốt lõi từ `UC-01` đến `UC-10`; mô tả đầy đủ luồng AI Hybrid tại `UC-07`.
- [x] Tích hợp 31 tiêu chí nghiệm thu Acceptance Criteria dạng Given - When - Then (BDD) cho toàn bộ Use Cases, bao gồm cả luồng thành công và kịch bản biên của Auth, Subscription, Exercise, AI/Fallback, Workout Log, Progress và khóa/mở khóa tài khoản.
- [x] Tạo [10-api-draft.md](./10-api-draft.md) phác thảo 32 API Contract chi tiết theo chuẩn RESTful cho các nhóm Auth, Member Profile, Membership, Exercise, Recommendation, Workout Plan, Workout Log, Body Progress và Admin; bổ sung ma trận truy vết endpoint → FR → UC → BR/NFR.
- [x] Tách biệt hoàn toàn API đăng ký mới và API gia hạn gói tập để thực thi chuẩn quy tắc `BR-24`.
- [x] Đồng bộ hóa Error Code Registry (`AUTH-002`, `ACC-001`, `ACC-002`, `ACC-004`, `ACC-005`, `ACC-006`, `ACC-007`, `SUB-001` đến `SUB-007`, `EXR-001`, `EXR-002`, `WRK-001`, `VAL-001`, `AI-001`, `CON-001`) và Warning Code Registry (`AI_TIMEOUT`, `AI_RESPONSE_INVALID`) từ file Quy tắc nghiệp vụ ([05-business-rules.md](./05-business-rules.md)) xuống FR, Use Case và API Contract.
- [x] Bổ sung `BR-25` để kiểm tra động hiệu lực subscription, `BR-26` để chốt vòng đời giáo án `DRAFT → ACTIVE → ARCHIVED`, `BR-27` để kiểm duyệt danh mục gói tập và `BR-28` để bảo vệ toàn vẹn tham chiếu khi ghi Workout Log.
- [x] Chuẩn hóa định dạng phản hồi JSON Success, Error và Fallback Recommendation (`recommendationSource = FALLBACK_TEMPLATE` kèm `warningCode`).
- [x] Kiểm tra và chuẩn hóa tất cả liên kết trong bộ tài liệu: chỉ sử dụng đường dẫn tương đối dạng `./`, không còn đường dẫn tuyệt đối cục bộ.
- [x] Hoàn tất Pre-Commit Quality Gate: xác nhận đủ 44 FR, 10 Use Case, 31 Acceptance Criteria, 32 API; 100 khối JSON mẫu parse hợp lệ; 18 Error Code đều được định nghĩa, sử dụng và khớp HTTP Status; 44/44 FR đều được ánh xạ trong API Traceability Matrix.

**Vấn đề gặp phải & Cách giải quyết:**
- *Vấn đề:* Phát hiện mâu thuẫn về HTTP Status Codes (400 vs 409) và lỗi trùng lặp mã lỗi giữa các file Ngày 1 và Ngày 2; luồng gia hạn gói tập bị chồng chéo với đăng ký mới.
- *Giải quyết:* Chốt lại Registry mã lỗi tập trung tại file Quy tắc nghiệp vụ, tách riêng endpoint yêu cầu gia hạn và thiết kế kịch bản xử lý logic cộng dồn thời hạn `endDate` của Admin khi phê duyệt.

**Quyết định đã chốt:**
- `JwtAuthenticationFilter` xác thực chữ ký/hạn dùng và nạp identity/roles để thiết lập `SecurityContext`; việc chặn tài khoản bị khóa/vô hiệu hóa do `AccountStatusGuard` truy vấn DB và thực thi ở các request tiếp theo. Cache trạng thái chưa được sử dụng trong implementation hiện tại.
- Toàn bộ API/UI phục vụ luồng MVP chỉ chạy độc lập giữa Admin và Member; vai trò PT vẫn được giữ nguyên ở phân hệ mở rộng `Should-have`.
- Recommendation fallback trả về HTTP 200 kèm `warningCode` và `calculatedTargets` do Backend tính cứng để đảm bảo trải nghiệm người dùng không bị gián đoạn khi AI gặp sự cố.

**Kế hoạch tiếp theo (Ngày 3):**
- [ ] Thiết kế mô hình thực thể liên kết (ERD), cấu trúc Database Schema chi tiết (Tables, Columns, Data Types, Constraints) và ánh xạ sang các Entity Java (Hibernate/Spring Data JPA).

**Minh chứng Git Ngày 2:**
- [x] Đã tạo commit sau khi hoàn tất hậu kiểm cuối cùng.
  - Commit: `c5a7a792bf1ff70ea46a4a782edcae1f399557e9`
  - Message: `docs: finalize Day 2 detailed requirements, use cases, and API contract`

---

### Nhật ký Ngày 3 (17/07/2026) - Tuần 1

**Mục tiêu:** Hoàn thiện thiết kế cơ sở dữ liệu vật lý, sơ đồ ERD, chiến lược
ánh xạ JPA/Hibernate và script DDL MySQL 8 làm nền tảng cho giai đoạn hiện thực
Backend.

**Đã hoàn thành:**
- [x] Tạo và hoàn thiện [11-database-design.md](./11-database-design.md), đặc tả
  đầy đủ 25 bảng vật lý thuộc tám nhóm Auth, Profile, Membership, Exercise,
  Workout Plan, Workout Log, Progress và AI/Nutrition.
- [x] Chốt 16 nguyên tắc thiết kế dữ liệu, danh sách Enum, khóa chính, khóa
  ngoại, unique/check constraints, soft delete, auditing fields và hệ thống
  index phục vụ các truy vấn nghiệp vụ cốt lõi.
- [x] Tách riêng bảng `subscription_renewal_requests` và thiết kế generated
  unique key để bảo vệ quy tắc một Subscription `ACTIVE`, một yêu cầu đăng ký
  mới `PENDING` và một yêu cầu gia hạn `PENDING` trong điều kiện concurrent.
- [x] Thiết kế composite unique key và composite foreign key cho
  `workout_sessions`/`workout_logs`, bảo đảm `member_id` và `log_date` của nhật
  ký luôn khớp với buổi tập theo BR-19 và BR-28.
- [x] Tạo sơ đồ ERD Mermaid
  [erd-gym-management.mmd](../diagrams/erd-gym-management.mmd), biểu diễn đủ
  25 thực thể và các quan hệ 1-1, 1-N, N-N; không tạo bảng Guest hoặc bảng
  nghiệp vụ PT riêng trong MVP.
- [x] Tạo và hoàn thiện
  [12-entity-relationship-mapping.md](./12-entity-relationship-mapping.md),
  ánh xạ 16 Java Entity và chín `@ElementCollection` theo 14 quy ước JPA;
  cấu hình LAZY loading, cascade có kiểm soát, DTO boundary, soft delete và
  optimistic locking bằng `@Version`.
- [x] Tạo [schema-draft.sql](../database/schema-draft.sql) theo đúng dependency
  tree của MySQL 8; toàn bộ 25 bảng dùng `InnoDB`, `utf8mb4` và
  `utf8mb4_unicode_ci`.
- [x] Đồng bộ tuyệt đối tên 25 bảng giữa SQL và JPA: 16 Entity tables và chín
  Element Collection tables; đồng bộ đủ 18 unique constraints của JPA và 14
  index đề xuất trong File 11.
- [x] Tách rõ dữ liệu AI và Backend tại `ai_recommendations`: Backend sở hữu
  `calculated_targets`; AI chỉ cung cấp `ai_suggestion`; lưu thêm
  `recommendation_source`, `validation_status` và `warning_code` có CHECK
  constraint trạng thái.
- [x] Seed duy nhất ba role tĩnh `ROLE_ADMIN`, `ROLE_MEMBER`, `ROLE_PT`; không
  seed Guest, User, password hoặc dữ liệu nhạy cảm.
- [x] Thực thi thành công toàn bộ script từ database trống trên MySQL 8.0.44.
  Kết quả metadata: 25 bảng, 54 CHECK constraints, 34 foreign keys, 18 unique
  constraints và ba role seed.
- [x] Hoàn thành 9/9 kiểm thử ràng buộc âm tại tầng database: chặn hai
  Subscription ACTIVE, hai Renewal PENDING, hai Workout Plan ACTIVE, planned
  sets/actual sets vượt giới hạn, Workout Log sai chủ sở hữu Session, Body
  Progress trùng ngày, trạng thái AI Recommendation sai và Gender ngoài Enum.
- [x] Database kiểm thử được tạo trong môi trường cô lập, dừng và dọn sạch sau
  khi hoàn thành; không tác động đến database phát triển hiện có.

**Vấn đề gặp phải và cách giải quyết:**
- *Vấn đề:* DDL ban đầu thiếu generated unique key cho các trạng thái
  `ACTIVE`/`PENDING`, khiến kiểm tra ở Service chưa đủ chống race condition.
  *Giải quyết:* Bổ sung generated columns kết hợp unique constraints và giữ
  `@Version` tại các Entity có khả năng xung đột.
- *Vấn đề:* Workout Log ban đầu có thể lệch Member/ngày so với Workout Session
  và cho phép mất liên kết lịch sử bằng `ON DELETE SET NULL`.
  *Giải quyết:* Sử dụng composite foreign key, chuyển các liên kết lịch sử sang
  `NOT NULL`/`ON DELETE RESTRICT` và chỉ dùng cascade cho thành phần được thực
  thể cha sở hữu hoàn toàn.
- *Vấn đề:* MySQL 8 hạn chế biểu thức CHECK tham chiếu trực tiếp các cột tham
  gia foreign key referential action.
  *Giải quyết:* Database kiểm tra vòng đời bằng trạng thái, ngày và timestamp;
  Service xác minh actor Admin và lưu khóa ngoại actor trong cùng transaction.
- *Vấn đề:* ERD ban đầu chưa thể hiện quan hệ trực tiếp từ `users` tới
  `workout_logs`, dù `workout_logs.member_id` đã tồn tại nhất quán trong thiết
  kế bảng, JPA Mapping và khóa ngoại vật lý.
  *Giải quyết:* Bổ sung quan hệ `users ||--o{ workout_logs` vào cả ERD nhúng
  trong File 11 và file Mermaid độc lập; đối chiếu tự động xác nhận toàn bộ 34
  khóa ngoại SQL đều đã được biểu diễn, không còn quan hệ thiếu hoặc thừa.

**Quyết định đã chốt:**
- Sử dụng đúng 25 bảng vật lý cho MVP; `ROLE_PT` tồn tại trong RBAC nhưng không
  có bảng nghiệp vụ PT riêng, còn Anonymous Guest không được lưu trong database.
- Dùng khóa chính `BIGINT AUTO_INCREMENT`, Enum lưu `VARCHAR`, audit timestamp
  theo UTC và ngày nghiệp vụ được Backend xác định theo timezone
  `Asia/Ho_Chi_Minh`.
- Không dùng cascade remove từ dữ liệu lịch sử sang `users`, `exercises` hoặc
  `membership_packages`; package và exercise được vô hiệu hóa bằng soft delete.
- `schema-draft.sql` là hợp đồng DDL đã kiểm chứng cho thiết kế; khi bắt đầu
  coding sẽ chuyển thành migration Flyway có version, không để Hibernate tự tạo
  schema production.

**Kế hoạch tiếp theo:**
- [ ] Chuyển DDL đã chốt thành Flyway migration đầu tiên khi khởi tạo Backend.
- [ ] Hiện thực các Entity, Enum, Embeddable ID và Repository theo File 12.
- [ ] Chuẩn bị dữ liệu master 30-50 bài tập phù hợp Enum và metadata Exercise
  để seed ở migration riêng.
- [ ] Viết integration test bằng MySQL/Testcontainers cho generated unique key,
  composite foreign key, soft delete và optimistic locking.

**Minh chứng Git Ngày 3:**
- [x] Đã tạo commit khép lại thiết kế dữ liệu Ngày 3 sau khi hoàn tất hậu kiểm.
- Commit: `652d4a9`
- Message: `docs(database): finalize Day 3 schema, ERD, and JPA mappings`

---

## Ngày 4 — Khởi tạo Backend Foundation

### Đã hoàn thành

- [x] Khởi tạo Maven Spring Boot Backend tại thư mục `backend/`.
- [x] Cấu hình Java 21 và Spring Boot 3.4.3.
- [x] Bổ sung dependencies nền tảng: Spring Web, Spring Data JPA, Validation,
  Spring Security, MySQL Driver, Flyway Core/MySQL, Actuator, Lombok và Test.
- [x] Chuẩn hóa repository bằng `.gitignore`, `.editorconfig`,
  `.gitattributes` và `.env.example` tại project root.
- [x] Cấu hình `application.yml` dùng biến môi trường, timezone UTC,
  `spring.jpa.hibernate.ddl-auto=validate` và `spring.jpa.open-in-view=false`.
- [x] Tạo 8 Flyway migrations theo module:
  `V1__create_auth_schema.sql` đến `V8__seed_system_roles.sql`.
- [x] Đối chiếu Flyway migrations với `database/schema-draft.sql`:
  25/25 bảng vật lý khớp thiết kế.
- [x] Bật JPA Auditing qua `@EnableJpaAuditing`.
- [x] Tạo `BaseEntity`, `AccountStatus`, `RoleName`.
- [x] Ánh xạ `User`, `Role`, `UserRole`, `UserRoleId` và các repository Auth.
- [x] Dùng entity trung gian `UserRole`; không dùng `@ManyToMany` trực tiếp
  vì bảng `user_roles` có audit fields.
- [x] Kiểm tra runtime: Maven test, Flyway migration, Hibernate validate
  và kết nối MySQL thành công.

### Quyết định đã chốt

- Flyway là nguồn sở hữu duy nhất của schema; Hibernate chỉ được phép validate.
- Không sửa migration đã áp dụng. Mọi thay đổi schema sau này phải tạo migration mới.
- Không lưu secret trong source code; cấu hình database lấy từ biến môi trường.
- `ROLE_PT` chỉ tồn tại trong RBAC; không tạo bảng nghiệp vụ PT trong MVP.
- Anonymous Guest không được lưu thành role hoặc bảng trong database.

### Vấn đề đã xử lý

- Loại bỏ class `@SpringBootApplication` và test context bị trùng.
- Loại bỏ các file `.gitkeep` không còn cần thiết.
- Đồng bộ file `.env.example` với các biến kết nối MySQL (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`); cổng cụ thể do môi trường cục bộ/Docker cấu hình.

### Quyết định kiến trúc trước Ngày 5 (26/07/2026)

- [x] Đối chiếu đề xuất kiến trúc với File 02–12, Flyway migration và Backend Foundation; chốt **Modular Layered Monolith** tại [13-architecture-decision.md](./13-architecture-decision.md).
- [x] Giữ Layered Architecture, DTO/Repository/Service, JWT stateless, Enum + state-policy, AI hybrid, Adapter tại provider boundary, fallback được hậu kiểm và mapper thủ công.
- [x] Điều chỉnh để khớp đặc tả hiện hành: Resilience4j và SpringDoc OpenAPI là bắt buộc; `SubscriptionRenewalRequestStatus` chỉ có `PENDING`/`PROCESSED`; `admin` là actor/API entry point, không phải module nghiệp vụ trùng lặp.
- [x] Chốt `Clock` ngày nghiệp vụ `Asia/Ho_Chi_Minh`, audit timestamp UTC; giữ optimistic locking kết hợp lock theo phạm vi Member tại các transition đa dòng.
- [x] Loại bỏ mật khẩu database hardcode khỏi `application.yml`; datasource đọc `DB_URL` hoặc các biến `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` từ environment. File `.env` bị ignore chỉ là nguồn để Docker Compose hoặc IDE/shell nạp các biến này, không được commit.

### Kế hoạch tiếp theo

- Ngày 5: hiện thực nền Spring Security, JWT Access Token,
  `AccountStatusGuard`, response/error chuẩn và OpenAPI; chưa làm Register/Login.

---

## Ngày 5 — Security/JWT Foundation (27/07/2026)

### Đã hoàn thành

- [x] Bổ sung JJWT `0.12.6`, hoàn thiện `JwtService` cho Access Token chứa
  `sub`, `iat`, `exp` và `roles`; không tạo Refresh Token.
- [x] Loại bỏ toàn bộ JWT secret và database credential fallback khỏi source;
  ứng dụng bắt buộc nhận `JWT_SECRET`, `DB_USER`, `DB_PASSWORD` từ environment.
- [x] Kiểm tra JWT secret khi khởi động và fail-fast nếu khóa hiệu lực ngắn hơn
  32 byte theo NFR-06.
- [x] Hoàn thiện `CustomUserDetailsService`, `JwtAuthenticationFilter`,
  `SecurityConfiguration`, RBAC tạm thời và Method Security nền tảng.
- [x] Chuẩn hóa phản hồi 401/403 bằng `ErrorResponse`; token thiếu/sai/hết hạn
  trả `ACC-005`, tài khoản `LOCKED` trả `ACC-004`, tài khoản `DISABLED` trả
  `ACC-006` kèm `details.accountStatus`.
- [x] Hoàn thiện nền `AccountStatusGuard` theo email/User ID; chưa gắn vào
  endpoint nghiệp vụ vì kế hoạch thực hiện việc này cùng endpoint bảo vệ sau.
- [x] Bổ sung SpringDoc OpenAPI và mở public `/v3/api-docs`, `/swagger-ui/**`.
- [x] Đồng bộ `.env.example` ở root/backend theo một bộ biến; `.env` không bị
  Git theo dõi; dọn toàn bộ `.gitkeep` không còn cần thiết.

### Cơ chế xác thực và phân quyền

Hệ thống sử dụng Spring Security để cung cấp một chuỗi xử lý thống nhất cho xác
thực, phân quyền RBAC và phản hồi lỗi 401/403. JWT Access Token được chọn vì phù
hợp với REST API và React client: server không lưu session đăng nhập, mỗi request
tự mang Bearer token đã ký. Mô hình stateless giúp backend mở rộng độc lập giữa
các request, nhưng token đã phát hành không bị thu hồi trực tiếp khi trạng thái
tài khoản thay đổi.

Trong luồng request, `JwtAuthenticationFilter` đọc Bearer token, kiểm tra chữ ký
và hạn dùng, nạp identity/roles qua `CustomUserDetailsService`, sau đó thiết lập
`SecurityContext`. Filter không dùng `accountStatus` để quyết định chặn request.
`AccountStatusGuard` truy vấn trạng thái hiện hành ở tầng endpoint/Method Security,
nhờ đó token cũ của tài khoản `LOCKED` hoặc `DISABLED` vẫn bị chặn bằng
`ACC-004`/`ACC-006`. Mật khẩu được băm bằng BCrypt strength 12 và không xuất hiện
trong JWT, response hoặc log.

Anonymous Guest chỉ là actor chưa xác thực, không được lưu thành `ROLE_GUEST`.
`ROLE_PT` được seed để mở rộng RBAC, nhưng MVP chưa có bảng hoặc API nghiệp vụ PT;
luồng bắt buộc hiện tại chỉ gồm Admin và Member.

### Kiểm thử và minh chứng

- [x] Chạy `mvnw.cmd clean test`: **26 test pass**,
  không failure/error.
- [x] Biên dịch sạch 21 main classes và 6 test classes bằng Java release 21.
- [x] Flyway validate thành công 8 migration trên MySQL 8.0.44; Hibernate
  khởi tạo `EntityManagerFactory` với schema hiện tại.
- [x] Test JWT bao phủ generate, extract username, đúng/sai user, hết hạn,
  sai chữ ký và secret dưới 32 byte.
- [x] Test Security bao phủ UserDetails `ACTIVE/LOCKED/DISABLED`,
  AccountStatusGuard, body lỗi 401/403, request thiếu token và OpenAPI public.

### Quyết định và lưu ý

- Generic RBAC 403 dùng `AUTH-002` đã được đăng ký trong Error Code Registry;
  các lỗi trạng thái tài khoản vẫn dùng đúng `ACC-004`/`ACC-006`.
- File `.env` cục bộ phải được cập nhật đúng database đang chạy và phải được
  IDE/shell hoặc Docker Compose nạp trước khi khởi động ứng dụng.

### Kế hoạch tiếp theo

- Ngày 6: hiện thực `POST /api/v1/auth/register` theo API Draft, gồm DTO,
  validation mật khẩu, normalize email, BCrypt, `ROLE_MEMBER`, `ACTIVE`,
  `ACC-001` và `ACC-002`; không cho client truyền role/account status.

---

## Ngày 6 — Register API (28/07/2026)

### Đã triển khai source

- [x] Tạo `RegisterRequest` và `RegisterResponse`; request trim họ tên/email,
  giữ nguyên password, áp dụng validation BR-18 và không nhận role/account status.
- [x] Tạo `ErrorCode`, `BusinessException`, `GlobalExceptionHandler`; chuẩn hóa
  `ACC-001`, `ACC-002`, `VAL-001`, `SYS-001` và không trả stack trace/SQL/class Java.
- [x] Tạo `UserRoleRepository` và `AuthService.register()` trong transaction:
  normalize email, lấy `ROLE_MEMBER`, BCrypt strength 12, tạo User `ACTIVE`,
  lưu `UserRole` và bắt race condition tại `uk_users_email`.
- [x] Tạo `AuthController` cho `POST /api/v1/auth/register`, trả HTTP 201 bằng
  `ApiResponse<RegisterResponse>` và bổ sung mô tả OpenAPI.
- [x] Thêm CORS từ `CORS_ALLOWED_ORIGINS`; mặc định chỉ cho
  `http://localhost:5173`, hỗ trợ nhiều origin cụ thể và fail-fast nếu có wildcard.
- [x] Tạo `AuthServiceTest` và `AuthControllerTest` bao phủ success, normalize,
  BCrypt, validation password/email, confirm password, duplicate/race condition,
  payload cố gửi role/status, malformed JSON và response không lộ password.
- [x] Tạo `WebCorsConfigurationTest` cho nhiều origin cụ thể, loại trùng và
  fail-fast với danh sách rỗng hoặc mọi dạng wildcard.
- [x] Tạo `AuthRegistrationIntegrationTest` để kiểm tra qua full Spring context
  và MySQL: lưu User/UserRole, BCrypt, chống privilege escalation và email trùng.
- [x] Hoàn thiện React Register bằng React Router, React Query, React Hook Form,
  Zod và Axios; LoginPage giữ đúng vai trò màn hình đích của Ngày 6.
- [x] Tạo Postman collection cho success, `ACC-001`, `ACC-002`, `VAL-001` và
  payload cố gắng tự gán role/account status.

### Kết quả kiểm thử xác nhận

- [x] `mvnw.cmd clean test`: 61 test pass, 0 failure, 0 error, 0 skipped;
  toàn bộ 26 test Ngày 5 tiếp tục pass.
- [x] Flyway validate đủ 8 migration, schema ở version 8 và Hibernate khởi tạo
  `EntityManagerFactory` thành công trên MySQL 8.
- [x] Frontend `npm run test -- --run`: 6 test pass.
- [x] Frontend `npm run build`: production build thành công.
- [x] Đã kiểm thử local Register success/error/CORS, dữ liệu `ACTIVE + ROLE_MEMBER`,
  BCrypt và unique email trong database.

**Kết luận:** Ngày 6 đã hoàn thành trong phạm vi local. Deploy staging được hoãn
theo phạm vi đã thống nhất và không được ghi nhận là đã thực hiện.

---

## Ngày 7 — Login và Current User API (29/07/2026)

### Đã triển khai source

- [x] Tạo `LoginRequest`, `LoginResponse`, `LoginUserResponse` và
  `CurrentUserResponse`; email được trim/lowercase, password đăng nhập không trim
  và không áp lại chính sách mật khẩu của Register.
- [x] Bổ sung `ACC-007` cho sai email/password, giữ `ACC-004` cho `LOCKED` và
  `ACC-006` cho `DISABLED`; Login Service tự ánh xạ lỗi, không dùng JWT entry point.
- [x] Tạo `AuthService.login()` qua `AuthenticationManager`; chỉ cấp JWT sau khi
  xác thực thành công và kiểm tra lại trạng thái tài khoản.
- [x] JWT tiếp tục chứa `sub`, `roles`, `iat`, `exp`; `expiresIn` được đổi từ
  millisecond cấu hình sang giây, mặc định là `3600`, không hardcode trong response.
- [x] Tạo `AuthenticatedUserPrincipal` chứa User ID, họ tên, email, role,
  authorities, account status và created time để các module sau lấy ownership từ
  `SecurityContext`, không nhận User ID tùy ý từ client.
- [x] Tạo `GET /api/v1/users/me`; endpoint gọi `AccountStatusGuard` theo User ID,
  nên token cũ của tài khoản `LOCKED`/`DISABLED` trả đúng `ACC-004`/`ACC-006`.
- [x] Thay Login mock bằng Axios API thật; thêm Auth Context, `sessionStorage`,
  Bearer request interceptor và response interceptor chỉ xóa phiên với `ACC-005`.
- [x] Sau Login, frontend gọi `/users/me` để xác nhận principal; nếu bước xác nhận
  thất bại thì rollback token vừa lưu và giữ trạng thái anonymous.
- [x] LoginPage dùng React Hook Form, Zod và TanStack Query; có loading, hiện/ẩn
  password, lỗi `ACC-004`/`ACC-006`/`ACC-007`, lỗi mạng và trạng thái thành công.
- [x] Cập nhật API Draft: ví dụ `expiresIn` là `3600`, khớp cấu hình mặc định
  `JWT_ACCESS_TOKEN_EXPIRATION_MS=3600000`.
- [x] Khai báo OpenAPI security scheme `bearerAuth`; Swagger UI có nút `Authorize`
  và tự gắn `Authorization: Bearer <token>` cho `/api/v1/users/me`.
- [x] Cập nhật Postman thành chuỗi UC-01/UC-02: Register, Login, `/users/me`,
  sai password, token sai và token thiếu.

### Kiểm thử và minh chứng

- [x] Unit/WebMvc test bao phủ login success, normalize email, giữ nguyên password,
  sai email/password cùng `ACC-007`, trạng thái tài khoản và không cấp token khi lỗi.
- [x] JWT test xác nhận claims bắt buộc và `expiresIn` khớp cấu hình.
- [x] Integration test qua full Spring Security chain và MySQL bao phủ login,
  `/users/me`, token thiếu/sai/hết hạn và token cũ sau khi khóa/vô hiệu hóa tài khoản.
- [x] Toàn bộ 61 test đến hết Ngày 6 tiếp tục pass; regression hiện có 83 test,
  0 failure, 0 error, 0 skipped.
- [x] Frontend có 22 test pass; production build thành công, gồm test chống hiển thị
  success từ phiên cũ và test xóa phiên cũ sau khi đăng ký tài khoản mới.
- [x] Flyway validate 8 migration, schema version 8; Hibernate khởi tạo thành công.

**Kết luận:** Source backend/frontend, kiểm thử tự động và kiểm thử localhost thủ công
theo mục 16 của Ngày 7 đã hoàn thành. Luồng Register → Login → `/users/me`, Swagger
Authorize và các trường hợp lỗi xác thực đã được xác nhận hoạt động đúng.

---

## Ngày 8 — Admin Account Status (30/07/2026)

### Đã triển khai source

- [x] Tạo `PageResponse`, DTO Admin User, `BusinessClockConfiguration`, projection
  và native query danh sách tài khoản có pagination, search, role/status filter.
- [x] `hasActiveSubscription` được tính động theo `status = ACTIVE`,
  `startDate <= today < endDate` bằng ngày nghiệp vụ `Asia/Ho_Chi_Minh`, không N+1.
- [x] Tạo `AdminUserService` và `AdminUserController` cho list/lock/unlock; transition
  target dùng pessimistic lock, chỉ `ACTIVE -> LOCKED -> ACTIVE` và không sửa subscription.
- [x] Chặn Admin tự khóa, khóa Admin khác, thao tác với `DISABLED`, lý do ngoài
  10-500 ký tự và lý do hết hạn gói tập; Admin hiện hành luôn qua `AccountStatusGuard`.
- [x] Tạo Protected Route, Role Route, layout Admin/Member, trang User Management,
  search debounce, filter, pagination, dialog lock/unlock và các trạng thái UI.
- [x] Login điều hướng theo role sau khi `/users/me` xác nhận principal; `ACC-005`
  tiếp tục xóa session, token cũ của Member bị khóa nhận `ACC-004`.

### Kiểm thử và minh chứng

- [x] `mvnw.cmd clean test`: 110 test pass, 0 failure, 0 error, 0 skipped;
  toàn bộ 83 test đến hết Ngày 7 tiếp tục pass.
- [x] Flyway validate 8 migration, schema version 8 và Hibernate khởi tạo thành công.
- [x] Frontend có 33 test pass; Vite production build thành công.
- [x] Kiểm thử localhost hoàn tất: Admin list/search/filter, lock Member, token cũ
  và login mới bị `ACC-004`, unlock rồi login lại thành công, Member bị `AUTH-002`
  tại Admin API và subscription trước/sau thao tác không thay đổi.
- [x] UI được kiểm tra ở viewport desktop/laptop theo phạm vi Ngày 8.

**Kết luận:** UC-10 đã hoàn thành trong phạm vi local. Admin quản lý trạng thái tài
khoản đúng RBAC và state transition; JWT stateless vẫn bị Guard chặn theo trạng thái
DB hiện hành, còn subscription độc lập với thao tác lock/unlock. Deploy tiếp tục hoãn
đến Gate đóng M1.

---

## Ngày 9 — React Skeleton và OpenAPI Gate (31/07/2026)

### Đã triển khai source

- [x] Chuẩn hóa OpenAPI cho toàn bộ Auth, Current User và Admin User API; response
  thành công/lỗi dùng schema cụ thể, Bearer JWT được khai báo nhất quán và các
  trường password chỉ dùng để ghi (`writeOnly`).
- [x] Bổ sung test OpenAPI kiểm tra đủ sáu operation M1, security requirement,
  response schema và bảo đảm password không xuất hiện trong response DTO.
- [x] Hoàn thiện React application shell với Public Only Route, Protected Route,
  Role Route, layout Member/Admin và điều hướng mặc định theo role hiện hành.
- [x] Khôi phục phiên bằng `/users/me`; interceptor xóa session khi gặp
  `ACC-004`, `ACC-005` hoặc `ACC-006`, ngăn trạng thái đăng nhập thành công cũ
  xuất hiện lại khi người dùng chưa submit form.
- [x] Sửa entry HTML dùng đúng `src/main.tsx`, cập nhật title dùng chung và giữ
  production build độc lập với source JavaScript cũ không tồn tại.
- [x] Postman có 21 request bao phủ Register, Login, Current User, Admin
  list/search/filter/lock/unlock và các mã lỗi Auth/RBAC bắt buộc của M1.

### Kiểm thử và minh chứng

- [x] `mvnw.cmd clean test`: 200 test pass, 0 failure, 0 error, 0 skipped;
  toàn bộ 110 test đến hết Ngày 8 tiếp tục pass.
- [x] Flyway validate đủ 8 migration, schema version 8; Hibernate khởi tạo
  `EntityManagerFactory` thành công trên MySQL 8.
- [x] Frontend có 43 test pass; Vite production build thành công.
- [x] Full flow localhost đã xác nhận: Register/Login/restore/logout, Member bị
  chặn khỏi Admin, Admin bị chặn khỏi Member, Admin list/search, lock Member làm
  token cũ bị chặn, unlock và đăng nhập lại thành công.
- [x] Swagger UI có nút `Authorize`; sáu operation M1 hiển thị response contract
  đúng và không yêu cầu ghép thủ công Authorization header.

### Lưu ý hardening

- Cảnh báo Mockito dynamic Java Agent và Spring `AuthenticationProvider` không
  làm thất bại build; xử lý trong đợt Review M1 đã lên kế hoạch, không thay đổi
  provider trong phạm vi Ngày 9.

**Kết luận:** Ngày 9 hoàn thành trong phạm vi local. OpenAPI contract, React shell,
auth session và route protection đã đồng bộ với Backend; M1 sẵn sàng bước Review,
hardening và tạo tag theo kế hoạch.
