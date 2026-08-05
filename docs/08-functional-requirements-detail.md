# 08. Đặc tả yêu cầu chức năng chi tiết

## 1. Mục đích tài liệu
Tài liệu này thực hiện phân rã các nhóm chức năng trong phạm vi sản phẩm khả dụng tối thiểu (MVP) thành các yêu cầu chức năng (Functional Requirements - FR) có mã định danh và mô tả nghiệp vụ chi tiết. Đây là cơ sở cốt lõi để đội ngũ phát triển thiết kế cơ sở dữ liệu (Database Schema), xây dựng các API Endpoints, kiểm soát phân quyền hệ thống và thiết lập các kịch bản kiểm thử (Test Cases) nhằm đảm bảo tính toàn vẹn và nhất quán của toàn bộ hệ thống "Hệ thống quản lý phòng gym thông minh".

## 2. Quy ước mã yêu cầu chức năng
- **FR-AUTH-xx:** Nhóm xác thực, phân quyền và bảo mật tài khoản.
- **FR-PROFILE-xx:** Nhóm quản lý hồ sơ thể trạng và chỉ số cá nhân.
- **FR-SUB-xx:** Nhóm quản lý gói tập và đăng ký thành viên (Subscription).
- **FR-EXR-xx:** Nhóm quản lý thư viện bài tập và danh mục cơ sở.
- **FR-WORKOUT-xx:** Nhóm lập lịch, theo dõi buổi tập và tương tác lịch tập AI.
- **FR-NUTRITION-xx:** Nhóm tính toán sinh học và gợi ý thực đơn dinh dưỡng.
- **FR-PROGRESS-xx:** Nhóm theo dõi, lưu trữ tiến độ biến đổi thể chất.
- **FR-ADMIN-xx:** Nhóm quản trị hệ thống và báo cáo đếm cơ bản.

## 3. Danh sách yêu cầu chức năng theo module

### [FR-AUTH-01] Đăng ký tài khoản Member
**Mức ưu tiên:** Must-have
**Actor:** Anonymous Guest
**Mô tả:** Khách vãng lai đăng ký tài khoản mới để tham gia vào hệ thống với tư cách hội viên. Hệ thống tự động gán vai trò hội viên, trạng thái tài khoản kích hoạt và lưu trữ bảo mật thông tin.
**Input chính:** `fullName`, `email`, `password`, `confirmPassword`
**Output:** Bản ghi User mới trong cơ sở dữ liệu với vai trò `ROLE_MEMBER`, trạng thái `accountStatus = ACTIVE`, mật khẩu được băm và trả về JSON chứa thông tin tài khoản cơ bản (không kèm mật khẩu).
**Business Rules liên quan:** BR-01 (Duy nhất email), BR-02 (Mã hóa mật khẩu), BR-15 (Trạng thái Auto-Active & ROLE_MEMBER), BR-18 (Chính sách mật khẩu), BR-20 (Chuẩn hóa email).
**Ghi chú kỹ thuật:** Email phải được trim và chuyển chữ thường trước khi kiểm tra trùng và lưu. Validate mật khẩu bằng Regex, kiểm tra `confirmPassword` khớp chính xác với `password`, sau đó mới mã hóa qua BCrypt. Không cho Client truyền `role` hoặc `accountStatus`; `confirmPassword` không được lưu vào DB.

### [FR-AUTH-02] Đăng nhập và nhận JWT
**Mức ưu tiên:** Must-have
**Actor:** Member, Admin
**Mô tả:** Người dùng đăng nhập hệ thống bằng email và mật khẩu. Nếu thông tin chính xác, hệ thống trả về một mã token JWT chứa thông tin định danh và vai trò để sử dụng cho các yêu cầu tiếp theo.
**Input chính:** `email`, `password`
**Output:** Access Token JWT hợp lệ chứa thông tin định danh (email, roles) và thời hạn hết hạn (Expiration Time). Sai email hoặc mật khẩu trả `ACC-007` (HTTP 401) mà không tiết lộ trường nào sai.
**Business Rules liên quan:** BR-16 (Chặn tài khoản LOCKED), BR-18 (Chính sách mật khẩu), BR-20 (Chuẩn hóa email), BR-21 (Chặn tài khoản DISABLED).
**NFR liên quan:** NFR-06 (Bảo vệ JWT Token), NFR-14 (Không log mật khẩu/JWT).
**Ghi chú kỹ thuật:** Email gửi lên phải được trim và lowercase trước khi tìm trong DB. Mật khẩu được so sánh bằng phương thức `passwordEncoder.matches()`. Token được ký bằng thuật toán HMAC-SHA256 với khóa bí mật dài tối thiểu 256-bit đọc từ biến môi trường.

### [FR-AUTH-03] Chặn tài khoản LOCKED/DISABLED
**Mức ưu tiên:** Must-have
**Actor:** Hệ thống
**Mô tả:** Hệ thống ngăn chặn những tài khoản đã bị khóa (LOCKED) hoặc vô hiệu hóa vĩnh viễn (DISABLED) thực hiện đăng nhập hoặc truy cập tài nguyên, ngay cả khi họ cung cấp đúng mật khẩu hoặc có JWT token cũ chưa hết hạn.
**Input chính:** `email` (khi đăng nhập), `Authorization` header chứa JWT (khi gửi request)
**Output:** HTTP Status 403 Forbidden kèm mã lỗi cụ thể (`ACC-004` cho LOCKED, `ACC-006` cho DISABLED).
**Business Rules liên quan:** BR-16 (Chặn đăng nhập tài khoản LOCKED), BR-21 (Chặn đăng nhập và token tài khoản DISABLED).
**Ghi chú kỹ thuật:** `JwtAuthenticationFilter` xác thực tính hợp lệ của Token (chữ ký, hết hạn), nạp identity/roles qua `UserDetailsService` và thiết lập Security Context; Filter không truy vấn hoặc đánh giá `accountStatus`. Khi đăng nhập, `UserDetailsService` kiểm tra `accountStatus`. Trên các endpoint yêu cầu xác thực, `AccountStatusGuard` (Custom Interceptor/Guard) hoặc Method Security kiểm tra `accountStatus`; nếu là `LOCKED` hoặc `DISABLED` thì từ chối request.

