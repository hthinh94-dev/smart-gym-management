# Smart Gym Management

Smart Gym Management là đồ án quản lý phòng tập theo luồng Admin và Member. Hệ
thống quản lý tài khoản, hồ sơ hội viên, gói tập, bài tập, giáo án, nhật ký tập
luyện và tiến trình thể trạng; AI chỉ đóng vai trò hỗ trợ tạo đề xuất đã được
Backend kiểm duyệt.

## Trạng thái hiện tại

Dự án đã nghiệm thu **Ngày 5 - Security/JWT Foundation** và hoàn thành phạm vi
local của **Ngày 6 - Register API và React Register flow**:

- Backend foundation bằng Java 21 và Spring Boot 3.4.3.
- 8 Flyway migration tạo schema MVP trên MySQL 8.
- Spring Security stateless, BCrypt strength 12 và JWT Access Token.
- RBAC nền với `ROLE_ADMIN`, `ROLE_MEMBER`, `ROLE_PT`; không có `ROLE_GUEST`.
- `AccountStatusGuard` phân biệt `ACTIVE`, `LOCKED`, `DISABLED`.
- Response lỗi Security chuẩn hóa bằng `ACC-004`, `ACC-005`, `ACC-006` và
  `AUTH-002`.
- Regression backend có 61 test pass, gồm toàn bộ 26 test Ngày 5 và 35 test
  Register/CORS của Ngày 6.
- `POST /api/v1/auth/register` đã có DTO, validation, transaction service,
  `ROLE_MEMBER`, `ACTIVE`, BCrypt, error handler và OpenAPI.
- Frontend Register dùng React Router, React Query, React Hook Form, Zod và Axios;
  6 test Vitest pass và production build thành công.
- Postman collection bao phủ success, duplicate email, password, confirm password,
  invalid email và chống client tự gán role/account status.
- CORS đọc danh sách origin cụ thể từ environment và từ chối wildcard.

Login API, Profile, Membership và AI API chưa được triển khai. `LoginPage` hiện
chỉ là màn hình đích sau Register; kết nối Login API thuộc Ngày 7. Deploy staging
chưa nằm trong phạm vi nghiệm thu local hiện tại. Thứ tự tiếp theo được quản lý tại
[Implementation Plan](./docs/14-implementation-plan.md).

## Công nghệ

| Nhóm | Công nghệ |
| --- | --- |
| Backend | Java 21, Spring Boot 3.4.3, Maven Wrapper |
| API | Spring Web, Bean Validation, SpringDoc OpenAPI 2.8.8 |
| Security | Spring Security, BCrypt, JJWT 0.12.6 |
| Persistence | Spring Data JPA, Hibernate, MySQL 8 |
| Migration | Flyway Core và Flyway MySQL |
| Frontend | React, Vite, React Router, React Query, React Hook Form, Zod, Axios |
| Test | JUnit 5, Mockito, MockMvc, Vitest, Testing Library |

## Kiến trúc

Backend sử dụng **Modular Layered Monolith**. Mỗi module nghiệp vụ tuân theo
luồng `Controller -> Service/Policy -> Repository`; package `common` chỉ chứa
cross-cutting concern.

Luồng request bảo mật hiện tại:

1. `JwtAuthenticationFilter` đọc Bearer token, xác thực chữ ký/hạn dùng và nạp
   identity/roles qua `CustomUserDetailsService`.
2. Spring Security thiết lập `SecurityContext` và áp dụng RBAC.
3. `AccountStatusGuard` cung cấp nền kiểm tra trạng thái tài khoản hiện hành để
   gắn tại endpoint hoặc Method Security; filter JWT không quyết định
   `accountStatus`.
4. Guard nghiệp vụ như `SubscriptionGuard` được bổ sung tại endpoint cần gói tập
   ACTIVE ở milestone tương ứng.

Chi tiết quyết định kỹ thuật nằm trong
[Architecture Decision](./docs/13-architecture-decision.md).

## Cấu trúc repository

