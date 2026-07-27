# ĐẶC TẢ VAI TRÒ VÀ PHÂN QUYỀN HỆ THỐNG (ACTORS & ROLES SPECIFICATION)

## 1. Giới thiệu cơ chế phân quyền
Hệ thống áp dụng mô hình **Kiểm soát quyền truy cập dựa trên vai trò (RBAC - Role-Based Access Control)**. Mọi yêu cầu truy cập tài nguyên hệ thống từ phía người dùng đều phải thông qua bộ lọc xác thực (Authentication Filter) dựa trên JSON Web Token (JWT) và kiểm tra quyền hạn (Authorization) tại tầng Back-end nhằm hỗ trợ bảo mật dữ liệu.

---

## 2. Chi tiết các vai trò (Actors & Roles)

### 2.1. Ban quản trị (Administrator)
Là đối tượng có đặc quyền cao nhất trong hệ thống, đảm nhiệm vai trò vận hành hành chính và cấu hình hệ thống:
- **Quản lý tài khoản:** Tạo mới, khóa hoặc mở khóa tài khoản người dùng (PT, Member) dựa trên tình trạng vi phạm nội quy phòng tập (lưu ý không khóa tài khoản người dùng chỉ vì hội viên hết hạn gói tập).
- **Quản lý cấu hình dịch vụ:** Khởi tạo, chỉnh sửa cấu hình danh mục các gói tập của phòng gym.
- **Thiết lập thư viện bài tập gốc:** Xây dựng danh mục các bài tập (tên bài, nhóm cơ tác động chính, hướng dẫn thực hiện) làm cơ sở dữ liệu cho AI Engine và PT tham chiếu.
- **Giám sát vận hành:** Trong MVP, xem số liệu đếm/tổng hợp cơ bản (Member, Subscription ACTIVE, Exercise và tổng giá trị subscription đã xác nhận mô phỏng). Biểu đồ theo thời gian và phân bổ gói thuộc Should-have.

### 2.2. Huấn luyện viên cá nhân (Personal Trainer - PT)
> **Trạng thái triển khai: Should-have.** Vai trò PT được thiết kế sẵn cấu trúc trong bảo mật RBAC và Database từ ban đầu, nhưng không phải điều kiện bắt buộc để luồng MVP hoạt động. Các API và giao diện nghiệp vụ của PT sẽ được hiện thực hóa cuốn chiếu sau khi luồng Admin và Member đã ổn định.

Là đối tượng cung cấp dịch vụ chuyên môn hỗ trợ trực tiếp cho hội viên:
- **Giám sát hội viên:** Xem danh sách và chi tiết hồ sơ thể chất (chỉ số sinh học, bệnh lý nền, hạn chế vận động) của các hội viên được phân công phụ trách.
- **Tiếp nhận và điều chỉnh giáo án (Tùy chọn):** Xem các giáo án tập luyện và gợi ý dinh dưỡng do AI Engine tạo ra và có thể tiến hành điều chỉnh theo chuyên môn; bước này là **tùy chọn**, không bắt buộc để hội viên nhận giáo án.
- **Đánh giá hiệu suất:** Theo dõi nhật ký tập luyện thực tế của hội viên để điều chỉnh mức độ vận động phù hợp.

### 2.3. Hội viên (Member)
Là khách hàng trực tiếp trải nghiệm các dịch vụ tự động hóa và tiện ích thông minh của hệ thống:
- **Quản lý thể trạng cá nhân:** Đăng ký tài khoản, cập nhật thường xuyên hồ sơ thể chất để hệ thống tự động tính toán các chỉ số cơ bản (BMI, BMR, TDEE).
- **Mua dịch vụ:** Xem danh mục và thực hiện đăng ký/gia hạn gói tập trực tuyến.
- **Tập luyện thông minh:** Gửi yêu cầu cá nhân hóa lộ trình đến AI Engine; tiếp nhận lịch tập và thực đơn dinh dưỡng khuyến nghị.
- **Ghi nhận tiến trình:** Điền nhật ký buổi tập (mức tạ, số set, số rep, chỉ số cảm nhận lực RPE) và theo dõi biểu đồ tiến độ thay đổi thể trạng theo thời gian.

