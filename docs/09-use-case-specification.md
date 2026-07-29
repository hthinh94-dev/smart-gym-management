# 09. Đặc tả Use Case

## 1. Mục đích tài liệu
Tài liệu này đặc tả chi tiết các tương tác giữa người dùng (Actors) và hệ thống đối với 10 Use Case cốt lõi thuộc phạm vi sản phẩm khả dụng tối thiểu (MVP). Tài liệu phân rã các kịch bản thành luồng xử lý chính (Basic Flow), luồng ngoại lệ (Exception Flows), liên kết các Quy tắc nghiệp vụ (Business Rules) và thiết lập Tiêu chí nghiệm thu (Acceptance Criteria) theo cấu trúc BDD (Given-When-Then) làm căn cứ thiết kế API Contract và viết các kịch bản kiểm thử tích hợp, kiểm thử chấp nhận (Acceptance Tests).

## 2. Danh sách Actor
- **Anonymous Guest:** Tác nhân chưa xác thực, đại diện cho người dùng vãng lai, có quyền đăng ký tài khoản hội viên và xem thông tin gói tập công khai. Anonymous Guest không được lưu trữ dưới dạng một vai trò `ROLE_GUEST` trong cơ sở dữ liệu.
- **Member:** Hội viên chính thức, có quyền quản lý hồ sơ thể chất, đăng ký gói tập, yêu cầu AI đề xuất lịch tập/thực đơn dinh dưỡng, ghi nhật ký và theo dõi tiến trình của bản thân.
- **Admin:** Quản trị viên hệ thống, quản lý danh mục bài tập, cấu hình gói tập, phê duyệt yêu cầu đăng ký/gia hạn gói tập mô phỏng và quản lý trạng thái tài khoản người dùng.
*(Ghi chú: PT - Personal Trainer là vai trò được thiết kế sẵn trong cấu trúc RBAC nhưng phân hệ nghiệp vụ PT là Should-have, không tham gia vào luồng hoạt động MVP độc lập của Member và Admin).*

## 3. Danh sách Use Case

| Mã UC | Tên Use Case | Ánh xạ mã FR từ File 08 | Actor chính | Mức ưu tiên |
| :--- | :--- | :--- | :--- | :---: |
| **UC-01** | Đăng ký tài khoản | FR-AUTH-01 | Anonymous Guest | Must-have |
| **UC-02** | Đăng nhập | FR-AUTH-02, FR-AUTH-03 | Anonymous Guest, Member, Admin | Must-have |
| **UC-03** | Cập nhật hồ sơ thể trạng | FR-PROFILE-02, FR-PROFILE-03, FR-PROFILE-04, FR-PROGRESS-01, FR-PROGRESS-02 | Member | Must-have |
| **UC-04** | Đăng ký mới hoặc gửi yêu cầu gia hạn gói tập | FR-SUB-04, FR-SUB-05, FR-SUB-07, FR-SUB-08 | Member | Must-have |
| **UC-05** | Admin xác nhận subscription | FR-SUB-06, FR-SUB-08 | Admin | Must-have |
| **UC-06** | Admin quản lý bài tập | FR-EXR-01, FR-EXR-02, FR-EXR-03 | Admin | Must-have |
| **UC-07** | Member yêu cầu AI tạo lịch tập và dinh dưỡng | FR-SUB-07, FR-EXR-06, FR-WORKOUT-01, FR-WORKOUT-02, FR-WORKOUT-03, FR-WORKOUT-04, FR-WORKOUT-05, FR-NUTRITION-01, FR-NUTRITION-02, FR-NUTRITION-03, FR-NUTRITION-04, FR-NUTRITION-05, FR-NUTRITION-06 | Member | Must-have |
| **UC-08** | Member ghi nhật ký tập luyện | FR-SUB-07, FR-WORKOUT-06 | Member | Must-have |
| **UC-09** | Member xem tiến độ tập luyện | FR-WORKOUT-07, FR-PROGRESS-01, FR-PROGRESS-02, FR-PROGRESS-03, FR-PROGRESS-04 | Member | Must-have |
| **UC-10** | Admin khóa/mở khóa tài khoản | FR-AUTH-03, FR-ADMIN-02 | Admin | Must-have |

### 3.1. Các FR hỗ trợ không tách thành Use Case cốt lõi

Mười Use Case trên mô tả các luồng nghiệp vụ End-to-End quan trọng nhất của Ngày 2. Các FR hỗ trợ/CRUD dưới đây vẫn thuộc MVP và được truy vết đầy đủ xuống API Contract, nhưng không tách thêm Use Case để giữ đúng phạm vi 10 Use Case đã chốt:

| Nhóm FR hỗ trợ | Vai trò trong hệ thống | API truy vết tại File 10 |
| :--- | :--- | :--- |
| FR-AUTH-04, FR-AUTH-05 | Bảo vệ endpoint và lấy tài khoản hiện hành | `GET /api/v1/users/me` và cơ chế phân quyền chéo |
| FR-PROFILE-01 | Xem hồ sơ cá nhân trước/sau cập nhật | `GET /api/v1/member/profile` |
| FR-SUB-01, FR-SUB-02, FR-SUB-03, FR-SUB-09 | CRUD gói tập và hủy subscription bởi Admin | Nhóm Membership API quản trị |
| FR-EXR-04, FR-EXR-05 | Xem, tìm kiếm, lọc thư viện bài tập | `GET /api/v1/exercises`, `GET /api/v1/exercises/{id}` |
| FR-ADMIN-01, FR-ADMIN-03 | Danh sách tài khoản và số liệu đếm cơ bản | `GET /api/v1/admin/users`, `GET /api/v1/admin/statistics/summary` |

---

## 4. Đặc tả chi tiết từng Use Case

### [UC-01] Đăng ký tài khoản
- **Mục tiêu:** Khách vãng lai đăng ký tài khoản hội viên thành công để có thể đăng nhập vào hệ thống.
- **Actor chính:** Anonymous Guest
- **Actor phụ (nếu có):** Không
- **Tiền điều kiện (Pre-conditions):** Người dùng chưa đăng nhập, truy cập trang đăng ký của ứng dụng.
- **Hậu điều kiện (Post-conditions):**
  - Tạo mới bản ghi User trong cơ sở dữ liệu với email chuẩn hóa, mật khẩu mã hóa BCrypt.
  - Mặc định trạng thái tài khoản `accountStatus = ACTIVE` và vai trò `ROLE_MEMBER` (chưa có gói tập kích hoạt).
- **Luồng chính (Basic Flow):**
  1. Khách vãng lai nhập Họ tên, Email, Mật khẩu và Xác nhận mật khẩu tại form Đăng ký.
  2. Hệ thống kiểm tra tính duy nhất của email sau khi trim và lowercase.
  3. Hệ thống validate tính hợp lệ của mật khẩu (độ dài 8-72, chữ hoa, số, không có khoảng trắng ở biên) và xác nhận `confirmPassword` khớp chính xác với `password`.
  4. Hệ thống băm mật khẩu bằng BCrypt.
  5. Hệ thống lưu người dùng mới với trạng thái ACTIVE và vai trò ROLE_MEMBER.
  6. Hệ thống hiển thị thông báo Đăng ký thành công và tự động chuyển hướng về trang Đăng nhập.
