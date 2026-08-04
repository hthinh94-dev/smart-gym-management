# ĐẶC TẢ QUY TẮC NGHIỆP VỤ HỆ THỐNG (SYSTEM BUSINESS RULES)

Tài liệu này định hình các ràng buộc nghiệp vụ của hệ thống. Đây là cơ sở để lập trình viên thiết lập logic kiểm duyệt dữ liệu (Validation), cấu hình ràng buộc Cơ sở dữ liệu (Database Constraints), viết Unit Test và phân chia luồng xử lý ngoại lệ (Exception Handling) tại tầng Backend trong phạm vi đề tài.

---

## 1. Hệ thống, Xác thực & Bảo mật (Authentication & Security Rules)

### [BR-01] - Tính duy nhất và chuẩn hóa định danh số (Email Uniqueness & Normalization)
- **Mô tả nghiệp vụ:** Mỗi tài khoản người dùng phải sử dụng một địa chỉ email duy nhất. Trước khi kiểm tra trùng và lưu trữ, email bắt buộc phải được **trim khoảng trắng đầu/cuối** và **chuyển thành chữ thường (lowercase)**. Lý do: `User@Gmail.com` và ` user@gmail.com ` đều phải được xác định là cùng một danh tính.
- **Hiện thực hóa kỹ thuật:** Gọi `email.trim().toLowerCase(Locale.ROOT)` tại tầng Service trước bất kỳ thao tác nào (kiểm tra tồn tại, lưu mới, tìm kiếm). Thiết lập `UNIQUE` tại trường `email` mức Database.

### [BR-02] - Băm mật khẩu (Password Hashing)
- **Mô tả nghiệp vụ:** Mật khẩu của người dùng tuyệt đối không được lưu trữ dưới dạng văn bản thuần (plain text) nhằm phòng ngừa rủi ro rò rỉ dữ liệu khi cơ sở dữ liệu bị xâm nhập trái phép.
- **Hiện thực hóa kỹ thuật:** Sử dụng `BCryptPasswordEncoder(12)` thông qua interface `PasswordEncoder` của Spring Security để băm mật khẩu tại tầng Service trước khi lưu vào Database.

### [BR-03] - Giới hạn quyền truy cập tài nguyên quản trị (Admin Authorization Constraint)
- **Mô tả nghiệp vụ:** Hội viên (Member) tuyệt đối không được phép tiếp cận hoặc thực thi các chức năng thuộc về phân hệ quản trị (như CRUD gói tập, quản lý tài khoản người dùng khác).
- **Hiện thực hóa kỹ thuật:** Sử dụng annotation `@PreAuthorize("hasRole('ADMIN')")` hoặc cấu hình phân quyền Http Security tại Spring Security Filter Chain để chặn đứng các truy cập trái phép ở mức Endpoint API.

---

## 2. Quản lý Gói tập & Thành viên (Membership & Subscription Rules)

### [BR-04] - Ràng buộc gói tập kích hoạt đồng thời (Single Active Subscription)
- **Mô tả nghiệp vụ:** Trong phạm vi phiên bản MVP, mỗi hội viên chỉ được sở hữu duy nhất một gói tập đang ở trạng thái kích hoạt (ACTIVE) tại một thời điểm cụ thể. Hội viên cũng chỉ được có tối đa một yêu cầu đăng ký mới ở trạng thái `PENDING` chưa được xử lý, tránh gửi lặp cùng một nghiệp vụ.
- **Hiện thực hóa kỹ thuật:** Trước khi tạo yêu cầu đăng ký mới, Service kiểm tra Subscription hợp lệ theo BR-25; nếu có, trả `SUB-004`. Nếu đã có yêu cầu đăng ký mới `PENDING`, trả `SUB-006`. Khi Admin phê duyệt, Service khóa phạm vi subscription của Member; mọi bản ghi còn mang `status = ACTIVE` nhưng có `endDate <= currentDate` phải chuyển sang `EXPIRED`. Service flush pha chuẩn hóa để giải phóng generated key, sau đó kiểm tra không còn bất kỳ bản ghi mang trạng thái `ACTIVE` trước khi chuyển request `PENDING` sang `ACTIVE` và flush lần hai. Hai pha nằm trong cùng transaction nên nếu pha kích hoạt lỗi, toàn bộ thay đổi rollback.

