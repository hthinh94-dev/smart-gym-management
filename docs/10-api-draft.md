# 10. API Draft

## 1. Quy ước chung
- **Base URL:** `/api/v1` (Tất cả endpoint phải sử dụng prefix này).
- **Mã hóa:** `application/json; charset=UTF-8`.
- **Xác thực:** Sử dụng Bearer JWT Token đặt trong HTTP Header `Authorization: Bearer <token>`.
- **Phân trang:** `page` là số nguyên từ 0; `size` từ 1–100. Giá trị mặc định do từng endpoint công bố. Vi phạm trả `VAL-001` (HTTP 400).
- **Thời gian:** Timestamp trả theo UTC ISO-8601 (hậu tố `Z`); ngày nghiệp vụ `recordDate`, `startDate`, `endDate` dùng `yyyy-MM-dd` theo timezone `Asia/Ho_Chi_Minh`.
- **HTTP Status Codes:**
  - `200 OK` / `201 Created`: Thành công.
  - `400 Bad Request`: Lỗi cú pháp dữ liệu hoặc validation đầu vào thất bại.
  - `401 Unauthorized`: Không có token xác thực hoặc token hết hạn/sai chữ ký.
  - `403 Forbidden`: Token hợp lệ nhưng không đủ quyền hoặc tài khoản bị LOCKED/DISABLED.
  - `404 Not Found`: Không tìm thấy tài nguyên.
  - `409 Conflict`: Vi phạm ràng buộc nghiệp vụ (trùng email, đã có gói ACTIVE, trùng tên bài tập...).

---

## 2. Response Format

### 2.1. Định dạng phản hồi thành công chung (Success Response Format)
```json
{
  "success": true,
  "message": "Request completed successfully",
  "data": {}
}
```

### 2.2. Định dạng phản hồi lỗi chung (Error Response Format)
```json
{
  "success": false,
  "errorCode": "VAL-001",
  "message": "Dữ liệu đầu vào không hợp lệ.",
  "details": {
    "field": "size",
    "rejectedValue": 150,
    "constraint": "size phải nằm trong khoảng từ 1 đến 100."
  }
}
```

---

## 3. Auth API

### POST /api/v1/auth/register
**Mô tả:** Đăng ký tài khoản hội viên mới. Email được chuẩn hóa (trim + lowercase) trước khi kiểm tra trùng lặp và lưu trữ. Mật khẩu được băm bằng BCrypt trước khi lưu vào DB. Tài khoản mới được gán mặc định `ROLE_MEMBER` và `accountStatus = ACTIVE`.

**Headers:**
- `Content-Type: application/json`

**Request Body:**
```json
{
  "fullName": "Nguyễn Văn An",
  "email": "  User@Gmail.Com  ",
  "password": "SecurePass1",
  "confirmPassword": "SecurePass1"
}
```

**Response thành công (HTTP 201 Created):**
```json
{
  "success": true,
  "message": "Đăng ký tài khoản thành công",
  "data": {
    "id": 101,
    "fullName": "Nguyễn Văn An",
    "email": "user@gmail.com",
    "role": "ROLE_MEMBER",
    "accountStatus": "ACTIVE",
    "createdAt": "2026-07-15T08:00:00Z"
  }
}
```

**Response lỗi - Trùng Email (HTTP 409 Conflict):**
```json
{
  "success": false,
  "errorCode": "ACC-001",
  "message": "Email này đã được sử dụng bởi một tài khoản khác trong hệ thống.",
  "details": {
    "field": "email",
    "rejectedValue": "user@gmail.com"
  }
}
```

**Response lỗi - Mật khẩu sai định dạng (HTTP 400 Bad Request):**
```json
{
  "success": false,
  "errorCode": "ACC-002",
  "message": "Mật khẩu không đáp ứng yêu cầu bảo mật.",
  "details": {
    "field": "password",
    "constraint": "Mật khẩu phải từ 8 đến 72 ký tự, chứa ít nhất 1 chữ hoa và 1 chữ số, không có khoảng trắng ở đầu hoặc cuối."
  }
}
```

Trường `password` và `confirmPassword` chỉ tồn tại trong request DTO để validation; cả hai không xuất hiện trong response, application log hoặc dữ liệu lưu trữ. Nếu hai trường không khớp, endpoint cũng trả `ACC-002` (HTTP 400) với `details.field = "confirmPassword"`.

---

### POST /api/v1/auth/login
**Mô tả:** Đăng nhập bằng Email và Mật khẩu. Hệ thống chuẩn hóa email trước khi tra cứu, kiểm tra trạng thái tài khoản và xác thực mật khẩu bằng BCrypt. Trả về JWT Access Token nếu hợp lệ.

**Headers:**
- `Content-Type: application/json`

**Request Body:**
```json
{
  "email": "user@gmail.com",
  "password": "SecurePass1"
}
```

**Response thành công (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "Đăng nhập thành công",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyQGdtYWlsLmNvbSIsInJvbGVzIjpbIlJPTEVfTUVNQkVSIl0sImlhdCI6MTc1MjU2MzIwMCwiZXhwIjoxNzUyNjQ5NjAwfQ.signature",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "user": {
      "id": 101,
      "fullName": "Nguyễn Văn An",
      "email": "user@gmail.com",
      "role": "ROLE_MEMBER"
    }
  }
}
```

**Response lỗi - Sai mật khẩu hoặc email (HTTP 401 Unauthorized):**
```json
{
  "success": false,
  "errorCode": "ACC-007",
  "message": "Tên đăng nhập hoặc mật khẩu không chính xác.",
  "details": {}
}
```

**Response lỗi - Tài khoản bị khóa (HTTP 403 Forbidden):**
```json
{
  "success": false,
  "errorCode": "ACC-004",
  "message": "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên để được hỗ trợ.",
  "details": {
    "accountStatus": "LOCKED"
  }
}
```

**Response lỗi - Tài khoản bị vô hiệu hóa vĩnh viễn (HTTP 403 Forbidden):**
```json
{
  "success": false,
  "errorCode": "ACC-006",
  "message": "Tài khoản đã bị vô hiệu hóa vĩnh viễn. Vui lòng liên hệ ban quản trị.",
  "details": {
    "accountStatus": "DISABLED"
  }
}
```

---

### GET /api/v1/users/me
**Mô tả:** Lấy thông tin tài khoản của người dùng hiện đang đăng nhập. Thông tin trích xuất trực tiếp từ JWT Security Context.

**Headers:**
- `Authorization: Bearer <token>`

**Response thành công (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "Lấy thông tin người dùng thành công",
  "data": {
    "id": 101,
    "fullName": "Nguyễn Văn An",
    "email": "user@gmail.com",
    "role": "ROLE_MEMBER",
    "accountStatus": "ACTIVE",
    "createdAt": "2026-07-15T08:00:00Z"
  }
}
```

**Response lỗi - Không có hoặc hết hạn token (HTTP 401 Unauthorized):**
```json
{
  "success": false,
  "errorCode": "ACC-005",
  "message": "Token xác thực không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại.",
  "details": {}
}
```

---

## 4. Member Profile API

### GET /api/v1/member/profile
**Mô tả:** Lấy toàn bộ thông tin hồ sơ thể trạng và sở thích dinh dưỡng của hội viên đang đăng nhập, cùng các chỉ số sinh học đã được Backend tính toán.

**Headers:**
- `Authorization: Bearer <token>` (Role: MEMBER)

**Response thành công (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "Lấy hồ sơ thể trạng thành công",
  "data": {
    "memberId": 101,
    "bioProfile": {
      "gender": "MALE",
      "dateOfBirth": "1998-05-15",
      "heightCm": 175.0,
      "weightKg": 70.0,
      "fitnessGoal": "BULK",
      "fitnessLevel": "BEGINNER",
      "activityLevel": "MODERATELY_ACTIVE",
      "workoutDaysPerWeek": 4,
      "maxSessionMinutes": 90,
      "availableEquipment": ["BARBELL", "DUMBBELL", "CABLE"],
      "targetMuscleGroups": ["CHEST", "BACK", "LEGS"],
      "injuryConstraints": ["LOWER_BACK_LOAD_LIMITED"]
    },
    "nutritionProfile": {
      "dietaryPreference": "OMNIVORE",
      "foodAllergies": ["PEANUTS"],
      "excludedFoods": ["BEEF"],
      "mealsPerDay": 4
    },
    "calculatedTargets": {
      "bmi": 22.86,
      "bmr": 1706.25,
      "tdee": 2644.69,
      "dailyCaloriesKcal": 2944.69,
      "proteinGrams": 154.0,
      "fatGrams": 81.74,
      "carbGrams": 383.26
    },
    "updatedAt": "2026-07-15T10:30:00Z"
  }
}
```

---

### PUT /api/v1/member/profile
**Mô tả:** Cập nhật toàn bộ hồ sơ thể trạng và dinh dưỡng. Hệ thống tự động tính toán lại các chỉ số BMI, BMR, TDEE, Calories và Macros sau khi lưu. Validate nghiêm ngặt theo BR-23.

**Headers:**
- `Authorization: Bearer <token>` (Role: MEMBER)
- `Content-Type: application/json`

**Request Body:**
```json
{
  "gender": "MALE",
  "dateOfBirth": "1998-05-15",
  "heightCm": 175.0,
  "weightKg": 72.5,
  "fitnessGoal": "BULK",
  "fitnessLevel": "BEGINNER",
  "activityLevel": "MODERATELY_ACTIVE",
  "workoutDaysPerWeek": 4,
  "maxSessionMinutes": 90,
  "availableEquipment": ["BARBELL", "DUMBBELL", "CABLE"],
  "targetMuscleGroups": ["CHEST", "BACK", "LEGS"],
  "injuryConstraints": ["LOWER_BACK_LOAD_LIMITED"],
  "dietaryPreference": "OMNIVORE",
  "foodAllergies": ["PEANUTS"],
  "excludedFoods": ["BEEF"],
  "mealsPerDay": 4
}
```

**Response thành công (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "Cập nhật hồ sơ thể trạng thành công",
  "data": {
    "memberId": 101,
    "bioProfile": {
      "gender": "MALE",
      "dateOfBirth": "1998-05-15",
      "heightCm": 175.0,
      "weightKg": 72.5,
      "fitnessGoal": "BULK",
      "fitnessLevel": "BEGINNER",
      "activityLevel": "MODERATELY_ACTIVE",
      "workoutDaysPerWeek": 4,
      "maxSessionMinutes": 90,
      "availableEquipment": ["BARBELL", "DUMBBELL", "CABLE"],
      "targetMuscleGroups": ["CHEST", "BACK", "LEGS"],
      "injuryConstraints": ["LOWER_BACK_LOAD_LIMITED"]
    },
    "nutritionProfile": {
      "dietaryPreference": "OMNIVORE",
      "foodAllergies": ["PEANUTS"],
      "excludedFoods": ["BEEF"],
      "mealsPerDay": 4
    },
    "calculatedTargets": {
      "bmi": 23.67,
      "bmr": 1729.0,
      "tdee": 2679.95,
      "dailyCaloriesKcal": 2979.95,
      "proteinGrams": 159.5,
      "fatGrams": 82.78,
      "carbGrams": 388.61
    },
    "updatedAt": "2026-07-15T11:00:00Z"
  }
}
```