- **Luồng ngoại lệ (Alternative / Exception Flows):**
  - **[Ngoại lệ 01.a] - Trùng Email:**
    - Hệ thống phát hiện email đã tồn tại trong DB (sau khi trim và lowercase).
    - Hệ thống ném mã lỗi `ACC-001` (HTTP 409 Conflict) kèm thông báo "Email đã được sử dụng".
  - **[Ngoại lệ 01.b] - Mật khẩu sai định dạng:**
    - Hệ thống phát hiện mật khẩu vi phạm chính sách mật khẩu (dưới 8 ký tự, trên 72 ký tự, thiếu chữ hoa hoặc số, có khoảng trắng ở biên) hoặc `confirmPassword` không khớp.
    - Hệ thống ném mã lỗi `ACC-002` (HTTP 400 Bad Request) kèm thông báo lỗi chi tiết.
  - **[Ngoại lệ 01.c] - Họ tên hoặc Email không hợp lệ:**
    - Họ tên rỗng/vượt quá 100 ký tự, hoặc Email rỗng/sai định dạng/vượt quá 150 ký tự.
    - Hệ thống từ chối gọi Service và trả `VAL-001` (HTTP 400 Bad Request) kèm lỗi theo trường.
- **Business Rules liên quan:** BR-01, BR-02, BR-15, BR-18, BR-20.
- **Acceptance Criteria (BDD):**
  - **AC-UC_01-01 (Đăng ký thành công):**
    - **Given** khách vãng lai điền thông tin đăng ký gồm Họ tên: `"Nguyễn Văn A"`, Email: `"  User@Gmail.Com  "`, Mật khẩu: `"Password123"` và Xác nhận mật khẩu: `"Password123"`.
    - **When** nhấn nút Đăng ký,
    - **Then** hệ thống chuẩn hóa email thành `"user@gmail.com"`, băm mật khẩu thành công bằng thuật toán BCrypt, khởi tạo tài khoản mới với vai trò `ROLE_MEMBER` và trạng thái `accountStatus = ACTIVE`, đồng thời trả về mã HTTP 201 Created.
  - **AC-UC_01-02 (Đăng ký thất bại do Email đã tồn tại):**
    - **Given** cơ sở dữ liệu đã tồn tại tài khoản hội viên có email chuẩn hóa là `"user@gmail.com"`.
    - **When** khách vãng lai điền thông tin đăng ký với email `" user@gmail.com "` hoặc `"USER@gmail.com"`,
    - **Then** hệ thống phát hiện trùng lặp, từ chối lưu bản ghi mới và trả về mã lỗi HTTP 409 Conflict với Error Code `ACC-001`.

---

### [UC-02] Đăng nhập
- **Mục tiêu:** Người dùng đăng nhập thành công vào hệ thống và nhận JWT Access Token.
- **Actor chính:** Anonymous Guest, Member, Admin
- **Actor phụ (nếu có):** Không
- **Tiền điều kiện (Pre-conditions):** Người dùng đã đăng ký tài khoản trên hệ thống.
- **Hậu điều kiện (Post-conditions):** Tạo và cấp mã JWT Access Token chứa vai trò (roles) của người dùng để thực thi các yêu cầu API tiếp theo.
- **Luồng chính (Basic Flow):**
  1. Người dùng nhập Email và Mật khẩu tại trang Đăng nhập.
  2. Hệ thống chuẩn hóa email bằng cách trim khoảng trắng biên và chuyển về dạng chữ thường.
  3. Hệ thống tìm kiếm bản ghi người dùng và kiểm tra trạng thái tài khoản.
  4. Hệ thống so khớp mật khẩu gửi lên với hash mật khẩu trong DB.
  5. Hệ thống sinh mã JWT Access Token có chữ ký bảo mật dài tối thiểu 256-bit.
  6. Hệ thống trả về token cùng quyền hạn và tên người dùng cho Client.
- **Luồng ngoại lệ (Alternative / Exception Flows):**
  - **[Ngoại lệ 02.a] - Tài khoản bị khóa (LOCKED):**
    - Hệ thống phát hiện trạng thái tài khoản là `LOCKED`.
    - Hệ thống từ chối đăng nhập, ném mã lỗi `ACC-004` (HTTP 403 Forbidden) kèm thông báo tài khoản bị khóa.
  - **[Ngoại lệ 02.b] - Tài khoản bị vô hiệu hóa (DISABLED):**
    - Hệ thống phát hiện trạng thái tài khoản là `DISABLED`.
    - Hệ thống từ chối đăng nhập, ném mã lỗi `ACC-006` (HTTP 403 Forbidden) kèm thông báo tài khoản bị vô hiệu hóa vĩnh viễn.
  - **[Ngoại lệ 02.c] - Sai Email hoặc Mật khẩu:**
    - Email không tồn tại hoặc mật khẩu băm không khớp.
    - Hệ thống ném mã lỗi `ACC-007` (HTTP 401 Unauthorized) kèm thông báo "Tên đăng nhập hoặc mật khẩu không chính xác"; không tiết lộ email hay mật khẩu là phần sai.
- **Business Rules liên quan:** BR-16, BR-18, BR-20, BR-21.
- **Acceptance Criteria (BDD):**
  - **AC-UC_02-01 (Đăng nhập thành công):**
    - **Given** người dùng có tài khoản hợp lệ có trạng thái `ACTIVE` và vai trò `ROLE_MEMBER` trong database.
    - **When** người dùng thực hiện yêu cầu đăng nhập bằng đúng Email và Mật khẩu,
    - **Then** hệ thống trả về mã trạng thái HTTP 200 OK cùng chuỗi JWT Access Token chứa thông tin User ID và vai trò của người dùng.
  - **AC-UC_02-02 (Đăng nhập thất bại khi tài khoản bị khóa/vô hiệu hóa):**
    - **Given** tài khoản của Member có `accountStatus = LOCKED` (hoặc `DISABLED`).
    - **When** người dùng cố gắng thực hiện đăng nhập bằng đúng email và mật khẩu,
    - **Then** hệ thống chặn đăng nhập, trả về mã trạng thái HTTP 403 Forbidden cùng Error Code `ACC-004` (cho LOCKED) hoặc `ACC-006` (cho DISABLED) và không cấp JWT.

---

### [UC-03] Cập nhật hồ sơ thể trạng
- **Mục tiêu:** Hội viên cập nhật thành công hồ sơ thể chất và dinh dưỡng cá nhân làm cơ sở đề xuất giáo án.
- **Actor chính:** Member
- **Actor phụ (nếu có):** Không
- **Tiền điều kiện (Pre-conditions):** Hội viên đã đăng nhập và được xác thực qua JWT.
- **Hậu điều kiện (Post-conditions):**
  - Cập nhật thông số thể chất trong bảng `bio_profiles`.
  - Tự động tính toán và cập nhật lại BMI, BMR, TDEE, Calories/Macros đích tại Backend.
  - Tự động ghi nhận một bản ghi biến động cân nặng (`BodyProgress`) cho ngày hiện hành trong DB.
