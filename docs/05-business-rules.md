# ĐẶC TẢ QUY TẮC NGHIỆP VỤ HỆ THỐNG (SYSTEM BUSINESS RULES)

Tài liệu này định hình các ràng buộc nghiệp vụ của hệ thống. Đây là cơ sở để lập trình viên thiết lập logic kiểm duyệt dữ liệu (Validation), cấu hình ràng buộc Cơ sở dữ liệu (Database Constraints), viết Unit Test và phân chia luồng xử lý ngoại lệ (Exception Handling) tại tầng Backend trong phạm vi đề tài.

---

## 1. Hệ thống, Xác thực & Bảo mật (Authentication & Security Rules)

### [BR-01] - Tính duy nhất và chuẩn hóa định danh số (Email Uniqueness & Normalization)
- **Mô tả nghiệp vụ:** Mỗi tài khoản người dùng phải sử dụng một địa chỉ email duy nhất. Trước khi kiểm tra trùng và lưu trữ, email bắt buộc phải được **trim khoảng trắng đầu/cuối** và **chuyển thành chữ thường (lowercase)**. Lý do: `User@Gmail.com` và ` user@gmail.com ` đều phải được xác định là cùng một danh tính.
- **Hiện thực hóa kỹ thuật:** Gọi `email.trim().toLowerCase()` tại tầng Service trước bất kỳ thao tác nào (kiểm tra tồn tại, lưu mới, tìm kiếm). Thiết lập `UNIQUE` tại trường `email` mức Database.

### [BR-02] - Mã hóa dữ liệu mật khẩu (Password Encryption)
- **Mô tả nghiệp vụ:** Mật khẩu của người dùng tuyệt đối không được lưu trữ dưới dạng văn bản thuần (plain text) nhằm phòng ngừa rủi ro rò rỉ dữ liệu khi cơ sở dữ liệu bị xâm nhập trái phép.
- **Hiện thực hóa kỹ thuật:** Sử dụng thuật toán băm mật khẩu bảo mật (ví dụ: BCrypt) thông qua class `PasswordEncoder` của Spring Security để mã hóa mật khẩu ở tầng Service trước khi lưu trữ vào Database.

### [BR-03] - Giới hạn quyền truy cập tài nguyên quản trị (Admin Authorization Constraint)
- **Mô tả nghiệp vụ:** Hội viên (Member) tuyệt đối không được phép tiếp cận hoặc thực thi các chức năng thuộc về phân hệ quản trị (như CRUD gói tập, quản lý tài khoản người dùng khác).
- **Hiện thực hóa kỹ thuật:** Sử dụng annotation `@PreAuthorize("hasRole('ADMIN')")` hoặc cấu hình phân quyền Http Security tại Spring Security Filter Chain để chặn đứng các truy cập trái phép ở mức Endpoint API.

---

## 2. Quản lý Gói tập & Thành viên (Membership & Subscription Rules)

### [BR-04] - Ràng buộc gói tập kích hoạt đồng thời (Single Active Subscription)
- **Mô tả nghiệp vụ:** Trong phạm vi phiên bản MVP, mỗi hội viên chỉ được sở hữu duy nhất một gói tập đang ở trạng thái kích hoạt (ACTIVE) tại một thời điểm cụ thể.
- **Hiện thực hóa kỹ thuật:** Triển khai kiểm tra logic nghiệp vụ tại tầng Service trước khi tạo mới một Subscription: Truy vấn xem hội viên có gói tập nào có trạng thái `ACTIVE` và ngày kết thúc (`end_date`) lớn hơn ngày hiện tại hay không. Nếu có, ném ra ngoại lệ `ActiveSubscriptionException`.

### [BR-05] - Hạn chế đăng ký gói tập đã ngừng kích hoạt (Inactive Package Registration Block)
- **Mô tả nghiệp vụ:** Người dùng không được phép đăng ký mua hoặc gia hạn các gói dịch vụ/gói tập đã bị chuyển trạng thái ngưng hoạt động (INACTIVE) bởi quản trị viên.
- **Hiện thực hóa kỹ thuật:** Sử dụng validation kiểm tra trường trạng thái `status` hoặc `is_active` của thực thể `MembershipPackage` ở tầng Service. Ném ra ngoại lệ `InactivePackageException` nếu phát hiện gói dịch vụ đã bị vô hiệu hóa.

