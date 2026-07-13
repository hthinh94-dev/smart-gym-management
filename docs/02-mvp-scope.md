# ĐẶC TẢ PHẠM VI SẢN PHẨM KHẢ DỤNG TỐI THIỂU (MVP SCOPE SPECIFICATION)

## 1. Chiến lược quản lý phạm vi và thời gian
Dự án được giới hạn thực hiện trong thời gian **9 tuần (tương đương khoảng 270 giờ làm việc thực tế)**. Để đảm bảo tính ổn định của hệ thống, kiểm soát rủi ro "phình to phạm vi" (Scope Creep) và tránh tình trạng vỡ tiến độ, phạm vi chức năng được cấu trúc nghiêm ngặt theo mô hình **MoSCoW (Must-have, Should-have, Won't-have)**. 

Mọi nguồn lực cốt lõi sẽ tập trung hoàn thiện tuyệt đối nhóm **Must-have** trước khi dịch chuyển tài nguyên sang các tính năng bổ trợ.

---

## 2. Phân loại yêu cầu chức năng theo mô hình MoSCoW

### 2.1. Nhóm MUST-HAVE (Yêu cầu bắt buộc hoàn thành)
Đây là các tính năng bắt buộc để hệ thống có thể vận hành khép kín (End-to-End) ở mức tối thiểu.

- **Xác thực & Phân quyền (Authentication & Authorization):**
  - *Mô tả:* Đăng ký, đăng nhập tài khoản, tích hợp Spring Security và cấu hình mã hóa JWT Access Token; phân quyền truy cập chặt chẽ giữa 3 vai trò (Administrator, Personal Trainer, Member). Vai trò PT chỉ được khai báo sẵn cấu trúc trong RBAC/database và không phải điều kiện bắt buộc để luồng MVP hoạt động. Toàn bộ API và giao diện nghiệp vụ PT là Should-have. Luồng MVP bắt buộc chỉ gồm Admin và Member. Member nhận đề xuất AI trực tiếp, không phải chờ PT phê duyệt.
  - *Liên kết kỹ thuật:* Cung cấp ngữ cảnh bảo mật và thông tin tài khoản được định danh cho mọi yêu cầu HTTP tiếp theo (JPA Auditing, kiểm tra sở hữu tài nguyên).
- **Hồ sơ thể trạng hội viên (Member Profile):**
  - *Mô tả:* Quản lý thông tin thể chất chi tiết gồm: Họ tên, ngày sinh, giới tính, chiều cao (`heightCm`), cân nặng (`weightKg`), mục tiêu thể hình (Tăng cơ `BULK`, Giảm mỡ `CUT`, Duy trì `MAINTAIN`), trình độ tập luyện (Mới bắt đầu `BEGINNER`, Trung cấp `INTERMEDIATE`, Nâng cao `ADVANCED`), mức độ hoạt động hàng ngày (`activityLevel`: SEDENTARY, LIGHTLY_ACTIVE, MODERATELY_ACTIVE, VERY_ACTIVE), tần suất tập luyện dự kiến (số buổi/tuần), thời lượng tối đa mỗi buổi, thiết bị tập luyện sẵn có, nhóm cơ ưu tiên, và các hạn chế vận động/chấn thương. **Dữ liệu bổ sung phục vụ gợi ý dinh dưỡng:** chế độ ăn (`dietaryPreference`, ví dụ: OMNIVORE, VEGETARIAN, VEGAN), dị ứng thực phẩm (`foodAllergies`), thực phẩm loại trừ (`excludedFoods`), số bữa/ngày (`mealsPerDay`).
  - *Liên kết kỹ thuật:* Dữ liệu từ hồ sơ thể trạng là đầu vào (Input Payload) bắt buộc cho thuật toán tính toán chỉ số sinh học ở Back-end và là ngữ cảnh (Context) gửi sang AI API để cá nhân hóa giáo án. Trường `activityLevel` cung cấp hệ số tương ứng (Sedentary ×1.2, Lightly Active ×1.375, Moderately Active ×1.55, Very Active ×1.725) đưa vào công thức tính TDEE tại Backend.
- **Quản lý gói dịch vụ phòng tập (Membership Management):**
  - *Mô tả:* Quản trị viên (Admin) thực hiện CRUD danh mục các gói tập. Hội viên gửi yêu cầu đăng ký gói tập, hệ thống tạo Subscription ở trạng thái **PENDING**; Admin xác nhận mô phỏng tại Dashboard để chuyển sang **ACTIVE**. Khi gia hạn một gói đang ACTIVE: `newEndDate = currentEndDate + packageDuration`. Vòng đời trạng thái: `PENDING → ACTIVE → EXPIRED` và `PENDING/ACTIVE → CANCELLED`. **Lưu ý:** Hết hạn subscription chỉ chặn các chức năng yêu cầu gói ACTIVE; user vẫn đăng nhập và gia hạn bình thường. Admin không được khóa tài khoản người dùng chỉ vì hội viên hết hạn gói tập.
  - *Liên kết kỹ thuật:* Trạng thái ACTIVE được kiểm tra qua Method Security Annotation tại các API chức năng cao cấp. Không tích hợp cổng thanh toán thực; chỉ mô phỏng luồng xác nhận nội bộ (Mock Approval Flow).
- **Thư viện bài tập nền tảng (Exercise Library):**
  - *Mô tả:* Admin quản trị danh mục bài tập gồm tên bài, nhóm cơ chính/phụ, kiểu chuyển động (`movementPattern`), vùng cơ thể tác động (`targetBodyRegions`), thiết bị bắt buộc (`equipmentRequired`), độ khó (`difficultyLevel`), các tag chống chỉ định (`contraindicationTags`), hướng dẫn thực hiện bằng văn bản và trạng thái hoạt động (`isActive`). Thiết lập API hỗ trợ phân trang (Pagination), tìm kiếm và lọc nâng cao trên giao diện React.
  - *Liên kết kỹ thuật:* Đóng vai trò là tập dữ liệu tham chiếu (Master Data) để AI Engine đối chiếu và liên kết các bài tập cụ thể vào giáo án huấn luyện của hội viên.
- **Lập giáo án & Ghi nhận nhật ký tập luyện (Workout Tracking):**
  - *Mô tả:* Thiết lập lịch tập thủ công, kết hợp gọi AI Engine đề xuất lịch tập tự động dựa trên hồ sơ thể trạng; hiển thị giáo án theo ngày và hỗ trợ hội viên ghi nhận kết quả tập luyện thực tế (`actualSets`, `actualReps`, `weightUsedKg`, `actualRpe`).
  - *Liên kết kỹ thuật:* Nhật ký tập luyện thực tế là nguồn dữ liệu lịch sử đầu vào cho Progress Analytics để vẽ biểu đồ và điều chỉnh độ khó của giáo án tiếp theo từ AI.
- **Tính toán chỉ số & Dinh dưỡng cốt lõi (Nutrition Core & AI Prompting):**
  - *Mô tả:* Lập trình bộ tính toán sinh học ở Backend (BMI, BMR, TDEE, Calorie mục tiêu và tỷ lệ dinh dưỡng đa lượng Macronutrients: Protein, Carb, Fat); tích hợp AI Engine gửi prompt đã làm sạch để nhận gợi ý bữa ăn chi tiết dạng cấu trúc JSON.
  - *Liên kết kỹ thuật:* Kết quả tính toán chỉ số sinh học định lượng từ Backend sẽ được gộp chung với dữ liệu hồ sơ để định hình cấu trúc Prompt gửi sang AI API, tránh phụ thuộc hoàn toàn vào quá trình tính toán của mô hình ngôn ngữ lớn bên ngoài.
- **Phân tích tiến độ tập luyện (Progress Analytics):**
  - *Mô tả:* Hệ thống ghi nhận log lịch sử biến đổi cân nặng và mức tạ sử dụng theo thời gian, cung cấp các REST API tổng hợp dữ liệu dạng Timeseries.
  - *Liên kết kỹ thuật:* Cung cấp nguồn dữ liệu đã cấu trúc hóa dạng chuỗi thời gian để Frontend sử dụng các thư viện biểu đồ vẽ tiến trình trực quan. **Estimated 1RM được chuyển sang Should-have; Must-have chỉ vẽ biểu đồ mức tạ và số reps qua thời gian.**
- **Làm sạch Prompt & Kiểm duyệt đầu vào AI (Prompt Sanitization & Input Validation):**
  - *Mô tả:* Triển khai các lớp lọc và định dạng dữ liệu đầu vào ở Backend trước khi chuyển tiếp sang API ngoài, giảm thiểu nguy cơ Prompt Injection để bảo vệ tính nhất quán của AI Engine.
  - *Liên kết kỹ thuật:* Bảo vệ toàn vẹn luồng giao tiếp API giữa Spring Boot Backend và nhà cung cấp LLM ngoài.
- **Hạ tầng & Đóng gói môi trường (Infrastructure & Environment Containerization):**
  - *Mô tả:* Thiết lập Dockerfile cho cấu phần Spring Boot Backend, Dockerfile cho React Frontend, và tích hợp cấu hình file `docker-compose.yml` để tự động hóa quy trình khởi chạy toàn bộ hệ thống (bao gồm cả container MySQL). Chuẩn bị sẵn bộ dữ liệu mẫu (Seed Data) gồm tài khoản hệ thống mặc định và thư viện 30-50 bài tập gốc để phục vụ môi trường thử nghiệm.
  - *Liên kết kỹ thuật:* Đảm bảo tính nhất quán môi trường vận hành giữa máy phát triển (Development) và máy trình diễn nghiệm thu đồ án (Production), giảm thiểu rủi ro xung đột cấu hình môi trường cục bộ.


### 2.2. Nhóm SHOULD-HAVE (Yêu cầu nên có)
Các chức năng bổ trợ, chỉ bắt đầu thực hiện khi toàn bộ nhóm Must-have đã chạy ổn định trên môi trường Staging và còn dư thời gian.

- **Phân hệ PT quản lý hội viên:** Cho phép huấn luyện viên cá nhân xem danh sách hội viên được giao, theo dõi hồ sơ thể trạng, giám sát nhật ký tập và duyệt/chỉnh sửa các đề xuất giáo án từ AI trước khi gửi tới hội viên.
- **Dashboard thống kê cho Admin:** Hiển thị biểu đồ tổng giá trị subscription đã xác nhận mô phỏng theo thời gian, số lượng gói tập đang kích hoạt, và tỷ lệ phân bổ gói tập để phục vụ công tác quản trị.
- **Cảnh báo hạn gói tập tự động:** Hệ thống tự động phát hiện các gói tập sắp hết hạn (trong vòng 5 ngày) để gửi thông báo nhắc nhở lên giao diện (UI Notification) hoặc Email.
- **Lưu trữ lịch sử gợi ý AI:** Lưu lại lịch sử các giáo án và thực đơn do AI đề xuất theo tuần để hội viên và PT dễ dàng đối chiếu, so sánh sự cải thiện.
- **Cơ chế lưu đệm danh mục tĩnh (Caching Static Data):** Tích hợp Spring Cache để lưu trữ thư viện bài tập tĩnh, giảm tải cho cơ sở dữ liệu và tăng tốc độ tải trang trên giao diện.
- **Đính kèm tài liệu minh họa:** Cho phép tải lên hình ảnh hoặc liên kết video hướng dẫn thực hiện động tác trong thư viện bài tập.

### 2.3. Nhóm WON'T-HAVE (Các tính năng ngoài phạm vi MVP)
Các tính năng phức tạp, tốn nhiều thời gian tích hợp và kiểm thử. Dự án cam kết **tuyệt đối không thực hiện** nhóm này trong phạm vi 9 tuần của đồ án.

- **Tích hợp cổng thanh toán thực tế:** Luồng thanh toán gói tập chỉ thực hiện mô phỏng (Mock Flow) trên hệ thống; không kết nối API với cổng thanh toán thực (VNPAY, Momo, Stripe) để tránh phát sinh thủ tục pháp lý và chi phí kiểm thử.
- **Nhận diện khuôn mặt:** Không phát triển tính năng điểm danh bằng camera thông minh hay Face Recognition tại cửa phòng tập.
- **Giao tiếp thời gian thực (Real-time Chat):** Loại bỏ việc xây dựng phân hệ chat trực tuyến (WebSocket/Socket.io) giữa PT và Hội viên.
- **Ứng dụng di động (Mobile App):** Hệ thống chỉ phát triển trên nền tảng Web; giao diện được tối ưu hiển thị trên các kích thước màn hình thiết bị di động (Responsive Web).
- **Đồng bộ thiết bị đeo (IoT Integration):** Không kết nối API để thu thập dữ liệu nhịp tim, bước chân từ Apple Watch, Garmin, Fitbit.
- **Tự huấn luyện mô hình Machine Learning riêng (Custom Model Fine-tuning):** Không tự huấn luyện hay tinh chỉnh mô hình máy học riêng nhằm tối ưu tài nguyên hạ tầng tính toán; hệ thống chỉ sử dụng các mô hình LLM sẵn có từ nhà cung cấp lớn thông qua Prompt Engineering.
- **Dự đoán tỷ lệ hội viên bỏ tập (Churn Prediction):** Không triển khai các thuật toán phân tích dữ liệu lớn để dự đoán hành vi khách hàng.
- **Hệ thống đặt lịch hẹn PT phức tạp:** Không xây dựng module đặt lịch và xử lý tranh chấp khung giờ giữa các huấn luyện viên.

---

## 3. Kết luận về quản lý rủi ro phạm vi (Scope Risk Management)
Để bảo vệ tiến độ dự án và đạt được kết quả nghiệm thu tốt nhất trước Hội đồng:
1. **Cam kết Feature Freeze:** Đóng băng toàn bộ các tính năng nâng cao (nhóm Should-have và Won't-have) cho đến khi 100% các tính năng thuộc nhóm Must-have được phát triển, kiểm thử liên kết (End-to-End) và đảm bảo không có lỗi nghiêm trọng phát sinh.
2. **Kiểm soát Scope Creep:** Bất kỳ đề xuất bổ sung tính năng mới nào trong quá trình phát triển đều phải được đưa ra thảo luận, đánh giá tác động thời gian và chỉ được chấp thuận nếu không ảnh hưởng đến hạn chót bàn giao của đồ án.