### [BR-05] - Hạn chế đăng ký gói tập đã ngừng kích hoạt (Inactive Package Registration Block)
- **Mô tả nghiệp vụ:** Người dùng không được phép đăng ký mua hoặc gia hạn các gói dịch vụ/gói tập đã bị chuyển trạng thái ngưng hoạt động (INACTIVE) bởi quản trị viên.
- **Hiện thực hóa kỹ thuật:** Chuẩn hóa một trường `isActive` ở Entity và cột `is_active` tại Database. Service trả `SUB-002` nếu không tìm thấy package và trả `SUB-003` nếu package có `isActive = false`; không dùng hai nguồn trạng thái `status` và `is_active` song song.

---

## 3. Lập lịch & Quản lý Bài tập (Workout & Exercise Rules)

### [BR-06] - Số lượng buổi tập tối thiểu trong giáo án (Minimum Workout Days)
- **Mô tả nghiệp vụ:** Mỗi giáo án tập luyện (Workout Plan) được tạo từ AI hoặc Fallback Template trong MVP bắt buộc phải chứa ít nhất một ngày tập (Workout Day). Chức năng Member tự soạn giáo án thủ công không thuộc phạm vi MVP.
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
- **Mô tả nghiệp vụ:** BMI, BMR, TDEE, `dailyCaloriesKcal`, `proteinGrams`, `carbGrams` và `fatGrams` là số liệu định lượng do Backend tính toán theo công thức đã chọn. AI chỉ được đề xuất `workoutSchedule` và `nutritionPlan.mealStructure`; không được trả, thay đổi hoặc quyết định các chỉ số này.
- **Hiện thực hóa kỹ thuật:** JSON Schema của AI không chứa các trường số liệu định lượng. Calculator dùng BMI và Mifflin-St Jeor, hệ số hoạt động `1.2/1.375/1.55/1.725`, điều chỉnh `BULK +300`, `CUT -500`, `MAINTAIN +0`, Protein cố định `2.2g/kg`, Fat cố định `25% calories` và Carb nhận phần calories còn lại. Kết quả làm tròn `HALF_UP` đến 2 chữ số. Sau khi AI response hợp lệ, `RecommendationService` ghép `calculatedTargets` do Backend tạo vào response; payload AI có trường ngoài schema bị từ chối và kích hoạt luồng retry/fallback.

---

## 4. Tích hợp AI Engine & Fallback (AI Integration & Resilience Rules)

### [BR-10] - Xác thực bài tập đề xuất từ AI (AI Exercise Existence Validation)
- **Mô tả nghiệp vụ:** Mọi bài tập nằm trong giáo án do AI tự động đề xuất bắt buộc phải là bài tập có thật và đã tồn tại trong Cơ sở dữ liệu gốc. **Nếu bất kỳ một `exerciseId` nào trong phản hồi AI không có trong Whitelist, Backend từ chối toàn bộ AI Response**; không tự ý thay thế bằng bài tập tương đương.
- **Hiện thực hóa kỹ thuật:** Sau khi nhận AI Response, thu thập `Set<Long> responseExerciseIds = collectAllExerciseIds(response)`. Kiểm tra logic whitelist: `whitelist.containsAll(responseExerciseIds)` và xác minh không có `exerciseId` lặp trong cùng một workout day. Nếu một trong hai điều kiện sai → từ chối toàn bộ phản hồi → thực hiện retry tối đa 1 lần → nếu vẫn lỗi thì dùng fallback template từ database (không tự thay thế bằng bài tập tương đương). Unique constraint `(workout_day_id, exercise_id)` bảo vệ bổ sung tại database để tương thích với khóa update-in-place của BR-19.

