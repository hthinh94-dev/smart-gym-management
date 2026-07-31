# 14. Kế hoạch thực thi từ Ngày 5 đến khi đóng đồ án

## 1. Mục đích

Tài liệu này chuẩn hóa kế hoạch triển khai từ **Ngày 5 - 27/07/2026** đến khi đóng đồ án vào **13/09/2026**. Kế hoạch được lập sau khi đối chiếu các tài liệu `01` đến `13`, giữ đúng phạm vi MVP 9 tuần và hiện trạng đã hoàn thành ở Ngày 4.

Nguồn đối chiếu chính:

- `01-project-overview.md`: mục tiêu đề tài và định hướng sản phẩm.
- `02-mvp-scope.md`: phạm vi Must-have, Should-have và Won't-have.
- `03-functional-requirements.md`: AI Hybrid, whitelist, schema và fallback.
- `04-non-functional-requirements.md`: hiệu năng, bảo mật, Docker, SpringDoc, Resilience4j.
- `05-business-rules.md`: ràng buộc nghiệp vụ, error code và warning code.
- `06-weekly-progress.md`: hiện trạng sau Ngày 4.
- `07-actors-and-roles.md`: RBAC và vai trò Admin/Member/PT.
- `08-functional-requirements-detail.md`: 44 FR.
- `09-use-case-specification.md`: 10 Use Case cốt lõi.
- `10-api-draft.md`: 32 API contract và response format.
- `11-database-design.md`: 25 bảng vật lý, constraints và indexes.
- `12-entity-relationship-mapping.md`: 16 Entity và 9 `@ElementCollection`.
- `13-architecture-decision.md`: Modular Layered Monolith và quy ước hiện thực.

## 2. Baseline trước Ngày 5

| Hạng mục | Trạng thái đã chốt |
| --- | --- |
| Kiến trúc | Modular Layered Monolith, package theo module nghiệp vụ dưới `com.thinh.smartgym`. |
| Database | MySQL 8, Flyway sở hữu DDL, 25 bảng vật lý đã chia migration theo module. |
| JPA | Hibernate chỉ `validate`, `open-in-view=false`, auditing đã bật. |
| Auth persistence | Đã có `User`, `Role`, `UserRole`, `UserRoleId`, repository Auth và seed `ROLE_ADMIN`, `ROLE_MEMBER`, `ROLE_PT`. |
| Bảo mật | JWT stateless; `JwtAuthenticationFilter` xác thực chữ ký/hạn dùng và nạp identity/roles nhưng không quyết định `accountStatus`; trạng thái tài khoản được kiểm tra bằng `AccountStatusGuard`/Method Security. |
| Frontend | Register, Login, auth state, Protected Route và phân layout Admin/Member đã kết nối API thật, pass test/build; M1 tiếp tục hardening và hoàn thiện auth shell. |
| Phạm vi | MVP chỉ yêu cầu luồng Admin và Member; PT, payment thật, refresh token, mobile app, IoT, chat realtime là ngoài phạm vi giai đoạn này. |

## 3. Quy tắc triển khai xuyên suốt

| Mã | Quy tắc bắt buộc |
| --- | --- |
| R1 | Không sửa migration Flyway đã áp dụng; thay đổi schema phải tạo migration mới. |
| R2 | Controller chỉ nhận/trả DTO; không serialize JPA Entity ra API. |
| R3 | Transaction đặt tại Service; Controller không chứa business rule hoặc gọi Repository trực tiếp. |
| R4 | Mọi tài nguyên cá nhân lấy `memberId` từ Principal/Security Context, không tin `memberId` do Client gửi. |
| R5 | Response tuân thủ `ApiResponse`, `ErrorResponse`, `PageResponse` và Error Code Registry ở File 05. |
| R6 | Swagger/OpenAPI bằng SpringDoc được cập nhật khi tạo controller mới. |
| R7 | Backend sở hữu BMI, BMR, TDEE, calories và macros; AI chỉ tạo `workoutSchedule` và `nutritionPlan.mealStructure`. |
| R8 | Recommendation/fallback đều phải qua cùng hậu kiểm: JSON schema, whitelist, planned values, số ngày tập, số bữa ăn và dietary constraints. |
| R9 | Các endpoint cao cấp gồm tạo recommendation, kích hoạt plan và ghi workout log phải kiểm tra subscription hợp lệ theo `status = ACTIVE`, `startDate <= today < endDate`. |
| R10 | Mỗi milestone chỉ được đóng khi có test, tài liệu cập nhật, demo được và tag phiên bản. |

