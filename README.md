# Smart Gym Management

Smart Gym Management là đồ án quản lý phòng tập theo luồng Admin và Member. Hệ
thống quản lý tài khoản, hồ sơ hội viên, gói tập, bài tập, giáo án, nhật ký tập
luyện và tiến trình thể trạng; AI chỉ đóng vai trò hỗ trợ tạo đề xuất đã được
Backend kiểm duyệt.

## Trạng thái hiện tại

Dự án đã hoàn tất nghiệm thu local **Milestone M1 - Authentication, Security,
OpenAPI và React Skeleton** ngày 01/08/2026, đã gắn tag `v0.1.0-m1-auth`.
Source **Milestone M2 - Member Profile, Calculator và Body Progress nền** đã
hoàn thành đến hết Ngày 14 (05/08/2026); phần mở rộng hồ sơ và theo dõi thành
phần cơ thể hiện đã được tích hợp local sau M2. Ngày 15 dành cho manual local
QA và đóng tag M2.

Nền M1:

- Backend foundation bằng Java 21 và Spring Boot 3.4.3.
- 8 Flyway migration tạo schema MVP trên MySQL 8.
- Spring Security stateless, BCrypt strength 12 và JWT Access Token.
- RBAC nền với `ROLE_ADMIN`, `ROLE_MEMBER`, `ROLE_PT`; không có `ROLE_GUEST`.
- `AccountStatusGuard` phân biệt `ACTIVE`, `LOCKED`, `DISABLED`.
- Response lỗi Security chuẩn hóa bằng `ACC-004`, `ACC-005`, `ACC-006` và
  `AUTH-002`.
- Gate M1 có 200 Backend test và 43 Frontend test pass.
- `POST /api/v1/auth/register` đã có DTO, validation, transaction service,
  `ROLE_MEMBER`, `ACTIVE`, BCrypt, error handler và OpenAPI.
- `POST /api/v1/auth/login` dùng `AuthenticationManager`, chuẩn hóa email, giữ
  nguyên password, cấp JWT có `sub`, `roles`, `iat`, `exp` và `expiresIn` theo cấu hình.
- `GET /api/v1/users/me` lấy `AuthenticatedUserPrincipal` từ `SecurityContext` và
  dùng `AccountStatusGuard` để chặn token cũ bằng `ACC-004`/`ACC-006`.
- Admin API hỗ trợ danh sách tài khoản có search/filter/pagination và thao tác
  lock/unlock; không cho tự khóa Admin, khóa Admin khác hoặc khóa vì hết hạn gói.
- Swagger/OpenAPI khai báo `bearerAuth` dạng HTTP Bearer JWT để kiểm thử endpoint
  bảo vệ bằng nút `Authorize`; success/error response có schema cụ thể và các
  trường password được đánh dấu `writeOnly`.
- Frontend Register/Login dùng React + TypeScript, React Router, React Query, React Hook Form, Zod,
  Auth Context và Axios; JWT được lưu trong `sessionStorage`, Bearer interceptor
  tự gắn token và `/users/me` xác nhận phiên trước khi cập nhật auth state.
- Frontend có Public Only Route, Protected Route, Role Route, layout Admin/Member
  và trang quản lý tài khoản; phiên cũ được xác minh qua `/users/me`, lỗi
  `ACC-004`/`ACC-005`/`ACC-006` xóa session.
- M1 Postman có 21 request bao phủ Register, Login, `/users/me`,
  `ACC-001`, `ACC-002`, `ACC-004`, `ACC-005`, `ACC-007`, `AUTH-002` và luồng
  Admin list/search/filter/lock/unlock.
- CORS đọc danh sách origin cụ thể từ environment và từ chối wildcard.
- Surefire nạp Mockito `5.14.2` bằng Java agent cố định; `AuthenticationManager`
  dùng `DaoAuthenticationProvider` được wiring tường minh với
  `CustomUserDetailsService` và BCrypt strength 12, không dùng auto-configuration
  ngầm.
- Gate cuối M1 đã xác nhận 6 OpenAPI operation, 21 request trong Postman
  collection và 66 assertion API thật cho Register/Login/Current User/RBAC/Admin
  lock-unlock; dữ liệu test được dọn và subscription không bị thay đổi.

Source M2 hiện tại:

- `GET/PUT /api/v1/member/profile` dùng Principal ownership,
  `AccountStatusGuard`, `PROF-001`, BR-23; hỗ trợ tối đa hai mục tiêu,
  cân nặng mục tiêu, ghi chú hạn chế vận động tự nhập và các lựa chọn phổ biến
  cho dị ứng/thực phẩm loại trừ.
- `BiometricCalculationService` tính BMI, BMR, TDEE, calories và macros bằng
  `Clock`/timezone Việt Nam; calculated targets chỉ trả trong response.
- `GET/POST /api/v1/member/body-progress` lưu lịch sử cân nặng, tùy chọn khối
  lượng cơ/mỡ (kg), chặn ngày tương lai và atomic upsert theo
  `(member_id, record_date)` đúng BR-22.
- Frontend có Profile Form, calculated targets, Body Progress form/history/widget
  và luồng Profile thành công → Progress; lịch sử lấy bản ghi sớm nhất làm cân
  nặng ban đầu để hiển thị tăng/giảm theo baseline; lỗi Progress có retry độc lập.
- Regression hiện tại có 274 Backend test và 80 Frontend test pass; Vite
  production build thành công. OpenAPI hiện có 10 operation M1–M2.
- Postman hiện có 29 request, gồm 21 request M1, 2 request Profile và 6 request
  Body Progress; payload mẫu đã bao phủ mục tiêu kép, cân nặng đích, ghi chú
  mobility và khối lượng cơ/mỡ tùy chọn.