### [BR-11] - Cơ chế dự phòng khi AI lỗi (Resilience Fallback Strategy)
- **Mô tả nghiệp vụ:** Trong trường hợp dịch vụ AI bên ngoài gặp sự cố, không phản hồi hoặc phản hồi sai cấu trúc JSON, hệ thống phải tự động chuyển sang cơ chế tạo lịch tập mặc định dựa trên các giáo án mẫu tĩnh (Rule-based Templates).
- **Hiện thực hóa kỹ thuật:** Fallback workout template phải được lọc lại bằng chính `exerciseIdWhitelist` của Member và hậu kiểm BR-09A/BR-10; fallback meal template phải có `dietaryTags`/`allergenTags` để lọc theo `dietaryPreference`, `foodAllergies`, `excludedFoods` và đúng `mealsPerDay`. Template không được bỏ qua chống chỉ định, thiết bị hoặc hạn chế ăn uống. Dùng Resilience4j Circuit Breaker/TimeLimiter để chuyển luồng. Nếu không có template an toàn hoặc nguồn template lỗi, không lưu dữ liệu một phần và trả `AI-001` (HTTP 502).

---

## 5. Tính Toàn vẹn Dữ liệu & Miễn trừ trách nhiệm (Data Integrity & Disclaimer Rules)

### [BR-12] - Miễn trừ trách nhiệm y tế của đề xuất AI (Medical Disclaimer)
- **Mô tả nghiệp vụ:** Mọi đề xuất lịch tập và dinh dưỡng từ AI chỉ mang tính chất tham khảo, không được coi là lời khuyên y tế, chỉ dẫn điều trị bệnh lý chuyên khoa.
- **Hiện thực hóa kỹ thuật:** Hiển thị một cảnh báo rõ ràng trên giao diện người dùng (Disclaimer Banner) trước khi hiển thị kết quả AI và yêu cầu người dùng xác nhận đồng ý điều khoản trước khi bắt đầu sử dụng lộ trình.

### [BR-13] - Giới hạn phạm vi sở hữu dữ liệu tiến độ (Data Ownership Constraint)
- **Mô tả nghiệp vụ:** Hội viên chỉ được quyền xem, sửa đổi hoặc xóa hồ sơ, dữ liệu nhật ký tập luyện và tiến trình phát triển thể trạng của chính tài khoản mình sở hữu. Member mới đăng ký chưa có `MemberProfile` là trạng thái hợp lệ; hệ thống không tự tạo hồ sơ bằng dữ liệu giả.
- **Hiện thực hóa kỹ thuật:** Service luôn lấy `userId` từ `AuthenticatedUserPrincipal` trong Security Context, không nhận `memberId` từ path/query/body. `GET /api/v1/member/profile` không tìm thấy hồ sơ của principal hiện hành trả HTTP 404 với `PROF-001`; `PUT` profile thực hiện upsert từ Ngày 13.

### [BR-14] - Xóa mềm danh mục bài tập (Soft Delete for Master Data)
- **Mô tả nghiệp vụ:** Trong MVP, mọi thao tác xóa Exercise đều là xóa mềm, không phân biệt bài tập đã có liên kết lịch sử hay chưa. Bài tập được chuyển sang trạng thái ngưng hoạt động (`isActive = false`) để bảo toàn tham chiếu và giữ hành vi API nhất quán.
- **Hiện thực hóa kỹ thuật:** Chuẩn hóa một cột duy nhất `is_active` (boolean). Mapping JPA của MVP dùng `@SQLDelete` để cập nhật `is_active = false, updated_at = CURRENT_TIMESTAMP(6)` và `@Where(clause = "is_active = true")` cho truy vấn danh mục hiện hành. Truy vấn lịch sử cần đọc Exercise inactive dùng native DTO projection để không bị entity-level filter che dữ liệu. Không đồng thời dùng cả `is_active` và `deleted`, tránh hai nguồn trạng thái mâu thuẫn.