- **Luồng chính (Basic Flow):**
  1. Hội viên truy cập trang Cấu hình hồ sơ và nhập đầy đủ: `heightCm`, `weightKg`, `fitnessGoal`, `fitnessLevel`, `activityLevel`, `dietaryPreference`, `foodAllergies`, `excludedFoods`, `mealsPerDay`; đồng thời có thể cập nhật `gender` (`MALE` hoặc `FEMALE`), `dateOfBirth`, `workoutDaysPerWeek`, `maxSessionMinutes`, `availableEquipment`, `targetMuscleGroups`, `injuryConstraints`.
  2. Hội viên nhấn nút Lưu.
  3. Hệ thống xác thực tính hợp lệ của dữ liệu đầu vào.
  4. Backend tính toán chỉ số BMI, BMR (Mifflin-St Jeor), TDEE (dựa theo activityLevel) và Calories/Macros (thâm hụt/thặng dư theo mục tiêu).
  5. Hệ thống lưu thông tin vào database và cập nhật/tạo mới bản ghi `BodyProgress` cho ngày hiện hành.
  6. Hệ thống trả về cấu trúc hồ sơ đầy đủ cùng các chỉ số vừa tính toán.
- **Luồng ngoại lệ (Alternative / Exception Flows):**
  - **[Ngoại lệ 03.a] - Dữ liệu không hợp lệ:**
  - Cân nặng/chiều cao âm hoặc rỗng; `gender` khác `MALE`/`FEMALE`; `dateOfBirth` ở tương lai; `workoutDaysPerWeek` ngoài khoảng 1–7; `maxSessionMinutes` không dương; `mealsPerDay` ngoài khoảng 1–6; `activityLevel`, `fitnessGoal`, `fitnessLevel` hoặc `dietaryPreference` không thuộc Enum; danh sách dị ứng/thực phẩm loại trừ vượt giới hạn BR-23.
    - Hệ thống ném mã lỗi `VAL-001` (HTTP 400 Bad Request) kèm thông báo lỗi chi tiết các trường.
- **Business Rules liên quan:** BR-13, BR-22, BR-23.
- **Acceptance Criteria (BDD):**
  - **AC-UC_03-01 (Cập nhật thành công):**
    - **Given** hội viên đã đăng nhập với JWT Token hợp lệ.
    - **When** gửi yêu cầu cập nhật `heightCm = 175.0`, `weightKg = 70.0`, `fitnessGoal = "BULK"`, `fitnessLevel = "BEGINNER"`, `activityLevel = "MODERATELY_ACTIVE"`, `dietaryPreference = "OMNIVORE"`, `foodAllergies = ["PEANUTS"]`, `excludedFoods = ["BEEF"]`, `mealsPerDay = 4`,
    - **Then** hệ thống lưu dữ liệu thành công, trả về HTTP 200 OK chứa các chỉ số sinh học được Backend tính toán định lượng (`bmi`, `bmr`, `tdee`, `dailyCaloriesKcal`, `proteinGrams`, `fatGrams`, `carbGrams`).
  - **AC-UC_03-02 (Cập nhật thất bại do dữ liệu input vượt ngưỡng):**
    - **Given** hội viên điền thông số có `mealsPerDay = 8` (vượt giới hạn quy định từ 1 đến 6 của BR-23) hoặc `activityLevel` sai kiểu Enum.
    - **When** hội viên nhấn nút Lưu hồ sơ,
    - **Then** hệ thống dừng xử lý, từ chối ghi nhận vào database và trả về mã lỗi HTTP 400 Bad Request cùng Error Code `VAL-001`.

---

### [UC-04] Đăng ký mới hoặc gửi yêu cầu gia hạn gói tập
- **Mục tiêu:** Hội viên gửi yêu cầu đăng ký gói mới khi chưa có gói ACTIVE hoặc gửi Renewal Request cho Subscription ACTIVE hiện hành.
- **Actor chính:** Member
- **Actor phụ (nếu có):** Không
- **Tiền điều kiện (Pre-conditions):** Hội viên đã đăng nhập. Luồng đăng ký mới yêu cầu không có Subscription hợp lệ; luồng gia hạn yêu cầu có Subscription thuộc sở hữu của hội viên thỏa mãn `status = ACTIVE`, `startDate <= currentDate < endDate`.
- **Hậu điều kiện (Post-conditions):** Đăng ký mới tạo Subscription Request `PENDING`; gia hạn tạo Renewal Request `PENDING` liên kết với Subscription ACTIVE hiện hành.
- **Luồng chính (Basic Flow):**
  1. Hội viên xem danh sách gói tập và chọn gói mong muốn.
  2. Hội viên gửi yêu cầu đăng ký mua gói tập.
  3. Hệ thống xác thực gói tập có đang được mở bán (`isActive = true`).
  4. Hệ thống kiểm tra động xem hội viên có gói tập nào thỏa mãn `status = ACTIVE`, `startDate <= currentDate < endDate` hay không.
  5. Hệ thống khởi tạo Subscription với trạng thái `PENDING` và ghi nhận lịch sử yêu cầu.
  6. Hệ thống hiển thị thông báo gửi yêu cầu thành công.
- **Luồng ngoại lệ (Alternative / Exception Flows):**
  - **[Ngoại lệ 04.a] - Gói tập đã ngưng hoạt động:**
    - Gói tập được chọn đã bị Admin vô hiệu hóa (`isActive = false`).
    - Hệ thống ném mã lỗi `SUB-003` (HTTP 409 Conflict) kèm thông báo "Gói tập đã ngừng hoạt động".
  - **[Ngoại lệ 04.b] - Đã có gói tập ACTIVE:**
    - Hội viên đã có một gói tập khác ở trạng thái `ACTIVE` và chưa hết hạn.
    - Hệ thống từ chối tạo yêu cầu đăng ký mới, ném mã lỗi `SUB-004` (HTTP 409 Conflict) kèm thông báo hội viên đã có gói tập đang hoạt động và hướng dẫn sử dụng luồng gia hạn riêng.
  - **[Luồng mở rộng 04.c] - Gửi yêu cầu gia hạn:**
    1. Hội viên chọn Subscription ACTIVE của chính mình và yêu cầu gia hạn.
    2. Hệ thống kiểm tra ownership, trạng thái ACTIVE và xác nhận `packageId` khớp package hiện hành.
    3. Hệ thống tạo Renewal Request `PENDING` liên kết với Subscription ACTIVE; không tạo Subscription ACTIVE mới.
    4. Nếu Subscription không tồn tại/không thuộc Member, trả `SUB-005` (HTTP 404); nếu `packageId` không khớp, trả `VAL-001` (HTTP 400); nếu package INACTIVE, trả `SUB-003` (HTTP 409).
  - **[Ngoại lệ 04.d] - Đã có yêu cầu PENDING chưa xử lý:**
    - Member đã có yêu cầu đăng ký mới `PENDING`, hoặc Subscription đích đã có Renewal Request `PENDING`.
    - Hệ thống từ chối tạo yêu cầu trùng, trả `SUB-006` (HTTP 409 Conflict) và giữ nguyên yêu cầu đang chờ xử lý.