## 4. Tổng quan milestone

| Milestone | Thời gian | Trọng tâm | FR/UC chính | Kết quả demo bắt buộc | Tag |
| --- | --- | --- | --- | --- | --- |
| M1 | 27/07 - 02/08 | Authentication, Security, OpenAPI và React skeleton | FR-AUTH, FR-ADMIN-02, UC-01, UC-02, UC-10 | Register, Login, JWT, RBAC, khóa/mở khóa tài khoản, React auth flow | `v0.1.0-m1-auth` |
| M2 | 03/08 - 09/08 | Member Profile, Calculator và Body Progress nền | FR-PROFILE, FR-NUTRITION-01..04, FR-PROGRESS-01..02, UC-03 | Member cập nhật profile, Backend tính BMI/BMR/TDEE/macros, ghi cân nặng trong ngày | `v0.2.0-m2-profile` |
| M3 | 10/08 - 16/08 | Membership, Subscription, Renewal và SubscriptionGuard | FR-SUB, UC-04, UC-05 | Guest xem gói, Member đăng ký/gia hạn, Admin duyệt/hủy, guard chặn đúng | `v0.3.0-m3-membership` |
| M4 | 17/08 - 23/08 | Exercise Library và Workout Plan core | FR-EXR, FR-WORKOUT-04..05, UC-06, một phần UC-07 | Admin quản lý bài tập, Member xem/kích hoạt plan, seed 30-50 bài tập | `v0.4.0-m4-exercise-workout` |
| M5 | 24/08 - 30/08 | AI Hybrid Recommendation, validator, fallback và nutrition suggestion | FR-WORKOUT-01..03, FR-NUTRITION-05..06, UC-07 | AI/fallback tạo DRAFT plan, whitelist hoạt động, warning code đúng | `v0.5.0-m5-ai-recommendation` |
| M6 | 31/08 - 06/09 | Workout Log và Progress Analytics | FR-WORKOUT-06..07, FR-PROGRESS-03..04, UC-08, UC-09 | Ghi log, update-in-place, xem lịch sử, biểu đồ cân nặng/mức tạ/tần suất | `v0.6.0-m6-progress` |
| M7 | 07/09 - 13/09 | Hardening, Docker, deploy, báo cáo, slide và demo | NFR-09, NFR-10, toàn bộ UC | Docker Compose chạy một lệnh, dữ liệu demo, báo cáo/slide/video/backups hoàn chỉnh | `v1.0.0-final` |

## 5. Kế hoạch chi tiết theo buổi

### M1 - Authentication, Security, OpenAPI và React skeleton

| Ngày/Buổi | Trọng tâm | Backend | Frontend | Test/QA | Đầu ra bắt buộc |
| --- | --- | --- | --- | --- | --- |
| 27/07 - Buổi 1 | Security foundation | Hoàn thiện `SecurityFilterChain`, `JwtService`, `JwtAuthenticationFilter`, `CustomUserDetailsService`, `AccountStatusGuard`, `AuthenticationEntryPoint`, `AccessDeniedHandler`; bổ sung SpringDoc nếu chưa có | Chưa bắt buộc | Context load, JWT valid/expired/invalid signature, request thiếu token | Endpoint bảo mật phân biệt được `401`, `403`, `AUTH-002`, `ACC-005`, `ACC-004`, `ACC-006` |
| 28/07 - Buổi 2 - Hoàn thành local | Register | Tạo DTO/controller/service cho `POST /api/v1/auth/register`; normalize email; validate password/confirmPassword; BCrypt; gán `ROLE_MEMBER`, `ACTIVE` | Trang Register, client validation, hiển thị lỗi API | 61 backend test và 6 frontend test pass; production build thành công | Register chạy đúng API Draft, không log password/confirmPassword |
| 29/07 - Buổi 3 - Hoàn thành local | Login, current user và RBAC | Đã tạo `POST /api/v1/auth/login`, `GET /api/v1/users/me`; giữ role matcher; bắt `ACC-007`, `ACC-004`, `ACC-006` | Login API thật, Auth Context, `sessionStorage`, Axios Bearer interceptor | 83 backend test và 22 frontend test pass; build và kiểm thử localhost thủ công thành công | Người dùng đăng nhập nhận JWT và gọi được `/users/me` |
| 30/07 - Buổi 4 - Hoàn thành local | Admin account status | Đã tạo `GET /api/v1/admin/users`, `PATCH /api/v1/admin/users/{id}/lock`, `PATCH /api/v1/admin/users/{id}/unlock`; không khóa vì hết hạn gói | Protected Route, layout Admin/Member và User Management | 110 backend test, 33 frontend test; lock/unlock, token cũ và bảo toàn subscription đã kiểm tra local | Admin khóa/mở khóa được, subscription không bị thay đổi |
| 31/07 - Buổi 5 - Hoàn thành local | React skeleton và OpenAPI gate | Đã chuẩn hóa OpenAPI annotation, typed success/error schema, Bearer JWT và password `writeOnly` | Hoàn thiện auth state, Public Only/Protected/Role Route, layout Member/Admin và điều hướng theo role | 175 backend test, 43 frontend test; full flow Register -> Login -> `/users/me` -> Admin denied/allowed, lock/unlock đã kiểm tra local | Demo M1 local và README/docs đã cập nhật |
| 01/08 - 02/08 | Review M1 | Sửa lỗi nhỏ, chuẩn hóa package theo File 13; xử lý cảnh báo Mockito Java Agent và rà cảnh báo cấu hình `AuthenticationProvider` | Chỉnh UI responsive tối thiểu | Chạy `mvn test`, test thủ công Swagger/Postman; xác nhận không còn cảnh báo hardening M1 | Tag `v0.1.0-m1-auth` |

