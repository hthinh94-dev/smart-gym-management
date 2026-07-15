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
- [x] **[P0]** Cập nhật [07-actors-and-roles.md](./07-actors-and-roles.md): Làm rõ vai trò PT là Should-have và Member nhận đề xuất AI trực tiếp. Khi đăng nhập, `UserDetailsService` kiểm tra `AccountStatus`; trên các endpoint yêu cầu xác thực, `AccountStatusGuard` hoặc Method Security kiểm tra trạng thái `ACTIVE`, `LOCKED`, `DISABLED`, trong khi JWT Security Filter chỉ xác thực chữ ký và hạn dùng của token. Đảm bảo hết hạn subscription không khóa tài khoản người dùng mà chỉ chặn các chức năng yêu cầu gói ACTIVE. Sửa đổi Dashboard từ "doanh thu" thành "tổng giá trị subscription đã xác nhận mô phỏng".
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
- [x] Đồng bộ hóa Error Code Registry (`ACC-001`, `ACC-002`, `ACC-004`, `ACC-005`, `ACC-006`, `ACC-007`, `SUB-001` đến `SUB-007`, `EXR-001`, `EXR-002`, `WRK-001`, `VAL-001`, `AI-001`) và Warning Code Registry (`AI_TIMEOUT`, `AI_RESPONSE_INVALID`) từ file Quy tắc nghiệp vụ ([05-business-rules.md](./05-business-rules.md)) xuống FR, Use Case và API Contract.
- [x] Bổ sung `BR-25` để kiểm tra động hiệu lực subscription, `BR-26` để chốt vòng đời giáo án `DRAFT → ACTIVE → ARCHIVED`, `BR-27` để kiểm duyệt danh mục gói tập và `BR-28` để bảo vệ toàn vẹn tham chiếu khi ghi Workout Log.
- [x] Chuẩn hóa định dạng phản hồi JSON Success, Error và Fallback Recommendation (`recommendationSource = FALLBACK_TEMPLATE` kèm `warningCode`).
- [x] Kiểm tra và chuẩn hóa tất cả liên kết trong bộ tài liệu: chỉ sử dụng đường dẫn tương đối dạng `./`, không còn đường dẫn tuyệt đối cục bộ.
- [x] Hoàn tất Pre-Commit Quality Gate: xác nhận đủ 44 FR, 10 Use Case, 31 Acceptance Criteria, 32 API; 100 khối JSON mẫu parse hợp lệ; 18 Error Code đều được định nghĩa, sử dụng và khớp HTTP Status; 44/44 FR đều được ánh xạ trong API Traceability Matrix.

**Vấn đề gặp phải & Cách giải quyết:**
- *Vấn đề:* Phát hiện mâu thuẫn về HTTP Status Codes (400 vs 409) và lỗi trùng lặp mã lỗi giữa các file Ngày 1 và Ngày 2; luồng gia hạn gói tập bị chồng chéo với đăng ký mới.
- *Giải quyết:* Chốt lại Registry mã lỗi tập trung tại file Quy tắc nghiệp vụ, tách riêng endpoint yêu cầu gia hạn và thiết kế kịch bản xử lý logic cộng dồn thời hạn `endDate` của Admin khi phê duyệt.

**Quyết định đã chốt:**
- JWT Security Filter chỉ xác thực tính hợp lệ của chữ ký và hạn dùng (Stateless); việc chặn tài khoản bị khóa/vô hiệu hóa do `AccountStatusGuard` hoặc cơ chế Method Security truy vấn DB/Cache và thực thi ở các request tiếp theo.
- Toàn bộ API/UI phục vụ luồng MVP chỉ chạy độc lập giữa Admin và Member; vai trò PT vẫn được giữ nguyên ở phân hệ mở rộng `Should-have`.
- Recommendation fallback trả về HTTP 200 kèm `warningCode` và `calculatedTargets` do Backend tính cứng để đảm bảo trải nghiệm người dùng không bị gián đoạn khi AI gặp sự cố.

**Kế hoạch tiếp theo (Ngày 3):**
- [ ] Thiết kế mô hình thực thể liên kết (ERD), cấu trúc Database Schema chi tiết (Tables, Columns, Data Types, Constraints) và ánh xạ sang các Entity Java (Hibernate/Spring Data JPA).

**Minh chứng Git Ngày 2:**
- [ ] Tạo commit sau khi hoàn tất hậu kiểm cuối cùng.
- Commit đề xuất: `docs: finalize Day 2 detailed requirements, use cases, and API contract`