---

## 3. Lập lịch & Quản lý Bài tập (Workout & Exercise Rules)

### [BR-06] - Số lượng buổi tập tối thiểu trong giáo án (Minimum Workout Days)
- **Mô tả nghiệp vụ:** Mỗi giáo án tập luyện (Workout Plan) được khởi tạo bởi hệ thống hoặc người dùng bắt buộc phải chứa ít nhất một ngày tập (Workout Day).
- **Hiện thực hóa kỹ thuật:** Áp dụng Validation ở Java Beans (`@NotEmpty` hoặc `@Size(min = 1)`) trên danh sách `workoutDays` trong DTO/Entity của Workout Plan.

### [BR-07] - Số lượng bài tập tối thiểu trong ngày tập (Minimum Exercises Per Day)
- **Mô tả nghiệp vụ:** Mỗi ngày tập (Workout Day) trong giáo án bắt buộc phải được thiết lập tối thiểu một bài tập (Exercise) để đảm bảo tính hợp lệ của hoạt động tập luyện.
- **Hiện thực hóa kỹ thuật:** Áp dụng Validation ở tầng Entity hoặc DTO (`@NotEmpty` trên Collection bài tập). Kiểm tra logic tại Service layer trước khi lưu trữ thực thể Workout Day.

### [BR-08] - Giới hạn chỉ số cảm nhận lực (RPE Constraint)
- **Mô tả nghiệp vụ:** Chỉ số RPE *khi nhận vào từ kết quả AI* chỉ được phép nằm trong khoảng **6–9**. Chỉ số RPE *do hội viên tự ghi nhận thực tế* sau buổi tập có phạm vi rộng hơn (1–10) theo thang điểm Borg tích hợp. Phải phân biệt rõ `plannedRpe` (giá trị 6–9, do AI tạo) và `actualRpe` (giá trị 1–10, do hội viên nhập).
- **Hiện thực hóa kỹ thuật:** Sử dụng hai cặp annotation tương ứng: `@Min(6) @Max(9)` trên trường `plannedRpe` của WorkoutPlan DTO và `@Min(1) @Max(10)` trên trường `actualRpe` của WorkoutLog DTO.

### [BR-09A] - Giới hạn thông số giáo án AI (AI Planned Exercise Parameters)
- **Mô tả nghiệp vụ:** Các tham số trong giáo án do AI đề xuất phải nằm trong ngưỡng an toàn sau: sets **1–5**, reps **1–30**, RPE kế hoạch **6–9**, thời gian nghỉ **30–300 giây**.
- **Hiện thực hóa kỹ thuật:** Áp dụng các nhóm annotation: `@Min(1) @Max(5)` trên `plannedSets`, `@Min(1) @Max(30)` trên `plannedReps`, `@Min(6) @Max(9)` trên `plannedRpe`, `@Min(30) @Max(300)` trên `restSeconds` của WorkoutPlan DTO. **Nếu bất kỳ giá trị nào vi phạm → từ chối AI Response → retry → fallback.** Không clamp giá trị âm thầm.

### [BR-09B] - Giới hạn dữ liệu nhật ký thực tế (Actual Workout Log Parameters)
- **Mô tả nghiệp vụ:** Dữ liệu hội viên tự ghi nhận sau buổi tập có ngưỡng rộng hơn: sets thực tế **1–10**, reps thực tế **1–100**, RPE thực tế **1–10** (thang Borg), khối lượng tạ (`weightUsedKg`) **≥ 0**.
- **Hiện thực hóa kỹ thuật:** Áp dụng tại WorkoutLog DTO: `@Min(1) @Max(10)` trên `actualSets`, `@Min(1) @Max(100)` trên `actualReps`, `@Min(1) @Max(10)` trên `actualRpe`, `@DecimalMin("0.0")` trên `weightUsedKg`.

### [BR-09C] - Quyền sở hữu số liệu dinh dưỡng định lượng của Backend (Backend-owned Nutrition Targets)
- **Mô tả nghiệp vụ:** BMI, BMR, TDEE, `dailyCaloriesKcal`, `proteinGrams`, `carbGrams` và `fatGrams` là số liệu định lượng do Backend tính toán theo công thức đã chọn. AI chỉ được đề xuất cấu trúc lịch tập và danh sách bữa ăn; không được trả, thay đổi hoặc quyết định các chỉ số này.
- **Hiện thực hóa kỹ thuật:** JSON Schema của AI không chứa các trường số liệu định lượng. Sau khi AI response hợp lệ, `RecommendationService` ghép `calculatedTargets` do `CalculationService` tạo vào response trả Client; payload AI có trường ngoài schema bị từ chối và kích hoạt luồng retry/fallback.

