# ĐẶC TẢ YÊU CẦU PHI CHỨC NĂNG (NON-FUNCTIONAL REQUIREMENTS SPECIFICATION)

## 1. Giới thiệu

Tài liệu này xác lập các tiêu chí đo lường chất lượng hệ thống (Quality Attributes) tối thiểu cần đảm bảo trong quá trình thiết kế, phát triển và nghiệm thu đồ án. Các yêu cầu phi chức năng là ràng buộc kiến trúc và kỹ thuật, không thể bổ sung sau khi thiết kế cốt lõi đã hoàn thành.

---

## 2. Hiệu năng (Performance)

### [NFR-01] - Thời gian phản hồi API nội bộ (Internal API Response Time)
- **Ngưỡng mục tiêu:** Thời gian phản hồi của các API không gọi AI bên ngoài (CRUD thông thường, xác thực JWT, tính toán chỉ số sinh học) phải ≤ **800ms** trong điều kiện môi trường demo cục bộ.
- **Phương pháp kiểm thử:** Sử dụng trình duyệt DevTools hoặc API client (Postman) để đo thủ công thời gian phản hồi của từng endpoint. Không yêu cầu công cụ Load Testing (k6, JMeter) trong phạm vi MVP đồ án.

### [NFR-02] - Thời gian phản hồi API tích hợp AI (AI-integrated API Response Time)
- **Ngưỡng mục tiêu:** Endpoint gọi AI được giới hạn bởi TimeLimiter tổng **30 giây** (bao gồm tối đa 1 lần retry). Mỗi lần gọi AI có ngân sách thời gian tối đa **15 giây** bao gồm cả kết nối và chờ phản hồi; lần retry chỉ được bắt đầu khi ngân sách thời gian tổng còn đủ. Giao diện phải hiển thị Loading State trong suốt khoảng thời gian này.
- **Lý do ngưỡng cao:** Hoàn toàn phụ thuộc vào độ trễ mạng và tốc độ phản hồi của nhà cung cấp LLM (OpenAI/Gemini), không kiểm soát được từ phía hệ thống.

### [NFR-03] - Tính toán chỉ số sinh học (Biometric Calculation Performance)
- **Ngưỡng mục tiêu:** Module tính toán BMI, BMR, TDEE, Macronutrients phải hoàn thành trong **≤ 50ms** do đây là phép tính số học thuần túy trong bộ nhớ (in-memory).
- **Hiện thực hóa kỹ thuật:** Đóng gói logic tính toán trong một `@Service` bean không trạng thái (stateless), tránh instantiate object không cần thiết trong vòng lặp.

---

## 3. Độ tin cậy & Khả năng phục hồi (Reliability & Resilience)

### [NFR-04] - Cơ chế Circuit Breaker cho AI Engine (AI Engine Circuit Breaker)
- **Mô tả:** Khi dịch vụ AI bên ngoài liên tục lỗi (timeout hoặc trả về HTTP 5xx), hệ thống phải tự động chuyển sang trạng thái OPEN và trả về giáo án mẫu dự phòng (Fallback Template) thay vì để người dùng chờ vô hạn.
- **Hiện thực hóa kỹ thuật:** Tích hợp thư viện **Resilience4j** vào Spring Boot. Cấu hình CircuitBreaker với tham số: `slidingWindowSize=5`, `failureRateThreshold=50`, `waitDurationInOpenState=60s`.

### [NFR-05] - Kiểm soát Transaction toàn vẹn (Transaction Integrity)
- **Mô tả:** Mọi thao tác ghi dữ liệu liên quan đến nhiều bảng phải được bảo vệ trong cùng một Database Transaction để đảm bảo nguyên lý All-or-Nothing.
- **Hiện thực hóa kỹ thuật:** Áp dụng annotation `@Transactional` đúng tầng Service. Không sử dụng `@Transactional` tại tầng Controller. Chú ý cấu hình `propagation` và `rollbackFor` khi xử lý ngoại lệ kiểm tra (Checked Exception).

---

## 4. Bảo mật (Security)