### [FR-AUTH-04] Phân quyền endpoint theo role
**Mức ưu tiên:** Must-have
**Actor:** Hệ thống
**Mô tả:** Hệ thống tự động kiểm tra vai trò của tài khoản thực hiện request và chặn đứng các truy cập trái phép đối với các tài nguyên nghiệp vụ thuộc về phân hệ quản trị hoặc phân hệ chuyên biệt.
**Input chính:** `Authorization` header chứa JWT, HTTP Request Path & Method
**Output:** HTTP Status 403 Forbidden với `AUTH-002` nếu người dùng không đủ quyền hạn, ngược lại cho phép thực thi API.
**Business Rules liên quan:** BR-03 (Giới hạn quyền quản trị của Member).
**Ghi chú kỹ thuật:** Sử dụng các cấu hình phân quyền tại Spring Security Filter Chain (`requestMatchers`) và Method Security (`@PreAuthorize`) bảo vệ API.

### [FR-AUTH-05] Lấy thông tin người dùng hiện tại
**Mức ưu tiên:** Must-have
**Actor:** Member, Admin
**Mô tả:** Người dùng yêu cầu hệ thống cung cấp thông tin tài khoản hiện tại để hiển thị trên giao diện làm việc sau khi đăng nhập thành công.
**Input chính:** `Authorization` header chứa JWT
**Output:** Thông tin chi tiết tài khoản hiện tại gồm ID, Họ tên, Email, Vai trò.
**Business Rules liên quan:** Không có.
**Ghi chú kỹ thuật:** Sau khi `AccountStatusGuard` đã kiểm tra trạng thái tài khoản qua DB/Cache, endpoint đọc các trường định danh cơ bản từ Security Context (Principal) để tránh một lần truy vấn lặp. `JwtAuthenticationFilter` thiết lập Context từ token hợp lệ và không thay thế `AccountStatusGuard`.

---

### [FR-PROFILE-01] Xem hồ sơ thể trạng cá nhân
**Mức ưu tiên:** Must-have
**Actor:** Member
**Mô tả:** Hội viên xem thông tin hồ sơ thể chất chi tiết của chính mình (chiều cao, cân nặng, mục tiêu, chấn thương, dị ứng...) cùng các chỉ số sinh học đã được tính toán.
**Input chính:** `Authorization` header chứa JWT
**Output:** JSON chứa thông tin hồ sơ thể trạng cá nhân (Bio Profile) và các chỉ số sinh học tính toán của hội viên (`calculatedTargets`).
**Business Rules liên quan:** BR-13 (Giới hạn quyền sở hữu dữ liệu).
**Ghi chú kỹ thuật:** Kiểm tra xem người yêu cầu có đúng là chủ sở hữu hồ sơ đó hay không (so sánh UserId trích xuất từ JWT với UserId của hồ sơ). Quyền xem/giám sát dữ liệu hội viên của PT thuộc phân hệ Should-have, được thiết kế sẵn cấu trúc phân quyền nhưng không nằm trong tiêu chí nghiệm thu MVP.

### [FR-PROFILE-02] Cập nhật hồ sơ thể trạng
**Mức ưu tiên:** Must-have
**Actor:** Member
**Mô tả:** Hội viên cập nhật các thông số thể chất cơ bản như chiều cao, cân nặng, chấn thương và thiết bị sẵn có. Hệ thống lưu lại và kích hoạt luồng tính toán chỉ số dinh dưỡng mới.
**Input chính:** `gender` (Enum: `MALE`, `FEMALE`), `dateOfBirth`, `heightCm`, `weightKg`, `activityLevel`, `workoutDaysPerWeek`, `maxSessionMinutes`, `availableEquipment`, `targetMuscleGroups`, `injuryConstraints`
**Output:** Hồ sơ thể chất mới được lưu trữ trong DB, tự động kích hoạt tính toán lại BMI, BMR, TDEE, Calories và Macros.
**Business Rules liên quan:** BR-13 (Quyền sở hữu dữ liệu), BR-22 (Chỉ có 1 bản ghi tiến độ thể chất trong ngày), BR-23 (Kiểm duyệt hồ sơ thể chất).
**Ghi chú kỹ thuật:** `PUT /api/v1/member/profile` chỉ upsert `MemberProfile` và trả lại các chỉ số Calculator trong response. Từ Ngày 14, Frontend gọi độc lập `POST /api/v1/member/body-progress` sau khi Profile thành công, sử dụng cân nặng trong response và ngày nghiệp vụ Việt Nam. Nếu Progress thất bại, Profile vẫn được xem là đã lưu; giao diện thông báo lỗi riêng và cho retry. Cách tách hai API không giả lập một transaction xuyên module, đồng thời giữ đúng ownership, timezone và update-in-place BR-22.

### [FR-PROFILE-03] Cập nhật mục tiêu tập luyện
**Mức ưu tiên:** Must-have
**Actor:** Member
**Mô tả:** Hội viên cập nhật mục tiêu thể chất mong muốn và trình độ tập luyện hiện tại của mình để hệ thống điều chỉnh thông số Calorie/Macro đích và chuẩn bị cho yêu cầu gợi ý AI.
**Input chính:** `fitnessGoal` (Enum: BULK, CUT, MAINTAIN), `fitnessLevel` (Enum: BEGINNER, INTERMEDIATE, ADVANCED)
**Output:** Cập nhật thông tin mục tiêu trong hồ sơ thể chất và trả về các chỉ số dinh dưỡng mới tương ứng.
**Business Rules liên quan:** BR-13 (Quyền sở hữu dữ liệu), BR-23 (Kiểm duyệt hồ sơ thể chất).
**Ghi chú kỹ thuật:** `fitnessGoal` sẽ ảnh hưởng đến việc thặng dư hoặc thâm hụt calorie đích trong công thức tính toán.

### [FR-PROFILE-04] Cập nhật dữ liệu dinh dưỡng cá nhân
**Mức ưu tiên:** Must-have
**Actor:** Member
**Mô tả:** Hội viên cập nhật các thông tin liên quan đến chế độ ăn uống cá nhân để hạn chế tối đa các thành phần thực phẩm không mong muốn trong gợi ý từ AI.
**Input chính:** `dietaryPreference` (Enum: OMNIVORE, VEGETARIAN, VEGAN), `foodAllergies` (List), `excludedFoods` (List), `mealsPerDay` (Integer)
**Output:** Hồ sơ thể trạng của hội viên được cập nhật dữ liệu dinh dưỡng cá nhân.
**Business Rules liên quan:** BR-13 (Quyền sở hữu dữ liệu), BR-23 (Kiểm duyệt hồ sơ thể chất).
**Ghi chú kỹ thuật:** Dữ liệu này sẽ được cấu trúc hóa vào prompt gửi sang AI Engine để tối ưu thực đơn gợi ý.