### [BR-15] - Trạng thái tài khoản sau đăng ký (Registration Account State)
- **Mô tả nghiệp vụ:** Sau khi đăng ký thành công, tài khoản phải ở trạng thái PENDING cho đến khi Admin xác nhận, hoặc hệ thống có thể mặc định kích hoạt ngay (AUTO-ACTIVE). Phải định nghĩa rõ trong cấu hình và ghi vào tài liệu quyết định.
- **Hiện thực hóa kỹ thuật:** Trong MVP, áp dụng **AUTO-ACTIVE**: Hội viên mới được gán quyền `ROLE_MEMBER` và trạng thái `accountStatus = ACTIVE` ngay sau khi đăng ký nhưng **chưa có gói tập Active**. Gói tập được kích hoạt riêng bởi luồng Subscription Management.

### [BR-16] - Kiểm thử trạng thái khóa tài khoản (Locked Account Testing)
- **Mô tả nghiệp vụ:** Hệ thống ngăn chặn rõ ràng tài khoản LOCKED khỏi đăng nhập và sử dụng dịch vụ, kể cả khi họ cung cấp đúng mật khẩu.
- **Hiện thực hóa kỹ thuật:** Khi đăng nhập, `UserDetailsService` kiểm tra `accountStatus`. Trên request cần xác thực, `AccountStatusGuard` hoặc Method Security kiểm tra trạng thái; nếu `LOCKED` → trả HTTP 403 với `ACC-004`. `JwtAuthenticationFilter` xác thực chữ ký/hạn dùng và có thể nạp identity/roles qua `UserDetailsService` để thiết lập `SecurityContext`, nhưng không dùng `accountStatus` để quyết định chặn request. Nếu Guard dùng cache, thao tác lock/unlock phải cập nhật hoặc evict cache trạng thái đồng bộ sau transaction để request tiếp theo quan sát ngay trạng thái mới.

### [BR-17] - Dọn dẹp tài khoản chưa hoàn tất đăng ký (Stale Registration Cleanup)
- **Mô tả nghiệp vụ:** Nếu trong tương lai hệ thống áp dụng luồng xác nhận email (OTP/Token), các tài khoản chưa xác thực email sau **24 giờ** phải bị xóa tự động để giải phóng chỗ cho Email/Username trùng lặp.
- **Hiện thực hóa kỹ thuật:** Triển khai `@Scheduled` Job chạy hằng ngày; truy vấn các tài khoản `status=PENDING` và `createdAt < now() - 24h`; thực hiện DELETE. **Quy tắc này là Should-have trong MVP.**

### [BR-18] - Chính sách mật khẩu (Password Policy)
- **Mô tả nghiệp vụ:** Mật khẩu tối thiểu **8 ký tự**, tối đa **72 ký tự** (giới hạn BCrypt), không chứa toàn khoảng trắng, chứa ít nhất 1 chữ hoa và 1 chữ số. Trường `confirmPassword` trong đăng ký công khai phải khớp chính xác với `password`. Không lưu trữ mật khẩu dưới dạng plaintext.
- **Hiện thực hóa kỹ thuật:** Không trim hoặc thay đổi mật khẩu do người dùng nhập. Từ chối mật khẩu có khoảng trắng ở đầu/cuối và kiểm tra cú pháp bằng `@Pattern(regexp = "^(?!\\s)(?!.*\\s$)(?=.*[A-Z])(?=.*\\d).{8,72}$")` tại DTO lớp `RegisterRequest`. Validate chéo `password.equals(confirmPassword)` trước khi mã hóa; `confirmPassword` không được lưu, log hoặc trả về response. Mã hóa bằng `BCryptPasswordEncoder(12)` trước khi lưu vào DB.