Membership và AI API chưa được triển khai. Deploy staging chưa nằm trong phạm vi
nghiệm thu local hiện tại. Thứ tự tiếp theo được quản lý tại
[Implementation Plan](./docs/14-implementation-plan.md).

## Công nghệ

| Nhóm | Công nghệ |
| --- | --- |
| Backend | Java 21, Spring Boot 3.4.3, Maven Wrapper |
| API | Spring Web, Bean Validation, SpringDoc OpenAPI 2.8.8 |
| Security | Spring Security, BCrypt, JJWT 0.12.6 |
| Persistence | Spring Data JPA, Hibernate, MySQL 8 |
| Migration | Flyway Core và Flyway MySQL |
| Frontend | React, TypeScript, Vite, React Router, React Query, React Hook Form, Zod, Axios |
| Test | JUnit 5, Mockito, MockMvc, Vitest, Testing Library |

## Kiến trúc

Backend sử dụng **Modular Layered Monolith**. Mỗi module nghiệp vụ tuân theo
luồng `Controller -> Service/Policy -> Repository`; package `common` chỉ chứa
cross-cutting concern.

Luồng request bảo mật hiện tại:

1. `JwtAuthenticationFilter` đọc Bearer token, xác thực chữ ký/hạn dùng và nạp
   identity/roles qua `CustomUserDetailsService`.
2. Spring Security thiết lập `SecurityContext` và áp dụng RBAC.
3. `AccountStatusGuard` kiểm tra trạng thái hiện hành tại Current User, Admin,
   Member Profile và Body Progress; filter JWT không quyết định `accountStatus`.
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
|-- frontend/                # React + TypeScript Auth, Admin và Member M1-M2
|-- postman/                 # 29 request Auth/RBAC/Admin/Profile/Progress
|-- .env.example             # Mẫu biến môi trường cấp repository
`-- README.md
```

Phân loại file:

- `backend/src/**`, `backend/pom.xml`, Maven Wrapper và 10 migration là thành phần
  chạy thực tế của Backend.
- `database/schema-draft.sql` là tài liệu DDL tổng hợp; Flyway migration vẫn là
  nguồn thực thi schema khi ứng dụng chạy.
- `diagrams/erd-gym-management.mmd` và `docs/01` đến `docs/14` là hồ sơ thiết kế
  và truy vết bắt buộc của đồ án.
- Hai file `.env.example` được giữ để hỗ trợ chạy từ project root hoặc thư mục
  `backend`; nội dung phải luôn đồng bộ.
- `frontend/` là workspace React + TypeScript cho Auth, Member và Admin shell; `postman/` chứa collection
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

Nạp biến môi trường vào đúng PowerShell process trước khi chạy Backend. Spring
Boot không tự đọc file `.env`:

```powershell
cd "F:\DO AN TOT NGHIEP\Smart-gym-management"
Get-Content -Encoding UTF8 .env | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]*)=(.*)$') {
        [Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2], 'Process')
    }
}

cd backend
.\mvnw.cmd clean test
```

Regression backend hiện tại sau khi hoàn tất source Ngày 14:

```text
Tests run: 274, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Chạy riêng test M2 trước khi chạy regression toàn bộ:

```powershell
.\mvnw.cmd "-Dtest=MemberProfileTest,MemberProfileServiceTest,MemberProfileControllerTest,MemberProfileIntegrationTest,BiometricCalculationServiceTest,BodyProgressTest,BodyProgressRepositoryTest,BodyProgressServiceTest,BodyProgressControllerTest,BodyProgressIntegrationTest" test
.\mvnw.cmd clean test
```

Toàn bộ test M1 tiếp tục pass. Flyway validate đủ 10 migration hiện hành và
Hibernate khởi tạo `EntityManagerFactory` thành công trên MySQL 8.0.44.

Frontend:

```powershell
cd ..\frontend
npm install
npm run test -- --run
npm run build
```

Kết quả xác nhận hiện tại: 80 test Vitest pass và Vite production build thành công.

Gate M1 local ngày 01/08/2026 cũng xác nhận Frontend phục vụ được các route
`/login`, `/register`, `/member`, `/admin/users`; API health `UP`, OpenAPI có đúng
6 operation và full flow Auth/RBAC/Admin pass 66 assertion. M1 không deploy web.

## Chạy Backend

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Các endpoint nền:

- Health check: `http://localhost:8080/actuator/health`
- Register: `POST http://localhost:8080/api/v1/auth/register`
- Login: `POST http://localhost:8080/api/v1/auth/login`
- Current user: `GET http://localhost:8080/api/v1/users/me`
- Admin users: `GET http://localhost:8080/api/v1/admin/users`
- Lock user: `PATCH http://localhost:8080/api/v1/admin/users/{id}/lock`
- Unlock user: `PATCH http://localhost:8080/api/v1/admin/users/{id}/unlock`
- Member profile: `GET/PUT http://localhost:8080/api/v1/member/profile`
- Body progress: `GET/POST http://localhost:8080/api/v1/member/body-progress`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## Chạy Frontend

```powershell
cd frontend
npm run dev
```

- Register: `http://localhost:5173/register`
- Login: `http://localhost:5173/login`
- Member home: `http://localhost:5173/member`
- Member profile: `http://localhost:5173/member/profile`
- Member progress: `http://localhost:5173/member/progress`
- Admin users: `http://localhost:5173/admin/users`
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
- [Graduation Report Diagram Specification](./docs/15-graduation-report-diagram-specification.md)
- [Graduation Report Draft through Day 14](./docs/graduation-report-draft-through-day-14.md)