### 2.4. Khách vãng lai (Anonymous Guest)
Là tác nhân chưa xác thực danh tính trên hệ thống. **Lưu ý:** Guest không được lưu trữ như một Role trong cơ sở dữ liệu; đây là trạng thái chưa xác thực (unauthenticated) của bất kỳ người dùng nào trước khi đăng nhập:
- Chỉ được phép xem các thông tin giới thiệu chung và danh sách gói tập công khai.
- Thực hiện đăng ký tài khoản mới (mặc định được gán quyền `ROLE_MEMBER`) để trở thành Hội viên.

---

## 3. Ma trận phân quyền (Permission Matrix)

Ký hiệu:
- ✔️: Cho phép truy cập / Thực hiện.
- ❌: Bị từ chối truy cập / Không có quyền.
- 🟡: Cho phép có điều kiện giới hạn phạm vi (chỉ áp dụng đối với dữ liệu liên quan trực tiếp đến tài khoản đó).

| Nhóm chức năng | Quyền hạn chi tiết | Administrator | Personal Trainer | Member | Guest |
| :--- | :--- | :---: | :---: | :---: | :---: |
| **Hệ thống & Tài khoản** | Đăng ký tài khoản mới | ❌ | ❌ | ❌ | ✔️ |
| | Đăng nhập hệ thống (JWT) | ✔️ | ✔️ | ✔️ | ❌ |
| | Cấu hình phân quyền hệ thống | ✔️ | ❌ | ❌ | ❌ |
| | Khóa / Mở khóa tài khoản | ✔️ | ❌ | ❌ | ❌ |
| **Hồ sơ & Thể trạng** | Xem danh sách tài khoản cơ bản, không gồm hồ sơ sức khỏe chi tiết | ✔️ | ❌ | ❌ | ❌ |
| | Cập nhật hồ sơ thể chất cá nhân | ❌ | ❌ | ✔️ | ❌ |
| | Xem hồ sơ thể chất hội viên được phân công (Should-have) | ❌ | 🟡 | ❌ | ❌ |
| **Quản lý Gói tập** | Thêm / Sửa / Xóa cấu hình gói tập | ✔️ | ❌ | ❌ | ❌ |
| | Xem danh sách gói tập công khai | ✔️ | ✔️ | ✔️ | ✔️ |
| | Mua / Gia hạn gói tập trực tuyến | ❌ | ❌ | ✔️ | ❌ |
| **Bài tập & Giáo án** | Cấu hình thư viện bài tập gốc | ✔️ | ❌ | ❌ | ❌ |
| | Xem thư viện bài tập | ✔️ | ✔️ | ✔️ | ❌ |
| | Kích hoạt giáo án DRAFT làm giáo án hiện hành | ❌ | ❌ | ✔️ | ❌ |
| | Góp ý/Chỉnh sửa chuyên môn giáo án (Should-have) | ❌ | 🟡 | ❌ | ❌ |
| | Ghi nhật ký buổi tập (Logs) | ❌ | ❌ | ✔️ | ❌ |
| | Xem nhật ký buổi tập của hội viên | ❌ | 🟡 | ❌ | ❌ |
| **Tích hợp AI Engine** | Yêu cầu AI gợi ý lịch tập & dinh dưỡng | ❌ | ❌ | ✔️ | ❌ |
| | Xem/Duyệt đề xuất dinh dưỡng & lịch tập | ❌ | 🟡 | ✔️ | ❌ |
| **Thống kê & Báo cáo** | Xem số liệu đếm cơ bản (Tổng user, gói tập, bài tập) | ✔️ | ❌ | ❌ | ❌ |
| | Xem Dashboard biểu đồ nâng cao (Should-have) | ✔️ | ❌ | ❌ | ❌ |
| | Xem biểu đồ tiến trình cá nhân | ❌ | 🟡 | ✔️ | ❌ |

---