---

## 4. Tích hợp AI Engine & Fallback (AI Integration & Resilience Rules)

### [BR-10] - Xác thực bài tập đề xuất từ AI (AI Exercise Existence Validation)
- **Mô tả nghiệp vụ:** Mọi bài tập nằm trong giáo án do AI tự động đề xuất bắt buộc phải là bài tập có thật và đã tồn tại trong Cơ sở dữ liệu gốc. **Nếu bất kỳ một `exerciseId` nào trong phản hồi AI không có trong Whitelist, Backend từ chối toàn bộ AI Response**; không tự ý thay thế bằng bài tập tương đương.
- **Hiện thực hóa kỹ thuật:** Sau khi nhận AI Response, thu thập `Set<Long> responseExerciseIds = collectAllExerciseIds(response)`. Kiểm tra logic whitelist: `whitelist.containsAll(responseExerciseIds)`. Nếu false → từ chối toàn bộ phản hồi → thực hiện retry tối đa 1 lần → nếu vẫn lỗi thì dùng fallback template từ database (không tự thay thế bằng bài tập tương đương).

### [BR-11] - Cơ chế dự phòng khi AI lỗi (Resilience Fallback Strategy)
- **Mô tả nghiệp vụ:** Trong trường hợp dịch vụ AI bên ngoài gặp sự cố, không phản hồi hoặc phản hồi sai cấu trúc JSON, hệ thống phải tự động chuyển sang cơ chế tạo lịch tập mặc định dựa trên các giáo án mẫu tĩnh (Rule-based Templates).
- **Hiện thực hóa kỹ thuật:** Sử dụng cấu trúc `try-catch` bao bọc lấy khối gọi API AI bên ngoài, kết hợp thiết lập cơ chế Circuit Breaker hoặc Fallback Method trong Spring Boot (sử dụng Resilience4j hoặc tự triển khai) để trả về một giáo án mẫu lưu sẵn trong DB.

---

## 5. Tính Toàn vẹn Dữ liệu & Miễn trừ trách nhiệm (Data Integrity & Disclaimer Rules)

### [BR-12] - Miễn trừ trách nhiệm y tế của đề xuất AI (Medical Disclaimer)
- **Mô tả nghiệp vụ:** Mọi đề xuất lịch tập và dinh dưỡng từ AI chỉ mang tính chất tham khảo, không được coi là lời khuyên y tế, chỉ dẫn điều trị bệnh lý chuyên khoa.
- **Hiện thực hóa kỹ thuật:** Hiển thị một cảnh báo rõ ràng trên giao diện người dùng (Disclaimer Banner) trước khi hiển thị kết quả AI và yêu cầu người dùng xác nhận đồng ý điều khoản trước khi bắt đầu sử dụng lộ trình.

### [BR-13] - Giới hạn phạm vi sở hữu dữ liệu tiến độ (Data Ownership Constraint)
- **Mô tả nghiệp vụ:** Hội viên chỉ được quyền xem, sửa đổi hoặc xóa dữ liệu nhật ký tập luyện và tiến trình phát triển thể trạng của chính tài khoản mình sở hữu.
- **Hiện thực hóa kỹ thuật:** Triển khai kiểm tra logic tại Service bằng cách so sánh `userId` của bản ghi cần cập nhật với `userId` trích xuất từ Principal trong Security Context của JWT Token hiện hành. Có thể sử dụng annotation `@PostAuthorize("returnObject.user.email == authentication.name")` ở tầng Repository hoặc Service.