---

### [FR-SUB-01] Admin tạo gói tập
**Mức ưu tiên:** Must-have
**Actor:** Admin
**Mô tả:** Quản trị viên khởi tạo các gói dịch vụ/gói tập mới của phòng gym để cung cấp ra công chúng.
**Input chính:** `name`, `durationDays`, `price`, `description`
**Output:** Gói tập mới được lưu vào database với trạng thái mặc định là `isActive = true`.
**Business Rules liên quan:** BR-03 (Chỉ Admin có quyền), BR-27 (Kiểm duyệt danh mục gói tập).
**Ghi chú kỹ thuật:** Tên gói được trim và kiểm tra duy nhất không phân biệt hoa/thường. Trùng tên trả `SUB-007`; dữ liệu vượt giới hạn trả `VAL-001`.

### [FR-SUB-02] Admin chỉnh sửa gói tập
**Mức ưu tiên:** Must-have
**Actor:** Admin
**Mô tả:** Quản trị viên cập nhật lại thông tin của một gói tập hiện có trên hệ thống (giá tiền, mô tả...).
**Input chính:** `id` (path variable), `name`, `durationDays`, `price`, `description`
**Output:** Cập nhật thông tin gói tập tương ứng trong database.
**Business Rules liên quan:** BR-03 (Chỉ Admin có quyền), BR-27 (Kiểm duyệt danh mục gói tập).
**Ghi chú kỹ thuật:** Kiểm tra trùng tên nhưng loại trừ chính package đang sửa. Request update chỉ thay đổi metadata, không thay đổi `isActive`; gói đã vô hiệu hóa vẫn giữ `isActive = false`. Việc kích hoạt lại không thuộc API MVP.

### [FR-SUB-03] Admin vô hiệu hóa gói tập
**Mức ưu tiên:** Must-have
**Actor:** Admin
**Mô tả:** Quản trị viên tạm ngưng kinh doanh một gói tập để không cho phép đăng ký mới.
**Input chính:** `id` (path variable)
**Output:** Trường `isActive` của gói tập chuyển sang `false`.
**Business Rules liên quan:** BR-03 (Quyền Admin), BR-05 (Chặn đăng ký gói đã ngưng hoạt động).
**Ghi chú kỹ thuật:** Áp dụng Soft Inactive. Tuyệt đối không xóa cứng khỏi cơ sở dữ liệu để đảm bảo các subscription hiện tại đang dùng gói này vẫn giữ nguyên liên kết và tiếp tục chu kỳ đến khi hết hạn.

### [FR-SUB-04] Guest/Member xem danh sách gói tập công khai
**Mức ưu tiên:** Must-have
**Actor:** Anonymous Guest, Member
**Mô tả:** Khách vãng lai hoặc hội viên xem danh mục các gói tập đang mở bán của phòng gym để tham khảo hoặc chuẩn bị mua.
**Input chính:** Không có.
**Output:** Danh sách các gói tập có trạng thái `isActive = true` trong database.
**Business Rules liên quan:** Không có.
**Ghi chú kỹ thuật:** API này không yêu cầu token bảo mật để Guest có thể truy cập được từ trang giới thiệu.

### [FR-SUB-05] Member gửi yêu cầu đăng ký gói tập
**Mức ưu tiên:** Must-have
**Actor:** Member
**Mô tả:** Hội viên gửi yêu cầu đăng ký mua một gói tập cụ thể. Hệ thống tạo một yêu cầu ở trạng thái chờ duyệt.
**Input chính:** `packageId`
**Output:** Bản ghi Subscription mới được tạo ở trạng thái `PENDING`.
**Business Rules liên quan:** BR-04 (Chỉ cho phép tối đa 1 subscription ACTIVE đồng thời), BR-05 (Chặn đăng ký gói tập INACTIVE), BR-13 (Quyền sở hữu), BR-25 (Hiệu lực động của Subscription ACTIVE).
**Ghi chú kỹ thuật:** Đây chỉ là luồng đăng ký mới. Nếu hội viên có Subscription hợp lệ theo BR-25 (`status = ACTIVE`, `startDate <= currentDate < endDate`), từ chối bằng `SUB-004` (HTTP 409) và hướng sang endpoint gia hạn. Nếu đã có yêu cầu đăng ký mới `PENDING`, từ chối bằng `SUB-006` (HTTP 409). Admin phải kiểm tra lại BR-04 trong transaction khi phê duyệt.

### [FR-SUB-06] Admin xác nhận subscription mô phỏng
**Mức ưu tiên:** Must-have
**Actor:** Admin
**Mô tả:** Quản trị viên phê duyệt yêu cầu đăng ký mua gói tập của hội viên (thay thế cổng thanh toán thực tế).
**Input chính:** `requestId` (path variable), `requestType` (Enum trong request body: `NEW_SUBSCRIPTION` hoặc `RENEWAL`)
**Output:** Với đăng ký mới, Subscription chuyển thành `ACTIVE`, `startDate = ngày phê duyệt`, `endDate = startDate + durationDays`. Với gia hạn, Renewal Request chuyển thành `PROCESSED` và Subscription `ACTIVE` hiện tại được cộng dồn `endDate` mà không tạo bản ghi ACTIVE mới.
**Business Rules liên quan:** BR-03 (Quyền Admin), BR-04 (Một Subscription ACTIVE), BR-24 (Gia hạn cộng dồn), BR-25 (Hiệu lực động của Subscription).
**Ghi chú kỹ thuật:** `requestType` loại bỏ sự mơ hồ khi ID của Subscription Request và Renewal Request thuộc hai tập dữ liệu khác nhau. Thực hiện toàn bộ thay đổi trong một transaction. Với đăng ký mới, khóa các subscription của Member, chuyển bản ghi còn mang `status = ACTIVE` nhưng có `endDate <= currentDate` sang `EXPIRED`, sau đó kiểm tra lại điều kiện một ACTIVE trước khi chuyển yêu cầu `PENDING` thành `ACTIVE`. Với yêu cầu gia hạn, khóa và kiểm tra lại Subscription đích còn hiệu lực, chuyển Renewal Request `PENDING` thành `PROCESSED`, chỉ cập nhật `endDate` của Subscription `ACTIVE` liên kết và ghi nhận `approvedBy`. `MemberSubscription`, `SubscriptionRenewalRequest` dùng `@Version`; lỗi khóa hoặc version conflict trả `CON-001` (HTTP 409), không dùng `SUB-006`.

