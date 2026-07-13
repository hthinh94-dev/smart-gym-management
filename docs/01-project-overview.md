# ĐỒ ÁN TỐT NGHIỆP: TỔNG QUAN ĐỀ TÀI

## 1. Thông tin chung
- **Tên đề tài (Tiếng Việt):** Hệ thống quản lý phòng gym thông minh tích hợp AI gợi ý lịch tập và dinh dưỡng cá nhân hóa
- **Tên đề tài (Tiếng Anh):** Smart Gym Management System with AI-based Personalized Workout and Nutrition Recommendations

## 2. Đặt vấn đề và Bài toán thực tế
Sự bùng nổ của xu hướng chăm sóc sức khỏe và thể hình trong kỷ nguyên số đã thúc đẩy sự phát triển mạnh mẽ của ngành công nghiệp thể thao, đặc biệt là các trung tâm thể hình (Gym/Fitness Center). Tuy nhiên, thực trạng vận hành và cung cấp dịch vụ tại các phòng gym hiện nay đang đối mặt với nhiều bất cập lớn, chia làm các phương diện chính:

- **Hạn chế của một số giải pháp phần mềm quản lý hiện hành:** Một số hệ thống quản trị phòng gym trên thị trường chủ yếu tập trung giải quyết các bài toán hành chính như kiểm soát vào/ra (check-in/check-out), quản lý thu chi và đăng ký gói tập. Các phần mềm này thường đóng vai trò lưu trữ dữ liệu hành chính, còn hạn chế ở khả năng tương tác hai chiều và cá nhân hóa trải nghiệm cho hội viên. Điều này khiến các trung tâm thể hình gặp khó khăn trong việc duy trì sự gắn kết khách hàng dài hạn do thiếu công cụ hỗ trợ thực chất trong hành trình cải thiện thể chất.
- **Khoảng trống trong việc thiết lập lộ trình tập luyện và dinh dưỡng cá nhân hóa:** Việc đạt được mục tiêu thể hình (giảm cân, tăng cơ, duy trì cân nặng) đòi hỏi một chương trình tập luyện khoa học và chế độ dinh dưỡng phù hợp với thể trạng từng cá nhân. Nhiều hội viên mới thiếu kiến thức chuyên môn để tự xây dựng lộ trình phù hợp, dễ dẫn đến tập luyện sai phương pháp hoặc không đạt kết quả mong muốn, từ đó giảm động lực duy trì thói quen tập luyện lâu dài.
- **Sự phụ thuộc vào nhân sự huấn luyện viên cá nhân (PT) và rào cản chi phí:** Đối với nhiều hội viên, thuê huấn luyện viên cá nhân (PT) là một trong các phương án phổ biến để có được lộ trình tập luyện bài bản. Tuy nhiên, chi phí thuê PT thường ở mức cao, tạo ra rào cản tiếp cận cho một bộ phận người dùng. Bên cạnh đó, năng lực tiếp nhận học viên của mỗi PT cũng có giới hạn nhất định về thời gian và số lượng học viên cùng lúc.
- **Thiếu công cụ theo dõi tiến trình trực quan và liên tục:** Cải thiện thể chất là quá trình dài hạn cần được theo dõi qua các số liệu định lượng như cân nặng, mức tạ và tần suất tập luyện theo thời gian. Sự thiếu hụt một hệ thống ghi nhận lịch sử tập luyện tập trung khiến hội viên khó nhận biết mức độ tiến bộ của bản thân, từ đó ảnh hưởng đến động lực duy trì tập luyện.

Từ những thực trạng nêu trên, việc nghiên cứu và phát triển một **Hệ thống quản lý phòng gym thông minh tích hợp công nghệ Trí tuệ nhân tạo (AI Engine)** mang ý nghĩa thiết thực. Hệ thống hướng đến tối ưu hóa quy trình vận hành hành chính cho ban quản lý và đồng thời cung cấp **công cụ hỗ trợ đề xuất cá nhân hóa** dựa trên dữ liệu thể trạng, giúp hội viên tiếp cận lộ trình tập luyện và gợi ý dinh dưỡng tham khảo một cách thuận tiện hơn.