- **Business Rules liên quan:** BR-04, BR-05, BR-13, BR-24, BR-25.
- **Acceptance Criteria (BDD):**
  - **AC-UC_04-01 (Đăng ký gói tập thành công):**
    - **Given** hội viên chưa có gói tập nào đang ở trạng thái `ACTIVE`.
    - **When** hội viên chọn và đăng ký gói tập có trạng thái hoạt động `isActive = true`,
    - **Then** hệ thống tạo mới một bản ghi Subscription ở trạng thái `PENDING` chờ Admin duyệt và trả về mã trạng thái HTTP 201 Created.
  - **AC-UC_04-02 (Đăng ký thất bại do chọn gói tập INACTIVE):**
    - **Given** hội viên chưa có gói tập active, và chọn gói tập đã bị Admin vô hiệu hóa (`isActive = false` / ngưng bán).
    - **When** hội viên gửi yêu cầu đăng ký mua gói tập này,
    - **Then** hệ thống từ chối tạo, ném ra mã lỗi HTTP 409 Conflict cùng Error Code `SUB-003`.
  - **AC-UC_04-03 (Đăng ký thất bại do đã có gói ACTIVE):**
    - **Given** hội viên hiện tại đang sở hữu một gói tập có trạng thái `ACTIVE` và còn thời hạn hiệu lực.
    - **When** hội viên cố tình gửi yêu cầu đăng ký mua một gói tập mới khác loại,
    - **Then** hệ thống chặn hành vi, trả về mã lỗi HTTP 409 Conflict với Error Code `SUB-004`.
  - **AC-UC_04-04 (Tạo Renewal Request thành công):**
    - **Given** Member có Subscription `ACTIVE` thuộc sở hữu của mình và package hiện hành vẫn `isActive = true`.
    - **When** Member gọi endpoint gia hạn với đúng `activeSubscriptionId` và `packageId` của gói hiện hành,
    - **Then** hệ thống tạo Renewal Request trạng thái `PENDING`, trả HTTP 201 Created và không tạo thêm Subscription `ACTIVE`.
  - **AC-UC_04-05 (Chặn yêu cầu PENDING trùng lặp):**
    - **Given** Member đã có một yêu cầu đăng ký mới hoặc gia hạn cùng loại ở trạng thái `PENDING`.
    - **When** Member gửi lại cùng loại yêu cầu trước khi Admin xử lý yêu cầu hiện có,
    - **Then** hệ thống không tạo bản ghi mới, trả HTTP 409 Conflict với Error Code `SUB-006` và giữ nguyên yêu cầu PENDING ban đầu.

---

### [UC-05] Admin xác nhận subscription
- **Mục tiêu:** Quản trị viên duyệt kích hoạt gói tập của hội viên hoặc duyệt gia hạn gói tập.
- **Actor chính:** Admin
- **Actor phụ (nếu có):** Không
- **Tiền điều kiện (Pre-conditions):** Admin đã đăng nhập; có Subscription Request hoặc Renewal Request đang ở trạng thái `PENDING`.
- **Hậu điều kiện (Post-conditions):** Với đăng ký mới, Subscription chuyển sang `ACTIVE` và xác lập ngày bắt đầu/kết thúc. Với gia hạn, Renewal Request chuyển `PENDING → PROCESSED` và chỉ `endDate` của Subscription `ACTIVE` liên kết được cập nhật.
- **Luồng chính (Basic Flow):**
  1. Admin truy cập danh sách yêu cầu đăng ký gói tập chờ xử lý.
  2. Admin chọn yêu cầu của Hội viên và nhấn "Phê duyệt"; Client gửi kèm `requestType = NEW_SUBSCRIPTION` hoặc `RENEWAL` theo loại yêu cầu đang hiển thị.
  3. Hệ thống dùng `requestType` để tải đúng Subscription Request hoặc Renewal Request và kiểm tra trạng thái `PENDING`.
  4. Với đăng ký mới, hệ thống khóa các subscription của Member, chuyển mọi bản ghi còn mang `status = ACTIVE` nhưng đã có `endDate <= currentDate` sang `EXPIRED`, rồi kiểm tra lại điều kiện một Subscription ACTIVE trước khi chuyển yêu cầu `PENDING` thành `ACTIVE`; với gia hạn, hệ thống chuyển Renewal Request `PENDING` thành `PROCESSED`.
  5. Hệ thống tính toán thời hạn:
     - Đăng ký mới: `startDate = ngày phê duyệt`, `endDate = startDate + durationDays` của gói.
     - Gia hạn gói đang hoạt động: thời gian kết thúc mới được cộng nối tiếp `newEndDate = currentEndDate + durationDays`.
  6. Hệ thống hiển thị thông báo duyệt thành công.
- **Luồng ngoại lệ (Alternative / Exception Flows):**
  - **[Ngoại lệ 05.a] - Gói tập của subscription bị ngưng bán đột ngột:**
    - Gói tập liên kết bị Admin đổi trạng thái thành ngưng hoạt động trước khi duyệt yêu cầu cũ.
    - Hệ thống từ chối kích hoạt hoặc gia hạn, trả `SUB-003` (HTTP 409 Conflict).
  - **[Ngoại lệ 05.b] - Đã phát sinh Subscription ACTIVE trước khi phê duyệt:**
    - Với yêu cầu đăng ký mới, hệ thống kiểm tra lại trong transaction và phát hiện Member đã có Subscription `ACTIVE` với `endDate > currentDate`.
    - Hệ thống từ chối phê duyệt bằng `SUB-004` (HTTP 409 Conflict), không tạo ACTIVE thứ hai và giữ yêu cầu để Admin xử lý hủy.
  - **[Ngoại lệ 05.c] - Subscription đích gia hạn không còn hiệu lực:**
    - Với Renewal Request, Subscription liên kết đã bị hủy, hết hạn hoặc không còn tồn tại tại thời điểm phê duyệt.
    - Hệ thống rollback transaction và trả `SUB-005` (HTTP 404 Not Found).
  - **[Ngoại lệ 05.d] - Xung đột cập nhật đồng thời:**
    - Một Admin hoặc transaction khác đã cập nhật Subscription, Renewal Request hoặc Workout Plan sau thời điểm request hiện tại nạp dữ liệu; hoặc request hiện tại không lấy được khóa ghi trong thời gian cấu hình.
    - Hệ thống rollback toàn bộ transaction và trả `CON-001` (HTTP 409 Conflict). Client phải tải lại trạng thái mới nhất trước khi thử lại; không dùng `SUB-006` cho lỗi khóa/version.
- **Business Rules liên quan:** BR-03, BR-04, BR-05, BR-24, BR-25.
- **Acceptance Criteria (BDD):**
  - **AC-UC_05-01 (Xác nhận đăng ký mới thành công):**
    - **Given** Admin đã đăng nhập, và tồn tại yêu cầu đăng ký mới `PENDING` của hội viên chưa có gói active.
    - **When** Admin nhấn phê duyệt yêu cầu vào ngày `15/07/2026` cho gói tập có thời hạn 30 ngày,
    - **Then** trạng thái Subscription chuyển sang `ACTIVE`, với `startDate = 15/07/2026` và `endDate = 14/08/2026` dạng exclusive (30 ngày hiệu lực từ 15/07 đến hết 13/08).
  - **AC-UC_05-02 (Xác nhận gia hạn thành công):**
    - **Given** hội viên đang có Subscription `ACTIVE` hạn đến ngày `30/07/2026` và đã gửi yêu cầu gia hạn `PENDING` cho cùng gói tập (30 ngày).
    - **When** Admin nhấn phê duyệt yêu cầu gia hạn đó,
    - **Then** hệ thống không tạo thêm thực thể subscription `ACTIVE` song song mà cập nhật trực tiếp thời hạn gói hiện tại có `newEndDate = 29/08/2026` dạng exclusive (cộng thêm 30 ngày vào hạn cũ), đồng thời chuyển yêu cầu gia hạn từ `PENDING` sang `PROCESSED`.
  - **AC-UC_05-03 (Chuẩn hóa trạng thái ACTIVE đã hết hạn trước khi duyệt đăng ký mới):**
    - **Given** Member có một bản ghi còn mang `status = ACTIVE` nhưng `endDate <= currentDate` và có một yêu cầu đăng ký mới `PENDING`,
    - **When** Admin phê duyệt yêu cầu đăng ký mới,
    - **Then** hệ thống chuyển bản ghi cũ sang `EXPIRED` và kích hoạt yêu cầu mới trong cùng transaction, không vi phạm unique constraint một Subscription ACTIVE.
  - **AC-UC_05-04 (Phát hiện cạnh tranh khi duyệt gia hạn):**
    - **Given** transaction thứ nhất đang giữ khóa ghi trên Renewal Request và Subscription đích trong lúc duyệt gia hạn,
    - **When** transaction thứ hai cố duyệt cùng request nhưng vượt quá thời gian chờ khóa cấu hình,
    - **Then** transaction thứ hai bị rollback, trả HTTP 409 Conflict với `errorCode = CON-001`; `endDate` chỉ được cộng đúng một lần.