### [FR-SUB-07] Hệ thống kiểm tra subscription ACTIVE
**Mức ưu tiên:** Must-have
**Actor:** Hệ thống
**Mô tả:** Hệ thống xác minh hiệu lực gói dịch vụ của hội viên mỗi khi họ tạo recommendation AI, kích hoạt giáo án hoặc ghi workout log mới. Việc xem dữ liệu lịch sử của chính hội viên không bị chặn khi gói hết hạn.
**Input chính:** `Authorization` header chứa JWT; User ID lấy từ `AuthenticatedUserPrincipal` sau khi `CustomUserDetailsService` tải User từ database.
**Output:** Cho phép tiếp tục nếu tồn tại subscription thỏa mãn `status = ACTIVE`, `startDate <= currentDate < endDate`; ngược lại trả `SUB-001` (HTTP 403 Forbidden).
**Business Rules liên quan:** BR-25 (Hiệu lực động của Subscription ACTIVE).
**Ghi chú kỹ thuật:** Triển khai qua `SubscriptionGuard` và `@PreAuthorize("@subscriptionGuard.hasActiveSubscription(authentication)")` tại endpoint cần gói hợp lệ. Guard kiểm tra động đầy đủ trạng thái và khoảng ngày; không chỉ dựa vào cột trạng thái hoặc Scheduled Job cập nhật `EXPIRED`.

### [FR-SUB-08] Gia hạn gói tập đang ACTIVE
**Mức ưu tiên:** Must-have
**Actor:** Member (Gửi yêu cầu), Admin (Duyệt)
**Mô tả:** Hội viên gia hạn gói tập cùng loại đang sử dụng. Khi Admin duyệt, thời hạn mới sẽ được cộng dồn tiếp nối vào ngày kết thúc cũ thay vì tính từ ngày duyệt.
**Input chính:** `activeSubscriptionId` (path variable), `packageId` (request body, bắt buộc khớp với package của Subscription đang ACTIVE)
**Output:** Bản ghi Renewal Request mới ở trạng thái `PENDING` được tạo, liên kết với Subscription `ACTIVE`. Khi Admin duyệt, Renewal Request chuyển `PROCESSED` và trường `endDate` của Subscription đang ACTIVE được cập nhật: `newEndDate = currentEndDate + durationDays`.
**Business Rules liên quan:** BR-04 (Chặn đăng ký mới khi đã có gói ACTIVE), BR-05 (Gói tập phải ACTIVE), BR-24 (Quy trình gia hạn gói dịch vụ), BR-25 (Subscription hiện tại phải còn hiệu lực).
**Ghi chú kỹ thuật:** Endpoint gia hạn tách biệt với endpoint đăng ký mới: `POST /api/v1/member/subscriptions/{activeSubscriptionId}/renewal-requests`. Backend kiểm tra ownership và hiệu lực theo BR-25; không tìm thấy hoặc không thuộc Member trả `SUB-005` (HTTP 404). `packageId` không khớp package hiện hành trả `VAL-001` (HTTP 400). Đã có Renewal Request `PENDING` cho cùng Subscription trả `SUB-006` (HTTP 409). Không tạo Subscription `ACTIVE` song song.

### [FR-SUB-09] Admin hủy subscription PENDING/ACTIVE
**Mức ưu tiên:** Must-have
**Actor:** Admin
**Mô tả:** Quản trị viên hủy yêu cầu đăng ký chưa duyệt hoặc chấm dứt sớm gói tập đang sử dụng trong luồng quản trị mô phỏng của MVP.
**Input chính:** `subscriptionId` (path variable)
**Output:** Trạng thái Subscription chuyển thành `CANCELLED`.
**Business Rules liên quan:** BR-03 (Quyền Admin), BR-25 (Hiệu lực động của Subscription).
**Ghi chú kỹ thuật:** Khi gói tập chuyển sang `CANCELLED`, `SubscriptionGuard` từ chối ngay các thao tác yêu cầu gói ACTIVE. Luồng Member tự hủy không thuộc phạm vi MVP và không được đặc tả endpoint riêng.

---

### [FR-EXR-01] Admin tạo bài tập
**Mức ưu tiên:** Must-have
**Actor:** Admin
**Mô tả:** Quản trị viên thêm bài tập thể hình mới vào danh mục bài tập gốc của hệ thống.
**Input chính:** `name`, `primaryMuscleGroup`, `secondaryMuscleGroups`, `movementPattern`, `targetBodyRegions`, `equipmentRequired`, `difficultyLevel`, `contraindicationTags`, `instructionText`
**Output:** Bản ghi Exercise mới được lưu trong database với trạng thái mặc định `isActive = true`.
**Business Rules liên quan:** BR-03 (Quyền Admin).
**Ghi chú kỹ thuật:** Validate tên bài tập là duy nhất (Unique) trước khi lưu. `MuscleGroup` dùng `CHEST`, `BACK`, `SHOULDERS`, `ARMS`, `LEGS`, `GLUTES`, `CORE`, `CARDIO`, `FULL_BODY`; `MovementPattern` dùng `PUSH`, `PULL`, `HINGE`, `SQUAT`, `LUNGE`, `CARRY`, `ROTATION`. Các danh mục thiết bị, vùng cơ thể, độ khó và chống chỉ định phải khớp chính xác với các Enum đã chốt.

### [FR-EXR-02] Admin chỉnh sửa bài tập
**Mức ưu tiên:** Must-have
**Actor:** Admin
**Mô tả:** Quản trị viên cập nhật thông tin bài tập hiện có trong thư viện gốc.
**Input chính:** `id` (path variable), `name`, `primaryMuscleGroup`, `secondaryMuscleGroups`, `movementPattern`, `targetBodyRegions`, `equipmentRequired`, `difficultyLevel`, `contraindicationTags`, `instructionText`
**Output:** Thông tin bài tập được cập nhật thành công trong database.
**Business Rules liên quan:** BR-03 (Quyền Admin).
**Ghi chú kỹ thuật:** Không cho phép đổi tên bài tập thành tên đã trùng với một bài tập khác đang tồn tại.