### [BR-14] - Xóa mềm danh mục bài tập (Soft Delete for Master Data)
- **Mô tả nghiệp vụ:** Quản trị viên không được phép xóa cứng (Hard Delete) các bài tập đã có liên kết lịch sử với nhật ký tập luyện của hội viên. Các bài tập này chỉ được chuyển sang trạng thái ngưng hoạt động (INACTIVE) để tránh đứt gãy dữ liệu lịch sử.
- **Hiện thực hóa kỹ thuật:** Áp dụng cơ chế Soft Delete toàn diện. Thêm trường `is_active` hoặc `deleted` (boolean) vào bảng `exercises`. Trong Spring Boot 3.x (tích hợp Hibernate 6), sử dụng trực tiếp annotation `@SoftDelete` tại JPA Entity để hệ thống tự động hóa luồng chuyển đổi lệnh DELETE thành lệnh UPDATE trạng thái và tự động lọc bỏ các bản ghi đã xóa khi thực hiện truy vấn.


### [BR-15] - Kiểm thử đăng ký (Registration Validation)
- **Mô tả nghiệp vụ:** Sau khi đăng ký thành công, tài khoản phải ở trạng thái PENDING cho đến khi Admin xác nhận, hoặc hệ thống có thể mặc định kích hoạt ngay (AUTO-ACTIVE). Phải định nghĩa rõ trong cấu hình và ghi vào tài liệu quyết định.
- **Hiện thực hóa kỹ thuật:** Trong MVP, áp dụng **AUTO-ACTIVE**: Hội viên mới được gán quyền `ROLE_MEMBER` và trạng thái `accountStatus = ACTIVE` ngay sau khi đăng ký nhưng **chưa có gói tập Active**. Gói tập được kích hoạt riêng bởi luồng Subscription Management.

### [BR-16] - Kiểm thử trạng thái khóa tài khoản (Locked Account Testing)
- **Mô tả nghiệp vụ:** Hệ thống ngăn chặn rõ ràng tài khoản LOCKED khỏi đăng nhập và sử dụng dịch vụ, kể cả khi họ cung cấp đúng mật khẩu.
- **Hiện thực hóa kỹ thuật:** Tầng Security/UserDetails layer được cấu hình để kiểm tra `accountStatus` cho mỗi request cần xác thực. Nếu `accountStatus == LOCKED` → ném `LockedException`, trả về HTTP 403 với Error Code `ACC-004`. Token hiện hành của tài khoản LOCKED cũng bị từ chối.

### [BR-17] - Dọn dẹp tài khoản chưa hoàn tất đăng ký (Stale Registration Cleanup)
- **Mô tả nghiệp vụ:** Nếu trong tương lai hệ thống áp dụng luồng xác nhận email (OTP/Token), các tài khoản chưa xác thực email sau **24 giờ** phải bị xóa tự động để giải phóng chỗ cho Email/Username trùng lặp.
- **Hiện thực hóa kỹ thuật:** Triển khai `@Scheduled` Job chạy hằng ngày; truy vấn các tài khoản `status=PENDING` và `createdAt < now() - 24h`; thực hiện DELETE. **Quy tắc này là Should-have trong MVP.**

### [BR-18] - Chính sách mật khẩu (Password Policy)
- **Mô tả nghiệp vụ:** Mật khẩu tối thiểu **8 ký tự**, tối đa **72 ký tự** (giới hạn BCrypt), không chứa toàn khoảng trắng, chứa ít nhất 1 chữ hoa và 1 chữ số. Không lưu trữ mật khẩu dưới dạng plaintext.
- **Hiện thực hóa kỹ thuật:** Không trim hoặc thay đổi mật khẩu do người dùng nhập. Từ chối mật khẩu có khoảng trắng ở đầu/cuối và kiểm tra cú pháp bằng `@Pattern(regexp = "^(?!\\s)(?!.*\\s$)(?=.*[A-Z])(?=.*\\d).{8,72}$")` tại DTO lớp `RegisterRequest`. Mã hóa bằng `BCryptPasswordEncoder(12)` trước khi lưu vào DB.

### [BR-19] - Giải quyết xung đột dữ liệu tiến trình (Progress Data Conflict Resolution)
- **Mô tả nghiệp vụ:** Nếu hội viên cập nhật nhật ký tập luyện có thời điểm `workoutDate` trùng với một bản ghi đã tồn tại, hệ thống kích hoạt chế độ **Update-in-place** (ghi đè bản ghi cũ) thay vì tạo bản sao mới. Timestamp `updatedAt` được cập nhật tự động qua JPA Auditing.
- **Hiện thực hóa kỹ thuật:** Logic tại `WorkoutLogService.saveOrUpdate()`: nếu tồn tại bản ghi `(userId, workoutDate, exerciseId)` → gọi `.save()` trên đối tượng Entity đã tải nhằm đảm bảo JPA Dirty-Checking được kích hoạt đúng.