**Response lỗi - Dữ liệu sai định dạng hoặc vượt ngưỡng (HTTP 400 Bad Request):**
```json
{
  "success": false,
  "errorCode": "VAL-001",
  "message": "Dữ liệu hồ sơ không hợp lệ. Vui lòng kiểm tra lại các trường bị lỗi.",
  "details": {
    "errors": [
      {
        "field": "mealsPerDay",
        "rejectedValue": 8,
        "constraint": "Số bữa ăn mỗi ngày phải nằm trong khoảng từ 1 đến 6 (BR-23)."
      },
      {
        "field": "activityLevel",
        "rejectedValue": "SUPER_ACTIVE",
        "constraint": "Giá trị activityLevel không hợp lệ. Chỉ chấp nhận: SEDENTARY, LIGHTLY_ACTIVE, MODERATELY_ACTIVE, VERY_ACTIVE."
      }
    ]
  }
}
```

---

## 5. Membership API

### GET /api/v1/packages
**Mô tả:** Xem danh sách gói tập công khai đang mở bán. Chỉ trả về các gói có `isActive = true`. Không yêu cầu xác thực.

**Query Parameters:** Không bắt buộc.

**Response thành công (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "Lấy danh sách gói tập thành công",
  "data": [
    {
      "id": 1,
      "name": "Gói Cơ Bản 1 Tháng",
      "durationDays": 30,
      "price": 500000,
      "description": "Tập luyện không giới hạn thời gian trong 30 ngày."
    },
    {
      "id": 2,
      "name": "Gói Tiêu Chuẩn 3 Tháng",
      "durationDays": 90,
      "price": 1200000,
      "description": "Tiết kiệm 20% so với mua gói 1 tháng liên tiếp."
    },
    {
      "id": 3,
      "name": "Gói Cao Cấp 6 Tháng",
      "durationDays": 180,
      "price": 2000000,
      "description": "Ưu tiên giờ tập cao điểm và sử dụng phòng tập cá nhân."
    }
  ]
}
```

---

### POST /api/v1/admin/packages
**Mô tả:** Admin tạo gói tập mới. Mặc định `isActive = true`.

**Headers:**
- `Authorization: Bearer <token>` (Role: ADMIN)
- `Content-Type: application/json`

**Request Body:**
```json
{
  "name": "Gói VIP 12 Tháng",
  "durationDays": 365,
  "price": 3500000,
  "description": "Gói thành viên VIP cao cấp nhất, ưu tiên tất cả dịch vụ phòng tập."
}
```

**Response thành công (HTTP 201 Created):**
```json
{
  "success": true,
  "message": "Tạo gói tập mới thành công",
  "data": {
    "id": 4,
    "name": "Gói VIP 12 Tháng",
    "durationDays": 365,
    "price": 3500000,
    "description": "Gói thành viên VIP cao cấp nhất, ưu tiên tất cả dịch vụ phòng tập.",
    "isActive": true,
    "createdAt": "2026-07-15T09:00:00Z"
  }
}
```

**Response lỗi - Dữ liệu gói tập không hợp lệ (HTTP 400 Bad Request):**
```json
{
  "success": false,
  "errorCode": "VAL-001",
  "message": "Dữ liệu gói tập không hợp lệ.",
  "details": {
    "field": "durationDays",
    "rejectedValue": 0,
    "constraint": "durationDays phải là số nguyên lớn hơn 0."
  }
}
```

**Response lỗi - Trùng tên gói tập (HTTP 409 Conflict):**
```json
{
  "success": false,
  "errorCode": "SUB-007",
  "message": "Tên gói tập đã tồn tại.",
  "details": {
    "field": "name",
    "rejectedValue": "Gói VIP 12 Tháng"
  }
}
```

---

### PUT /api/v1/admin/packages/{id}
**Mô tả:** Admin cập nhật thông tin gói tập hiện có.

**Headers:**
- `Authorization: Bearer <token>` (Role: ADMIN)
- `Content-Type: application/json`

**Path Parameters:** `id` - ID của gói tập cần cập nhật.

**Request Body:**
```json
{
  "name": "Gói VIP 12 Tháng - Phiên bản 2",
  "durationDays": 365,
  "price": 3800000,
  "description": "Gói thành viên VIP cao cấp, đã cập nhật giá theo chính sách mới."
}
```

**Response thành công (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "Cập nhật gói tập thành công",
  "data": {
    "id": 4,
    "name": "Gói VIP 12 Tháng - Phiên bản 2",
    "durationDays": 365,
    "price": 3800000,
    "description": "Gói thành viên VIP cao cấp, đã cập nhật giá theo chính sách mới.",
    "isActive": true,
    "updatedAt": "2026-07-15T10:00:00Z"
  }
}
```

**Response lỗi - Không tìm thấy gói tập (HTTP 404 Not Found):**
```json
{
  "success": false,
  "errorCode": "SUB-002",
  "message": "Không tìm thấy gói tập cần cập nhật.",
  "details": {
    "packageId": 999
  }
}
```

**Response lỗi - Tên mới trùng với gói tập khác (HTTP 409 Conflict):**
```json
{
  "success": false,
  "errorCode": "SUB-007",
  "message": "Tên gói tập đã được sử dụng bởi một gói khác.",
  "details": {
    "packageId": 4,
    "rejectedValue": "Gói Tiêu Chuẩn 3 Tháng"
  }
}
```

---

### DELETE /api/v1/admin/packages/{id}
**Mô tả:** Admin vô hiệu hóa gói tập (Soft Inactive). Không xóa cứng khỏi DB. Đặt `isActive = false` để ngưng bán. Các subscription đang active dùng gói này vẫn tiếp tục chu kỳ bình thường.

**Headers:**
- `Authorization: Bearer <token>` (Role: ADMIN)

**Path Parameters:** `id` - ID của gói tập cần vô hiệu hóa.

**Response thành công (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "Gói tập đã được vô hiệu hóa thành công. Hội viên đang sử dụng gói này sẽ tiếp tục được phục vụ đến hết thời hạn.",
  "data": {
    "id": 4,
    "isActive": false,
    "updatedAt": "2026-07-15T11:00:00Z"
  }
}
```

**Response lỗi - Không tìm thấy gói tập (HTTP 404 Not Found):**
```json
{
  "success": false,
  "errorCode": "SUB-002",
  "message": "Không tìm thấy gói tập cần vô hiệu hóa.",
  "details": {
    "packageId": 999
  }
}
```

---

### POST /api/v1/member/subscriptions
**Mô tả:** Hội viên gửi yêu cầu **đăng ký gói mới**. Subscription được tạo ở trạng thái `PENDING` chờ Admin duyệt. Endpoint này không dùng cho gia hạn; gia hạn sử dụng endpoint Renewal Request riêng.

**Headers:**
- `Authorization: Bearer <token>` (Role: MEMBER)
- `Content-Type: application/json`

**Request Body:**
```json
{
  "packageId": 2
}
```

**Response thành công (HTTP 201 Created):**
```json
{
  "success": true,
  "message": "Yêu cầu đăng ký gói tập đã được gửi thành công. Vui lòng chờ quản trị viên phê duyệt.",
  "data": {
    "subscriptionId": 55,
    "memberId": 101,
    "packageId": 2,
    "packageName": "Gói Tiêu Chuẩn 3 Tháng",
    "price": 1200000,
    "status": "PENDING",
    "requestedAt": "2026-07-15T12:00:00Z"
  }
}
```

**Response lỗi - Đã có gói tập ACTIVE (HTTP 409 Conflict):**
```json
{
  "success": false,
  "errorCode": "SUB-004",
  "message": "Bạn hiện đang có gói tập ACTIVE. Vui lòng sử dụng endpoint tạo yêu cầu gia hạn cho gói hiện hành.",
  "details": {
    "currentActiveSubscriptionId": 48,
    "currentEndDate": "2026-09-30"
  }
}
```

**Response lỗi - Không tìm thấy gói tập (HTTP 404 Not Found):**
```json
{
  "success": false,
  "errorCode": "SUB-002",
  "message": "Không tìm thấy gói tập được yêu cầu.",
  "details": {
    "packageId": 999
  }
}
```

**Response lỗi - Gói tập đã ngưng bán (HTTP 409 Conflict):**
```json
{
  "success": false,
  "errorCode": "SUB-003",
  "message": "Gói tập này đã ngừng kinh doanh và không thể đăng ký. Vui lòng chọn gói tập khác.",
  "details": {
    "packageId": 4,
    "packageStatus": "INACTIVE"
  }
}
```

**Response lỗi - Đã có yêu cầu đăng ký mới PENDING (HTTP 409 Conflict):**
```json
{
  "success": false,
  "errorCode": "SUB-006",
  "message": "Bạn đã có một yêu cầu đăng ký gói mới đang chờ xử lý.",
  "details": {
    "pendingRequestId": 54,
    "pendingRequestStatus": "PENDING"
  }
}
```

---

### GET /api/v1/member/subscriptions/current
**Mô tả:** Xem thông tin gói tập hiện hành của hội viên đang đăng nhập. Subscription được xem là còn hiệu lực khi `status = ACTIVE` và `startDate <= currentDate < endDate`; `endDate` là biên exclusive theo BR-25.

**Headers:**
- `Authorization: Bearer <token>` (Role: MEMBER)

**Response thành công (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "Lấy thông tin gói tập hiện hành thành công",
  "data": {
    "subscriptionId": 55,
    "memberId": 101,
    "packageId": 2,
    "packageName": "Gói Tiêu Chuẩn 3 Tháng",
    "status": "ACTIVE",
    "startDate": "2026-07-15",
    "endDate": "2026-10-13",
    "daysRemaining": 90,
    "approvedAt": "2026-07-15T14:00:00Z"
  }
}
```