---

### [UC-06] Admin quản lý bài tập
- **Mục tiêu:** Quản trị viên thêm mới, cập nhật thông tin hoặc xóa mềm bài tập trong danh mục gốc.
- **Actor chính:** Admin
- **Actor phụ (nếu có):** Không
- **Tiền điều kiện (Pre-conditions):** Admin đã đăng nhập tài khoản quản trị thành công.
- **Hậu điều kiện (Post-conditions):** Danh mục bài tập gốc (`exercises`) được cập nhật mới thông tin hoặc chuyển đổi trạng thái xóa mềm.
- **Luồng chính (Basic Flow):**
  - **[Luồng 06.A] - Tạo bài tập:**
    1. Admin chọn chức năng tạo bài tập và nhập `name`, `primaryMuscleGroup`, `secondaryMuscleGroups`, `movementPattern`, `targetBodyRegions`, `equipmentRequired`, `difficultyLevel`, `contraindicationTags`, `instructionText`.
    2. Backend trim tên, kiểm tra tên không trùng, validate toàn bộ Enum/Collection và nội dung hướng dẫn.
    3. Backend lưu Exercise với `isActive = true` và trả HTTP 201 Created.
  - **[Luồng 06.B] - Cập nhật bài tập:**
    1. Admin chọn một Exercise tồn tại và chỉnh sửa các metadata cho phép.
    2. Backend kiểm tra quyền Admin, sự tồn tại của ID và tên mới không trùng với Exercise khác (loại trừ chính ID đang sửa).
    3. Backend cập nhật bản ghi, giữ nguyên lịch sử liên kết và trả HTTP 200 OK.
  - **[Luồng 06.C] - Xóa mềm bài tập:**
    1. Admin tìm kiếm và chọn bài tập cần xóa.
    2. Backend luôn cập nhật `isActive = false`, không thực hiện DELETE vật lý dù bài tập đã có dữ liệu lịch sử hay chưa.
    3. Hệ thống loại bài tập khỏi danh mục hiện hành và Exercise Whitelist, nhưng dữ liệu lịch sử vẫn hiển thị tên bài tập.
- **Luồng ngoại lệ (Alternative / Exception Flows):**
  - **[Ngoại lệ 06.a] - Thêm bài trùng tên:**
    - Admin tạo bài tập mới có tên trùng với một bài tập đã tồn tại trong DB.
    - Hệ thống từ chối lưu, ném mã lỗi `EXR-002` (HTTP 409 Conflict) kèm thông báo tên bài tập đã tồn tại.
  - **[Ngoại lệ 06.b] - Không tìm thấy bài tập cần sửa/xóa:**
    - ID Exercise không tồn tại hoặc đã không còn khả dụng cho thao tác yêu cầu.
    - Hệ thống trả `EXR-001` (HTTP 404 Not Found) và không thay đổi dữ liệu.
  - **[Ngoại lệ 06.c] - Metadata không hợp lệ:**
    - Một Enum không được hỗ trợ, tên rỗng hoặc danh sách metadata sai định dạng.
    - Hệ thống trả `VAL-001` (HTTP 400 Bad Request) kèm lỗi theo trường.
- **Business Rules liên quan:** BR-03, BR-14.
- **Acceptance Criteria (BDD):**
  - **AC-UC_06-01 (Xóa mềm bài tập thành công):**
    - **Given** Admin đã đăng nhập và chọn bài tập `"Barbell Squat"` vốn đã được ghi nhận trong lịch sử nhật ký tập của một số hội viên.
    - **When** Admin thực hiện lệnh xóa bài tập,
    - **Then** hệ thống không xóa cứng bản ghi khỏi cơ sở dữ liệu, mà đặt thuộc tính `isActive = false` (Soft Delete) nhằm bảo toàn dữ liệu nhật ký tập của hội viên cũ.
  - **AC-UC_06-02 (Tạo bài tập mới thất bại do trùng tên):**
    - **Given** cơ sở dữ liệu đã tồn tại bài tập tên là `"Flat Bench Press"`.
    - **When** Admin tạo bài tập mới cũng có tên là `"Flat Bench Press"`,
    - **Then** hệ thống từ chối lưu trữ và trả về mã lỗi HTTP 409 Conflict cùng Error Code `EXR-002`.
  - **AC-UC_06-03 (Tạo bài tập mới thành công):**
    - **Given** Admin đã xác thực và tên `"Incline Barbell Bench Press"` chưa tồn tại.
    - **When** Admin gửi đầy đủ metadata Exercise hợp lệ,
    - **Then** Backend tạo Exercise với `isActive = true`, trả HTTP 201 Created và bài tập xuất hiện trong danh mục hiện hành.
  - **AC-UC_06-04 (Cập nhật bài tập thành công):**
    - **Given** Admin đã xác thực và Exercise ID 49 tồn tại.
    - **When** Admin cập nhật hướng dẫn cùng `contraindicationTags` bằng dữ liệu hợp lệ,
    - **Then** Backend trả HTTP 200 OK, lưu metadata mới và không làm thay đổi các liên kết lịch sử của Exercise.

---

### [UC-07] Member yêu cầu AI tạo lịch tập và dinh dưỡng
- **Mục tiêu:** Hội viên nhận được lịch tập luyện theo tuần và thực đơn ăn uống cá nhân hóa chất lượng và an toàn từ AI Engine.
- **Actor chính:** Member
- **Actor phụ (nếu có):** AI Engine (Mô hình ngôn ngữ lớn ngoài)
- **Tiền điều kiện (Pre-conditions):**
  - Hội viên đã đăng nhập thành công.
  - Hồ sơ thể trạng và dinh dưỡng đã được điền đầy đủ.
  - Hội viên sở hữu Subscription hợp lệ với `status = ACTIVE`, `startDate <= currentDate < endDate`; `SubscriptionGuard` kiểm tra động điều kiện này theo FR-SUB-07 và BR-25, trả `SUB-001` nếu không thỏa mãn.
- **Hậu điều kiện (Post-conditions):**
  - Tạo mới lộ trình tập luyện (`WorkoutPlan`) ở trạng thái `DRAFT` và thực đơn ăn uống (`NutritionPlan`) được liên kết với hội viên trong DB.
  - Trạng thái lộ trình được đánh dấu nguồn tạo (`recommendationSource = AI_GENERATED` hoặc `FALLBACK_TEMPLATE`).