## 3. Mục tiêu của đề tài

### 3.1. Mục tiêu tổng quát
Nghiên cứu, thiết kế và xây dựng một hệ thống quản lý phòng gym thông minh trong phạm vi đề tài, tích hợp công nghệ Trí tuệ nhân tạo (AI Engine) nhằm cải thiện công tác quản trị hành chính và cung cấp giải pháp cá nhân hóa lộ trình tập luyện, dinh dưỡng cho hội viên dựa trên các thông số thể trạng và lịch sử tập luyện thực tế.

### 3.2. Mục tiêu cụ thể

#### Nhóm 1: Hệ thống & Nghiệp vụ cốt lõi (Core System & Business Operations)
- **Đăng nhập và Phân quyền:** Xây dựng cơ chế xác thực và RBAC hỗ trợ ba vai trò Administrator, Personal Trainer và Member. Vai trò PT chỉ được khai báo cấu trúc sẵn trong RBAC/database để sẵn sàng tích hợp về sau, còn toàn bộ API và giao diện nghiệp vụ chuyên biệt cho PT là chức năng mở rộng *(Should-have)*. Luồng MVP bắt buộc chỉ gồm Admin và Member; Member nhận đề xuất AI trực tiếp từ hệ thống mà không cần PT phê duyệt.
- **Quản lý Hội viên & Thể trạng:** Phát triển phân hệ quản lý thông tin hội viên, quản lý hồ sơ thể trạng định kỳ của từng cá nhân.
- **Quản lý Gói tập:** Xây dựng chức năng quản lý, phân loại và gia hạn các gói dịch vụ/gói tập của phòng gym.
- **Quản lý Thư viện bài tập:** Xây dựng phân hệ quản lý danh mục bài tập, hỗ trợ phân loại theo nhóm cơ và thiết lập các chương trình tập luyện mẫu.
- **Lập lịch & Theo dõi tiến trình:** Thiết kế và triển khai tính năng lập lịch tập, ghi nhận nhật ký tập luyện chi tiết (mức tạ, số reps, số sets) và theo dõi tiến trình thay đổi **cân nặng, mức tạ và tần suất tập luyện** theo thời gian.

#### Nhóm 2: Tính toán & Tích hợp AI (Computational & AI Integration)
- **Tính toán chỉ số sinh học:** Lập trình mô đun tính toán tự động các chỉ số sinh học cơ bản và nâng cao bao gồm BMI, BMR, TDEE cùng tỷ lệ dinh dưỡng đa lượng (Macronutrients: Protein, Carbohydrate, Fat) phù hợp với mục tiêu cụ thể của hội viên.
- **Gợi ý cá nhân hóa từ AI:** Tích hợp công nghệ Trí tuệ nhân tạo (AI Engine) / Mô hình ngôn ngữ lớn (LLM) để phân tích dữ liệu thể trạng và mục tiêu cá nhân, từ đó tự động đề xuất lịch tập luyện phù hợp và thực đơn dinh dưỡng hàng ngày phù hợp.

#### Nhóm 3: Triển khai & Vận hành (Deployment & Operations)
- **Giao diện Demo:** Thiết kế và xây dựng giao diện người dùng (Frontend) trực quan, đáp ứng tốt trải nghiệm người dùng (UX) bằng framework React để phục vụ quá trình trình diễn và đánh giá hệ thống.
- **Đóng gói hệ thống:** Đóng gói các thành phần phần mềm bao gồm Back-end (Spring Boot), Front-end (React) và Cơ sở dữ liệu (MySQL) bằng công nghệ Container hóa (Docker) nhằm tối ưu quy trình triển khai, bảo trì và đảm bảo tính nhất quán trên các môi trường vận hành khác nhau.

