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
- **Giám sát vận hành:** Khai thác các dữ liệu báo cáo thống kê tổng quan (tổng giá trị subscription đã xác nhận mô phỏng, lượng hội viên kích hoạt, mức độ phân bổ gói tập).

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
| **Hồ sơ & Thể trạng** | Xem thông tin hồ sơ của mọi người dùng | ✔️ | ❌ | ❌ | ❌ |
| | Cập nhật hồ sơ thể chất cá nhân | ❌ | 🟡 | ✔️ | ❌ |
| | Xem hồ sơ thể chất hội viên | ✔️ | 🟡 | ❌ | ❌ |
| **Quản lý Gói tập** | Thêm / Sửa / Xóa cấu hình gói tập | ✔️ | ❌ | ❌ | ❌ |
| | Xem danh sách gói tập công khai | ✔️ | ✔️ | ✔️ | ✔️ |
| | Mua / Gia hạn gói tập trực tuyến | ❌ | ❌ | ✔️ | ❌ |
| **Bài tập & Giáo án** | Cấu hình thư viện bài tập gốc | ✔️ | ❌ | ❌ | ❌ |
| | Xem thư viện bài tập | ✔️ | ✔️ | ✔️ | ❌ |
| | Lập & Chỉnh sửa giáo án tập luyện | ❌ | 🟡 | 🟡 | ❌ |
| | Ghi nhật ký buổi tập (Logs) | ❌ | ❌ | ✔️ | ❌ |
| | Xem nhật ký buổi tập của hội viên | ❌ | 🟡 | ❌ | ❌ |
| **Tích hợp AI Engine** | Yêu cầu AI gợi ý lịch tập & dinh dưỡng | ❌ | ❌ | ✔️ | ❌ |
| | Xem/Duyệt đề xuất dinh dưỡng & lịch tập | ❌ | 🟡 | ✔️ | ❌ |
| **Thống kê & Báo cáo** | Xem số liệu đếm cơ bản (Tổng user, gói tập, bài tập) | ✔️ | ❌ | ❌ | ❌ |
| | Xem Dashboard biểu đồ tổng giá trị subscription đã xác nhận mô phỏng & vận hành | ✔️ | ❌ | ❌ | ❌ |
| | Xem biểu đồ tiến trình cá nhân | ❌ | 🟡 | ✔️ | ❌ |

---

## 4. Thiết kế Cơ chế kiểm tra quyền và Chiến lược triển khai MVP

### 4.1. Phân định trách nhiệm cơ chế bảo mật
Hệ thống phân tách rõ ràng trách nhiệm giữa hai tầng kiểm soát:

- **JWT Filter (Xác thực danh tính):** Chịu trách nhiệm duy nhất là xác minh chữ ký JWT Token và trích xuất thông tin User và Role đưa vào Security Context. Filter này không thực hiện các kiểm tra trạng thái tài khoản hay các nghiệp vụ khác.
- **Security/UserDetails Layer (Kiểm tra trạng thái tài khoản):** Spring Security thông qua tầng UserDetails / UserDetailsService sẽ kiểm tra thuộc tính `AccountStatus` (`ACTIVE`, `LOCKED`, `DISABLED`) khi người dùng đăng nhập hoặc gửi request cần xác thực. Tài khoản `LOCKED` và `DISABLED` không được đăng nhập. Token của tài khoản đã bị `DISABLED` cũng sẽ bị từ chối tại đây.
- **Method Security (Kiểm tra nghiệp vụ):** Việc kiểm tra gói tập ACTIVE tại các API chức năng cao cấp được xử lý thông qua Method Security Annotation thay vì kiểm tra ở JWT Filter:

```java
@PreAuthorize("@subscriptionGuard.hasActiveSubscription(authentication)")
```

- **Scheduled Job (Cập nhật trạng thái):** Một tiến trình chạy ngầm hằng ngày quét và cập nhật trạng thái EXPIRED cho các Subscription có `endDate` đã qua, thay vì thực hiện kiểm tra này trên từng HTTP Request. Tiến trình này chỉ thay đổi trạng thái của Subscription, hoàn toàn không khóa tài khoản người dùng (`accountStatus` của người dùng vẫn giữ nguyên là `ACTIVE` và họ vẫn có thể đăng nhập bình thường để gia hạn gói tập).

### 4.2. Trạng thái Luồng Đề xuất AI trong MVP và khi có PT

```text
Luồng MVP (không có PT):
AI_GENERATED → AVAILABLE_TO_MEMBER (trực tiếp)

Khi có PT Module (Should-have):
AI_GENERATED → [PT_OPTIONAL_REVIEW] → APPROVED/ADJUSTED → AVAILABLE_TO_MEMBER
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