### [NFR-06] - Bảo vệ JWT Token (JWT Security)
- **Mô tả:** JWT Secret Key phải có độ dài tối thiểu **256-bit** (32 byte), được lưu trữ qua biến môi trường, không được hardcode trong source code hay file cấu hình commit lên Version Control.
- **Hiện thực hóa kỹ thuật:** Đọc giá trị từ biến môi trường thông qua `@Value("${JWT_SECRET}")`. File `.env` phải được liệt kê trong `.gitignore`. Đính kèm file `.env.example` trong repository.

### [NFR-07] - Kiểm soát truy cập tài nguyên (Resource Ownership Control)
- **Mô tả:** Người dùng chỉ được phép đọc, sửa, xóa tài nguyên thuộc chính tài khoản của mình. Nghiêm cấm truy cập ngang hàng (Horizontal Privilege Escalation).
- **Hiện thực hóa kỹ thuật:** Kiểm tra `userId` trong Service Layer bằng cách so sánh với `Principal` từ Security Context. Bổ sung `@PostAuthorize("returnObject.userId == principal.id")` nếu áp dụng tại Repository.

### [NFR-08] - Ngăn chặn Prompt Injection (Prompt Injection Prevention)
- **Mô tả:** Mọi dữ liệu đầu vào từ người dùng trước khi được nhúng vào Prompt gửi đến LLM phải được làm sạch (sanitize) để giảm thiểu nguy cơ Prompt Injection.
- **Hiện thực hóa kỹ thuật:** Triển khai một `PromptSanitizerService` trung tâm; loại bỏ các ký tự đặc biệt và hướng dẫn lồng nhúng (như `\nSystem:`, `Ignore previous instructions`) trước khi xây dựng Prompt.

---

## 5. Khả năng di chuyển & Triển khai (Portability & Deployability)

### [NFR-09] - Container hóa toàn bộ hệ thống (Full System Containerization)
- **Mô tả:** Toàn bộ hệ thống (Backend, Frontend, MySQL) phải có khả năng khởi chạy hoàn toàn bằng lệnh `docker compose up --build` duy nhất mà không cần cài đặt thêm bất kỳ phụ thuộc môi trường cục bộ nào khác.
- **Hiện thực hóa kỹ thuật:**
  - `Dockerfile` (Backend): Multi-stage build (`FROM maven:3.9 AS builder` → `FROM eclipse-temurin:21-jre`) để giảm kích thước image cuối.
  - `Dockerfile` (Frontend): Multi-stage build (`FROM node:20 AS builder` → `FROM nginx:alpine`).
  - `docker-compose.yml`: Định nghĩa 3 service (db, backend, frontend), cấu hình `depends_on`, `healthcheck` và `volume` cho MySQL data persistence. Biến môi trường nhạy cảm được truyền qua file `.env`. **Sử dụng cú pháp Docker Compose V2:** `docker compose up --build` (không có dấu gạch nối).

### [NFR-10] - Dữ liệu mẫu khởi tạo (Seed Data)
- **Mô tả:** Môi trường demo phải được tự động khởi tạo với bộ dữ liệu mẫu đủ để trình diễn toàn bộ luồng nghiệp vụ end-to-end mà không cần nhập liệu thủ công.
- **Nội dung tối thiểu:**
  - 1 tài khoản `admin@gym.com` với quyền `ROLE_ADMIN`.
  - 3–5 tài khoản hội viên mẫu với hồ sơ thể trạng đã điền.
  - 2–3 gói tập mẫu (Gói Cơ Bản 1 tháng, Gói Tiêu Chuẩn 3 tháng, Gói Cao Cấp 6 tháng).
  - 30–50 bài tập gốc phân loại theo nhóm cơ (Chest, Back, Shoulder, Biceps, Triceps, Quads, Hamstrings, Glutes, Core, Cardio).
- **Hiện thực hóa kỹ thuật:** Sử dụng file SQL `init.sql` được mount vào container MySQL qua Docker Volume, hoặc triển khai `DataInitializer` Component trong Spring Boot (`ApplicationRunner`) chỉ chạy khi Database rỗng (idempotent).

