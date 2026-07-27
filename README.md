# Smart Gym Management

Smart Gym Management là đồ án quản lý phòng tập theo luồng Admin và Member. Hệ
thống quản lý tài khoản, hồ sơ hội viên, gói tập, bài tập, giáo án, nhật ký tập
luyện và tiến trình thể trạng; AI chỉ đóng vai trò hỗ trợ tạo đề xuất đã được
Backend kiểm duyệt.

## Trạng thái hiện tại

Dự án đã hoàn thành kế hoạch đến **Ngày 5 - Security/JWT Foundation**:

- Backend foundation bằng Java 21 và Spring Boot 3.4.3.
- 8 Flyway migration tạo schema MVP trên MySQL 8.
- Spring Security stateless, BCrypt strength 12 và JWT Access Token.
- RBAC nền với `ROLE_ADMIN`, `ROLE_MEMBER`, `ROLE_PT`; không có `ROLE_GUEST`.
- `AccountStatusGuard` phân biệt `ACTIVE`, `LOCKED`, `DISABLED`.
- Response lỗi Security chuẩn hóa bằng `ACC-004`, `ACC-005`, `ACC-006` và
  `AUTH-002`.
- 26 test đang pass, gồm unit test Security/JWT và Spring context integration.

Register, Login, Profile, Membership, AI API và frontend nghiệp vụ chưa được
triển khai ở Ngày 5. Thứ tự tiếp theo được quản lý tại
[Implementation Plan](./docs/14-implementation-plan.md).

## Công nghệ

| Nhóm | Công nghệ |
| --- | --- |
| Backend | Java 21, Spring Boot 3.4.3, Maven Wrapper |
| API | Spring Web, Bean Validation, SpringDoc OpenAPI 2.8.8 |
| Security | Spring Security, BCrypt, JJWT 0.12.6 |
| Persistence | Spring Data JPA, Hibernate, MySQL 8 |
| Migration | Flyway Core và Flyway MySQL |
| Test | JUnit 5, Mockito, Spring Security Test, MockMvc |

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
|-- frontend/                # Placeholder cho React ở milestone tiếp theo
|-- postman/                 # Placeholder cho collection kiểm thử API
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
- `frontend/.gitkeep` và `postman/.gitkeep` là placeholder hợp lệ cho các giai
  đoạn chưa triển khai.
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

Baseline sau Ngày 5:

```text
Tests run: 26, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Flyway phải validate đủ 8 migration và Hibernate phải khởi tạo
`EntityManagerFactory` thành công.

## Chạy Backend

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Các endpoint nền:

- Health check: `http://localhost:8080/actuator/health`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

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