### M2 - Member Profile, Calculator và Body Progress nền

| Ngày/Buổi | Trọng tâm | Backend | Frontend | Test/QA | Đầu ra bắt buộc |
| --- | --- | --- | --- | --- | --- |
| 03/08 - Buổi 1 | Profile persistence | Tạo enum/profile entity còn thiếu, repository, DTO và mapper thủ công; `GET /api/v1/member/profile` | Trang Profile shell | Test ownership đọc hồ sơ chính mình | Profile API không trả Entity |
| 04/08 - Buổi 2 | Profile update | `PUT /api/v1/member/profile`; validate BR-23; sanitize list `foodAllergies`, `excludedFoods`; cập nhật/tạo `BodyProgress` khi weight thay đổi | Form profile đầy đủ trường Must-have | Test `VAL-001`, enum sai, `mealsPerDay` ngoài 1-6 | Lưu profile và trả dữ liệu chuẩn |
| 05/08 - Buổi 3 | Calculator | `BiometricCalculationService`: BMI, BMR Mifflin-St Jeor, TDEE, daily calories, protein/fat/carb | Dashboard hiển thị chỉ số | Unit test công thức nam/nữ, tuổi, boundary | `calculatedTargets` nhất quán ở GET/PUT profile |
| 06/08 - Buổi 4 | Body Progress nền | `POST /api/v1/member/body-progress`; upsert theo `(member_id, record_date)`; Clock `Asia/Ho_Chi_Minh` | Widget nhập cân nặng hôm nay | Test tạo mới/cập nhật cùng ngày, ngày tương lai | BR-22 hoạt động, không sinh bản ghi trùng ngày |
| 07/08 - Buổi 5 | Integration và tài liệu | Rà OpenAPI Profile/Progress; cập nhật API notes | Hoàn thiện UX lỗi validation | Regression Auth + Profile end-to-end | Ảnh minh chứng profile/calculator |
| 08/08 - 09/08 | Review M2 | Sửa lỗi nhỏ | Responsive profile page | `mvn test`, Postman Profile | Tag `v0.2.0-m2-profile` |

### M3 - Membership, Subscription, Renewal và SubscriptionGuard

