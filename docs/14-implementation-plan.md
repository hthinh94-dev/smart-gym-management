# 14. Kế hoạch thực thi từ Ngày 5 đến khi đóng đồ án

## 1. Mục đích

Tài liệu này chuẩn hóa kế hoạch triển khai từ **Ngày 5 - 27/07/2026** đến khi đóng đồ án vào **31/08/2026**. Kế hoạch tăng tốc được chốt ngày 03/08/2026 sau khi M1 đã hoàn thành local, bỏ Ngày 11 (02/08), tăng khối lượng công việc mỗi ngày và giữ nguyên các quality gate bắt buộc.

Nguồn đối chiếu chính:

- `01-project-overview.md`: mục tiêu đề tài và định hướng sản phẩm.
- `02-mvp-scope.md`: phạm vi Must-have, Should-have và Won't-have.
- `03-functional-requirements.md`: AI Hybrid, whitelist, schema và fallback.
- `04-non-functional-requirements.md`: hiệu năng, bảo mật, Docker, SpringDoc, Resilience4j.
- `05-business-rules.md`: ràng buộc nghiệp vụ, error code và warning code.
- `06-weekly-progress.md`: hiện trạng triển khai và kết quả kiểm thử.
- `07-actors-and-roles.md`: RBAC và vai trò Admin/Member/PT.
- `08-functional-requirements-detail.md`: yêu cầu chức năng chi tiết.
- `09-use-case-specification.md`: các Use Case cốt lõi.
- `10-api-draft.md`: API contract và response format.
- `11-database-design.md`: schema, constraints và indexes.
- `12-entity-relationship-mapping.md`: quy tắc ánh xạ JPA.
- `13-architecture-decision.md`: Modular Layered Monolith và quy ước hiện thực.

## 2. Baseline trước Ngày 12

| Hạng mục | Trạng thái đã chốt |
| --- | --- |
| M1 | Hoàn thành local ngày 01/08; Backend 200 test, Frontend 43 test và production build pass; tag `v0.1.0-m1-auth`. |
| Kiến trúc | Modular Layered Monolith, package theo module nghiệp vụ dưới `com.thinh.smartgym`. |
| Database | MySQL 8, Flyway sở hữu DDL; không sửa migration đã áp dụng. |
| Bảo mật | JWT stateless, RBAC, `AccountStatusGuard`, Protected/Public Only/Role Route đã hoạt động. |
| Frontend | Register, Login, Auth Context, Member/Admin shell và Admin User Management đã kết nối API thật. |
| Deploy | Không deploy riêng M1; deploy theo cụm sau M3, sau M6 và bản final M7. |
| Ngày 11 | **Bỏ Ngày 11 - 02/08/2026**; phần chuẩn bị M2 được nhập vào Ngày 12. |
| Phạm vi PT | Không làm module PT đầy đủ. Chỉ bổ sung phạm vi đọc thời hạn gói và cảnh báo 30 ngày cho hội viên được phân công, theo yêu cầu nghiệp vụ mới. |

## 3. Quy tắc triển khai xuyên suốt

| Mã | Quy tắc bắt buộc |
| --- | --- |
| R1 | Không sửa migration Flyway đã áp dụng; thay đổi schema phải tạo migration mới. |
| R2 | Controller chỉ nhận/trả DTO; không serialize JPA Entity ra API. |
| R3 | Transaction đặt tại Service; Controller không chứa business rule hoặc gọi Repository trực tiếp. |
| R4 | Tài nguyên cá nhân lấy `memberId` từ Principal/Security Context; không tin ID do Client gửi. |
| R5 | Response tuân thủ `ApiResponse`, `ErrorResponse`, `PageResponse` và Error Code Registry File 05. |
| R6 | Swagger/OpenAPI và Postman được cập nhật khi tạo endpoint mới. |
| R7 | Backend sở hữu BMI, BMR, TDEE, calories và macros; AI không được tự tính các giá trị chính thức. |
| R8 | AI và fallback đều phải qua JSON schema, whitelist và business validator dùng chung. |
| R9 | Recommendation, kích hoạt plan và ghi workout log phải kiểm tra subscription hợp lệ động. |
| R10 | Ngày code phải viết test cùng chức năng và chỉ chạy targeted unit/integration test cho phần vừa thay đổi; không bắt buộc full regression, production build hoặc test thủ công localhost. |
| R11 | Ngày cuối mỗi milestone được dành riêng cho full local QA và fix: chạy toàn bộ Backend test, Frontend test/build, kiểm thử thủ công localhost, sửa lỗi rồi chạy lại đến khi pass. |
| R12 | Deploy web theo cụm: M1-M3 ngày 12/08, M4-M6 ngày 27/08, final ngày 31/08. |
| R13 | Không làm mobile app hoặc tối ưu giao diện mobile; chỉ kiểm tra desktop/laptop. |
| R14 | Không chuyển sang milestone tiếp theo nếu ngày local QA/fix chưa pass; mỗi milestone phải có tài liệu, commit sạch, demo local và tag phiên bản. |
| R15 | Không thêm Should-have mới ngoài Landing Page và PT expiry view đã được chốt trong kế hoạch tăng tốc. |