### [FR-EXR-03] Admin xóa mềm bài tập
**Mức ưu tiên:** Must-have
**Actor:** Admin
**Mô tả:** Quản trị viên loại bỏ bài tập khỏi danh mục hoạt động mà không làm ảnh hưởng đến dữ liệu lịch sử tập luyện của hội viên.
**Input chính:** `id` (path variable)
**Output:** Trạng thái `isActive` của bài tập chuyển sang `false` (Soft Delete).
**Business Rules liên quan:** BR-03 (Quyền Admin), BR-14 (Xóa mềm danh mục bài tập).
**Ghi chú kỹ thuật:** Với Hibernate từ 6.4, dùng `@SoftDelete(strategy = SoftDeleteType.ACTIVE, columnName = "is_active")`; nếu không, viết query cập nhật `isActive = false` và lọc `isActive = true` ở các truy vấn danh mục hiện hành. Dữ liệu lịch sử tập vẫn hiển thị tên bài tập này bình thường.

### [FR-EXR-04] Member xem thư viện bài tập
**Mức ưu tiên:** Must-have
**Actor:** Member
**Mô tả:** Hội viên xem danh sách toàn bộ các bài tập đang hoạt động trong thư viện gốc của hệ thống.
**Input chính:** Không có.
**Output:** Danh sách các bài tập có trạng thái `isActive = true`.
**Business Rules liên quan:** Không có.
**Ghi chú kỹ thuật:** API này cần được bảo vệ bởi bộ lọc JWT, chỉ người dùng đã đăng nhập mới được phép truy cập. Quyền xem/giám sát dữ liệu hội viên của PT thuộc phân hệ Should-have, được thiết kế sẵn cấu trúc phân quyền nhưng không nằm trong tiêu chí nghiệm thu MVP.

### [FR-EXR-05] Tìm kiếm, lọc và phân trang bài tập
**Mức ưu tiên:** Must-have
**Actor:** Member, Admin
**Mô tả:** Cho phép người dùng tìm kiếm bài tập theo từ khóa, lọc theo nhóm cơ chính (`primaryMuscleGroup`), thiết bị yêu cầu (`equipmentRequired`) và độ khó (`difficultyLevel`) kèm phân trang.
**Input chính:** `page`, `size`, `searchQuery` (từ khóa tìm kiếm), `muscleGroup`, `equipment`, `difficulty`
**Output:** Danh sách phân trang (`Page<Exercise>`) thỏa mãn các điều kiện tìm kiếm và lọc.
**Business Rules liên quan:** Không có.
**Ghi chú kỹ thuật:** Sử dụng Spring Data JPA Specification để xây dựng câu truy vấn động an toàn chống SQL Injection.

### [FR-EXR-06] Backend lọc Exercise Whitelist cho AI
**Mức ưu tiên:** Must-have
**Actor:** Hệ thống
**Mô tả:** Hệ thống tự động tạo ra một danh sách ID các bài tập an toàn và khả dụng cho hội viên dựa trên thiết bị phòng tập sẵn có của họ và loại bỏ hoàn toàn các bài tập nằm trong chống chỉ định chấn thương.
**Input chính:** User ID
**Output:** Danh sách các ID bài tập hợp lệ (`exerciseIdWhitelist`) gửi sang AI Engine.
**Business Rules liên quan:** BR-10 (Xác thực bài tập đề xuất từ AI).
**Ghi chú kỹ thuật:** Sử dụng logic lọc tập hợp: Chỉ giữ lại các bài tập mà thiết bị yêu cầu của bài tập là tập con hoặc bằng tập thiết bị sẵn có của hội viên (`availableEquipment.containsAll(exercise.equipmentRequired)`), đồng thời không có chấn thương nào trùng với chống chỉ định của bài tập (`Collections.disjoint(exercise.contraindicationTags, member.injuryConstraints)`).

---

### [FR-WORKOUT-01] Member yêu cầu AI tạo lịch tập
**Mức ưu tiên:** Must-have
**Actor:** Member
**Mô tả:** Hội viên gửi yêu cầu lên hệ thống để gọi AI thiết lập một lộ trình tập luyện cá nhân hóa theo tuần.
**Input chính:** `workoutDaysPerWeek`, `maxSessionMinutes` (từ hồ sơ), `fitnessGoal`, `fitnessLevel`, `exerciseIdWhitelist` (do Backend tạo)
**Output:** Một cấu trúc `workoutSchedule` thô từ AI Engine; sau hậu kiểm thành công, Backend lưu giáo án với trạng thái `DRAFT` để Member chủ động kích hoạt.
**Business Rules liên quan:** BR-06 (Số lượng buổi tối thiểu), BR-07 (Số bài tập tối thiểu mỗi buổi), BR-10 (Lọc bài tập AI), BR-12 (Cảnh báo y tế), BR-25 (Subscription hợp lệ), BR-26 (Vòng đời giáo án).
**NFR liên quan:** NFR-02, NFR-08, NFR-13, NFR-14.
**Ghi chú kỹ thuật:** Sử dụng Structured Outputs. `PromptSanitizerService` chuẩn hóa structured fields, loại bỏ control characters/chỉ dẫn lồng ghép và không nhận `customPrompt` tự do trong MVP. Endpoint được bảo vệ bởi SubscriptionGuard; hệ thống hiển thị disclaimer trước khi gửi request.

### [FR-WORKOUT-02] Backend hậu kiểm kết quả AI
**Mức ưu tiên:** Must-have
**Actor:** Hệ thống
**Mô tả:** Backend thực hiện quét toàn bộ cấu trúc phản hồi từ AI để xác thực tính hợp lệ về mặt kỹ thuật và an toàn thể chất.
**Input chính:** JSON Payload từ AI Engine
**Output:** Lưu lịch tập ở trạng thái `DRAFT` nếu vượt qua hậu kiểm, ngược lại kích hoạt luồng Retry hoặc Fallback.
**Business Rules liên quan:** BR-08 (RPE kế hoạch từ 6-9), BR-09A (Giới hạn thông số planned), BR-10 (Xác thực bài tập AI phải có trong Whitelist).
**NFR liên quan:** NFR-13 (Chính sách retry và ngân sách thời gian).
**Ghi chú kỹ thuật:** Kiểm tra nghiêm ngặt: `plannedSets` (1-5), `plannedReps` (1-30), `plannedRpe` (6-9), `restSeconds` (30-300); số phần tử `workoutSchedule` phải đúng `workoutDaysPerWeek`, `dayNumber` duy nhất và liên tục từ 1, đồng thời không được lặp cùng `exerciseId` trong một workout day. Nếu bất kỳ bài tập nào có ID ngoài Whitelist hoặc bất kỳ điều kiện nào sai -> từ chối toàn bộ phản hồi.