### [BR-19] - Giải quyết xung đột dữ liệu tiến trình (Progress Data Conflict Resolution)
- **Mô tả nghiệp vụ:** Nếu hội viên cập nhật nhật ký tập luyện có `logDate` và `exerciseId` trùng với một bản ghi của chính hội viên đã tồn tại, hệ thống kích hoạt chế độ **Update-in-place** thay vì tạo bản sao mới. Timestamp `updatedAt` được cập nhật tự động qua JPA Auditing.
- **Hiện thực hóa kỹ thuật:** Logic tại `WorkoutLogService.saveOrUpdate()`: nếu tồn tại bản ghi `(memberId, logDate, exerciseId)` → gọi `.save()` trên Entity đã tải để JPA Dirty-Checking cập nhật. Thiết lập unique constraint `(member_id, log_date, exercise_id)` ở Database để chống race condition.

### [BR-20] - Chuẩn hóa Email (Email Trim & Lowercase)
- **Mô tả nghiệp vụ:** Trước bất kỳ thao tác nào (kiểm tra tồn tại, đăng nhập, lưu mới), Email phải được **trim khoảng trắng đầu/cuối** và **chuyển lowercase** để kiểm tra trùng có ý nghĩa. Quy tắc này bổ sung thêm chi tiết thực hiện cho BR-01.
- **Hiện thực hóa kỹ thuật:** `AuthService.register()` chuẩn hóa bằng `email.trim().toLowerCase(Locale.ROOT)` trước khi kiểm tra tồn tại và lưu. `CustomUserDetailsService.loadUserByUsername()` áp dụng cùng quy tắc trước khi gọi `UserRepository.findByEmailWithRolesIgnoreCase()`. Không để Spring Security truy vấn bằng email chưa chuẩn hóa.

### [BR-21] - Chặn tài khoản DISABLED (Permanently Disabled Account Block)
- **Mô tả nghiệp vụ:** Tài khoản ở trạng thái `DISABLED` (vô hiệu hóa vĩnh viễn, khác `LOCKED` có thể mở lại) không được đăng nhập, và token của tài khoản đã `DISABLED` cũng phải bị hệ thống từ chối trên mọi request yêu cầu xác thực.
- **Hiện thực hóa kỹ thuật:** Enum `AccountStatus`: `ACTIVE`, `LOCKED`, `DISABLED`. Khi đăng nhập, `UserDetailsService` kiểm tra trạng thái tài khoản. Trên mỗi endpoint yêu cầu xác thực, `AccountStatusGuard` hoặc Method Security kiểm tra trạng thái; nếu `DISABLED` → ném `DisabledException` (hoặc custom exception tương ứng), trả HTTP 403 với Error Code `ACC-006`. `JwtAuthenticationFilter` có thể truy vấn User/Role qua `UserDetailsService` để dựng principal nhưng không truy vấn hoặc đánh giá `accountStatus`; subscription được kiểm tra riêng bằng Method Security.

### [BR-22] - Chỉ duy nhất một bản ghi tiến trình thể trạng trong một ngày (Body Progress Daily Uniqueness)
- **Mô tả nghiệp vụ:** Mỗi hội viên chỉ có tối đa một bản ghi `BodyProgress` (cân nặng, các chỉ số thể trạng) trong một ngày cụ thể. Nếu hội viên nhập lại trong cùng ngày, hệ thống **cập nhật (Update-in-place)** bản ghi cũ, không tạo mới.
- **Hiện thực hóa kỹ thuật:** `recordDate` là ngày nghiệp vụ theo timezone `Asia/Ho_Chi_Minh` và lưu bằng SQL `DATE`; `createdAt`/`updatedAt` lưu UTC. Database thiết lập unique constraint `(member_id, record_date)`. Service dùng một câu lệnh MySQL nguyên tử `INSERT INTO body_progress ... ON DUPLICATE KEY UPDATE weight_kg = :weightKg, updated_at = CURRENT_TIMESTAMP(6)` để chống race condition giữa hai request đồng thời; sau đó tải bản ghi và ánh xạ sang DTO. Không bắt lỗi duplicate rồi tiếp tục trong cùng transaction vì transaction có thể đã bị đánh dấu rollback-only.