---

## 6. Khả năng bảo trì & Kiểm thử (Maintainability & Testability)

### [NFR-11] - Cấu trúc code phân lớp (Layered Architecture Compliance)
- **Mô tả:** Source code Backend phải tuân thủ nghiêm ngặt kiến trúc phân lớp: `Controller → Service → Repository`. Nghiêm cấm gọi Repository trực tiếp từ Controller hoặc chứa logic nghiệp vụ trong Controller.
- **Hiện thực hóa kỹ thuật:** Code review theo tiêu chí: Controller chỉ chứa HTTP mapping và request/response DTO conversion; Service chứa toàn bộ business logic; Repository chỉ chứa Database query.

### [NFR-12] - Tài liệu API tự động (Automated API Documentation)
- **Mô tả:** Toàn bộ REST API endpoint phải được mô tả tự động qua Swagger UI để phục vụ quá trình kiểm thử và trình diễn đồ án.
- **Hiện thực hóa kỹ thuật:** Tích hợp thư viện **SpringDoc OpenAPI** (`springdoc-openapi-starter-webmvc-ui`). Truy cập tại `http://localhost:8080/swagger-ui.html`. Thêm annotation `@Operation` và `@ApiResponse` cho các endpoint quan trọng.

---

## 7. AI Engine — Timeout, Retry và Bảo mật (AI Engine Operational Rules)

### [NFR-13] - AI Timeout và Chính sách Retry (AI Timeout & Retry Policy)
- **Mô tả:** Mọi lần gọi AI Engine được áp dụng giới hạn timeout cứng. Nếu AI không phản hồi trong 15 giây, Backend ngắt kết nối và thực hiện Retry tối đa 1 lần với timeout tiếp theo cũng là 15 giây. Tổng thời gian endpoint không vượt quá **30 giây**.
- **Điều kiện Retry:** Chỉ retry tối đa 1 lần khi gặp timeout, HTTP 429 (Rate Limit của nhà cung cấp), hoặc HTTP 5xx (Server Error). Với lỗi hậu kiểm nghiệp vụ như `exerciseId` ngoài Whitelist hoặc sai JSON Schema, Backend không retry ở tầng HTTP Client; Backend có thể yêu cầu AI tái sinh phản hồi tối đa 1 lần theo luồng validation. Nếu phản hồi tái sinh vẫn không hợp lệ, hệ thống kích hoạt Fallback Template.
- **Hiện thực hóa kỹ thuật:** Cấu hình mỗi attempt có ngân sách tối đa 15 giây, ví dụ `connectTimeout=3s` và `readTimeout=12s`. Bọc toàn bộ endpoint bằng Resilience4j `TimeLimiter` 30 giây; retry thứ hai chỉ chạy khi TimeLimiter còn đủ ngân sách. Sử dụng Resilience4j `Retry` bean với `maxAttempts=2`, `waitDuration=0s`, retry cho `TimeoutException`, HTTP 429 (qua exception chuyên biệt hoặc predicate kiểm tra status code) và HTTP 5xx (`HttpServerErrorException`).

### [NFR-14] - Không ghi dữ liệu nhạy cảm vào Application Log (Sensitive Data Logging Prevention)
- **Mô tả:** Nghiêm cấm ghi các dữ liệu sau vào Application Log (dù ở bất kỳ mức log nào):
  - Mật khẩu dưới mọi dạng (plaintext hoặc hash BCrypt).
  - JWT Access Token hoặc Refresh Token.
  - JWT Secret Key hoặc AI API Key.
  - Toàn bộ hồ sơ y tế / thể chất chi tiết (body profile, injury constraints).
- **Hiện thực hóa kỹ thuật:** Sử dụng Logback `PatternLayout` custom loại bỏ các field nhạy cảm. Đánh dấu các đối tượng DTO nhạy cảm bằng `@JsonIgnore` hoặc tạo `toString()` có che phiếm. Không sử dụng `DEBUG` log toàn bộ request/response body trong môi trường Production.