- **Luồng chính (Basic Flow):**
  1. Member nhấn nút "Yêu cầu AI tạo lịch tập và thực đơn".
  2. Backend tự động tính toán BMI, BMR, TDEE và Calories/Macros đích theo công thức Mifflin-St Jeor và mục tiêu thể trạng của Member (quy chuẩn tất định từ Backend).
  3. Backend truy vấn database, tự lọc ra danh sách ID các bài tập an toàn (`exerciseIdWhitelist`) khớp với thiết bị phòng tập sẵn có của Member và loại trừ hoàn toàn các bài tập có tag chống chỉ định dính chấn thương của Member.
  4. Backend đóng gói thông số dinh dưỡng đích và danh sách whitelist ID bài tập, xây dựng Prompt làm sạch gửi yêu cầu sang AI Engine dưới dạng Strict JSON Schema.
  5. AI Engine chỉ trả cấu trúc `workoutSchedule` và `nutritionPlan.mealStructure`, phân bổ đủ số ngày tập được yêu cầu kèm planned values; AI không được trả hoặc quyết định calories/macros.
  6. Backend nhận JSON phản hồi và thực hiện hậu kiểm (Post-Validation Hook):
     - Xác minh toàn bộ `exerciseId` do AI trả về đều nằm trong whitelist của Backend.
     - Xác minh các thông số kế hoạch nằm trong ngưỡng: `plannedSets` (1-5), `plannedReps` (1-30), `plannedRpe` (6-9) và `restSeconds` (30-300 giây).
     - Xác minh số ngày bằng `workoutDaysPerWeek`, `dayNumber` duy nhất/liên tục, mỗi ngày có ít nhất một bài tập và không lặp cùng `exerciseId` trong một ngày.
     - Xác minh số bữa bằng `mealsPerDay`, đồng thời kiểm tra lại món ăn theo chế độ ăn, dị ứng và danh sách thực phẩm loại trừ.
  7. Backend tự ghép (Merge) chỉ số Calories/Macros do Backend tự tính toán ở Bước 2 vào JSON thực đơn của AI.
  8. Hệ thống lưu giáo án ở trạng thái `DRAFT`, lưu thực đơn với nguồn tạo `recommendationSource = AI_GENERATED` và trả kết quả cho Client.
  9. Khi Member chấp nhận giáo án, Member yêu cầu kích hoạt giáo án `DRAFT`; Backend chuyển giáo án `ACTIVE` cũ (nếu có) sang `ARCHIVED`, rồi chuyển giáo án đích sang `ACTIVE` trong cùng transaction.
- **Luồng ngoại lệ (Alternative / Exception Flows):**
  - **[Ngoại lệ 07.a] - AI phản hồi sai cấu trúc hoặc đề xuất bài tập ngoài whitelist:**
    - Backend phát hiện có ID bài tập lạ hoặc thông số vượt ngưỡng an toàn.
    - Backend từ chối toàn bộ phản hồi, thực hiện Retry cuộc gọi AI tối đa 1 lần.
    - Nếu lần thử lại tiếp tục lỗi, hệ thống kích hoạt Fallback: tải template tĩnh, lọc workout theo whitelist và lọc meal template theo chế độ ăn/dị ứng/thực phẩm loại trừ của Member, hậu kiểm rồi lưu với nguồn `recommendationSource = FALLBACK_TEMPLATE`; response HTTP 200 có `warningCode = AI_RESPONSE_INVALID`.
  - **[Ngoại lệ 07.b] - Lỗi AI Timeout hoặc lỗi mạng (5xx/429):**
    - Cuộc gọi AI API vượt quá timeout 15 giây hoặc trả về HTTP 429/5xx.
    - Backend ngắt attempt hiện tại, retry tối đa 1 lần nếu còn ngân sách tổng 30 giây; nếu vẫn thất bại hoặc hết ngân sách, lọc và hậu kiểm template an toàn như Ngoại lệ 07.a rồi trả Fallback HTTP 200 với `warningCode = AI_TIMEOUT`.
  - **[Ngoại lệ 07.c] - Không thể tạo fallback an toàn:**
    - Backend không tìm được template có bài tập nằm hoàn toàn trong whitelist hoặc nguồn template gặp lỗi.
    - Hệ thống không lưu Workout Plan/Nutrition Plan một phần, trả `AI-001` (HTTP 502 Bad Gateway) và ghi log kỹ thuật đã che dữ liệu nhạy cảm.
  - **[Ngoại lệ 07.d] - Hai request đồng thời kích hoạt giáo án:**
    - Hai request cùng cố archive plan cũ và kích hoạt hai plan DRAFT khác nhau của một Member.
    - Service khóa danh sách plan theo Member để tuần tự hóa hai transaction. Request đến sau chỉ xử lý trên trạng thái mới nhất; nếu hết thời gian chờ khóa hoặc phát sinh version conflict thì rollback và trả `CON-001` (HTTP 409). Trong mọi trường hợp không tồn tại hai plan ACTIVE.
- **Business Rules liên quan:** BR-06, BR-07, BR-08, BR-09A, BR-09C, BR-10, BR-11, BR-12, BR-13, BR-23, BR-25, BR-26.
- **NFR liên quan:** NFR-02 (Ngân sách phản hồi endpoint AI), NFR-13 (Timeout 15 giây mỗi attempt và retry tối đa một lần).
- **Acceptance Criteria (BDD):**
  - **AC-UC_07-01 (Tạo lộ trình đề xuất AI thành công):**
    - **Given** hội viên có gói active, đã hoàn thành cập nhật hồ sơ thể chất và dinh dưỡng đầu vào.
    - **When** gửi yêu cầu tạo lộ trình và cuộc gọi đến AI thành công trong 15 giây,
    - **Then** hệ thống trả về cấu trúc gồm `recommendationSource = "AI_GENERATED"`, các chỉ số sinh học `calculatedTargets` do Backend tính cứng và phần `aiSuggestion` chứa thực đơn và lịch tập đã ánh xạ ID bài tập trong DB.
  - **AC-UC_07-02 (AI phản hồi ID bài tập ngoài whitelist):**
    - **Given** hội viên bị hạn chế vận động vai (`OVERHEAD_MOVEMENT_LIMITED`) nên Backend loại `exerciseId = 99` (bài Overhead Press trong Master Data) khỏi whitelist.
    - **When** AI Engine phản hồi `workoutSchedule` chứa `exerciseId = 99`,
    - **Then** Backend phát hiện vi phạm whitelist, thực hiện từ chối lưu, gọi lại AI API (Retry) lần 2. Nếu lần 2 vẫn dính lỗi, hệ thống kích hoạt Fallback gán giáo án mẫu tĩnh từ DB, trả về HTTP 200 OK cùng `recommendationSource = "FALLBACK_TEMPLATE"` và mã cảnh báo `AI_RESPONSE_INVALID`.
  - **AC-UC_07-03 (AI phản hồi sai cấu trúc JSON Schema):**
    - **Given** AI Engine phản hồi dữ liệu bị thiếu trường bắt buộc hoặc sai kiểu dữ liệu của cấu trúc JSON.
    - **When** Backend kiểm tra tính hợp lệ của schema,
    - **Then** Backend từ chối toàn bộ payload và không lưu dữ liệu sai lệch, Retry tối đa 1 lần; nếu phản hồi lần hai vẫn sai schema thì kích hoạt Fallback template tĩnh, trả về HTTP 200 OK với `recommendationSource = "FALLBACK_TEMPLATE"` và `warningCode = "AI_RESPONSE_INVALID"`.
  - **AC-UC_07-04 (AI API bị Timeout hoặc lỗi server 5xx):**
    - **Given** hệ thống cấu hình TimeLimiter tối đa 15 giây cho mỗi lần gọi AI và ngân sách tổng của endpoint là 30 giây.
    - **When** cuộc gọi AI API vượt quá 15 giây, trả HTTP 429 hoặc lỗi 5xx,
    - **Then** TimeLimiter ngắt lần gọi hiện tại; Backend chỉ Retry tối đa 1 lần nếu còn đủ ngân sách 30 giây, còn nếu hết ngân sách hoặc Retry thất bại thì kích hoạt Fallback template tĩnh ngay, trả về `recommendationSource = "FALLBACK_TEMPLATE"` và `warningCode = "AI_TIMEOUT"`.
  - **AC-UC_07-05 (Hội viên không có gói ACTIVE bị chặn trước khi gọi AI):**
    - **Given** Member đã xác thực nhưng không có Subscription thỏa mãn `status = ACTIVE`, `startDate <= currentDate < endDate`.
    - **When** Member gọi chức năng tạo gợi ý AI,
    - **Then** `SubscriptionGuard` hoặc Method Security chặn request trước bước tính toán và gọi AI, trả HTTP 403 Forbidden với mã `SUB-001`; Backend không tạo recommendation mới.
  - **AC-UC_07-06 (Kích hoạt giáo án và lưu duy nhất một bản ACTIVE):**
    - **Given** Member có Subscription hợp lệ, sở hữu một giáo án `DRAFT` mới và đang có một giáo án cũ ở trạng thái `ACTIVE`.
    - **When** Member kích hoạt giáo án `DRAFT` mới,
    - **Then** Backend chuyển giáo án cũ sang `ARCHIVED`, chuyển giáo án mới sang `ACTIVE` trong cùng transaction và bảo đảm Member chỉ còn đúng một giáo án `ACTIVE` theo BR-26.
  - **AC-UC_07-07 (Chặn xung đột kích hoạt hai giáo án):**
    - **Given** Member có hai giáo án `DRAFT` và hai request kích hoạt được gửi gần như đồng thời,
    - **When** hai transaction cùng cố thay đổi tập Workout Plan của Member,
    - **Then** khóa theo Member tuần tự hóa hai transaction và unique generated key bảo đảm tại mọi thời điểm chỉ có một plan `ACTIVE`; nếu request sau không lấy được khóa hoặc gặp version conflict thì trả HTTP 409 với `errorCode = CON-001`.