## 4. Tổng quan milestone tăng tốc

| Milestone | Thời gian mới | Trọng tâm | Kết quả demo bắt buộc | Tag |
| --- | --- | --- | --- | --- |
| M1 | 27/07 - 01/08 - Hoàn thành local | Authentication, Security, OpenAPI, React shell | Register, Login, JWT, RBAC, khóa/mở khóa | `v0.1.0-m1-auth` |
| M2 | **03/08 - 06/08** | Member Profile, Calculator, Body Progress nền | Cập nhật profile, tính BMI/BMR/TDEE/macros, ghi cân nặng | `v0.2.0-m2-profile` |
| M3 | **07/08 - 11/08** | Landing Page, Membership, Renewal, SubscriptionGuard, expiry reminder | Guest xem Landing/gói; Member đăng ký/gia hạn; Admin duyệt; Admin/PT thấy thời hạn | `v0.3.0-m3-membership` |
| Deploy 1 | **12/08** | Deploy và regression M1-M3 | Full flow Landing/Auth/Profile/Membership pass trên web | `deploy-m1-m3` |
| M4 | **13/08 - 17/08** | Exercise Library và Workout Plan core | Exercise CRUD, seed, plan activation, fallback nền | `v0.4.0-m4-exercise-workout` |
| M5 | **18/08 - 22/08** | AI Hybrid Recommendation | AI/fallback tạo DRAFT plan, validator và whitelist hoạt động | `v0.5.0-m5-ai-recommendation` |
| M6 | **23/08 - 26/08** | Workout Log và Progress Analytics | Ghi log, lịch sử và biểu đồ tiến trình | `v0.6.0-m6-progress` |
| Deploy 2 | **27/08** | Deploy và regression M4-M6 | Full Member journey M1-M6 pass trên web | `deploy-m4-m6` |
| M7 | **28/08 - 31/08** | Docker, hardening, tài liệu, slide, demo và đóng gói | Bản final chạy được, báo cáo/slide/video/backups hoàn chỉnh | `v1.0.0-final` |

## 5. Kế hoạch chi tiết theo ngày

### M1 - Authentication, Security, OpenAPI và React shell

M1 đã hoàn thành local ngày 01/08/2026. Giữ nguyên toàn bộ kết quả, test và tag đã ghi trong `06-weekly-progress.md`. Không deploy riêng M1.

### M2 - Member Profile, Calculator và Body Progress nền