### [BR-20] - Chuẩn hóa Email (Email Trim & Lowercase)
- **Mô tả nghiệp vụ:** Trước bất kỳ thao tác nào (kiểm tra tồn tại, đăng nhập, lưu mới), Email phải được **trim khoảng trắng đầu/cuối** và **chuyển lowercase** để kiểm tra trùng có ý nghĩa. Quy tắc này bổ sung thêm chi tiết thực hiện cho BR-01.
- **Hiện thực hóa kỹ thuật:** Tại `UserService.register()` và `UserService.findByEmail()`: `String normalizedEmail = email.trim().toLowerCase();`. Không để Spring Security thực hiện so sánh trước khi email được chuẩn hóa.

### [BR-21] - Chặn tài khoản DISABLED (Permanently Disabled Account Block)
- **Mô tả nghiệp vụ:** Tài khoản ở trạng thái `DISABLED` (vô hiệu hóa vĩnh viễn, khác `LOCKED` có thể mở lại) không được đăng nhập, và token của tài khoản đã `DISABLED` cũng phải bị hệ thống từ chối trên mọi request yêu cầu xác thực.
- **Hiện thực hóa kỹ thuật:** Enum `AccountStatus`: `ACTIVE`, `LOCKED`, `DISABLED`. Tầng Security/UserDetails layer kiểm tra `accountStatus` cho mỗi request; nếu `DISABLED` → ném `DisabledException` (hoặc custom exception tương ứng), trả HTTP 403 với Error Code `ACC-006`. Không gọi đây là kiểm tra nghiệp vụ subscription (subscription kiểm tra riêng bằng Method Security).

### [BR-22] - Chỉ duy nhất một bản ghi tiến trình thể trạng trong một ngày (Body Progress Daily Uniqueness)
- **Mô tả nghiệp vụ:** Mỗi hội viên chỉ có tối đa một bản ghi `BodyProgress` (cân nặng, các chỉ số thể trạng) trong một ngày cụ thể. Nếu hội viên nhập lại trong cùng ngày, hệ thống **cập nhận (Update-in-place)** bản ghi cũ, không tạo mới.
- **Hiện thực hóa kỹ thuật:** Tại `BodyProgressService.save()`: tìm kiếm bản ghi `(userId, recordDate)` — nếu tồn tại → gọi `.save()` trên Entity đã tải (JPA Dirty-Checking); nếu không → tạo mới. `updatedAt` được tự động cập nhận qua `@LastModifiedDate` (JPA Auditing).

---

## 6. Bảng mã lỗi công khai (Public Error Code Registry)

Bảng này xác lập các `ErrorCode` cần kết xuất trong phản hồi API lỗi (`ApiErrorResponse`) của hệ thống. Mã lỗi được đặt theo nhóm chức năng, giúp Frontend xử lý logic phía client.

| Mã lỗi | Nhóm | HTTP Status | Mô tả ngắn |
| :--- | :--- | :---: | :--- |
| `ACC-001` | Tài khoản | 400 | Email đã được dùng |
| `ACC-002` | Tài khoản | 400 | Định dạng mật khẩu không hợp lệ |
| `ACC-003` | Tài khoản | 400 | Số điện thoại đã tồn tại |
| `ACC-004` | Tài khoản | 403 | Tài khoản bị khóa (LOCKED) |
| `ACC-005` | Tài khoản | 401 | JWT Token hết hạn hoặc không hợp lệ |
| `ACC-006` | Tài khoản | 403 | Tài khoản đã bị vô hiệu hóa vĩnh viễn (DISABLED) |
| `SUB-001` | Gói tập | 403 | Không có gói tập Active |
| `SUB-002` | Gói tập | 404 | Không tìm thấy gói tập |
| `EXR-001` | Bài tập | 404 | ID bài tập không tồn tại |
| `AI-001` | AI Engine | 502 | Dịch vụ AI không phản hồi (timeout/server error) |
| `AI-002` | AI Engine | 200 | AI Response không hợp lệ (ID ngoài Whitelist hoặc sai cấu trúc) — trả Fallback kèm `warningCode: AI_RESPONSE_INVALID` |