**Response lỗi - Không có Subscription hiện hành (HTTP 404 Not Found):**
```json
{
  "success": false,
  "errorCode": "SUB-005",
  "message": "Không tìm thấy Subscription hiện hành của hội viên.",
  "details": {
    "memberId": 101
  }
}
```

---

### POST /api/v1/member/subscriptions/{activeSubscriptionId}/renewal-requests
**Mô tả:** Hội viên tạo yêu cầu gia hạn riêng cho Subscription còn hiệu lực theo BR-25 (`status = ACTIVE`, `startDate <= currentDate < endDate`). Endpoint không tạo thêm Subscription `ACTIVE`; chỉ tạo Renewal Request `PENDING` liên kết với Subscription hiện hành theo BR-24.

**Headers:**
- `Authorization: Bearer <token>` (Role: MEMBER)
- `Content-Type: application/json`

**Path Parameters:** `activeSubscriptionId` - ID Subscription đang `ACTIVE` thuộc sở hữu của Member.

**Request Body:**
```json
{
  "packageId": 5
}
```

**Response thành công (HTTP 201 Created):**
```json
{
  "success": true,
  "message": "Yêu cầu gia hạn gói tập đã được gửi và đang chờ phê duyệt.",
  "data": {
    "renewalRequestId": 88,
    "renewalRequestStatus": "PENDING",
    "activeSubscriptionId": 48,
    "memberId": 101,
    "packageId": 5,
    "requestedAt": "2026-07-15T12:10:00Z"
  }
}
```

**Response lỗi - Subscription ACTIVE không thuộc Member hoặc không hợp lệ (HTTP 404 Not Found):**
```json
{
  "success": false,
  "errorCode": "SUB-005",
  "message": "Không tìm thấy subscription ACTIVE hợp lệ để gia hạn.",
  "details": {
    "activeSubscriptionId": 48
  }
}
```

**Response lỗi - Package không khớp Subscription hiện hành (HTTP 400 Bad Request):**
```json
{
  "success": false,
  "errorCode": "VAL-001",
  "message": "packageId của yêu cầu gia hạn phải khớp với package của Subscription ACTIVE hiện hành.",
  "details": {
    "activeSubscriptionId": 48,
    "currentPackageId": 5,
    "requestedPackageId": 6
  }
}
```

**Response lỗi - Gói gia hạn INACTIVE (HTTP 409 Conflict):**
```json
{
  "success": false,
  "errorCode": "SUB-003",
  "message": "Gói tập đã ngừng hoạt động và không thể dùng để gia hạn.",
  "details": {
    "packageId": 5,
    "packageStatus": "INACTIVE"
  }
}
```

**Response lỗi - Đã có Renewal Request PENDING (HTTP 409 Conflict):**
```json
{
  "success": false,
  "errorCode": "SUB-006",
  "message": "Subscription này đã có một yêu cầu gia hạn đang chờ xử lý.",
  "details": {
    "activeSubscriptionId": 48,
    "pendingRenewalRequestId": 88
  }
}
```

---

### POST /api/v1/admin/subscriptions/{id}/approve
**Mô tả:** Admin phê duyệt một yêu cầu đang ở trạng thái `PENDING`. Client bắt buộc gửi `requestType` để Backend tải đúng loại bản ghi, tránh xung đột ID giữa Subscription Request và Renewal Request. Nếu là gia hạn, hệ thống cộng dồn thời hạn theo BR-24: `newEndDate = currentEndDate + durationDays`; không tạo thêm bản ghi ACTIVE thứ hai.

**Headers:**
- `Authorization: Bearer <token>` (Role: ADMIN)

**Path Parameters:** `id` - ID yêu cầu đang `PENDING`; có thể là Subscription Request đăng ký mới hoặc Renewal Request gia hạn.

**Request Body khi duyệt đăng ký mới:**
```json
{
  "requestType": "NEW_SUBSCRIPTION"
}
```

**Request Body khi duyệt gia hạn:**
```json
{
  "requestType": "RENEWAL"
}
```

**Response thành công - Đăng ký mới (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "Phê duyệt đăng ký gói tập mới thành công",
  "data": {
    "subscriptionId": 55,
    "memberId": 101,
    "packageId": 2,
    "packageName": "Gói Tiêu Chuẩn 3 Tháng",
    "status": "ACTIVE",
    "renewalType": "NEW_SUBSCRIPTION",
    "startDate": "2026-07-15",
    "endDate": "2026-10-13",
    "approvedBy": "admin@smartgym.com",
    "approvedAt": "2026-07-15T14:00:00Z"
  }
}
```

**Response thành công - Gia hạn cộng dồn (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "Subscription renewal approved and active subscription duration extended",
  "data": {
    "renewalRequestId": 88,
    "renewalRequestStatus": "PROCESSED",
    "activeSubscriptionId": 48,
    "previousEndDate": "2026-10-13",
    "newEndDate": "2027-01-11",
    "approvedBy": "admin@smartgym.com",
    "approvedAt": "2026-07-15T14:00:00Z"
  }
}
```

**Response lỗi - Yêu cầu không ở trạng thái PENDING (HTTP 400 Bad Request):**
```json
{
  "success": false,
  "errorCode": "VAL-001",
  "message": "Chỉ có yêu cầu subscription hoặc renewal ở trạng thái PENDING mới được phê duyệt.",
  "details": {
    "requestId": 88,
    "currentStatus": "PROCESSED"
  }
}
```

**Response lỗi - Gói tập đã ngưng hoạt động trước khi duyệt (HTTP 409 Conflict):**
```json
{
  "success": false,
  "errorCode": "SUB-003",
  "message": "Gói tập liên kết đã ngừng hoạt động nên yêu cầu không thể được phê duyệt.",
  "details": {
    "requestId": 88,
    "packageId": 5,
    "packageStatus": "INACTIVE"
  }
}
```

**Response lỗi - Member đã phát sinh Subscription ACTIVE (HTTP 409 Conflict):**
```json
{
  "success": false,
  "errorCode": "SUB-004",
  "message": "Member đã có Subscription ACTIVE; không thể phê duyệt thêm đăng ký mới.",
  "details": {
    "requestId": 55,
    "currentActiveSubscriptionId": 48
  }
}
```

**Response lỗi - Subscription đích của yêu cầu gia hạn không còn hợp lệ (HTTP 404 Not Found):**
```json
{
  "success": false,
  "errorCode": "SUB-005",
  "message": "Không tìm thấy Subscription ACTIVE hợp lệ để áp dụng yêu cầu gia hạn.",
  "details": {
    "renewalRequestId": 88,
    "activeSubscriptionId": 48
  }
}
```

---

### POST /api/v1/admin/subscriptions/{id}/cancel
**Mô tả:** Admin hủy một subscription đang ở trạng thái `PENDING` hoặc `ACTIVE`.

**Headers:**
- `Authorization: Bearer <token>` (Role: ADMIN)

**Path Parameters:** `id` - ID của subscription cần hủy.