---

### [BR-23] - Chuẩn hóa và Kiểm duyệt Hồ sơ Thể chất (Physical Profile Validation Rules)
- **Mô tả nghiệp vụ:** Hồ sơ dùng cho tính toán sinh học và AI Recommendation phải hợp lệ trước khi được lưu hoặc gửi sang AI Engine. Trong MVP, `gender` chỉ nhận `MALE` hoặc `FEMALE` để ánh xạ tất định vào hai nhánh công thức BMR Mifflin-St Jeor; `dateOfBirth` không được ở tương lai; `heightCm` và `weightKg` phải lớn hơn 0; `workoutDaysPerWeek` từ 1 đến 7; `maxSessionMinutes` phải lớn hơn 0. `activityLevel` chỉ nhận `SEDENTARY`, `LIGHTLY_ACTIVE`, `MODERATELY_ACTIVE`, `VERY_ACTIVE`; `dietaryPreference` chỉ nhận `OMNIVORE`, `VEGETARIAN`, `VEGAN`; `mealsPerDay` phải từ **1 đến 6**. Hai danh sách `foodAllergies` và `excludedFoods` chứa tối đa **10** phần tử mỗi danh sách, mỗi phần tử tối đa **50** ký tự.
- **Hiện thực hóa kỹ thuật:** Backend validate các trường scalar và Enum tại DTO; trim từng phần tử của danh sách, loại bỏ ký tự điều khiển nguy hiểm (control characters) trước khi lưu/prompting, loại bỏ phần tử rỗng và kiểm tra giới hạn số phần tử/độ dài. Chỉ sau khi toàn bộ trường hợp lệ, `RecommendationService` mới được tạo AI Payload; nếu không, trả `VAL-001` và tuyệt đối không gọi AI Engine.

### [BR-24] - Quy trình gia hạn Gói dịch vụ (Subscription Renewal Constraint)
- **Mô tả nghiệp vụ:** BR-04 áp dụng cho đăng ký gói mới và cấm tạo thêm Subscription `ACTIVE` song song. Khi hội viên đã có một Subscription `ACTIVE` hợp lệ và yêu cầu gia hạn, hệ thống tạo một bản ghi yêu cầu gia hạn ở trạng thái `PENDING`, liên kết tới Subscription `ACTIVE` hiện tại; yêu cầu này không tạo thêm gói `ACTIVE`. Mỗi Subscription chỉ được có tối đa một Renewal Request `PENDING` tại một thời điểm.
- **Hiện thực hóa kỹ thuật:** Trước khi tạo Renewal Request, nếu đã tồn tại yêu cầu `PENDING` cho cùng Subscription thì trả `SUB-006`. Khi Admin duyệt, Service khóa renewal request và subscription đích, kiểm tra BR-25 cùng `package.isActive = true`, cập nhật trực tiếp `newEndDate = currentEndDate + packageDurationDays`, rồi đánh dấu request `PROCESSED` trong cùng transaction. Cả hai Entity có `@Version`; xung đột khóa/version trả `CON-001`, không dùng `SUB-006`. Không tạo một thực thể `ACTIVE` thứ hai.