| Ngày/Buổi | Trọng tâm | Backend | Frontend | Test/QA | Đầu ra bắt buộc |
| --- | --- | --- | --- | --- | --- |
| 10/08 - Buổi 1 | Package catalog | Entity/repository/service package; `GET /api/v1/packages`, Admin CRUD package; normalize name; soft inactive | Trang gói tập public và Admin package | Test `SUB-002`, `SUB-003`, `SUB-007`, `VAL-001` | Guest xem gói, Admin tạo/sửa/vô hiệu hóa |
| 11/08 - Buổi 2 | New subscription | `POST /api/v1/member/subscriptions`, `GET /api/v1/member/subscriptions/current`; snapshot package; chặn ACTIVE/PENDING | Member chọn gói, xem trạng thái hiện hành | Test `SUB-004`, `SUB-006`, ownership | Tạo request PENDING đúng BR-04 |
| 12/08 - Buổi 3 | Approval/cancel | `POST /api/v1/admin/subscriptions/{id}/approve`, `POST /api/v1/admin/subscriptions/{id}/cancel`; lock theo Member; `@Version`; Clock | Admin duyệt/hủy request | Test phê duyệt, hủy, concurrent conflict `CON-001` nếu khả thi | Subscription ACTIVE có `startDate`, `endDate` exclusive |
| 13/08 - Buổi 4 | Renewal và Guard | `POST /api/v1/member/subscriptions/{activeSubscriptionId}/renewal-requests`; duyệt renewal cộng dồn `endDate`; `SubscriptionGuard` reusable | Member gửi gia hạn, Admin duyệt gia hạn | Test BR-24, BR-25, package mismatch, renewal PENDING trùng | Guard có thể dùng cho Recommendation/WorkoutLog ở M5/M6 |
| 14/08 - Buổi 5 | Admin/member flow | Admin users/package/subscription views; public package polish | Hoàn thiện UI membership | Regression Auth/Profile/Membership | Demo đăng ký mới và gia hạn mô phỏng |
| 15/08 - 16/08 | Review M3 | Cập nhật OpenAPI, README, weekly progress | Chỉnh lỗi UI | `mvn test`, Postman collection | Tag `v0.3.0-m3-membership` |

### M4 - Exercise Library và Workout Plan core

| Ngày/Buổi | Trọng tâm | Backend | Frontend | Test/QA | Đầu ra bắt buộc |
| --- | --- | --- | --- | --- | --- |
| 17/08 - Buổi 1 | Exercise entity và seed | Tạo `Exercise` + 4 collection metadata; seed 30-50 bài tập qua Flyway/DataInitializer idempotent | Chưa bắt buộc | Test Flyway/seed, enum khớp File 11 | Master data đủ để demo và whitelist |
| 18/08 - Buổi 2 | Exercise API | `GET /api/v1/exercises`, `GET /api/v1/exercises/{id}`, Admin CRUD; search/filter/pagination/sorting; soft delete | Exercise Library và Admin Exercise | Test `EXR-001`, `EXR-002`, soft delete, pagination | Danh mục hiện hành không trả exercise inactive |
| 19/08 - Buổi 3 | Workout Plan core | Entity/repository/service cho `WorkoutPlan`, `WorkoutDay`, `WorkoutPlanExercise`; `GET /api/v1/member/workout-plans/current`; activation service | Trang Current Workout Plan | Test DRAFT/ACTIVE/ARCHIVED, planned constraints | Kích hoạt plan trong transaction, chỉ một ACTIVE |
| 20/08 - Buổi 4 | Fallback base | Tạo fallback/rule-based generator nội bộ dùng whitelist và validate BR-09A/BR-10; chưa cần gọi AI thật | Preview plan/fallback cơ bản | Unit test whitelist, day count, no duplicate exercise/day | Có nguồn plan an toàn để M5 dùng lại |
| 21/08 - Buổi 5 | UI và tài liệu | OpenAPI Exercise/Workout Plan; cập nhật docs ảnh UI/API | Hoàn thiện Exercise/Admin/Plan UI | Regression M1-M4 | Demo exercise CRUD và plan activation |
| 22/08 - 23/08 | Review M4 | Sửa lỗi nhỏ, rà query N+1 | Responsive library/plan | `mvn test`, Swagger/Postman | Tag `v0.4.0-m4-exercise-workout` |

### M5 - AI Hybrid Recommendation