**Response thành công (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "Hủy subscription thành công. Hội viên sẽ mất quyền truy cập các tính năng cao cấp ngay lập tức.",
  "data": {
    "subscriptionId": 55,
    "status": "CANCELLED",
    "cancelledAt": "2026-07-15T14:30:00Z"
  }
}
```

**Response lỗi - Không tìm thấy Subscription để hủy (HTTP 404 Not Found):**
```json
{
  "success": false,
  "errorCode": "SUB-005",
  "message": "Không tìm thấy Subscription cần hủy.",
  "details": {
    "subscriptionId": 999
  }
}
```

---

## 6. Exercise API

### GET /api/v1/exercises
**Mô tả:** Xem thư viện bài tập có tính năng phân trang và lọc động. Chỉ trả về các bài tập có `isActive = true`.

**Headers:**
- `Authorization: Bearer <token>`

**Query Parameters:**
- `page` (Integer, mặc định 0): Trang hiện tại.
- `size` (Integer, mặc định 10): Số bài tập mỗi trang.
- `search` (String, tùy chọn): Từ khóa tìm theo tên bài tập.
- `muscleGroup` (Enum, tùy chọn): Lọc theo nhóm cơ chính (CHEST, BACK, LEGS, SHOULDERS, ARMS, CORE).
- `equipment` (Enum, tùy chọn): Lọc theo thiết bị vật lý (BARBELL, DUMBBELL, MACHINE, CABLE, BENCH). Bài bodyweight có `equipmentRequired = []` và vẫn xuất hiện khi không áp dụng bộ lọc thiết bị.
- `difficulty` (Enum, tùy chọn): Lọc theo độ khó (BEGINNER, INTERMEDIATE, ADVANCED).

**Response thành công (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "Lấy danh sách bài tập thành công",
  "data": {
    "content": [
      {
        "id": 12,
        "name": "Flat Dumbbell Press",
        "primaryMuscleGroup": "CHEST",
        "secondaryMuscleGroups": ["SHOULDERS", "ARMS"],
        "movementPattern": "PUSH",
        "targetBodyRegions": ["UPPER_BODY"],
        "equipmentRequired": ["DUMBBELL"],
        "difficultyLevel": "BEGINNER",
        "contraindicationTags": ["WRIST_FLEXION_LIMITED"],
        "instructionText": "Nằm ngửa trên ghế phẳng, cầm hai tạ tay ở vị trí hai bên ngực, đẩy thẳng lên cao rồi hạ xuống có kiểm soát.",
        "isActive": true
      },
      {
        "id": 23,
        "name": "Barbell Back Squat",
        "primaryMuscleGroup": "LEGS",
        "secondaryMuscleGroups": ["CORE", "BACK"],
        "movementPattern": "SQUAT",
        "targetBodyRegions": ["LOWER_BODY", "CORE"],
        "equipmentRequired": ["BARBELL"],
        "difficultyLevel": "INTERMEDIATE",
        "contraindicationTags": ["KNEE_FLEXION_LIMITED", "LOWER_BACK_LOAD_LIMITED"],
        "instructionText": "Đứng thẳng, đặt thanh đòn lên vai sau, hạ thấp cơ thể xuống cho đến khi đùi song song với sàn, rồi đứng lên.",
        "isActive": true
      }
    ],
    "totalElements": 48,
    "totalPages": 5,
    "currentPage": 0,
    "pageSize": 10
  }
}
```

---

### GET /api/v1/exercises/{id}
**Mô tả:** Xem chi tiết một bài tập theo ID.

**Headers:**
- `Authorization: Bearer <token>`

**Path Parameters:** `id` - ID của bài tập.

**Response thành công (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "Lấy thông tin bài tập thành công",
  "data": {
    "id": 12,
    "name": "Flat Dumbbell Press",
    "primaryMuscleGroup": "CHEST",
    "secondaryMuscleGroups": ["SHOULDERS", "ARMS"],
    "movementPattern": "PUSH",
    "targetBodyRegions": ["UPPER_BODY"],
    "equipmentRequired": ["DUMBBELL"],
    "difficultyLevel": "BEGINNER",
    "contraindicationTags": ["WRIST_FLEXION_LIMITED"],
    "instructionText": "Nằm ngửa trên ghế phẳng, cầm hai tạ tay ở vị trí hai bên ngực, đẩy thẳng lên cao rồi hạ xuống có kiểm soát.",
    "isActive": true
  }
}
```

**Response lỗi - Không tìm thấy (HTTP 404 Not Found):**
```json
{
  "success": false,
  "errorCode": "EXR-001",
  "message": "Không tìm thấy bài tập với ID đã cung cấp.",
  "details": {
    "exerciseId": 999
  }
}
```

---

### POST /api/v1/admin/exercises
**Mô tả:** Admin tạo bài tập mới vào thư viện gốc. Mặc định `isActive = true`.

**Headers:**
- `Authorization: Bearer <token>` (Role: ADMIN)
- `Content-Type: application/json`

**Request Body:**
```json
{
  "name": "Incline Barbell Bench Press",
  "primaryMuscleGroup": "CHEST",
  "secondaryMuscleGroups": ["SHOULDERS", "ARMS"],
  "movementPattern": "PUSH",
  "targetBodyRegions": ["UPPER_BODY"],
  "equipmentRequired": ["BARBELL"],
  "difficultyLevel": "INTERMEDIATE",
  "contraindicationTags": ["OVERHEAD_MOVEMENT_LIMITED"],
  "instructionText": "Điều chỉnh ghế ngồi ở góc 30-45 độ. Nằm lên ghế, cầm thanh đòn rộng hơn vai, hạ thanh đòn về phía ngực trên rồi đẩy lên."
}
```

**Response thành công (HTTP 201 Created):**
```json
{
  "success": true,
  "message": "Tạo bài tập mới thành công",
  "data": {
    "id": 49,
    "name": "Incline Barbell Bench Press",
    "primaryMuscleGroup": "CHEST",
    "secondaryMuscleGroups": ["SHOULDERS", "ARMS"],
    "movementPattern": "PUSH",
    "targetBodyRegions": ["UPPER_BODY"],
    "equipmentRequired": ["BARBELL"],
    "difficultyLevel": "INTERMEDIATE",
    "contraindicationTags": ["OVERHEAD_MOVEMENT_LIMITED"],
    "instructionText": "Điều chỉnh ghế ngồi ở góc 30-45 độ. Nằm lên ghế, cầm thanh đòn rộng hơn vai, hạ thanh đòn về phía ngực trên rồi đẩy lên.",
    "isActive": true,
    "createdAt": "2026-07-15T09:30:00Z"
  }
}
```

**Response lỗi - Trùng tên bài tập (HTTP 409 Conflict):**
```json
{
  "success": false,
  "errorCode": "EXR-002",
  "message": "Tên bài tập đã tồn tại trong thư viện gốc. Vui lòng sử dụng tên khác.",
  "details": {
    "field": "name",
    "rejectedValue": "Incline Barbell Bench Press"
  }
}
```

---

### PUT /api/v1/admin/exercises/{id}
**Mô tả:** Admin cập nhật thông tin của bài tập trong thư viện gốc.

**Headers:**
- `Authorization: Bearer <token>` (Role: ADMIN)
- `Content-Type: application/json`

**Path Parameters:** `id` - ID của bài tập cần cập nhật.

**Request Body:**
```json
{
  "name": "Incline Barbell Bench Press",
  "primaryMuscleGroup": "CHEST",
  "secondaryMuscleGroups": ["SHOULDERS", "ARMS"],
  "movementPattern": "PUSH",
  "targetBodyRegions": ["UPPER_BODY"],
  "equipmentRequired": ["BARBELL"],
  "difficultyLevel": "INTERMEDIATE",
  "contraindicationTags": ["OVERHEAD_MOVEMENT_LIMITED", "WRIST_FLEXION_LIMITED"],
  "instructionText": "Điều chỉnh ghế ngồi ở góc 30-45 độ. Kiểm soát đường đi của thanh đòn về phía ngực trên."
}
```

**Response thành công (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "Cập nhật thông tin bài tập thành công",
  "data": {
    "id": 49,
    "name": "Incline Barbell Bench Press",
    "primaryMuscleGroup": "CHEST",
    "secondaryMuscleGroups": ["SHOULDERS", "ARMS"],
    "movementPattern": "PUSH",
    "targetBodyRegions": ["UPPER_BODY"],
    "equipmentRequired": ["BARBELL"],
    "difficultyLevel": "INTERMEDIATE",
    "contraindicationTags": ["OVERHEAD_MOVEMENT_LIMITED", "WRIST_FLEXION_LIMITED"],
    "instructionText": "Điều chỉnh ghế ngồi ở góc 30-45 độ. Kiểm soát đường đi của thanh đòn về phía ngực trên.",
    "isActive": true,
    "updatedAt": "2026-07-15T10:15:00Z"
  }
}
```

**Response lỗi - Không tìm thấy bài tập để cập nhật (HTTP 404 Not Found):**
```json
{
  "success": false,
  "errorCode": "EXR-001",
  "message": "Không tìm thấy bài tập cần cập nhật.",
  "details": {
    "exerciseId": 999
  }
}
```

---

### DELETE /api/v1/admin/exercises/{id}
**Mô tả:** Admin xóa mềm bài tập (Soft Delete). Đặt `isActive = false`. Không xóa cứng khỏi DB để bảo toàn dữ liệu lịch sử nhật ký của hội viên (áp dụng BR-14).

**Headers:**
- `Authorization: Bearer <token>` (Role: ADMIN)

**Path Parameters:** `id` - ID của bài tập cần xóa mềm.