### [FR-WORKOUT-03] Backend tạo fallback workout plan
**Mức ưu tiên:** Must-have
**Actor:** Hệ thống
**Mô tả:** Trong trường hợp AI Engine gặp sự cố mạng, bị timeout, trả về sai JSON Schema hoặc liên tục đề xuất bài tập sai quy định (sau 1 lần retry), hệ thống tự động gán giáo án mẫu tĩnh.
**Input chính:** `fitnessLevel`, `workoutDaysPerWeek`, `exerciseIdWhitelist` của hội viên
**Output:** Giáo án mẫu được lọc lại theo whitelist, hậu kiểm và lưu ở trạng thái `DRAFT`. Nếu nguyên nhân là timeout/429/5xx, response HTTP 200 có `warningCode = AI_TIMEOUT`; nếu nguyên nhân là sai schema, ID ngoài whitelist hoặc planned values sai, response HTTP 200 có `warningCode = AI_RESPONSE_INVALID`. Nếu không tạo được fallback an toàn, trả `AI-001` (HTTP 502) và không lưu dữ liệu một phần.
**Business Rules liên quan:** BR-11 (Cơ chế dự phòng khi AI lỗi), BR-26 (Vòng đời giáo án).
**NFR liên quan:** NFR-02, NFR-04, NFR-13.
**Ghi chú kỹ thuật:** Cấu hình Circuit Breaker/TimeLimiter (30s tổng, 15s mỗi lần gọi) qua Resilience4j. Fallback phải dùng cùng whitelist và cùng Post-Validation Hook như AI response; không được chọn bài tập tương đương ngoài whitelist.

### [FR-WORKOUT-04] Member xem lịch tập hiện tại
**Mức ưu tiên:** Must-have
**Actor:** Member
**Mô tả:** Hội viên xem lộ trình tập luyện đang hoạt động của mình được phân bổ chi tiết theo từng ngày trong tuần.
**Input chính:** Không có.
**Output:** JSON chứa thông tin giáo án đang kích hoạt gồm danh sách ngày tập, bài tập, thông số planned và trạng thái hoàn thành của từng ngày.
**Business Rules liên quan:** BR-13 (Quyền sở hữu), BR-26 (Vòng đời giáo án).
**Ghi chú kỹ thuật:** API chỉ trả về giáo án có trạng thái `ACTIVE` gắn với User ID hiện hành. Member vẫn được xem giáo án hiện hành và dữ liệu lịch sử sau khi subscription hết hạn; BR-25 chỉ bắt buộc khi kích hoạt giáo án mới.

### [FR-WORKOUT-05] Member lưu hoặc kích hoạt lịch tập
**Mức ưu tiên:** Must-have
**Actor:** Member
**Mô tả:** Hội viên chấp nhận lộ trình tập luyện do AI đề xuất (hoặc lộ trình fallback) và kích hoạt nó làm lộ trình luyện tập chính thức.
**Input chính:** `workoutPlanId`
**Output:** Giáo án đích chuyển từ `DRAFT` sang `ACTIVE`; giáo án `ACTIVE` cũ của hội viên (nếu có) chuyển sang `ARCHIVED`.
**Business Rules liên quan:** BR-13 (Quyền sở hữu), BR-25 (Subscription hợp lệ), BR-26 (Vòng đời và tính duy nhất của giáo án ACTIVE).
**Ghi chú kỹ thuật:** Thực hiện archive giáo án cũ và kích hoạt giáo án mới trong cùng transaction; không sử dụng trạng thái `INACTIVE`. Service khóa danh sách Workout Plan của Member và dùng `@Version` trên `WorkoutPlan`; xung đột khóa/version trả `CON-001` (HTTP 409).

### [FR-WORKOUT-06] Member ghi nhật ký buổi tập
**Mức ưu tiên:** Must-have
**Actor:** Member
**Mô tả:** Sau khi hoàn thành một bài tập thực tế tại phòng gym, hội viên ghi nhận kết quả thực tế đạt được để theo dõi tiến độ.
**Input chính:** `workoutPlanDetailId`, `exerciseId`, `logDate`, `actualSets`, `actualReps`, `weightUsedKg`, `actualRpe`
**Output:** Tạo mới bản ghi `WorkoutLog`; nếu trùng khóa nghiệp vụ `(memberId, logDate, exerciseId)` thì cập nhật bản ghi hiện có theo cơ chế update-in-place.
**Business Rules liên quan:** BR-09B (Giới hạn thực tế), BR-13 (Quyền sở hữu), BR-19 (Ghi trùng ngày), BR-25 (Subscription hợp lệ), BR-28 (Toàn vẹn tham chiếu Workout Log).
**Ghi chú kỹ thuật:** Endpoint được bảo vệ bởi SubscriptionGuard. Trước khi lưu, xác minh `workoutPlanDetailId` thuộc giáo án ACTIVE của Member, `exerciseId` khớp chi tiết và `logDate` không ở tương lai. Ghi trùng cùng bài/ngày thực hiện Update-in-place; xem lịch sử không yêu cầu subscription còn hiệu lực.

### [FR-WORKOUT-07] Member xem lịch sử tập luyện
**Mức ưu tiên:** Must-have
**Actor:** Member
**Mô tả:** Hội viên xem lại toàn bộ nhật ký tập luyện thực tế của chính mình đã được ghi nhận trong quá khứ để theo dõi quá trình thực hiện.
**Input chính:** `page`, `size`, `startDate`, `endDate` (tùy chọn)
**Output:** Danh sách phân trang các bản ghi nhật ký tập luyện (`WorkoutLog`) của hội viên.
**Business Rules liên quan:** BR-13 (Quyền sở hữu).
**Ghi chú kỹ thuật:** Chỉ truy vấn các bản ghi có liên kết với User ID của chính hội viên đó. Quyền xem/giám sát dữ liệu hội viên của PT thuộc phân hệ Should-have, được thiết kế sẵn cấu trúc phân quyền nhưng không nằm trong tiêu chí nghiệm thu MVP.

---

### [FR-NUTRITION-01] Backend tính BMI
**Mức ưu tiên:** Must-have
**Actor:** Hệ thống
**Mô tả:** Hệ thống tự động tính toán chỉ số khối cơ thể (Body Mass Index - BMI) dựa trên chiều cao và cân nặng trong hồ sơ của hội viên.
**Input chính:** `heightCm`, `weightKg` từ hồ sơ thể trạng
**Output:** Chỉ số BMI dạng số thực (`Double`).
**Business Rules liên quan:** BR-09C (Backend sở hữu tính toán).
**Ghi chú kỹ thuật:** Công thức: $BMI = weightKg / (heightCm / 100)^2$. Phép tính được thực hiện cục bộ trong bộ nhớ Backend.