### [BR-25] - Hiệu lực động của Subscription ACTIVE (Active Subscription Validity)
- **Mô tả nghiệp vụ:** Một Subscription chỉ cấp quyền sử dụng tính năng cao cấp khi đồng thời thỏa mãn `status = ACTIVE`, `startDate <= currentDate` và `currentDate < endDate`. `endDate` là mốc hết hiệu lực dạng **exclusive**; từ đầu ngày `endDate`, subscription không còn hợp lệ dù bản ghi chưa được Scheduled Job chuyển sang `EXPIRED`.
- **Hiện thực hóa kỹ thuật:** `currentDate` được xác định theo timezone nghiệp vụ `Asia/Ho_Chi_Minh`. `SubscriptionGuard` hoặc Method Security phải truy vấn theo toàn bộ điều kiện hiệu lực ở mỗi request cần gói ACTIVE. Scheduled Job chạy hằng ngày có thể cập nhật các bản ghi quá hạn sang `EXPIRED` để đồng bộ dữ liệu, nhưng quyết định phân quyền không được chỉ dựa vào Job. Member có gói đã hết hạn vẫn được đăng nhập và xem dữ liệu lịch sử, nhưng phải sử dụng luồng đăng ký gói mới; Renewal Request trong MVP chỉ áp dụng cho Subscription còn hiệu lực. Các thao tác tạo recommendation AI, kích hoạt giáo án và ghi workout log bị chặn bằng `SUB-001` khi không có Subscription hợp lệ.

### [BR-26] - Vòng đời và kích hoạt giáo án (Workout Plan Lifecycle)
- **Mô tả nghiệp vụ:** Giáo án có ba trạng thái `DRAFT`, `ACTIVE`, `ARCHIVED`. Recommendation hợp lệ hoặc fallback tạo giáo án ở trạng thái `DRAFT`. Mỗi Member chỉ có tối đa một giáo án `ACTIVE`; khi kích hoạt một giáo án `DRAFT`, hệ thống chuyển giáo án `ACTIVE` cũ (nếu có) sang `ARCHIVED`, rồi chuyển giáo án đích sang `ACTIVE` trong cùng transaction.
- **Hiện thực hóa kỹ thuật:** Chỉ chủ sở hữu có Subscription hợp lệ theo BR-25 mới được kích hoạt giáo án. Service khóa các plan của Member, chuyển plan ACTIVE cũ sang `ARCHIVED` và flush để giải phóng `active_member_key`, sau đó kích hoạt plan DRAFT và flush lần hai trong cùng transaction. `@Version` cùng unique generated key chặn hai request đồng thời. Không dùng trạng thái `INACTIVE`.

### [BR-27] - Kiểm duyệt danh mục gói tập (Membership Package Validation)
- **Mô tả nghiệp vụ:** Tên gói tập sau khi trim phải dài 3–100 ký tự và duy nhất không phân biệt hoa/thường; `durationDays` từ 1–3650; `price` không âm; `description` tối đa 1000 ký tự. Vô hiệu hóa gói không làm mất hiệu lực các Subscription đã ACTIVE trước đó, nhưng chặn đăng ký/gia hạn mới theo BR-05.
- **Hiện thực hóa kỹ thuật:** Chuẩn hóa tên trước khi kiểm tra và tạo unique index trên giá trị tên chuẩn hóa. Trùng tên trả `SUB-007` (HTTP 409); các vi phạm giới hạn còn lại trả `VAL-001` (HTTP 400). Khi cập nhật, loại trừ chính `packageId` hiện hành khỏi truy vấn kiểm tra trùng; request update không được thay đổi `isActive`.

### [BR-28] - Toàn vẹn tham chiếu Workout Log (Workout Log Reference Integrity)
- **Mô tả nghiệp vụ:** Khi ghi nhật ký, `workoutPlanDetailId` phải thuộc giáo án `ACTIVE` của chính Member; `exerciseId` phải đúng với bài tập gắn trên chi tiết giáo án đó; `logDate` không được ở tương lai theo timezone nghiệp vụ. Member không thể dùng ID của giáo án hoặc chi tiết thuộc người khác.
- **Hiện thực hóa kỹ thuật:** Service tải chi tiết bằng truy vấn kết hợp `detailId`, `memberId` và `planStatus = ACTIVE`; không tìm thấy trả `WRK-001` (HTTP 404). Nếu `exerciseId` không khớp chi tiết hoặc `logDate > currentDate`, trả `VAL-001` (HTTP 400). Chỉ sau các kiểm tra này mới áp dụng BR-19 để tạo mới hoặc update-in-place.