**Response thành công (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "Bài tập đã được xóa khỏi danh mục hiển thị. Dữ liệu lịch sử tập của hội viên được bảo toàn.",
  "data": {
    "id": 49,
    "isActive": false,
    "softDeletedAt": "2026-07-15T11:00:00Z"
  }
}
```

**Response lỗi - Không tìm thấy bài tập để xóa mềm (HTTP 404 Not Found):**
```json
{
  "success": false,
  "errorCode": "EXR-001",
  "message": "Không tìm thấy bài tập cần xóa mềm.",
  "details": {
    "exerciseId": 999
  }
}
```

---

## 7. Recommendation API

### POST /api/v1/member/recommendations
**Mô tả:** Hội viên yêu cầu AI tạo lịch tập luyện và thực đơn dinh dưỡng cá nhân hóa. Backend tự tính toán cứng các chỉ số Calories/Macros, lọc bài tập Whitelist, gọi AI Engine, hậu kiểm và ghép kết quả cuối cùng. Yêu cầu hội viên có Subscription hợp lệ theo BR-25. Giáo án sinh ra được lưu ở trạng thái `DRAFT`; Member chủ động kích hoạt bằng Workout Plan API.

**Headers:**
- `Authorization: Bearer <token>` (Role: MEMBER)

**Request Body:** Không cần body — Hệ thống tự động đọc từ hồ sơ thể trạng đang lưu trong DB của hội viên.
```json
{}
```

**Response thành công - AI_GENERATED (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "Lộ trình tập luyện và dinh dưỡng đã được tạo thành công.",
  "data": {
    "recommendationId": 201,
    "workoutPlanId": 301,
    "workoutPlanStatus": "DRAFT",
    "recommendationSource": "AI_GENERATED",
    "generatedAt": "2026-07-15T14:05:00Z",
    "calculatedTargets": {
      "bmi": 23.67,
      "bmr": 1729.0,
      "tdee": 2679.95,
      "dailyCaloriesKcal": 2979.95,
      "proteinGrams": 159.5,
      "fatGrams": 82.78,
      "carbGrams": 388.61
    },
    "aiSuggestion": {
      "splitModel": "Push/Pull/Legs",
      "explanation": "Với trình độ BEGINNER và 4 buổi tập mỗi tuần, lịch tập Push/Pull/Legs giúp mỗi nhóm cơ được nghỉ ngơi đủ 48 giờ trước khi tập lại.",
      "workoutSchedule": [
        {
          "dayNumber": 1,
          "dayName": "Buổi 1: Đẩy (Ngực, Vai, Tay sau)",
          "exercises": [
            {
              "exerciseId": 12,
              "exerciseName": "Flat Dumbbell Press",
              "plannedSets": 4,
              "plannedReps": 10,
              "plannedRpe": 8,
              "restSeconds": 90,
              "notes": "Kiểm soát tốc độ hạ tạ, không để vai nhô lên."
            },
            {
              "exerciseId": 19,
              "exerciseName": "Dumbbell Lateral Raise",
              "plannedSets": 3,
              "plannedReps": 15,
              "plannedRpe": 7,
              "restSeconds": 60,
              "notes": "Giữ khuỷu tay hơi cong, nâng đến ngang vai."
            }
          ]
        },
        {
          "dayNumber": 2,
          "dayName": "Buổi 2: Kéo (Lưng, Tay trước)",
          "exercises": [
            {
              "exerciseId": 31,
              "exerciseName": "Dumbbell One-Arm Row",
              "plannedSets": 4,
              "plannedReps": 12,
              "plannedRpe": 8,
              "restSeconds": 90,
              "notes": "Giữ lưng thẳng trong suốt động tác."
            }
          ]
        },
        {
          "dayNumber": 3,
          "dayName": "Buổi 3: Chân và mông",
          "exercises": [
            {
              "exerciseId": 44,
              "exerciseName": "Dumbbell Bulgarian Split Squat",
              "plannedSets": 3,
              "plannedReps": 10,
              "plannedRpe": 7,
              "restSeconds": 90,
              "notes": "Giữ thân người ổn định và kiểm soát biên độ phù hợp."
            }
          ]
        },
        {
          "dayNumber": 4,
          "dayName": "Buổi 4: Thân trên tổng hợp",
          "exercises": [
            {
              "exerciseId": 27,
              "exerciseName": "Cable Seated Row",
              "plannedSets": 4,
              "plannedReps": 12,
              "plannedRpe": 8,
              "restSeconds": 90,
              "notes": "Kéo bằng cơ lưng, không giật thân người."
            }
          ]
        }
      ],
      "nutritionPlan": {
        "mealStructure": [
          {
            "mealName": "Bữa sáng",
            "timeSuggest": "07:00",
            "foods": ["3 quả trứng gà luộc", "100g yến mạch nấu chín", "1 quả chuối"],
            "description": "Bữa sáng cân bằng đạm và tinh bột giúp nạp năng lượng cho ngày dài."
          },
          {
            "mealName": "Bữa trưa",
            "timeSuggest": "12:00",
            "foods": ["200g ức gà nướng", "150g cơm gạo lứt", "Rau xanh xào tỏi"],
            "description": "Bữa chính giàu đạm và carb phức tạp."
          },
          {
            "mealName": "Bữa sau tập",
            "timeSuggest": "17:30",
            "foods": ["150g cá hồi áp chảo", "100g khoai lang hấp", "Salad rau trộn"],
            "description": "Hồi phục cơ bắp sau buổi tập."
          },
          {
            "mealName": "Bữa tối",
            "timeSuggest": "20:00",
            "foods": ["150g thịt heo nạc", "100g bún gạo", "Canh rau xanh"],
            "description": "Bữa tối nhẹ nhàng, hỗ trợ giấc ngủ và hồi phục."
          }
        ]
      }
    }
  }
}
```

**Response Fallback - FALLBACK_TEMPLATE do timeout/429/5xx (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "Dịch vụ AI tạm thời không khả dụng. Hệ thống đã tự động cung cấp lộ trình tập luyện mẫu phù hợp cho bạn.",
  "data": {
    "recommendationId": 202,
    "workoutPlanId": 302,
    "workoutPlanStatus": "DRAFT",
    "recommendationSource": "FALLBACK_TEMPLATE",
    "warningCode": "AI_TIMEOUT",
    "generatedAt": "2026-07-15T14:05:30Z",
    "calculatedTargets": {
      "bmi": 23.67,
      "bmr": 1729.0,
      "tdee": 2679.95,
      "dailyCaloriesKcal": 2979.95,
      "proteinGrams": 159.5,
      "fatGrams": 82.78,
      "carbGrams": 388.61
    },
    "aiSuggestion": {
      "splitModel": "Full Body",
      "explanation": "Giáo án mẫu chuẩn hóa dành cho trình độ BEGINNER tập 4 buổi mỗi tuần.",
      "workoutSchedule": [
        {
          "dayNumber": 1,
          "dayName": "Buổi 1: Full Body A",
          "exercises": [
            {
              "exerciseId": 12,
              "exerciseName": "Flat Dumbbell Press",
              "plannedSets": 3,
              "plannedReps": 10,
              "plannedRpe": 7,
              "restSeconds": 90,
              "notes": "Lịch tập mẫu chuẩn."
            }
          ]
        },
        {
          "dayNumber": 2,
          "dayName": "Buổi 2: Full Body B",
          "exercises": [
            {
              "exerciseId": 31,
              "exerciseName": "Dumbbell One-Arm Row",
              "plannedSets": 3,
              "plannedReps": 12,
              "plannedRpe": 7,
              "restSeconds": 90,
              "notes": "Lịch tập mẫu chuẩn."
            }
          ]
        },
        {
          "dayNumber": 3,
          "dayName": "Buổi 3: Full Body C",
          "exercises": [
            {
              "exerciseId": 44,
              "exerciseName": "Dumbbell Bulgarian Split Squat",
              "plannedSets": 3,
              "plannedReps": 10,
              "plannedRpe": 7,
              "restSeconds": 90,
              "notes": "Lịch tập mẫu chuẩn."
            }
          ]
        },
        {
          "dayNumber": 4,
          "dayName": "Buổi 4: Full Body D",
          "exercises": [
            {
              "exerciseId": 27,
              "exerciseName": "Cable Seated Row",
              "plannedSets": 3,
              "plannedReps": 12,
              "plannedRpe": 7,
              "restSeconds": 90,
              "notes": "Lịch tập mẫu chuẩn."
            }
          ]
        }
      ],
      "nutritionPlan": {
        "mealStructure": [
          {
            "mealName": "Bữa sáng",
            "timeSuggest": "07:00",
            "foods": ["3 quả trứng gà luộc", "100g yến mạch"],
            "description": "Thực đơn mẫu chuẩn hóa."
          },
          {
            "mealName": "Bữa trưa",
            "timeSuggest": "12:00",
            "foods": ["200g ức gà", "150g cơm gạo lứt", "Rau xanh"],
            "description": "Bữa chính mẫu giàu protein và carbohydrate phức hợp."
          },
          {
            "mealName": "Bữa phụ",
            "timeSuggest": "16:00",
            "foods": ["1 quả chuối", "1 hộp sữa chua không đường"],
            "description": "Bữa phụ mẫu trước hoặc sau tập."
          },
          {
            "mealName": "Bữa tối",
            "timeSuggest": "20:00",
            "foods": ["150g cá hồi", "100g khoai lang", "Salad rau xanh"],
            "description": "Bữa tối mẫu hỗ trợ phục hồi."
          }
        ]
      }
    }
  }
}
```

Nếu Fallback được kích hoạt do AI trả sai JSON Schema, chứa `exerciseId` ngoài whitelist hoặc vi phạm giới hạn planned values, toàn bộ cấu trúc response giữ nguyên và `warningCode` bắt buộc là `AI_RESPONSE_INVALID`. Fallback workout template phải được lọc bằng whitelist và hậu kiểm planned values; fallback meal template phải được lọc theo `dietaryPreference`, `foodAllergies`, `excludedFoods` và `mealsPerDay` trước khi lưu. Nếu Fallback thành công, endpoint vẫn trả HTTP 200.

**Response lỗi - AI và fallback đều thất bại (HTTP 502 Bad Gateway):**
```json
{
  "success": false,
  "errorCode": "AI-001",
  "message": "Không thể tạo lộ trình an toàn tại thời điểm hiện tại. Vui lòng thử lại sau.",
  "details": {
    "recommendationPersisted": false,
    "retryable": true
  }
}
```

**Response lỗi - Không có gói ACTIVE (HTTP 403 Forbidden):**
```json
{
  "success": false,
  "errorCode": "SUB-001",
  "message": "Bạn cần có gói tập ACTIVE để sử dụng chức năng tạo gợi ý AI.",
  "details": {
    "requiredSubscriptionStatus": "ACTIVE"
  }
}
```

**Response lỗi - Hồ sơ chưa đủ dữ liệu (HTTP 400 Bad Request):**
```json
{
  "success": false,
  "errorCode": "VAL-001",
  "message": "Hồ sơ thể trạng và dinh dưỡng chưa đầy đủ để tạo gợi ý.",
  "details": {
    "missingFields": ["activityLevel", "mealsPerDay"]
  }
}
```

---

### GET /api/v1/member/recommendations/latest
**Mô tả:** Lấy lộ trình đề xuất mới nhất của hội viên đang đăng nhập.

**Headers:**
- `Authorization: Bearer <token>` (Role: MEMBER)

**Response thành công (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "Lấy lộ trình đề xuất mới nhất thành công",
  "data": {
    "recommendationId": 201,
    "workoutPlanId": 301,
    "workoutPlanStatus": "DRAFT",
    "recommendationSource": "AI_GENERATED",
    "generatedAt": "2026-07-15T14:05:00Z",
    "calculatedTargets": {
      "bmi": 23.67,
      "bmr": 1729.0,
      "tdee": 2679.95,
      "dailyCaloriesKcal": 2979.95,
      "proteinGrams": 159.5,
      "fatGrams": 82.78,
      "carbGrams": 388.61
    },
    "aiSuggestion": {
      "splitModel": "Push/Pull/Legs",
      "explanation": "Với trình độ BEGINNER và 4 buổi tập mỗi tuần, lịch tập Push/Pull/Legs giúp mỗi nhóm cơ được nghỉ ngơi đủ 48 giờ.",
      "workoutSchedule": [
        {
          "dayNumber": 1,
          "dayName": "Buổi 1: Đẩy",
          "exercises": [
            {
              "exerciseId": 12,
              "exerciseName": "Flat Dumbbell Press",
              "plannedSets": 4,
              "plannedReps": 10,
              "plannedRpe": 8,
              "restSeconds": 90,
              "notes": "Kiểm soát tốc độ hạ tạ."
            }
          ]
        },
        {
          "dayNumber": 2,
          "dayName": "Buổi 2: Kéo",
          "exercises": [
            {
              "exerciseId": 31,
              "exerciseName": "Dumbbell One-Arm Row",
              "plannedSets": 4,
              "plannedReps": 12,
              "plannedRpe": 8,
              "restSeconds": 90,
              "notes": "Giữ lưng thẳng trong suốt động tác."
            }
          ]
        },
        {
          "dayNumber": 3,
          "dayName": "Buổi 3: Chân và mông",
          "exercises": [
            {
              "exerciseId": 44,
              "exerciseName": "Dumbbell Bulgarian Split Squat",
              "plannedSets": 3,
              "plannedReps": 10,
              "plannedRpe": 7,
              "restSeconds": 90,
              "notes": "Kiểm soát biên độ phù hợp."
            }
          ]
        },
        {
          "dayNumber": 4,
          "dayName": "Buổi 4: Thân trên tổng hợp",
          "exercises": [
            {
              "exerciseId": 27,
              "exerciseName": "Cable Seated Row",
              "plannedSets": 4,
              "plannedReps": 12,
              "plannedRpe": 8,
              "restSeconds": 90,
              "notes": "Kéo bằng cơ lưng, không giật thân người."
            }
          ]
        }
      ],
      "nutritionPlan": {
        "mealStructure": [
          {
            "mealName": "Bữa sáng",
            "timeSuggest": "07:00",
            "foods": ["3 quả trứng gà luộc", "100g yến mạch", "1 quả chuối"],
            "description": "Bữa sáng cân bằng protein và carbohydrate."
          },
          {
            "mealName": "Bữa trưa",
            "timeSuggest": "12:00",
            "foods": ["200g ức gà", "150g cơm gạo lứt", "Rau xanh"],
            "description": "Bữa trưa giàu protein và chất xơ."
          },
          {
            "mealName": "Bữa phụ",
            "timeSuggest": "16:00",
            "foods": ["1 quả chuối", "1 hộp sữa chua không đường"],
            "description": "Bữa phụ hỗ trợ năng lượng tập luyện."
          },
          {
            "mealName": "Bữa tối",
            "timeSuggest": "20:00",
            "foods": ["150g cá hồi", "100g khoai lang", "Salad rau xanh"],
            "description": "Bữa tối hỗ trợ phục hồi sau tập."
          }
        ]
      }
    }
  }
}
```