## 4. Thiết kế Cơ chế kiểm tra quyền và Chiến lược triển khai MVP

### 4.1. Phân định trách nhiệm cơ chế bảo mật
Hệ thống phân tách rõ ràng trách nhiệm giữa hai tầng kiểm soát:

- **JWT Filter (Xác thực danh tính):** `JwtAuthenticationFilter` xác minh chữ ký và hạn dùng JWT, sau đó nạp User và Role qua `UserDetailsService` để thiết lập Security Context. Filter này không thực hiện các kiểm tra trạng thái tài khoản hay các nghiệp vụ khác.
- **Account Status Guard (Kiểm tra trạng thái tài khoản):** Khi đăng nhập, `UserDetailsService` kiểm tra `AccountStatus` (`ACTIVE`, `LOCKED`, `DISABLED`). Trên các endpoint yêu cầu xác thực, `AccountStatusGuard` (Custom Interceptor/Guard) hoặc Method Security kiểm tra trạng thái tài khoản và từ chối `LOCKED`/`DISABLED`. `JwtAuthenticationFilter` không truy vấn hoặc đánh giá `accountStatus` để chặn request.
- **Method Security (Kiểm tra nghiệp vụ):** Việc kiểm tra gói tập ACTIVE tại các API chức năng cao cấp được xử lý thông qua Method Security Annotation thay vì kiểm tra ở JWT Filter:

```java
@PreAuthorize("@subscriptionGuard.hasActiveSubscription(authentication)")
```

- **Subscription Guard và Scheduled Job:** Tại mỗi request tạo recommendation AI, kích hoạt giáo án hoặc ghi workout log mới, `SubscriptionGuard` kiểm tra động điều kiện `status = ACTIVE`, `startDate <= currentDate < endDate`; `endDate` là biên exclusive. Scheduled Job chạy hằng ngày chỉ hỗ trợ đồng bộ các bản ghi quá hạn sang `EXPIRED`, không thay thế kiểm tra động và không khóa tài khoản người dùng. Member hết hạn vẫn đăng nhập và xem dữ liệu lịch sử bình thường.

### 4.2. Trạng thái Luồng Đề xuất AI trong MVP và khi có PT

```text
Luồng MVP (không có PT):
AI_GENERATED/FALLBACK_TEMPLATE → DRAFT_AVAILABLE_TO_MEMBER
→ MEMBER_ACTIVATES → ACTIVE

Khi có PT Module (Should-have):
AI_GENERATED/FALLBACK_TEMPLATE → DRAFT_AVAILABLE_TO_MEMBER
→ [PT_OPTIONAL_REVIEW/ADJUSTMENT] → MEMBER_ACTIVATES → ACTIVE
```

Bước PT review là **tùy chọn**, không phải điều kiện bắt buộc để Hội viên nhận giáo án.

### 4.3. Chiến lược triển khai cuốn chiếu (Phased Rollout)
1. **Ưu tiên Luồng cốt lõi (Administrator & Member):**
   - Tập trung hoàn thiện toàn bộ tính năng và cơ sở dữ liệu phục vụ cho luồng hoạt động giữa Quản trị viên và Hội viên trước.
   - Hội viên có thể gửi yêu cầu trực tiếp tới **AI Engine / LLM** để nhận lịch tập và thực đơn đề xuất, tự ghi nhật ký tập luyện mà không cần bước phê duyệt trung gian của PT.
   - Điều này đảm bảo hệ thống có một chu trình vận hành khép kín (End-to-End) tối thiểu nhưng hoàn toàn khả thi ngay lập tức.
2. **Tích hợp mở rộng (Personal Trainer) — Should-have:**
   - Phân hệ của PT sẽ được tích hợp cuốn chiếu ở giai đoạn tiếp theo.
   - Do cấu trúc cơ sở dữ liệu và cơ chế bảo mật (RBAC) đã được thiết kế sẵn sàng hỗ trợ đa vai trò từ ban đầu, việc bổ sung các API và giao diện cho PT sẽ không làm ảnh hưởng hay thay đổi kiến trúc hiện có của luồng Admin và Member.