```text
smart-gym-management/
|-- backend/                 # Spring Boot API, migration và test
|-- database/
|   `-- schema-draft.sql     # Bản thiết kế DDL tổng hợp để đối chiếu
|-- diagrams/
|   `-- erd-gym-management.mmd
|-- docs/                    # Đặc tả, API, dữ liệu, kiến trúc và tiến độ
|-- frontend/                # React Register/Login shell và test frontend
|-- postman/                 # Collection kiểm thử UC-01 Register
|-- .env.example             # Mẫu biến môi trường cấp repository
`-- README.md
```

Phân loại file:

- `backend/src/**`, `backend/pom.xml`, Maven Wrapper và 8 migration là thành phần
  chạy thực tế của Backend.
- `database/schema-draft.sql` là tài liệu DDL tổng hợp; Flyway migration vẫn là
  nguồn thực thi schema khi ứng dụng chạy.
- `diagrams/erd-gym-management.mmd` và `docs/01` đến `docs/14` là hồ sơ thiết kế
  và truy vết bắt buộc của đồ án.
- Hai file `.env.example` được giữ để hỗ trợ chạy từ project root hoặc thư mục
  `backend`; nội dung phải luôn đồng bộ.
- `frontend/` là workspace React của luồng Register; `postman/` chứa collection
  kiểm thử API có thể chạy tuần tự trên local.
- `.env`, `backend/target/`, log và cấu hình IDE là file cục bộ hoặc sinh tự động,
  không được commit.

## Yêu cầu môi trường

- JDK 21 trở lên, compiler của dự án khóa ở Java release 21.
- MySQL 8 đang chạy và có database `smart_gym_management`.
- User MySQL có quyền tạo/đọc/cập nhật schema để Flyway hoạt động.
- `JWT_SECRET` có tối thiểu 32 byte hiệu lực.

Các biến được hỗ trợ:

| Biến | Bắt buộc | Mô tả |
| --- | --- | --- |
| `DB_HOST` | Không | Mặc định `localhost` |
| `DB_PORT` | Không | Mặc định `3306` |
| `DB_NAME` | Không | Mặc định `smart_gym_management` |
| `DB_URL` | Không | Ghi đè toàn bộ JDBC URL nếu được cấu hình |
| `DB_USER` | Có | Tài khoản MySQL |
| `DB_PASSWORD` | Có | Mật khẩu MySQL |
| `JWT_SECRET` | Có | Khóa ký JWT, tối thiểu 32 byte |
| `JWT_ACCESS_TOKEN_EXPIRATION_MS` | Không | Mặc định `3600000` ms |
| `CORS_ALLOWED_ORIGINS` | Không | Danh sách origin cụ thể, phân tách bằng dấu phẩy; mặc định `http://localhost:5173` |
| `AI_API_KEY` | Chưa dùng | Dành cho milestone AI |

File `.env` bị Git ignore và Spring Boot không tự động nạp file này. Trên
PowerShell, có thể nạp các biến vào process hiện tại từ project root:

```powershell
Get-Content -Encoding UTF8 .env | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]*)=(.*)$') {
        [Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2], 'Process')
    }
}
```

Không commit `.env`, JWT thật, password hoặc API key.

## Build và kiểm thử

Sau khi nạp biến môi trường:

```powershell
cd backend
.\mvnw.cmd clean test
```

Regression backend sau Ngày 6:

```text
Tests run: 61, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Chạy riêng test Ngày 6 trước khi chạy regression toàn bộ:

```powershell
.\mvnw.cmd "-Dtest=AuthServiceTest,AuthControllerTest,WebCorsConfigurationTest,AuthRegistrationIntegrationTest" test
.\mvnw.cmd clean test
```

Snapshot có 26 test invocation nền Ngày 5 và 35 test invocation cho Register/CORS,
tổng 61 test. Flyway validate đủ 8 migration và Hibernate khởi tạo
`EntityManagerFactory` thành công.

Frontend:

```powershell
cd ..\frontend
npm install
npm run test -- --run
npm run build
```

Kết quả xác nhận: 6 test Vitest pass và Vite production build thành công.

## Chạy Backend

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Các endpoint nền:

- Health check: `http://localhost:8080/actuator/health`
- Register: `POST http://localhost:8080/api/v1/auth/register`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## Chạy Frontend

```powershell
cd frontend
npm run dev
```

- Register: `http://localhost:5173/register`
- Login shell: `http://localhost:5173/login`
- API base URL local: `http://localhost:8080/api/v1`

## Tài liệu chính

- [Project Overview](./docs/01-project-overview.md)
- [MVP Scope](./docs/02-mvp-scope.md)
- [Business Rules](./docs/05-business-rules.md)
- [Weekly Progress](./docs/06-weekly-progress.md)
- [Functional Requirements Detail](./docs/08-functional-requirements-detail.md)
- [Use Case Specification](./docs/09-use-case-specification.md)
- [API Draft](./docs/10-api-draft.md)
- [Database Design](./docs/11-database-design.md)
- [Entity Relationship Mapping](./docs/12-entity-relationship-mapping.md)
- [Architecture Decision](./docs/13-architecture-decision.md)
- [Implementation Plan](./docs/14-implementation-plan.md)