| Ngày/Buổi | Trọng tâm | Backend | Frontend | Test/QA | Đầu ra bắt buộc |
| --- | --- | --- | --- | --- | --- |
| 24/08 - Buổi 1 | Recommendation pipeline | `POST /api/v1/member/recommendations`; kiểm tra subscription, profile completeness, calculator, exercise whitelist | Recommendation page shell, disclaimer | Test chặn `SUB-001`, profile invalid không gọi AI | Pipeline nội bộ có đủ input sạch |
| 25/08 - Buổi 2 | AI port và prompt | `AiRecommendationClient` port, provider adapter, `PromptSanitizerService`, `PromptBuilder`, JSON schema, thêm Resilience4j | Loading state 30s, disable double submit | Unit test sanitizer, không nhận `customPrompt` | AI chỉ nhận dữ liệu allowlist và whitelist |
| 26/08 - Buổi 3 | Validator và fallback | `AiResponseValidator` dùng chung cho AI/fallback; reject toàn bộ payload sai; retry/tái sinh tối đa 1 lần; warning code | Preview `AI_GENERATED`/`FALLBACK_TEMPLATE` | Test invalid JSON, ID ngoài whitelist, planned values sai, meal count sai | Fallback trả HTTP 200 với `AI_TIMEOUT`/`AI_RESPONSE_INVALID` |
| 27/08 - Buổi 4 | Persist recommendation | Lưu `WorkoutPlan DRAFT`, `AiRecommendation`, `NutritionMealSuggestion` trong cùng transaction; `GET /api/v1/member/recommendations/latest` | Member xem latest recommendation và kích hoạt plan | Test rollback khi fallback không an toàn, `AI-001` | Không lưu dữ liệu recommendation một phần |
| 28/08 - Buổi 5 | Frontend integration | Hoàn thiện page generate, preview workout/meal, calculatedTargets, warning banner | Nút activate nối API M4 | E2E: profile + active sub + generate + activate | Demo AI/fallback hoàn chỉnh |
| 29/08 - 30/08 | Review M5 | Cập nhật OpenAPI, docs AI Hybrid, screenshot | Chỉnh UI fallback/disclaimer | `mvn test`, mock AI timeout/429/5xx | Tag `v0.5.0-m5-ai-recommendation` |

### M6 - Workout Log và Progress Analytics

| Ngày/Buổi | Trọng tâm | Backend | Frontend | Test/QA | Đầu ra bắt buộc |
| --- | --- | --- | --- | --- | --- |
| 31/08 - Buổi 1 | Workout Session/Log | `POST /api/v1/member/workout-logs`; tạo/tải session theo ngày; validate BR-09B, BR-28; update-in-place BR-19 | Form ghi log theo bài trong plan ACTIVE | Test create/update, actual limits, ngày tương lai | Member ghi log mới khi có subscription hợp lệ |
| 01/09 - Buổi 2 | Workout log history | `GET /api/v1/member/workout-logs`, `GET /api/v1/member/workout-logs/exercises/{exerciseId}`; DTO projection đọc exercise inactive | Lịch sử log và filter ngày | Test pagination, ownership, inactive exercise history | Lịch sử vẫn xem được khi gói hết hạn |
| 02/09 - Buổi 3 | Body Progress analytics | `GET /api/v1/member/body-progress`; timeseries cân nặng và `workoutFrequencyByWeek` | Biểu đồ cân nặng/tần suất | Test sort tăng dần, week ISO, ownership | Dữ liệu biểu đồ dùng trực tiếp trên UI |
| 03/09 - Buổi 4 | Progress chart strength | Aggregate max weight theo exercise/date; tối ưu query | Biểu đồ mức tạ/reps cơ bản | Test group by ngày, exercise không thuộc Member không lộ dữ liệu | Progress Analytics đủ UC-09 |
| 04/09 - Buổi 5 | UI completion | Rà workflow Member: profile -> subscription -> recommendation -> activation -> log -> progress | Hoàn thiện dashboard Member | Regression M1-M6 | Demo end-to-end Member |
| 05/09 - 06/09 | Review M6 | Cập nhật docs, screenshot, Postman | Chỉnh responsive | `mvn test`, Postman collection | Tag `v0.6.0-m6-progress` |

### M7 - Hardening, Docker, báo cáo và demo