| Ngày | Trọng tâm | Backend | Frontend | Test/QA và đầu ra |
| --- | --- | --- | --- | --- |
| **Ngày 12 - 03/08** | Profile persistence | Chốt `PROF-001`; tạo enum, `MemberProfile`, repository, DTO, mapper và `GET /api/v1/member/profile`; ownership theo Principal; không trả Entity | Dành thời gian tự thiết kế Profile desktop/laptop; tạo Profile shell, route và empty state `PROF-001` | Viết và chạy targeted entity/repository/service/controller test; chưa chạy full M1 regression hoặc manual localhost |
| **Ngày 13 - 04/08** | Profile update và Calculator | `PUT /api/v1/member/profile` upsert; BR-23; sanitize collection; `BiometricCalculationService` cho BMI/BMR/TDEE/calories/macros | Form profile đầy đủ và khu vực `calculatedTargets`; loading/error/success state | Targeted test create/update, validation và toàn bộ công thức/boundary; chưa chạy full local QA |
| **Ngày 14 - 05/08** | Body Progress và tích hợp M2 | `POST /api/v1/member/body-progress`; atomic upsert theo `(member_id, record_date)`; Clock; OpenAPI và Postman | Widget cân nặng, hoàn thiện Profile desktop/laptop và kết nối GET/PUT/Progress | Targeted test upsert, ngày tương lai, không trùng dòng; hoàn thiện source M2 trước ngày QA |
| **Ngày 15 - 06–07/08** | **Local QA và fix M2** | Không thêm phân hệ mới; chạy full Backend regression M1-M2, kiểm tra Flyway/MySQL, sửa lỗi rồi chạy lại | Chạy full Frontend test/build; hoàn thiện Profile/Calculator/Body Progress trên localhost, gồm baseline cân nặng, khoảng cách mục tiêu và thành phần cơ/mỡ | Dành trọn ngày test/fix; cập nhật docs/ảnh/Postman; tag `v0.2.0-m2-profile` chỉ khi tất cả pass |

**Trạng thái đến cuối Ngày 14:** source M2 đã hoàn thành và nối với nền M1 qua
`AuthenticatedUserPrincipal`, RBAC, `AccountStatusGuard`, response/error
contract và Axios Bearer interceptor. Full regression đạt 271 Backend test và
75 Frontend test; production build pass; Postman có 29 request. Manual localhost
toàn M2 và tag `v0.2.0-m2-profile` vẫn là gate Ngày 15, chưa được ghi nhận hoàn
thành sớm.

**Trạng thái Ngày 15 (07/08/2026):** gate local M2 đã hoàn tất. Backend đạt
274/274 test, Flyway validate 10 migration trên schema version 10 và Hibernate
validate thành công. Frontend đạt 80/80 test, TypeScript và Vite production
build pass. Manual Profile/Calculator/Body Progress, Auth/RBAC, Swagger và
Postman đã được xác nhận thành công; không còn lỗi P0/P1. M2 đủ điều kiện commit,
push và gắn tag `v0.2.0-m2-profile`.

### M3 - Landing Page, Membership và SubscriptionGuard

| Ngày | Trọng tâm | Backend | Frontend | Test/QA và đầu ra |
| --- | --- | --- | --- | --- |
| **Ngày 16 - 07/08** | Landing Page và Package catalog | Package entity/repository/service; public GET; Admin CRUD, normalize name và soft inactive | Dành 2 giờ tự thiết kế Landing; tạo `/`, visual hero, package preview, CTA Login/Register và Admin Package UI | Targeted Package API/component tests; không chạy full local QA |
| **Ngày 17 - 08/08** | New subscription | Tạo/current subscription; package snapshot; chặn ACTIVE/PENDING trùng | Member chọn gói, xem trạng thái PENDING/ACTIVE và current subscription | Targeted BR-04, `SUB-004`, `SUB-006`, ownership và inactive package tests |
| **Ngày 18 - 09/08** | Approval, cancel và renewal | Admin list/approve/cancel; lock theo Member; `@Version`; thiết lập ngày; renewal request/approve và cộng dồn `endDate` | Admin duyệt/hủy; Member xem ngày và gửi gia hạn | Targeted transition, concurrency, renewal trùng và package mismatch tests |
| **Ngày 19 - 10/08** | Guard, expiry reminder và tích hợp M3 | `SubscriptionGuard`; query `1..30` ngày; quan hệ PT-Member tối thiểu; rà OpenAPI/Postman/query | Admin expiry list; PT read-only assigned member; hoàn thiện Landing/Membership desktop/laptop | Targeted guard, mốc 30/1/0 ngày và PT ownership tests; hoàn thiện source M3 |
| **Ngày 20 - 11/08** | **Local QA và fix M3** | Không thêm feature; full Backend regression M1-M3; Flyway, scheduler và query verification; sửa lỗi | Full Frontend test/build; test thủ công Landing/Auth/Profile/Membership/Admin/PT trên localhost; sửa lỗi | Dành trọn ngày test/fix; cập nhật docs/ảnh/Postman; tag `v0.3.0-m3-membership` khi pass |