---

## 8. Workout Plan API

### GET /api/v1/member/workout-plans/current
**Mô tả:** Lấy giáo án `ACTIVE` hiện hành thuộc sở hữu của Member. Endpoint chỉ đọc dữ liệu nên Member vẫn được xem giáo án hiện hành sau khi Subscription hết hạn.

**Headers:**
- `Authorization: Bearer <token>` (Role: MEMBER)

**Response thành công (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "Lấy giáo án hiện hành thành công",
  "data": {
    "workoutPlanId": 301,
    "planName": "Full Body 1 Day",
    "status": "ACTIVE",
    "recommendationSource": "AI_GENERATED",
    "activatedAt": "2026-07-15T14:15:00Z",
    "workoutSchedule": [
      {
        "dayNumber": 1,
        "dayName": "Buổi 1: Toàn thân",
        "exercises": [
          {
            "exerciseId": 12,
            "exerciseName": "Flat Dumbbell Press",
            "plannedSets": 4,
            "plannedReps": 10,
            "plannedRpe": 8,
            "restSeconds": 90,
            "notes": "Kiểm soát tốc độ hạ tạ và giữ vai ổn định."
          }
        ]
      }
    ]
  }
}
```

**Response lỗi - Không tìm thấy giáo án ACTIVE (HTTP 404 Not Found):**
```json
{
  "success": false,
  "errorCode": "WRK-001",
  "message": "Không tìm thấy giáo án ACTIVE của hội viên.",
  "details": {
    "requiredPlanStatus": "ACTIVE"
  }
}
```

---

### PATCH /api/v1/member/workout-plans/{id}/activate
**Mô tả:** Kích hoạt giáo án `DRAFT` thuộc sở hữu của Member. Trong cùng transaction, hệ thống chuyển giáo án `ACTIVE` cũ sang `ARCHIVED`, sau đó chuyển giáo án đích sang `ACTIVE` theo BR-26. Endpoint yêu cầu Subscription hợp lệ theo BR-25.

**Headers:**
- `Authorization: Bearer <token>` (Role: MEMBER)

**Path Parameters:** `id` - ID giáo án `DRAFT` cần kích hoạt.

**Response thành công (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "Kích hoạt giáo án thành công",
  "data": {
    "workoutPlanId": 301,
    "previousStatus": "DRAFT",
    "status": "ACTIVE",
    "archivedWorkoutPlanId": 245,
    "activatedAt": "2026-07-15T14:15:00Z"
  }
}
```

**Response lỗi - Không tìm thấy hoặc không sở hữu giáo án (HTTP 404 Not Found):**
```json
{
  "success": false,
  "errorCode": "WRK-001",
  "message": "Không tìm thấy giáo án hoặc giáo án không thuộc hội viên hiện hành.",
  "details": {
    "workoutPlanId": 999
  }
}
```

**Response lỗi - Giáo án không ở trạng thái DRAFT (HTTP 400 Bad Request):**
```json
{
  "success": false,
  "errorCode": "VAL-001",
  "message": "Chỉ giáo án DRAFT mới được phép kích hoạt.",
  "details": {
    "workoutPlanId": 301,
    "currentStatus": "ARCHIVED"
  }
}
```

**Response lỗi - Subscription không còn hiệu lực (HTTP 403 Forbidden):**
```json
{
  "success": false,
  "errorCode": "SUB-001",
  "message": "Bạn cần có Subscription ACTIVE còn hiệu lực để kích hoạt giáo án.",
  "details": {
    "requiredCondition": "status = ACTIVE and startDate <= currentDate < endDate"
  }
}
```

---

## 9. Workout Log API

### POST /api/v1/member/workout-logs
**Mô tả:** Hội viên ghi nhận kết quả thực tế sau khi hoàn thành một bài tập. Endpoint yêu cầu Subscription hợp lệ theo BR-25. Backend xác minh chi tiết thuộc giáo án ACTIVE của Member, `exerciseId` khớp chi tiết và `logDate` không ở tương lai theo BR-28. Validate actual values theo BR-09B; ghi trùng ngày cho cùng bài tập thực hiện Update-in-place.

**Headers:**
- `Authorization: Bearer <token>` (Role: MEMBER)
- `Content-Type: application/json`

**Request Body:**
```json
{
  "exerciseId": 12,
  "workoutPlanDetailId": 1055,
  "logDate": "2026-07-15",
  "actualSets": 4,
  "actualReps": 10,
  "weightUsedKg": 22.5,
  "actualRpe": 8
}
```