| Ngày | Trọng tâm | Công việc bắt buộc | Gate |
| --- | --- | --- | --- |
| 07/09 | Test sweep | Chạy toàn bộ unit/integration test; bổ sung test thiếu cho calculator, subscription renewal, AI validator/fallback, activation plan, workout log ownership | Không còn lỗi P0/P1 |
| 08/09 | Docker và seed demo | Hoàn thiện Dockerfile backend/frontend, `docker-compose.yml`, `.env.example`, MySQL volume/healthcheck; seed admin, member mẫu, package, 30-50 exercise | `docker compose up --build` chạy được từ máy sạch |
| 09/09 | Documentation | README, Swagger, Postman collection, API/ERD cập nhật nếu có migration mới; chụp screenshot UI/API; viết Chương 4/5/Kết luận | Tài liệu đủ tái chạy và đủ minh chứng |
| 10/09 | Feature Freeze | Đóng băng chức năng mới; chỉ cho phép sửa bug demo, lỗi bảo mật, lỗi dữ liệu hoặc lỗi build | Không thêm endpoint/module mới sau ngày này |
| 11/09 | Rehearsal demo | Tập demo với dữ liệu mẫu; ghi lại lỗi/điểm vấp; chuẩn bị script demo dự phòng | Demo chạy liên tục không cần nhập dữ liệu thủ công phức tạp |
| 12/09 | Bugfix cuối và slide | Chỉ sửa lỗi đã ghi ở rehearsal; hoàn thiện slide, video demo dự phòng, database backup | Bản trình diễn ổn định |
| 13/09 | Đóng gói | Xuất PDF báo cáo, backup source/database/video/Postman, tạo tag `v1.0.0-final` | Sẵn sàng nộp và bảo vệ |

## 6. Quality gate theo milestone

| Gate | Áp dụng | Điều kiện đạt |
| --- | --- | --- |
| Build/Test | Mọi milestone | `mvn test` pass; không có test context lỗi; frontend build pass từ M1 trở đi. |
| API Contract | Mọi endpoint mới | Đúng prefix `/api/v1`, response format File 10, error/warning code File 05. |
| Security | Mọi endpoint bảo vệ | JWT hợp lệ, role đúng, `AccountStatusGuard` chạy; endpoint cá nhân kiểm tra ownership ở Service/query. |
| Database | Mọi thay đổi persistence | Entity/Repository khớp Flyway; không sửa migration cũ; không tạo Entity ngoài 16 Entity MVP nếu chưa có lý do. |
| AI | M5 trở đi | AI không trả calculated targets; response/fallback đều qua validator; lỗi không lưu dữ liệu một phần. |
| Demo | Mỗi milestone | Có kịch bản demo ngắn, screenshot hoặc Postman evidence, cập nhật `06-weekly-progress.md`. |
| Tag | Mỗi milestone | Commit sạch các thay đổi thuộc milestone và tạo tag đúng bảng tổng quan. |

## 7. Checklist demo cuối

| Nhóm | Checklist |
| --- | --- |
| Auth/RBAC | Register; Login; `/users/me`; Member bị chặn ở Admin API; Admin khóa/mở khóa; LOCKED/DISABLED bị chặn. |
| Profile/Calculator | Cập nhật profile; validation BR-23; tính BMI, BMR, TDEE, daily calories, protein/carb/fat; BodyProgress update-in-place. |
| Membership | Guest xem gói; Admin CRUD/vô hiệu hóa package; Member đăng ký mới; Admin duyệt; Member gia hạn; Admin duyệt renewal; subscription hết hạn chặn tính năng cao cấp nhưng không khóa login. |
| Exercise/Workout | Admin CRUD exercise; search/filter/pagination; soft delete; Member xem plan hiện hành; kích hoạt DRAFT và archive ACTIVE cũ. |
| AI Hybrid | Tạo recommendation bằng AI; timeout/invalid response chuyển fallback; whitelist chặn ID ngoài danh sách; warning code hiển thị; calculated targets do Backend ghép. |
| Workout Log/Progress | Ghi workout log; update-in-place cùng bài/ngày; xem lịch sử; xem biểu đồ cân nặng, tần suất tập và mức tạ theo bài. |
| Deploy/Docs | `docker compose up --build`; Swagger truy cập được; README đủ; Postman collection; dữ liệu demo; video demo; backup database/source. |

## 8. Nội dung không triển khai trước khi đóng MVP

| Không làm | Lý do |
| --- | --- |
| Refresh token/OAuth2 login | Không có trong File 02/10; tăng bề mặt bảo mật và thời gian test. |
| Payment gateway thật | Won't-have; MVP chỉ dùng luồng xác nhận mô phỏng của Admin. |
| API/UI nghiệp vụ PT | Should-have; không chặn luồng Admin/Member. |
| Nutrition log chi tiết | Không nằm trong schema 25 bảng MVP. |
| Mobile app, realtime chat, IoT, face recognition | Won't-have theo phạm vi MVP. |
| Microservice, Kafka/RabbitMQ, CQRS/Event Sourcing | Trái quyết định Modular Layered Monolith ở File 13. |