**Kết quả thực tế Ngày 16 (07/08/2026):** Landing Page, public Package API,
Admin Package CRUD, normalize tên, soft inactive và targeted verification đã
hoàn thành. Backend đạt 29/29 targeted tests; Frontend đạt 21/21 targeted tests
và Vite production build pass. Full local QA M1-M3, Subscription lifecycle và
Deploy Gate vẫn chưa được tính là hoàn thành và tiếp tục theo các Ngày 17-21.

### Deploy Gate 1 - M1 đến M3

| Ngày | Công việc | Gate bắt buộc |
| --- | --- | --- |
| **Ngày 21 - 12/08** | Deploy Backend, Frontend và database; cấu hình environment, CORS, JWT, Flyway; test Landing/Auth/Profile/Package/Subscription/Renewal/expiry reminder | Full flow M1-M3 pass trên web; không còn lỗi P0/P1; chỉ sau đó mới chuyển M4 |

### M4 - Exercise Library và Workout Plan core

| Ngày | Trọng tâm | Backend | Frontend | Test/QA và đầu ra |
| --- | --- | --- | --- | --- |
| **Ngày 22 - 13/08** | Exercise persistence và API | `Exercise`, metadata, seed 30-50 bài; list/detail/Admin CRUD/search/filter/page/sort | Tạo Exercise Library và Admin Exercise nền | Targeted Flyway/seed/API/soft inactive tests |
| **Ngày 23 - 14/08** | Exercise UI và Workout Plan persistence | Hoàn thiện Exercise query; tạo `WorkoutPlan`, `WorkoutDay`, `WorkoutPlanExercise`, current endpoint | Hoàn thiện Exercise UI; tạo Current Workout Plan shell | Targeted repository/service/controller tests; chưa chạy full local QA |
| **Ngày 24 - 15/08** | Plan activation | Activation transaction, ownership, archive ACTIVE cũ và chỉ một ACTIVE | Preview/current/activate plan flow | Targeted lifecycle, rollback và concurrency tests |
| **Ngày 25 - 16/08** | Fallback và tích hợp M4 | Rule-based generator, whitelist và planned-value validator; OpenAPI/query review | Preview fallback; hoàn thiện Exercise/Plan desktop/laptop | Targeted whitelist/day count/injury/no-duplicate tests; hoàn thiện source M4 |
| **Ngày 26 - 17/08** | **Local QA và fix M4** | Full Backend regression M1-M4, Flyway/seed/N+1 review; sửa lỗi và chạy lại | Full Frontend test/build; test thủ công Exercise CRUD/Plan activation/fallback localhost | Dành trọn ngày test/fix; docs/ảnh/Postman; tag `v0.4.0-m4-exercise-workout` |

### M5 - AI Hybrid Recommendation

| Ngày | Trọng tâm | Backend | Frontend | Test/QA và đầu ra |
| --- | --- | --- | --- | --- |
| **Ngày 27 - 18/08** | Pipeline và AI port | Recommendation pre-check; `AiRecommendationClient` và provider adapter | Recommendation shell, disclaimer và loading nền | Targeted `SUB-001`, profile completeness và port tests |
| **Ngày 28 - 19/08** | Prompt và resilience | Sanitizer, prompt builder, JSON schema, timeout/retry/circuit breaker | Loading 30s và chống submit trùng | Targeted sanitizer/allowlist/secret tests |
| **Ngày 29 - 20/08** | Validator và fallback | Validator dùng chung, tái sinh tối đa một lần, warning code và fallback | Preview nguồn AI/fallback và warning state | Targeted timeout/429/5xx/invalid payload/whitelist tests |
| **Ngày 30 - 21/08** | Persist và frontend integration | Transaction lưu DRAFT plan/recommendation/nutrition; latest endpoint; OpenAPI | Generate, preview, targets, warning, latest và activate | Targeted rollback/`AI-001`/component integration tests; hoàn thiện source M5 |
| **Ngày 31 - 22/08** | **Local QA và fix M5** | Full Backend regression M1-M5 với provider mock success/timeout/invalid; sửa lỗi | Full Frontend test/build; local E2E Profile + Subscription + Generate + Activate | Dành trọn ngày test/fix; docs/ảnh/Postman; tag `v0.5.0-m5-ai-recommendation` |

### M6 - Workout Log và Progress Analytics