**Response thành công - Update-in-place (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "Nhật ký cùng ngày đã được cập nhật thành công",
  "data": {
    "logId": 8801,
    "memberId": 101,
    "exerciseId": 12,
    "exerciseName": "Flat Dumbbell Press",
    "workoutPlanDetailId": 1055,
    "logDate": "2026-07-15",
    "actualSets": 5,
    "actualReps": 8,
    "weightUsedKg": 25.0,
    "actualRpe": 9,
    "updatedAt": "2026-07-15T17:00:00Z"
  }
}
```

**Response thành công (HTTP 201 Created):**
```json
{
  "success": true,
  "message": "Ghi nhật ký tập luyện thành công",
  "data": {
    "logId": 8801,
    "memberId": 101,
    "exerciseId": 12,
    "exerciseName": "Flat Dumbbell Press",
    "workoutPlanDetailId": 1055,
    "logDate": "2026-07-15",
    "actualSets": 4,
    "actualReps": 10,
    "weightUsedKg": 22.5,
    "actualRpe": 8,
    "createdAt": "2026-07-15T16:30:00Z"
  }
}
```

**Response lỗi - Subscription không còn hiệu lực (HTTP 403 Forbidden):**
```json
{
  "success": false,
  "errorCode": "SUB-001",
  "message": "Bạn cần có Subscription ACTIVE còn hiệu lực để ghi nhật ký tập luyện mới.",
  "details": {
    "requiredCondition": "status = ACTIVE and startDate <= currentDate < endDate"
  }
}
```

**Response lỗi - Dữ liệu thực tế vượt ngưỡng (HTTP 400 Bad Request):**
```json
{
  "success": false,
  "errorCode": "VAL-001",
  "message": "Dữ liệu nhật ký tập luyện không hợp lệ. Vui lòng kiểm tra lại các trường bị lỗi.",
  "details": {
    "errors": [
      {
        "field": "actualSets",
        "rejectedValue": 12,
        "constraint": "Số set thực tế phải nằm trong khoảng từ 1 đến 10 (BR-09B)."
      },
      {
        "field": "actualRpe",
        "rejectedValue": 11,
        "constraint": "Chỉ số RPE thực tế phải nằm trong khoảng từ 1 đến 10 (BR-09B)."
      }
    ]
  }
}
```

**Response lỗi - Không tìm thấy chi tiết giáo án thuộc Member (HTTP 404 Not Found):**
```json
{
  "success": false,
  "errorCode": "WRK-001",
  "message": "Không tìm thấy chi tiết giáo án ACTIVE thuộc hội viên hiện hành.",
  "details": {
    "workoutPlanDetailId": 9999
  }
}
```

**Response lỗi - Exercise không khớp chi tiết giáo án (HTTP 400 Bad Request):**
```json
{
  "success": false,
  "errorCode": "VAL-001",
  "message": "exerciseId không khớp với bài tập trong chi tiết giáo án.",
  "details": {
    "workoutPlanDetailId": 1055,
    "expectedExerciseId": 12,
    "receivedExerciseId": 31
  }
}
```

---

### GET /api/v1/member/workout-logs
**Mô tả:** Xem lịch sử nhật ký tập luyện với phân trang.

**Headers:**
- `Authorization: Bearer <token>` (Role: MEMBER)

**Query Parameters:**
- `page` (Integer, mặc định 0)
- `size` (Integer, mặc định 10)
- `startDate` (String yyyy-MM-dd, tùy chọn)
- `endDate` (String yyyy-MM-dd, tùy chọn)

**Response thành công (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "Lấy lịch sử nhật ký tập luyện thành công",
  "data": {
    "content": [
      {
        "logId": 8801,
        "exerciseId": 12,
        "exerciseName": "Flat Dumbbell Press",
        "logDate": "2026-07-15",
        "actualSets": 4,
        "actualReps": 10,
        "weightUsedKg": 22.5,
        "actualRpe": 8,
        "createdAt": "2026-07-15T16:30:00Z"
      }
    ],
    "totalElements": 35,
    "totalPages": 4,
    "currentPage": 0,
    "pageSize": 10
  }
}
```

---

### GET /api/v1/member/workout-logs/exercises/{exerciseId}
**Mô tả:** Lấy toàn bộ lịch sử tập của một bài tập cụ thể, sắp xếp theo ngày tập tăng dần để phục vụ vẽ biểu đồ tiến trình mức tạ.

**Headers:**
- `Authorization: Bearer <token>` (Role: MEMBER)

**Path Parameters:** `exerciseId` - ID của bài tập cần xem lịch sử.

**Response thành công (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "Lấy lịch sử tập luyện theo bài tập thành công",
  "data": {
    "exerciseId": 12,
    "exerciseName": "Flat Dumbbell Press",
    "history": [
      {
        "logDate": "2026-07-01",
        "maxWeightUsedKg": 18.0,
        "totalSets": 3,
        "avgReps": 12,
        "avgRpe": 7
      },
      {
        "logDate": "2026-07-08",
        "maxWeightUsedKg": 20.0,
        "totalSets": 4,
        "avgReps": 10,
        "avgRpe": 8
      },
      {
        "logDate": "2026-07-15",
        "maxWeightUsedKg": 22.5,
        "totalSets": 4,
        "avgReps": 10,
        "avgRpe": 8
      }
    ]
  }
}
```

---

## 10. Body Progress API

### POST /api/v1/member/body-progress
**Mô tả:** Hội viên ghi nhận chỉ số cân nặng theo ngày. Áp dụng BR-22: nếu đã tồn tại bản ghi cùng ngày (cùng `recordDate`), hệ thống thực hiện cập nhật ghi đè (Update-in-place) thay vì tạo bản ghi mới.

**Headers:**
- `Authorization: Bearer <token>` (Role: MEMBER)
- `Content-Type: application/json`

**Request Body:**
```json
{
  "weightKg": 72.2,
  "recordDate": "2026-07-15"
}
```

**Response thành công - Tạo mới (HTTP 201 Created):**
```json
{
  "success": true,
  "message": "Ghi nhận chỉ số cân nặng thành công",
  "data": {
    "progressId": 305,
    "memberId": 101,
    "weightKg": 72.2,
    "recordDate": "2026-07-15",
    "isUpdated": false,
    "createdAt": "2026-07-15T07:30:00Z"
  }
}
```

**Response thành công - Cập nhật ghi đè cùng ngày (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "Chỉ số cân nặng trong ngày hôm nay đã được cập nhật thành công.",
  "data": {
    "progressId": 305,
    "memberId": 101,
    "weightKg": 72.0,
    "recordDate": "2026-07-15",
    "isUpdated": true,
    "updatedAt": "2026-07-15T18:00:00Z"
  }
}
```

**Response lỗi - Dữ liệu Body Progress không hợp lệ (HTTP 400 Bad Request):**
```json
{
  "success": false,
  "errorCode": "VAL-001",
  "message": "Dữ liệu tiến trình thể trạng không hợp lệ.",
  "details": {
    "errors": [
      {
        "field": "weightKg",
        "rejectedValue": -2.5,
        "constraint": "weightKg phải lớn hơn 0."
      }
    ]
  }
}
```

---

### GET /api/v1/member/body-progress
**Mô tả:** Lấy lịch sử biến động cân nặng và tần suất số ngày tập theo tuần. Tần suất được tính theo số `logDate` phân biệt trong tuần ISO, không phải số dòng exercise log.

**Headers:**
- `Authorization: Bearer <token>` (Role: MEMBER)

**Query Parameters:**
- `startDate` (String yyyy-MM-dd, tùy chọn)
- `endDate` (String yyyy-MM-dd, tùy chọn)

**Response thành công (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "Lấy lịch sử tiến trình thể trạng thành công",
  "data": {
    "memberId": 101,
    "timeseries": [
      {
        "recordDate": "2026-06-01",
        "weightKg": 75.5
      },
      {
        "recordDate": "2026-06-15",
        "weightKg": 74.2
      },
      {
        "recordDate": "2026-07-01",
        "weightKg": 73.0
      },
      {
        "recordDate": "2026-07-15",
        "weightKg": 72.0
      }
    ],
    "workoutFrequencyByWeek": [
      {
        "weekStartDate": "2026-06-29",
        "workoutDaysLogged": 3
      },
      {
        "weekStartDate": "2026-07-06",
        "workoutDaysLogged": 4
      },
      {
        "weekStartDate": "2026-07-13",
        "workoutDaysLogged": 2
      }
    ]
  }
}
```

---

## 11. Admin API

### GET /api/v1/admin/users
**Mô tả:** Xem danh sách tất cả người dùng với phân trang và lọc theo vai trò hoặc trạng thái tài khoản.

**Headers:**
- `Authorization: Bearer <token>` (Role: ADMIN)

**Query Parameters:**
- `page` (Integer, mặc định 0)
- `size` (Integer, mặc định 20)
- `role` (Enum, tùy chọn): ROLE_MEMBER, ROLE_PT, ROLE_ADMIN.
- `status` (Enum, tùy chọn): ACTIVE, LOCKED, DISABLED.
- `search` (String, tùy chọn): Từ khóa tìm theo tên hoặc email.

**Response thành công (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "Lấy danh sách người dùng thành công",
  "data": {
    "content": [
      {
        "id": 101,
        "fullName": "Nguyễn Văn An",
        "email": "user@gmail.com",
        "role": "ROLE_MEMBER",
        "accountStatus": "ACTIVE",
        "createdAt": "2026-07-15T08:00:00Z",
        "hasActiveSubscription": true
      },
      {
        "id": 102,
        "fullName": "Trần Thị Bình",
        "email": "binh@gmail.com",
        "role": "ROLE_MEMBER",
        "accountStatus": "LOCKED",
        "createdAt": "2026-07-10T09:00:00Z",
        "hasActiveSubscription": false
      }
    ],
    "totalElements": 52,
    "totalPages": 3,
    "currentPage": 0,
    "pageSize": 20
  }
}
```

---

### PATCH /api/v1/admin/users/{id}/lock
**Mô tả:** Admin khóa tài khoản người dùng. Gói tập subscription hiện tại không bị thay đổi. Do JWT stateless, token đã phát hành không bị thu hồi trực tiếp tại `JwtSecurityFilter`; Filter chỉ kiểm tra chữ ký và hạn dùng. Sau transaction lock, hệ thống cập nhật/evict cache trạng thái tài khoản. Trên request xác thực tiếp theo, `AccountStatusGuard` hoặc Method Security truy vấn DB/Cache theo User ID và trả HTTP 403 với `ACC-004` nếu trạng thái là `LOCKED`.