### [FR-NUTRITION-02] Backend tính BMR
**Mức ưu tiên:** Must-have
**Actor:** Hệ thống
**Mô tả:** Hệ thống tự động tính toán tỷ lệ trao đổi chất cơ bản (Basal Metabolic Rate - BMR) theo công thức khoa học cứng để xác định mức năng lượng tối thiểu của cơ thể.
**Input chính:** `gender` (Enum: `MALE`, `FEMALE`), `dateOfBirth`, `heightCm`, `weightKg`
**Output:** Chỉ số BMR (Calories/ngày).
**Business Rules liên quan:** BR-09C (Backend sở hữu tính toán).
**Ghi chú kỹ thuật:** Sử dụng công thức Mifflin-St Jeor:
- Nam: $BMR = 10 \times weightKg + 6.25 \times heightCm - 5 \times Age + 5$
- Nữ: $BMR = 10 \times weightKg + 6.25 \times heightCm - 5 \times Age - 161$
Tuổi (Age) được tính theo số năm đã hoàn tất bằng cách so sánh `dateOfBirth` với ngày hiện hành từ `Clock` của hệ thống; không phụ thuộc timezone mặc định của máy chạy.

### [FR-NUTRITION-03] Backend tính TDEE
**Mức ưu tiên:** Must-have
**Actor:** Hệ thống
**Mô tả:** Hệ thống tính toán tổng năng lượng tiêu thụ hàng ngày (Total Daily Energy Expenditure - TDEE) dựa trên BMR và hệ số hoạt động thể chất.
**Input chính:** BMR, `activityLevel` (Enum)
**Output:** Chỉ số TDEE (Calories/ngày).
**Business Rules liên quan:** BR-09C (Backend sở hữu tính toán).
**Ghi chú kỹ thuật:** Hệ số nhân tương ứng với `activityLevel`:
- `SEDENTARY`: BMR * 1.2
- `LIGHTLY_ACTIVE`: BMR * 1.375
- `MODERATELY_ACTIVE`: BMR * 1.55
- `VERY_ACTIVE`: BMR * 1.725

### [FR-NUTRITION-04] Backend tính calories và macros
**Mức ưu tiên:** Must-have
**Actor:** Hệ thống
**Mô tả:** Backend tính toán tổng lượng Calories mục tiêu cần nạp hàng ngày dựa theo TDEE và mục tiêu tập luyện, sau đó phân bổ lượng chất dinh dưỡng đa lượng Macronutrients (Protein, Carb, Fat) theo gram.
**Input chính:** TDEE, `fitnessGoal`, `weightKg`
**Output:** `dailyCaloriesKcal`, `proteinGrams`, `fatGrams`, `carbGrams` mục tiêu.
**Business Rules liên quan:** BR-09C (Backend sở hữu tính toán).
**Ghi chú kỹ thuật:**
- *Calories*: `BULK` (+300 kcal), `CUT` (-500 kcal), `MAINTAIN` (giữ nguyên TDEE).
- *Protein*: chốt cố định `2.2g * weightKg` trong MVP.
- *Fat*: chốt cố định `25%` tổng lượng Calories, sau đó chia `9` để đổi sang gram.
- *Carbohydrate*: Lượng calories còn lại chia cho 4 (1g Carb = 4 kcal).

Toàn bộ kết quả BMI, BMR, TDEE, Calories và Macronutrients được làm tròn `HALF_UP` đến 2 chữ số thập phân. Calculator phải từ chối kết quả nếu calories còn lại cho Carbohydrate âm.

### [FR-NUTRITION-05] AI gợi ý cấu trúc bữa ăn
**Mức ưu tiên:** Must-have
**Actor:** Member
**Mô tả:** AI Engine phân tích dữ liệu ăn kiêng của hội viên và các chỉ số dinh dưỡng đích để gợi ý thực đơn món ăn thực tế phân chia theo số bữa ăn mong muốn trong ngày.
**Input chính:** `dietaryPreference`, `foodAllergies`, `excludedFoods`, `mealsPerDay`, `dailyCaloriesKcal` (từ Backend), `proteinGrams`, `fatGrams`, `carbGrams`
**Output:** Cấu trúc bữa ăn (`mealStructure` dạng JSON) chứa tên món ăn gợi ý, thời gian ăn khuyến nghị và mô tả dinh dưỡng từng bữa.
**Business Rules liên quan:** BR-09C (AI không được tự tính calories/macros), BR-12 (Cảnh báo y tế), BR-23 (Kiểm duyệt hồ sơ thể chất).
**Ghi chú kỹ thuật:** AI chỉ đóng vai trò phân bổ thực phẩm để đạt chỉ số đích; JSON Schema của AI không chứa các trường số liệu Calories và Macros tổng. Backend yêu cầu `mealStructure` có đúng `mealsPerDay` phần tử và kiểm tra lại từng món theo `dietaryPreference`, `foodAllergies`, `excludedFoods` trước khi lưu; phản hồi vi phạm bị reject → retry tối đa 1 lần → fallback an toàn theo BR-11.

### [FR-NUTRITION-06] Backend ghép calculatedTargets với mealStructure
**Mức ưu tiên:** Must-have
**Actor:** Hệ thống
**Mô tả:** Hệ thống tiến hành tích hợp và đóng gói dữ liệu: Lấy dữ liệu tính toán sinh học chính xác của Backend và ghép nối với JSON thực đơn gợi ý của AI thành một phản hồi thống nhất trước khi trả về Client.
**Input chính:** Kết quả tính toán từ Backend, Kết quả JSON của AI Engine
**Output:** Response JSON hoàn chỉnh trả về Client chứa cả 2 phần: `calculatedTargets` và `aiSuggestion.nutritionPlan`.
**Business Rules liên quan:** BR-09C (Tính toàn vẹn của chỉ số Backend).
**Ghi chú kỹ thuật:** Đảm bảo cấu trúc hiển thị ở Frontend hiển thị rõ ràng chỉ số Calories đích là do hệ thống tính toán khoa học, còn thực đơn là gợi ý tham khảo từ AI.

---