---

### [UC-08] Member ghi nhật ký tập luyện
- **Mục tiêu:** Hội viên lưu lại thành công kết quả thực hiện thực tế của một bài tập để lưu trữ lịch sử.
- **Actor chính:** Member
- **Actor phụ (nếu có):** Không
- **Tiền điều kiện (Pre-conditions):** Hội viên có Subscription hợp lệ theo BR-25; đã kích hoạt giáo án luyện tập của mình.
- **Hậu điều kiện (Post-conditions):** Bản ghi `WorkoutLog` được lưu vào database và liên kết với bài tập của hội viên trong ngày.
- **Luồng chính (Basic Flow):**
  1. Hội viên mở ngày tập hiện tại, nhấn chọn bài tập vừa thực hiện xong.
  2. Hội viên điền các chỉ số thực tế: số set thực tế, số reps thực tế, khối lượng tạ sử dụng (kg) và cảm nhận lực RPE thực tế.
  3. Hội viên nhấn nút Lưu.
  4. Hệ thống kiểm duyệt dữ liệu nhập nằm trong ngưỡng cho phép.
  5. Hệ thống kiểm tra xem hội viên có ghi trùng ngày tập cho bài tập này trước đó chưa. Nếu chưa, tạo bản ghi mới.
  6. Hệ thống báo ghi nhật ký tập luyện thành công.
- **Luồng ngoại lệ (Alternative / Exception Flows):**
  - **[Ngoại lệ 08.a] - Ghi trùng ngày tập cho cùng một bài:**
    - Hội viên thực hiện ghi nhật ký lần thứ 2 cho cùng một bài tập trong cùng một ngày.
    - Hệ thống chuyển sang chế độ Update-in-place, ghi đè thông số mới lên bản ghi cũ (áp dụng BR-19).
  - **[Ngoại lệ 08.b] - Dữ liệu thực tế vượt ngưỡng quy định:**
    - Số set > 10 hoặc số reps > 100 hoặc RPE ngoài khoảng 1-10.
    - Hệ thống ném mã lỗi `VAL-001` (HTTP 400 Bad Request) và yêu cầu nhập lại thông số.
  - **[Ngoại lệ 08.c] - Subscription không còn hiệu lực:**
    - Member không có Subscription thỏa mãn `status = ACTIVE`, `startDate <= currentDate < endDate`.
    - `SubscriptionGuard` chặn thao tác ghi log mới và trả `SUB-001` (HTTP 403 Forbidden); dữ liệu lịch sử vẫn được phép xem.
  - **[Ngoại lệ 08.d] - Tham chiếu giáo án không hợp lệ:**
    - `workoutPlanDetailId` không thuộc giáo án ACTIVE của Member, `exerciseId` không khớp chi tiết hoặc `logDate` ở tương lai.
    - Không tìm thấy/không sở hữu trả `WRK-001` (HTTP 404); dữ liệu không khớp hoặc ngày tương lai trả `VAL-001` (HTTP 400). Hệ thống không lưu log.
- **Business Rules liên quan:** BR-08, BR-09B, BR-13, BR-19, BR-25, BR-28.
- **Acceptance Criteria (BDD):**
  - **AC-UC_08-01 (Ghi nhật ký thành công):**
    - **Given** hội viên có gói active và đã hoàn thành buổi tập Bench Press.
    - **When** hội viên điền nhật ký buổi tập với các chỉ số (`actualSets = 4`, `actualReps = 10`, `actualRpe = 8`, `weightUsedKg = 60.0`),
    - **Then** hệ thống validate thành công (nằm trong ngưỡng của BR-09B), thực hiện lưu nhật ký vào database và trả về mã HTTP 201 Created.
  - **AC-UC_08-02 (Ghi nhật ký thất bại do nhập vượt giới hạn thực tế):**
    - **Given** hội viên điền thông số thực tế ngoài tầm an toàn (`actualSets = 12` hoặc `actualRpe = 11`).
    - **When** hội viên bấm lưu kết quả,
    - **Then** hệ thống chặn hành vi, trả về mã trạng thái HTTP 400 Bad Request cùng Error Code `VAL-001` kèm thông báo lỗi trường cụ thể.
  - **AC-UC_08-03 (Ghi nhật ký bị chặn khi gói hết hiệu lực):**
    - **Given** Member đã xác thực nhưng Subscription có `endDate` bằng ngày hiện hành hoặc trạng thái không phải `ACTIVE`.
    - **When** Member gửi yêu cầu ghi workout log mới,
    - **Then** `SubscriptionGuard` từ chối lưu, trả HTTP 403 Forbidden với Error Code `SUB-001`; các workout log đã có không bị xóa và Member vẫn xem được lịch sử.

---