**Headers:**
- `Authorization: Bearer <token>` (Role: ADMIN)
- `Content-Type: application/json`

**Path Parameters:** `id` - ID của người dùng cần khóa.

**Request Body:**
```json
{
  "reason": "Vi phạm nội quy phòng tập: gây mất trật tự nghiêm trọng ngày 14/07/2026."
}
```

**Response thành công (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "Tài khoản đã được khóa thành công. Người dùng sẽ không thể đăng nhập cho đến khi được mở khóa.",
  "data": {
    "userId": 102,
    "fullName": "Trần Thị Bình",
    "accountStatus": "LOCKED",
    "lockedBy": "admin@smartgym.com",
    "lockedAt": "2026-07-15T15:00:00Z",
    "reason": "Vi phạm nội quy phòng tập: gây mất trật tự nghiêm trọng ngày 14/07/2026.",
    "subscriptionStatus": "ACTIVE (không thay đổi)"
  }
}
```

**Response lỗi - Không thể khóa do dữ liệu không hợp lệ (HTTP 400 Bad Request):**
```json
{
  "success": false,
  "errorCode": "VAL-001",
  "message": "Lý do khóa tài khoản là bắt buộc.",
  "details": {
    "field": "reason",
    "constraint": "reason phải có từ 10 đến 500 ký tự."
  }
}
```

---

### PATCH /api/v1/admin/users/{id}/unlock
**Mô tả:** Admin mở khóa tài khoản người dùng. Tài khoản được trả về trạng thái `ACTIVE`; sau transaction hệ thống cập nhật/evict cache trạng thái để người dùng có thể đăng nhập và truy cập lại ngay.

**Headers:**
- `Authorization: Bearer <token>` (Role: ADMIN)

**Path Parameters:** `id` - ID của người dùng cần mở khóa.

**Response thành công (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "Tài khoản đã được mở khóa thành công. Người dùng có thể đăng nhập bình thường.",
  "data": {
    "userId": 102,
    "fullName": "Trần Thị Bình",
    "accountStatus": "ACTIVE",
    "unlockedBy": "admin@smartgym.com",
    "unlockedAt": "2026-07-15T16:00:00Z"
  }
}
```

**Response lỗi - Tài khoản không ở trạng thái LOCKED (HTTP 400 Bad Request):**
```json
{
  "success": false,
  "errorCode": "VAL-001",
  "message": "Chỉ tài khoản đang ở trạng thái LOCKED mới có thể được mở khóa.",
  "details": {
    "userId": 102,
    "currentAccountStatus": "ACTIVE"
  }
}
```

---

### GET /api/v1/admin/statistics/summary
**Mô tả:** Lấy số liệu đếm cơ bản tổng quan hệ thống phục vụ Dashboard Admin. Không tải tập thực thể lớn lên bộ nhớ, chỉ thực hiện các câu lệnh COUNT và SUM trực tiếp trong DB.

**Headers:**
- `Authorization: Bearer <token>` (Role: ADMIN)

**Response thành công (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "Lấy số liệu thống kê tổng quan thành công",
  "data": {
    "totalMembers": 52,
    "activeSubscriptions": 38,
    "totalExercises": 48,
    "totalConfirmedSubscriptionValue": 58400000,
    "generatedAt": "2026-07-15T16:00:00Z"
  }
}
```

---

## 12. Ma trận truy vết API (API Traceability Matrix)

Ma trận này là điểm kiểm soát bắt buộc giữa API Contract với Functional Requirements (File 08), Use Case (File 09), Business Rules (File 05) và Non-functional Requirements (File 04). Dấu `—` nghĩa là endpoint hỗ trợ vận hành trực tiếp cho FR nhưng không thuộc một trong 10 Use Case cốt lõi được đặc tả chi tiết. `FR-AUTH-04` áp dụng chéo cho mọi endpoint có header `Authorization`; endpoint quản trị yêu cầu `ROLE_ADMIN`, endpoint hội viên yêu cầu `ROLE_MEMBER`.

Các ràng buộc NFR áp dụng chéo: NFR-01 cho API nội bộ; NFR-03 cho phép tính sinh học; NFR-06 cho JWT; NFR-07 cho tài nguyên sở hữu cá nhân; NFR-08 cho Prompt AI; NFR-09 và NFR-10 cho môi trường chạy/seed data; NFR-11 cho kiến trúc phân lớp; NFR-12 cho Swagger/OpenAPI; NFR-14 cho logging an toàn. NFR-02, NFR-04, NFR-05 và NFR-13 được ghi trực tiếp tại các endpoint chịu ảnh hưởng đặc thù trong bảng dưới đây.

| Endpoint | FR nguồn | Use Case | BR/NFR chi phối |
| :--- | :--- | :---: | :--- |
| `POST /api/v1/auth/register` | FR-AUTH-01 | UC-01 | BR-01, BR-02, BR-15, BR-18, BR-20 |
| `POST /api/v1/auth/login` | FR-AUTH-02, FR-AUTH-03 | UC-02 | BR-16, BR-18, BR-20, BR-21 |
| `GET /api/v1/users/me` | FR-AUTH-05 | — | BR-16, BR-21 |
| `GET /api/v1/member/profile` | FR-PROFILE-01 | UC-03 | BR-13 |
| `PUT /api/v1/member/profile` | FR-PROFILE-02, FR-PROFILE-03, FR-PROFILE-04, FR-PROGRESS-01, FR-PROGRESS-02 | UC-03 | BR-13, BR-22, BR-23 |
| `GET /api/v1/packages` | FR-SUB-04 | UC-04 | — |
| `POST /api/v1/admin/packages` | FR-SUB-01 | — | BR-03, BR-27 |
| `PUT /api/v1/admin/packages/{id}` | FR-SUB-02 | — | BR-03, BR-27 |
| `DELETE /api/v1/admin/packages/{id}` | FR-SUB-03 | — | BR-03, BR-05 |
| `POST /api/v1/member/subscriptions` | FR-SUB-05 | UC-04 | BR-04, BR-05, BR-13, BR-25 |
| `GET /api/v1/member/subscriptions/current` | FR-SUB-07 | UC-04 | BR-13, BR-25 |
| `POST /api/v1/member/subscriptions/{activeSubscriptionId}/renewal-requests` | FR-SUB-08 | UC-04 | BR-05, BR-13, BR-24, BR-25 |
| `POST /api/v1/admin/subscriptions/{id}/approve` | FR-SUB-06, FR-SUB-08 | UC-05 | BR-03, BR-04, BR-05, BR-24, BR-25, NFR-05 |
| `POST /api/v1/admin/subscriptions/{id}/cancel` | FR-SUB-09 | — | BR-03, BR-25 |
| `GET /api/v1/exercises` | FR-EXR-04, FR-EXR-05 | — | BR-14 |
| `GET /api/v1/exercises/{id}` | FR-EXR-04 | — | BR-14 |
| `POST /api/v1/admin/exercises` | FR-EXR-01 | UC-06 | BR-03 |
| `PUT /api/v1/admin/exercises/{id}` | FR-EXR-02 | UC-06 | BR-03 |
| `DELETE /api/v1/admin/exercises/{id}` | FR-EXR-03 | UC-06 | BR-03, BR-14 |
| `POST /api/v1/member/recommendations` | FR-SUB-07, FR-EXR-06, FR-WORKOUT-01, FR-WORKOUT-02, FR-WORKOUT-03, FR-NUTRITION-01, FR-NUTRITION-02, FR-NUTRITION-03, FR-NUTRITION-04, FR-NUTRITION-05, FR-NUTRITION-06 | UC-07 | BR-06, BR-07, BR-08, BR-09A, BR-09C, BR-10, BR-11, BR-12, BR-13, BR-23, BR-25, BR-26, NFR-02, NFR-04, NFR-13 |
| `GET /api/v1/member/recommendations/latest` | FR-WORKOUT-01, FR-WORKOUT-03, FR-NUTRITION-06 | UC-07 | BR-13 |
| `GET /api/v1/member/workout-plans/current` | FR-WORKOUT-04 | UC-07 | BR-13, BR-26 |
| `PATCH /api/v1/member/workout-plans/{id}/activate` | FR-WORKOUT-05 | UC-07 | BR-13, BR-25, BR-26, NFR-05 |
| `POST /api/v1/member/workout-logs` | FR-SUB-07, FR-WORKOUT-06 | UC-08 | BR-09B, BR-13, BR-19, BR-25, BR-28 |
| `GET /api/v1/member/workout-logs` | FR-WORKOUT-07 | UC-09 | BR-13 |
| `GET /api/v1/member/workout-logs/exercises/{exerciseId}` | FR-PROGRESS-04 | UC-09 | BR-13 |
| `POST /api/v1/member/body-progress` | FR-PROGRESS-01, FR-PROGRESS-02 | UC-09 | BR-13, BR-22 |
| `GET /api/v1/member/body-progress` | FR-PROGRESS-03 | UC-09 | BR-13 |
| `GET /api/v1/admin/users` | FR-ADMIN-01 | — | BR-03 |
| `PATCH /api/v1/admin/users/{id}/lock` | FR-AUTH-03, FR-ADMIN-02 | UC-10 | BR-03, BR-16, BR-21 |
| `PATCH /api/v1/admin/users/{id}/unlock` | FR-AUTH-03, FR-ADMIN-02 | UC-10 | BR-03, BR-16, BR-21 |
| `GET /api/v1/admin/statistics/summary` | FR-ADMIN-03 | — | BR-03 |