---

## 6. Bảng mã lỗi công khai (Public Error Code Registry)

Bảng này xác lập các `ErrorCode` cần kết xuất trong phản hồi API lỗi (`ApiErrorResponse`) của hệ thống. Mã lỗi được đặt theo nhóm chức năng, giúp Frontend xử lý logic phía client.

| Mã lỗi | Nhóm | HTTP Status | Mô tả ngắn |
| :--- | :--- | :---: | :--- |
| `AUTH-002` | Xác thực/Phân quyền | 403 | Token hợp lệ nhưng tài khoản không có role cần thiết để truy cập tài nguyên |
| `ACC-001` | Tài khoản | 409 | Email đã được dùng |
| `ACC-002` | Tài khoản | 400 | Định dạng mật khẩu không hợp lệ |
| `ACC-004` | Tài khoản | 403 | Tài khoản bị khóa (LOCKED) |
| `ACC-005` | Tài khoản | 401 | JWT Token hết hạn hoặc không hợp lệ |
| `ACC-006` | Tài khoản | 403 | Tài khoản đã bị vô hiệu hóa vĩnh viễn (DISABLED) |
| `ACC-007` | Tài khoản | 401 | Email hoặc mật khẩu đăng nhập không chính xác |
| `PROF-001` | Hồ sơ hội viên | 404 | Member hiện hành chưa hoàn thiện hồ sơ thể trạng |
| `SUB-001` | Gói tập | 403 | Không có gói tập Active |
| `SUB-002` | Gói tập | 404 | Không tìm thấy gói tập |
| `SUB-003` | Gói tập | 409 | Gói tập đã ngừng hoạt động, không thể đăng ký hoặc gia hạn |
| `SUB-004` | Gói tập | 409 | Đã có gói tập ACTIVE, không thể đăng ký gói mới |
| `SUB-005` | Gói tập | 404 | Không tìm thấy Subscription hoặc Subscription không thuộc Member hiện hành |
| `SUB-006` | Gói tập | 409 | Đã tồn tại yêu cầu đăng ký mới hoặc gia hạn cùng loại ở trạng thái PENDING |
| `SUB-007` | Gói tập | 409 | Tên gói tập đã tồn tại |
| `EXR-001` | Bài tập | 404 | ID bài tập không tồn tại |
| `EXR-002` | Bài tập | 409 | Tên bài tập đã tồn tại |
| `WRK-001` | Giáo án | 404 | Không tìm thấy giáo án/chi tiết giáo án hoặc tài nguyên không thuộc Member hiện hành |
| `VAL-001` | Validation | 400 | Dữ liệu đầu vào hoặc trạng thái request không hợp lệ |
| `AI-001` | AI Engine | 502 | Không thể tạo recommendation vì cả AI Engine và cơ chế fallback đều thất bại |
| `CON-001` | Đồng thời | 409 | Dữ liệu đã được request khác cập nhật hoặc tài nguyên đang bị khóa; client phải tải lại trạng thái trước khi thử lại |
| `SYS-001` | Hệ thống | 500 | Thiếu dữ liệu cấu hình bắt buộc hoặc xảy ra lỗi nội bộ không thể công khai chi tiết |

### Warning Code Registry cho response fallback HTTP 200

| Warning Code | Điều kiện | HTTP Status | Ý nghĩa |
| :--- | :--- | :---: | :--- |
| `AI_TIMEOUT` | AI timeout, HTTP 429 hoặc lỗi 5xx sau chính sách retry | 200 | Backend trả recommendation từ template tĩnh |
| `AI_RESPONSE_INVALID` | AI trả sai JSON Schema, ID ngoài whitelist hoặc planned values sai | 200 | Backend từ chối payload AI và trả template tĩnh |