| Ngày | Trọng tâm | Backend | Frontend | Test/QA và đầu ra |
| --- | --- | --- | --- | --- |
| **Ngày 32 - 23/08** | Workout Log và history | Create/update-in-place log, session theo ngày, guard, history endpoints và inactive exercise projection | Form log, lịch sử và filter ngày | Targeted BR-09B/BR-19/BR-28, ownership, pagination và history tests |
| **Ngày 33 - 24/08** | Progress analytics API | Body Progress read, ISO-week frequency và max weight theo exercise/date | Biểu đồ cân nặng/tần suất/mức tạ nền | Targeted grouping/timezone/ownership/empty-data tests |
| **Ngày 34 - 25/08** | Dashboard và tích hợp M6 | Tối ưu query, OpenAPI và Postman | Hoàn thiện Member Dashboard, charts và full journey UI | Targeted API/component tests; hoàn thiện source M6 |
| **Ngày 35 - 26/08** | **Local QA và fix M6** | Full Backend regression M1-M6; kiểm tra query, guard, history khi hết hạn; sửa lỗi | Full Frontend test/build; local E2E toàn Member journey; sửa lỗi | Dành trọn ngày test/fix; docs/ảnh/Postman; tag `v0.6.0-m6-progress` |

### Deploy Gate 2 - M4 đến M6

| Ngày | Công việc | Gate bắt buộc |
| --- | --- | --- |
| **Ngày 36 - 27/08** | Deploy M4-M6; regression toàn hệ thống; test AI success/fallback và Member journey | Landing -> Register -> Profile -> Subscription -> Recommendation -> Activate -> Log -> Progress pass trên web |

### M7 - Hardening, Docker, báo cáo và đóng gói

| Ngày | Trọng tâm | Công việc bắt buộc | Gate |
| --- | --- | --- | --- |
| **Ngày 37 - 28/08** | Docker và seed demo | Dockerfile Backend/Frontend, `docker-compose.yml`, MySQL volume/healthcheck, `.env.example`, seed Admin/Member/PT/package/exercise/demo assignment | `docker compose up --build` chạy từ môi trường sạch |
| **Ngày 38 - 29/08** | Tài liệu và chuẩn bị nghiệm thu | Cập nhật README, Swagger, Postman, ERD/API, screenshot, Chương 4/5/Kết luận, slide/video và checklist demo | Tài liệu đủ tái chạy; chưa thực hiện full local QA final |
| **Ngày 39 - 30/08** | **Full local QA/fix final và Feature Freeze** | Chạy toàn bộ unit/integration/E2E và Docker Compose local; chỉ sửa lỗi P0/P1, security, data hoặc build; rehearsal demo | Dành trọn ngày test/fix; không thêm feature; demo local chạy liên tục và bản trình diễn ổn định |
| **Ngày 40 - 31/08** | Final deploy và đóng đồ án | Deploy final, smoke test, xuất PDF báo cáo, backup source/database/video/Postman, tạo tag `v1.0.0-final` | Sẵn sàng nộp và bảo vệ trong tháng 8 |

## 6. Quality gate bắt buộc

| Gate | Áp dụng | Điều kiện đạt |
| --- | --- | --- |
| Coding Day Check | Ngày phát triển feature | Viết test cùng code và chạy targeted unit/integration/component test cho phần vừa thay đổi; không yêu cầu full regression hoặc manual localhost. |
| Module Local QA | Ngày 06, 11, 17, 22, 26 và 30/08 | Dành trọn ngày chạy full Backend regression, Frontend test/build, manual localhost và fix; chạy lại toàn bộ sau mỗi lỗi ảnh hưởng rộng. |
| API Contract | Endpoint mới | Prefix `/api/v1`, DTO đúng File 10, error/warning code đúng File 05, OpenAPI cập nhật. |
| Security | Endpoint bảo vệ | JWT, role, AccountStatus, subscription và ownership được kiểm tra đúng tầng. |
| Database | Persistence | Entity/Repository khớp Flyway; migration mới có test; không sửa migration cũ. |
| Landing | M3 | Guest thấy Landing trước Login/Register; authenticated user được chuyển dashboard; package API có loading/error/empty. |
| PT expiry | M3 | Admin thấy toàn bộ gói sắp hết hạn; PT chỉ thấy assigned member; cảnh báo khi `1 <= daysRemaining <= 30`. |
| AI | M5 trở đi | AI/fallback qua validator chung; timeout/invalid có fallback; không lưu dữ liệu một phần. |
| Deploy 1 | 12/08 | M1-M3 pass trên web trước khi bắt đầu M4. |
| Deploy 2 | 27/08 | M1-M6 pass trên web trước khi đóng M7. |
| Final | 31/08 | Docker, deploy, báo cáo, slide, video, backup và tag final hoàn chỉnh. |

