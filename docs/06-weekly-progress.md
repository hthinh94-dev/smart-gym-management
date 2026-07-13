# 06. Tiến độ hàng tuần (Weekly Progress)

## Tuần 1 (Bắt đầu từ 13/07/2026)

### Nhật ký Ngày 1 (13/07/2026) - Tuần 1
- **Đã hoàn thành:**
  - [x] Khởi tạo cấu trúc dự án chuẩn (`backend/`, `frontend/`, `database/`, `docs/`, `diagrams/`, `postman/`, `README.md`, `.gitignore`, `.env`).
  - [x] Viết tài liệu mô tả tổng quan đề tài học thuật ([01-project-overview.md](./01-project-overview.md)).
  - [x] Xác định các tác nhân và cơ chế phân quyền RBAC ([07-actors-and-roles.md](./07-actors-and-roles.md)).
  - [x] Chốt phạm vi sản phẩm khả dụng tối thiểu MVP theo phương pháp MoSCoW ([02-mvp-scope.md](./02-mvp-scope.md)).
  - [x] Định nghĩa vai trò của AI Engine và thiết kế luồng dữ liệu Hybrid ([03-functional-requirements.md](./03-functional-requirements.md)).
  - [x] Xây dựng bộ quy tắc nghiệp vụ hệ thống làm cơ sở phát triển backend ([05-business-rules.md](./05-business-rules.md)).
  - [ ] Commit lịch sử đầu tiên của dự án: Chưa tạo Git commit; sẽ cập nhật hash sau khi commit thực tế được tạo.

- **Chưa hoàn thành / Kế hoạch cho các ngày tiếp theo của Tuần 1:**
  - [ ] Nghiên cứu sâu về tài liệu API của LLM (OpenAI/Gemini) và thư viện LangChain4j cho Java.
  - [ ] Tìm hiểu cơ chế cấu hình Spring Security 6.x + JWT và viết thử code mẫu (Boilerplate) cho phần Auth.
  - [ ] Khảo sát các thư viện UI Component của React (MUI, Ant Design) và thiết lập cấu trúc thư mục frontend nháp.
  - [ ] Chuẩn bị danh sách thô 30-50 bài tập gym gốc (Master Data) để chuẩn bị nạp vào Database ở tuần sau.

- **Vấn đề gặp phải:**
  - Cần kiểm soát chặt chẽ thiết kế prompt và định dạng phản hồi JSON từ AI Engine để đảm bảo Backend có thể parse dữ liệu ổn định và hậu kiểm an toàn.

- **Quyết định đã chốt:**
  - **Phát triển ứng dụng thực tế**: Hệ thống tập trung quản lý thực tế phòng gym và tập luyện; AI đóng vai trò công cụ tối ưu hóa và định dạng giáo án tự động dựa trên dữ liệu thật.
  - **Không tự huấn luyện mô hình**: Trong phạm vi MVP 9 tuần, dự án sẽ sử dụng các LLM API thương mại sẵn có thông qua Prompt Engineering thay vì tự train/fine-tune model để đảm bảo tính khả thi.
  - **Phát triển cuốn chiếu**: Ưu tiên hoàn thiện luồng nghiệp vụ khép kín giữa Admin và Member trước, tích hợp phân hệ PT ở giai đoạn sau.

---

### Phiên làm việc bổ sung (13/07/2026 — Phản biện & Hoàn thiện tài liệu)
- **Mô tả:** Tiến hành các phiên phản biện toàn diện (24 lỗi logic, 15 thiếu sót, cùng các mâu thuẫn giữa các file) đối với bộ tài liệu Ngày 1 và thực hiện hoàn thiện.
- **Đã hoàn thành:**
  - [x] **[P0]** Tinh chỉnh văn phong học thuật [01-project-overview.md](./01-project-overview.md): loại bỏ ngôn từ khẳng định thái quá (toàn diện → trong phạm vi đề tài, tối ưu → phù hợp), xác nhận PT là Should-have và không chặn luồng MVP.
  - [x] **[P0]** Cập nhật [07-actors-and-roles.md](./07-actors-and-roles.md): Làm rõ vai trò PT là Should-have và Member nhận đề xuất AI trực tiếp. Thống nhất cơ chế Security/UserDetails layer kiểm tra `AccountStatus` (`ACTIVE`, `LOCKED`, `DISABLED`) cho mỗi request cần xác thực. Đảm bảo hết hạn subscription không khóa tài khoản người dùng mà chỉ chặn các chức năng yêu cầu gói ACTIVE. Sửa đổi Dashboard từ "doanh thu" thành "tổng giá trị subscription đã xác nhận mô phỏng".
  - [x] **[P0]** Cập nhật [02-mvp-scope.md](./02-mvp-scope.md): Thêm `activityLevel` Enum, trường dinh dưỡng, luồng Subscription, chuyển 1RM sang Should-have; đồng bộ metadata Exercise gồm `movementPattern`, `targetBodyRegions`, `equipmentRequired`, `contraindicationTags` và `isActive`.
  - [x] **[P0]** Cập nhật [03-functional-requirements.md](./03-functional-requirements.md): Đồng bộ giới hạn Planned và Actual; xóa hoàn toàn calorie/macro khỏi output schema của AI; làm rõ Backend merge chỉ số dinh dưỡng, whitelist, metadata Exercise và điều kiện thiết bị `containsAll`.
  - [x] **[P0]** Cập nhật [05-business-rules.md](./05-business-rules.md): Chuẩn hóa email; tách BR-09A (Planned) và BR-09B (Actual); bổ sung BR-09C xác nhận Backend sở hữu số liệu dinh dưỡng; cấu hình whitelist `whitelist.containsAll(responseExerciseIds)` và chính sách mật khẩu không tự ý trim dữ liệu nhập.
  - [x] **[P1]** Tạo mới và hiệu chỉnh [04-non-functional-requirements.md](./04-non-functional-requirements.md): Đặc tả hiệu năng, TimeLimiter tổng 30 giây, timeout mỗi attempt 15 giây, retry tối đa 1 lần, không log password/JWT/API key và Docker Compose V2 `docker compose up --build`.