### [FR-PROGRESS-01] Member ghi nhận cân nặng theo ngày
**Mức ưu tiên:** Must-have
**Actor:** Member
**Mô tả:** Hội viên ghi nhận cân nặng hiện tại hàng ngày để theo dõi sự biến đổi thể trạng theo thời gian thực tế.
**Input chính:** `weightKg`, `recordDate` (ngày ghi nhận)
**Output:** Bản ghi `BodyProgress` được lưu vào database kèm timestamp cập nhật tự động.
**Business Rules liên quan:** BR-13 (Quyền sở hữu), BR-22 (Chỉ có duy nhất 1 bản ghi thể trạng trong ngày).
**Ghi chú kỹ thuật:** Lưu `recordDate` bằng SQL `DATE` theo timezone nghiệp vụ `Asia/Ho_Chi_Minh`; các timestamp audit lưu UTC. Ràng buộc duy nhất áp dụng trên `(memberId, recordDate)`.

### [FR-PROGRESS-02] Member cập nhật lại tiến độ trong cùng ngày
**Mức ưu tiên:** Must-have
**Actor:** Member
**Mô tả:** Nếu hội viên ghi nhận cân nặng nhiều hơn một lần trong cùng một ngày, hệ thống sẽ thực hiện cập nhật ghi đè lên bản ghi cũ của ngày hôm đó thay vì tạo bản ghi mới.
**Input chính:** `weightKg`, `recordDate`
**Output:** Bản ghi `BodyProgress` cũ của ngày hiện hành được cập nhật cân nặng mới, cập nhật timestamp `updatedAt`.
**Business Rules liên quan:** BR-13 (Quyền sở hữu), BR-22 (Chuẩn hóa ghi đè trong ngày).
**Ghi chú kỹ thuật:** Sử dụng JPA Dirty-Checking để cập nhật tự động bằng cách tải bản ghi cũ lên trước khi gán và lưu.

### [FR-PROGRESS-03] Member xem biểu đồ cân nặng
**Mức ưu tiên:** Must-have
**Actor:** Member
**Mô tả:** Hội viên xem biểu đồ trực quan về lịch sử biến động cân nặng cá nhân và tần suất số buổi đã ghi workout log theo tuần.
**Input chính:** `startDate`, `endDate` (tùy chọn)
**Output:** JSON chứa `timeseries` cân nặng sắp xếp tăng dần theo `recordDate` và `workoutFrequencyByWeek` tổng hợp số ngày tập có log trong từng tuần.
**Business Rules liên quan:** BR-13 (Quyền sở hữu).
**Ghi chú kỹ thuật:** Chuyển đổi dữ liệu thành dạng chuỗi thời gian (Timeseries); tần suất được tính theo số `logDate` phân biệt trong tuần ISO, không phải số dòng exercise log. Frontend sử dụng trực tiếp dữ liệu này với Chart.js/Recharts. Quyền xem/giám sát dữ liệu hội viên của PT thuộc phân hệ Should-have, được thiết kế sẵn cấu trúc phân quyền nhưng không nằm trong tiêu chí nghiệm thu MVP.

### [FR-PROGRESS-04] Member xem tiến độ mức tạ theo bài tập
**Mức ưu tiên:** Must-have
**Actor:** Member
**Mô tả:** Hội viên theo dõi sự tiến bộ về sức mạnh cơ bắp thông qua biểu đồ biến thiên mức tạ lớn nhất (`weightUsedKg`) đạt được ở một bài tập cụ thể qua các buổi tập.
**Input chính:** `exerciseId` (path variable)
**Output:** Danh sách các mốc ngày tập và mức tạ tối đa tương ứng đạt được của bài tập đó.
**Business Rules liên quan:** BR-13 (Quyền sở hữu).
**Ghi chú kỹ thuật:** Thực hiện câu lệnh Group By theo ngày tập và lấy giá trị Max của trường `weightUsedKg` từ bảng nhật ký tập luyện `workout_logs`. Quyền xem/giám sát dữ liệu hội viên của PT thuộc phân hệ Should-have, được thiết kế sẵn cấu trúc phân quyền nhưng không nằm trong tiêu chí nghiệm thu MVP.

---

### [FR-ADMIN-01] Admin xem danh sách user
**Mức ưu tiên:** Must-have
**Actor:** Admin
**Mô tả:** Quản trị viên quản lý danh sách toàn bộ người dùng đăng ký trên hệ thống để phục vụ công tác vận hành.
**Input chính:** `page`, `size`, `role` (lọc), `status` (lọc), `searchQuery` (tên/email)
**Output:** Danh sách phân trang chứa thông tin cơ bản của người dùng (ID, Họ tên, Email, Trạng thái, Vai trò, Ngày tạo).
**Business Rules liên quan:** BR-03 (Quyền Admin).
**Ghi chú kỹ thuật:** Không bao gồm thông tin nhạy cảm như mật khẩu hay token bảo mật trong payload trả về.

### [FR-ADMIN-02] Admin khóa/mở khóa tài khoản
**Mức ưu tiên:** Must-have
**Actor:** Admin
**Mô tả:** Quản trị viên thay đổi trạng thái hoạt động của tài khoản người dùng (Khóa khi vi phạm, Mở khóa khi hoàn thành xử lý).
**Input chính:** `userId` (path variable), `action` (LOCK / UNLOCK), `reason` (nếu LOCK)
**Output:** Trạng thái tài khoản người dùng cập nhật thành `LOCKED` hoặc `ACTIVE` trong DB.
**Business Rules liên quan:** BR-03 (Quyền Admin), BR-16 (Không khóa tài khoản chỉ vì hết gói tập).
**Ghi chú kỹ thuật:** Cần ghi nhận lý do khóa vào trường log hệ thống để kiểm toán hành vi quản trị. `JwtAuthenticationFilter` xác thực chữ ký/hạn dùng, nạp identity/roles nhưng không truy vấn hoặc đánh giá `accountStatus`. `AccountStatusGuard` hoặc Method Security kiểm tra trạng thái `LOCKED`/`DISABLED` tại endpoint yêu cầu xác thực và từ chối token hiện hành của tài khoản bị khóa.

### [FR-ADMIN-03] Admin xem số liệu đếm cơ bản
**Mức ưu tiên:** Must-have
**Actor:** Admin
**Mô tả:** Hiển thị nhanh các thông số tổng quan cơ bản của hệ thống lên Dashboard của Admin.
**Input chính:** Không có.
**Output:** Số lượng tổng thành viên (`ROLE_MEMBER`), Số lượng subscription đang `ACTIVE`, Tổng số bài tập gốc hiện có, Tổng giá trị các subscription đã xác nhận mô phỏng.
**Business Rules liên quan:** BR-03 (Quyền Admin).
**Ghi chú kỹ thuật:** Thực hiện các câu lệnh COUNT và SUM tối ưu hiệu năng cơ sở dữ liệu, không tải các tập thực thể lớn lên bộ nhớ Backend để đếm.