## 7. Kiểm thử, báo cáo và Git theo milestone

### Ngày code

- Viết unit/integration/component test cùng chức năng, không dồn việc viết test sang ngày QA.
- Chỉ chạy test mục tiêu cho package, service, controller hoặc component vừa sửa.
- Có thể compile/build phần liên quan khi cần, nhưng không bắt buộc chạy full regression hoặc kiểm thử thủ công localhost.
- Commit và push phần source đã hoàn thành để tránh dồn thay đổi lớn vào ngày QA.

### Ngày local QA và fix cuối milestone

```powershell
cd backend
.\mvnw.cmd clean test

cd ..\frontend
npm.cmd test
npm.cmd run build
```

Sau full automation, khởi động Backend/Frontend và kiểm thử thủ công trên localhost toàn bộ module vừa hoàn thành cùng regression module cũ. Ngày QA không thêm feature mới; chỉ sửa bug, chạy lại test và hoàn thiện tài liệu/tag. Chỉ deploy ở ba gate đã quy định.

### Phần báo cáo cần cập nhật theo milestone

- M2: Chương 3 - thiết kế Profile/Calculator; Chương 4 - hiện thực và kiểm thử M2.
- M3: Chương 3 - Subscription lifecycle/Guard; Chương 4 - Landing Page, Membership và expiry reminder.
- M4: Chương 3 - Exercise/Workout domain; Chương 4 - hiện thực và kiểm thử M4.
- M5: Chương 3 - AI Hybrid/validator/fallback; Chương 4 - kết quả AI và fallback.
- M6: Chương 3 - Workout Log/Progress aggregation; Chương 4 - biểu đồ và full Member journey.
- M7: hoàn thiện Chương 4, Chương 5, Kết luận, phụ lục, slide và kịch bản demo.

### Git

- Chỉ `git add` đúng file thuộc nhiệm vụ; không dùng `git add .` khi còn file ngoài phạm vi.
- Tách commit Backend, Frontend và docs/test khi thay đổi đủ lớn.
- Push `main` sau mỗi nhóm source hoàn chỉnh; tạo tag chỉ sau ngày module local QA/fix vượt gate.
- Không commit `.env`, credential, JWT secret, AI key, log, `target/` hoặc `dist/`.

## 8. Nội dung không triển khai trước khi đóng MVP

| Không làm | Lý do |
| --- | --- |
| Refresh token/OAuth2 login | Không có trong phạm vi API hiện tại; tăng bề mặt bảo mật. |
| Payment gateway thật | MVP chỉ dùng luồng Admin xác nhận mô phỏng. |
| Module PT đầy đủ | Chỉ làm read-only expiry view cho assigned member; không làm dashboard/plan/chat PT. |
| Nutrition log chi tiết | Không nằm trong schema MVP. |
| Mobile app hoặc tối ưu mobile | Ngoài phạm vi; chỉ desktop/laptop. |
| Realtime chat, IoT, face recognition | Won't-have. |
| Microservice, Kafka/RabbitMQ, CQRS/Event Sourcing | Trái quyết định Modular Layered Monolith. |

## 9. Cảnh báo tiến độ

- Lịch mới yêu cầu trung bình **8-10 giờ làm việc tập trung mỗi ngày** và gần như không còn buffer.
- Nếu M2 chưa đóng ngày 06/08, phải cắt polish không bắt buộc để bảo vệ Deploy Gate 1 ngày 12/08.
- AI key/provider và môi trường deploy phải sẵn sàng trước ngày 18/08; không chờ đến M5/M7 mới chuẩn bị.
- Nếu trễ quá một ngày, chỉ được giảm trang trí hoặc Should-have chưa chốt; không cắt security, ownership, validation, test, Landing Page, expiry reminder hoặc luồng MVP chính.
- Feature Freeze bắt buộc từ ngày 30/08; ngày 31/08 chỉ dành cho final deploy, smoke test và đóng gói.