### [UC-09] Member xem tiến độ tập luyện
- **Mục tiêu:** Hội viên xem được biểu đồ trực quan về lịch sử thay đổi thể trạng (cân nặng), sự gia tăng sức mạnh (mức tạ) và tần suất tập theo tuần.
- **Actor chính:** Member
- **Actor phụ (nếu có):** Không
- **Tiền điều kiện (Pre-conditions):** Hội viên đã đăng nhập; có dữ liệu cân nặng hoặc lịch sử tập luyện được ghi nhận trong DB.
- **Hậu điều kiện (Post-conditions):** Cung cấp cấu trúc chuỗi dữ liệu thời gian (Timeseries) phục vụ hiển thị biểu đồ trên giao diện.
- **Luồng chính (Basic Flow):**
  1. Hội viên truy cập trang Phân tích tiến độ.
  2. Hội viên nhập thông số cân nặng mới trong ngày (nếu muốn cập nhật thể trạng hôm nay).
  3. Hệ thống lưu cân nặng mới. Nếu hội viên đã nhập cân nặng ngày hôm nay trước đó, hệ thống thực hiện cập nhật ghi đè bản ghi cũ trong ngày (Update-in-place - áp dụng BR-22).
  4. Backend tổng hợp dữ liệu cân nặng, lịch sử mức tạ tối đa theo bài tập và số ngày tập có workout log phân biệt trong từng tuần ISO.
  5. Hệ thống trả về các chuỗi dữ liệu cân nặng, mức tạ và `workoutFrequencyByWeek` sắp xếp tăng dần theo mốc thời gian.
  6. Client tiếp nhận dữ liệu và vẽ biểu đồ đường trực quan cho Hội viên.
- **Luồng ngoại lệ (Alternative / Exception Flows):**
  - **[Ngoại lệ 09.a] - Truy cập sai sở hữu dữ liệu:**
    - Người dùng cố truy vấn API phân tích tiến độ sử dụng User ID của người dùng khác.
    - Hệ thống từ chối truy cập, trả về lỗi `AUTH-002` (HTTP 403 Forbidden).
- **Business Rules liên quan:** BR-13, BR-22.
- **Acceptance Criteria (BDD):**
  - **AC-UC_09-01 (Ghi nhận BodyProgress trùng ngày thực hiện Update-in-place):**
    - **Given** hội viên đã lưu chỉ số cân nặng buổi sáng là `70.5kg` cho ngày hiện hành trong DB.
    - **When** buổi chiều cùng ngày hội viên tiếp tục lưu chỉ số cân nặng là `70.2kg` trên giao diện,
    - **Then** hệ thống kích hoạt cơ chế Update-in-place theo BR-22, tiến hành ghi đè cập nhật giá trị cân nặng thành `70.2kg` trên bản ghi cũ thay vì sinh ra dòng dữ liệu mới trong database.
  - **AC-UC_09-02 (Chỉ xem được tiến độ thuộc sở hữu của chính mình):**
    - **Given** Member A đã xác thực và Member B có dữ liệu BodyProgress riêng.
    - **When** Member A yêu cầu xem hoặc truy vấn dữ liệu tiến độ bằng định danh của Member B,
    - **Then** Backend từ chối truy cập với `AUTH-002` (HTTP 403 Forbidden) theo BR-13 và không trả dữ liệu tiến độ của Member B.

---

### [UC-10] Admin khóa/mở khóa tài khoản
- **Mục tiêu:** Quản trị viên thay đổi trạng thái hoạt động của tài khoản người dùng để thực thi các chính sách an toàn.
- **Actor chính:** Admin
- **Actor phụ (nếu có):** Không
- **Tiền điều kiện (Pre-conditions):** Admin đã đăng nhập tài khoản quản trị; tài khoản người dùng cần xử lý tồn tại trên hệ thống.
- **Hậu điều kiện (Post-conditions):**
  - Cập nhật trạng thái `accountStatus` của người dùng thành `LOCKED` hoặc `ACTIVE` trong DB.
  - Quyền đăng nhập mới bị chặn ngay lập tức; JWT hiện hành không bị thu hồi trực tiếp nhưng mọi request yêu cầu xác thực tiếp theo của tài khoản LOCKED bị `AccountStatusGuard` hoặc Method Security từ chối.
- **Luồng chính (Basic Flow):**
  1. Admin truy cập danh sách tài khoản người dùng tại Dashboard Quản trị.
  2. Admin tìm kiếm tài khoản cần xử lý và nhấn nút "Khóa tài khoản" (hoặc "Mở khóa").
  3. Hệ thống kiểm tra xem tài khoản có đang ở đúng trạng thái hợp lệ để thực thi hành động hay không.
  4. Admin nhập lý do thực hiện khóa (nếu khóa tài khoản).
  5. Hệ thống cập nhật trạng thái `accountStatus` trong DB.
  6. Do JWT có kiến trúc stateless, hệ thống không thu hồi hoặc vô hiệu hóa trực tiếp JWT đã phát hành. `JwtAuthenticationFilter` xác thực chữ ký/thời hạn và nạp identity/roles nhưng không đánh giá `accountStatus`; ở request yêu cầu xác thực tiếp theo, `AccountStatusGuard` hoặc Method Security truy vấn DB/Cache theo User ID trong Security Context và trả HTTP 403 với `ACC-004` nếu tài khoản là `LOCKED`. **Lưu ý:** Gói tập subscription hiện tại vẫn được bảo lưu ngày hết hạn cũ và không bị xóa hoặc thay đổi.
  7. Hệ thống hiển thị thông báo thay đổi trạng thái tài khoản thành công.
- **Luồng ngoại lệ (Alternative / Exception Flows):**
  - **[Ngoại lệ 10.a] - Khóa tài khoản do hết hạn gói tập:**
    - Hệ thống phát hiện lý do khóa là "hết hạn gói tập".
    - Hệ thống từ chối thực hiện, hiển thị cảnh báo yêu cầu Admin chỉ khóa tài khoản khi có vi phạm nội quy, hết hạn gói tập chỉ tự động ngưng quyền truy cập tính năng cao cấp của gói thay vì khóa đăng nhập (áp dụng BR-16).
- **Business Rules liên quan:** BR-03, BR-16, BR-21.
- **Acceptance Criteria (BDD):**
  - **AC-UC_10-01 (Chặn truy cập đối với tài khoản bị khóa):**
    - **Given** Admin khóa tài khoản của hội viên B thành công (`accountStatus = LOCKED`) nhưng gói tập Subscription đang ACTIVE vẫn được bảo lưu thời hạn.
    - **When** hội viên B cố gắng dùng mã JWT Token hiện hành của mình để truy xuất API của Member,
    - **Then** bộ kiểm soát trạng thái (`AccountStatusGuard`) chặn request ở endpoint bảo vệ và trả `ACC-004` (HTTP 403 Forbidden).
  - **AC-UC_10-02 (Chặn truy cập khách vãng lai chưa xác thực):**
    - **Given** người dùng chưa thực hiện đăng nhập và không có JWT token hợp lệ.
    - **When** người dùng này cố tình gửi request trực tiếp vào các API bảo mật của Member,
    - **Then** Spring Security từ chối request và trả `ACC-005` (HTTP 401 Unauthorized).
  - **AC-UC_10-03 (Mở khóa tài khoản thành công):**
    - **Given** Admin đã xác thực và tài khoản Member đang có `accountStatus = LOCKED`.
    - **When** Admin thực hiện thao tác mở khóa tài khoản,
    - **Then** Backend chuyển `accountStatus` thành `ACTIVE`, xóa hiệu lực của trạng thái khóa trong DB/Cache và trả HTTP 200 OK; Member có thể đăng nhập lại bằng thông tin hợp lệ.
