# BÁO CÁO ĐỒ ÁN TỐT NGHIỆP

**Đề tài:** Hệ thống quản lý phòng gym thông minh tích hợp AI gợi ý lịch tập và dinh dưỡng cá nhân hóa

**Sinh viên thực hiện:** Trần Hưng Thịnh

**MSSV:** 2351010199

# MỞ ĐẦU

Hoạt động của một phòng gym phát sinh nhiều dữ liệu liên quan đến tài khoản, hồ sơ thể trạng, gói tập, giáo án và lịch sử luyện tập. Khi các dữ liệu này được quản lý bằng sổ sách, bảng tính hoặc nhiều công cụ tách rời, việc tra cứu và đối chiếu mất thời gian, dễ xuất hiện dữ liệu trùng hoặc sai trạng thái. Về phía hội viên, người mới tập cũng gặp khó khăn khi tự xây dựng lịch tập và chế độ dinh dưỡng phù hợp với thể trạng, mục tiêu và điều kiện luyện tập.

Từ vấn đề trên, đề tài **“Hệ thống quản lý phòng gym thông minh tích hợp AI gợi ý lịch tập và dinh dưỡng cá nhân hóa”** được thực hiện để xây dựng một hệ thống Web phục vụ quản trị viên và hội viên. Hệ thống quản lý tập trung tài khoản, hồ sơ, gói tập, thư viện bài tập, giáo án, nhật ký và tiến độ. AI chỉ tạo lịch tập và cấu trúc bữa ăn từ dữ liệu đã được làm sạch; các chỉ số BMI, BMR, TDEE, calories và chất dinh dưỡng đa lượng do Backend tính toán. Phản hồi AI phải đúng JSON Schema, dùng bài tập thuộc whitelist và vượt qua bước hậu kiểm trước khi lưu. Nếu AI lỗi hoặc trả dữ liệu không hợp lệ, hệ thống chuyển sang phương án fallback đã được kiểm tra bằng cùng bộ quy tắc.

Giải pháp được thiết kế theo kiến trúc Modular Layered Monolith, sử dụng Spring Boot 3.4.3 và Java 21 cho Backend, React cho Frontend, MySQL 8 cho cơ sở dữ liệu và RESTful API cho giao tiếp giữa hai phía. Bảo mật dựa trên JWT stateless, phân quyền theo vai trò và kiểm tra trạng thái tài khoản. Các đề xuất tập luyện, dinh dưỡng chỉ mang tính tham khảo, không thay thế tư vấn y tế hoặc chuyên môn trực tiếp.

Báo cáo gồm năm chương: tổng quan đề tài; phân tích yêu cầu; thiết kế hệ thống; triển khai; kiểm thử và đánh giá.

# CHƯƠNG 1. TỔNG QUAN ĐỀ TÀI

## 1.1. Bối cảnh và vấn đề cần giải quyết

### 1.1.1. Bối cảnh quản lý phòng gym

Hoạt động của một phòng gym phát sinh nhiều nhóm dữ liệu liên quan đến nhau, từ tài khoản người dùng, hồ sơ thể trạng và gói tập đến danh mục bài tập, giáo án và lịch sử luyện tập. Các dữ liệu này thay đổi theo thời gian và cần được kiểm tra theo những quy tắc cụ thể. Chẳng hạn, một hội viên chỉ được có một gói tập đang hoạt động tại cùng một thời điểm; giáo án hiện hành phải thuộc chính hội viên đó; nhật ký tập luyện cần giữ được lịch sử ngay cả khi bài tập đã ngừng sử dụng.

Về phía hội viên, việc lựa chọn lịch tập phụ thuộc vào mục tiêu, trình độ, thời gian, thiết bị hiện có và hạn chế vận động của từng người. Hội viên mới thường khó tự xác định cường độ, bài tập và chế độ dinh dưỡng phù hợp. Nếu không có công cụ ghi nhận tập trung, người dùng cũng khó theo dõi sự thay đổi về cân nặng, mức tạ và tần suất tập luyện theo thời gian.

### 1.1.2. Hạn chế của phương thức quản lý rời rạc

Khi dữ liệu được lưu bằng sổ sách, bảng tính hoặc nhiều công cụ riêng biệt, quá trình tra cứu và tổng hợp thường tốn thời gian. Thông tin có thể bị trùng, thiếu liên kết hoặc không phản ánh đúng trạng thái hiện tại. Người quản lý phải kiểm tra thủ công thời hạn gói tập, trong khi hội viên thiếu một nơi tập trung để quản lý hồ sơ, giáo án và tiến độ cá nhân.

Một số hệ thống quản lý phòng gym chủ yếu giải quyết nghiệp vụ hành chính. Các chức năng hỗ trợ trực tiếp cho quá trình tập luyện như xây dựng giáo án, ghi nhật ký và phân tích tiến độ chưa được kết nối chặt chẽ với dữ liệu thể trạng. Đây là vấn đề mà đề tài hướng đến giải quyết.

### 1.1.3. Lý do chọn đề tài

Đề tài được lựa chọn vì có tính ứng dụng thực tế và phù hợp để vận dụng kiến thức của ngành Kỹ thuật Phần mềm. Quá trình xây dựng hệ thống bao gồm phân tích yêu cầu, thiết kế cơ sở dữ liệu, xây dựng RESTful API, triển khai cơ chế bảo mật và thực hiện kiểm thử. Các API được tổ chức theo tài nguyên, sử dụng phương thức HTTP phù hợp, mã trạng thái HTTP và tiền tố phiên bản `/api/v1`.

Việc tích hợp AI đặt ra một vấn đề kỹ thuật cần xử lý: kết quả do mô hình ngôn ngữ sinh ra không thể được xem là dữ liệu hợp lệ ngay lập tức. Hệ thống phải kiểm soát dữ liệu đầu vào, giới hạn danh sách bài tập, quy định cấu trúc phản hồi và kiểm tra lại kết quả tại Backend. Vì vậy, AI trong đề tài được sử dụng như thành phần hỗ trợ tạo đề xuất, không thay thế toàn bộ logic nghiệp vụ.

## 1.2. Mục tiêu đề tài

### 1.2.1. Mục tiêu tổng quát

Mục tiêu của đề tài là xây dựng một hệ thống Web hỗ trợ quản lý phòng gym và cá nhân hóa quá trình tập luyện cho hội viên. Hệ thống cần quản lý dữ liệu tập trung, kiểm soát quyền truy cập và cung cấp đề xuất tập luyện, dinh dưỡng dựa trên thông tin thể trạng của từng người dùng.

### 1.2.2. Mục tiêu cụ thể

- Xây dựng chức năng đăng ký, đăng nhập và phân quyền người dùng.
- Quản lý hồ sơ thể trạng, mục tiêu và hạn chế vận động của hội viên.
- Quản lý danh mục gói tập, yêu cầu đăng ký, phê duyệt, hủy và gia hạn.
- Quản lý thư viện bài tập và trạng thái hoạt động của từng bài tập.
- Tính BMI, BMR, TDEE, calories và các chất dinh dưỡng đa lượng tại Backend.
- Tạo giáo án tập luyện bằng AI hoặc phương án fallback.
- Cho phép hội viên kích hoạt giáo án, ghi nhật ký và theo dõi tiến độ.
- Cung cấp RESTful API có tài liệu OpenAPI và khả năng đóng gói bằng Docker.

### 1.2.3. Kết quả dự kiến

Sau khi hoàn thành, đề tài dự kiến cung cấp một hệ thống Web có khả năng vận hành khép kín cho quản trị viên và hội viên. Quản trị viên có thể quản lý tài khoản, gói tập, subscription và thư viện bài tập. Hội viên có thể cập nhật hồ sơ, đăng ký hoặc gia hạn gói tập, nhận giáo án, ghi nhật ký và theo dõi tiến độ. Vai trò PT được chuẩn bị trong cơ chế phân quyền để phục vụ khả năng mở rộng sau giai đoạn MVP.

Về kỹ thuật, hệ thống dự kiến sử dụng React cho Frontend, Spring Boot 3.4.3 và Java 21 cho Backend, MySQL 8 cho cơ sở dữ liệu. Frontend và Backend trao đổi thông qua 32 RESTful API dưới tiền tố `/api/v1`. Cơ sở dữ liệu gồm 25 bảng vật lý, được quản lý bằng Flyway và ánh xạ bằng JPA Hibernate.

Hệ thống dự kiến triển khai xác thực JWT stateless, phân quyền theo vai trò và kiểm tra trạng thái tài khoản tại thời điểm xử lý request. Chức năng AI được xây dựng theo mô hình Hybrid, có kiểm tra JSON Schema, exercise whitelist, giới hạn nghiệp vụ và fallback. Sản phẩm cuối cùng dự kiến được đóng gói bằng Docker Compose, có tài liệu OpenAPI, dữ liệu mẫu và bộ kiểm thử cho các luồng nghiệp vụ quan trọng.

## 1.3. Phạm vi đề tài

### 1.3.1. Phạm vi chức năng MVP

Phạm vi sản phẩm khả dụng tối thiểu gồm Authentication, Member Profile, Membership, Exercise Library, Workout Plan, Workout Log, Progress Analytics và AI Hybrid Recommendation. Thanh toán gói tập được mô phỏng bằng thao tác xác nhận của Admin, không kết nối cổng thanh toán thật.

Backend chịu trách nhiệm tính các chỉ số sinh học và dinh dưỡng định lượng. AI chỉ tạo `workoutSchedule` và `nutritionPlan.mealStructure`. Kết quả AI phải vượt qua JSON Schema, exercise whitelist và các giới hạn nghiệp vụ trước khi được lưu.

### 1.3.2. Đối tượng sử dụng

| Đối tượng | Phạm vi sử dụng |
|---|---|
| Admin | Quản lý tài khoản, gói tập, subscription, bài tập và số liệu tổng hợp cơ bản |
| Member | Quản lý hồ sơ, đăng ký gói, nhận giáo án, ghi nhật ký và theo dõi tiến độ |
| PT | Được khai báo bằng `ROLE_PT`, chưa có API và giao diện nghiệp vụ trong MVP |
| Anonymous Guest | Trạng thái chưa xác thực; được xem gói tập công khai và đăng ký tài khoản |

Anonymous Guest không phải một role trong cơ sở dữ liệu. `ROLE_PT` được seed để chuẩn bị khả năng mở rộng, nhưng không phải điều kiện để luồng Admin–Member hoạt động.

### 1.3.3. Nội dung ngoài phạm vi

Đề tài không triển khai cổng thanh toán thật, refresh token, OAuth2, ứng dụng di động, nhận diện khuôn mặt, thiết bị đeo, trò chuyện thời gian thực, đặt lịch PT phức tạp hoặc tự huấn luyện mô hình máy học. Hệ thống cũng không cung cấp chức năng chẩn đoán hoặc điều trị y khoa.

## 1.4. Phương pháp thực hiện

### 1.4.1. Phân tích và đặc tả yêu cầu

Đề tài bắt đầu bằng việc xác định vấn đề trong quản lý phòng gym và nhu cầu của từng nhóm người dùng. Phạm vi chức năng được phân loại theo phương pháp MoSCoW, gồm Must-have, Should-have và Won’t-have. Cách phân loại này giúp ưu tiên các chức năng bắt buộc và hạn chế phát sinh yêu cầu vượt quá thời gian thực hiện.

Từ phạm vi đã xác định, hệ thống được đặc tả thành 44 yêu cầu chức năng, 14 yêu cầu phi chức năng, 10 Use Case cốt lõi và 30 mục quy tắc nghiệp vụ trong dải mã BR-01 đến BR-28; BR-09 được tách thành BR-09A, BR-09B và BR-09C. Bộ đặc tả còn có 34 Acceptance Criteria theo cấu trúc Given–When–Then. Mỗi Use Case được mô tả bằng tác nhân, tiền điều kiện, hậu điều kiện, luồng chính và luồng ngoại lệ. Các yêu cầu tiếp tục được ánh xạ xuống 32 RESTful API, cấu trúc dữ liệu và tiêu chí nghiệm thu. Ma trận truy vết được sử dụng để kiểm tra sự liên kết giữa yêu cầu, Use Case, Business Rule, API và Test Case.

### 1.4.2. Thiết kế và phát triển theo milestone

Hệ thống được thiết kế theo kiến trúc Modular Layered Monolith. Backend được phân chia theo module nghiệp vụ, trong đó Controller xử lý giao tiếp HTTP, Service thực hiện nghiệp vụ và quản lý transaction, còn Repository chịu trách nhiệm truy cập dữ liệu. Cơ sở dữ liệu được thiết kế ở mức vật lý và quản lý bằng Flyway Migration.

Quá trình phát triển được chia thành bảy milestone. Các milestone lần lượt tập trung vào Authentication và Security; Member Profile; Membership; Exercise và Workout Plan; AI Hybrid Recommendation; Workout Log và Progress Analytics; cuối cùng là kiểm thử tổng thể, Docker và hoàn thiện tài liệu.

Mỗi milestone tạo ra một nhóm chức năng có thể chạy và kiểm tra độc lập. Một milestone chỉ được xem là hoàn thành khi mã nguồn biên dịch thành công, các kiểm thử liên quan đạt yêu cầu, API contract được cập nhật và luồng nghiệp vụ có thể trình diễn.

### 1.4.3. Kiểm thử và đánh giá

Đề tài áp dụng nhiều mức kiểm thử. Unit Test được sử dụng cho các thành phần có logic độc lập như JWT, tính toán chỉ số sinh học, kiểm tra trạng thái tài khoản và hậu kiểm phản hồi AI. Integration Test được sử dụng để kiểm tra sự phối hợp giữa Controller, Service, Repository, Spring Security, Flyway và MySQL. Các luồng nghiệp vụ chính được kiểm tra theo Acceptance Criteria của từng Use Case.

RESTful API được kiểm tra thông qua SpringDoc OpenAPI, Swagger UI và Postman. Các trường hợp kiểm thử bao gồm luồng thành công, dữ liệu không hợp lệ, thiếu quyền, token sai hoặc hết hạn, xung đột dữ liệu và lỗi dịch vụ AI. Những thao tác liên quan đến nhiều bảng còn được kiểm tra về transaction rollback và Optimistic Locking.

Kết quả cuối cùng được đánh giá dựa trên mức độ đáp ứng yêu cầu chức năng, yêu cầu phi chức năng, quy tắc nghiệp vụ và tiêu chí nghiệm thu. Các số liệu như số lượng test, tỷ lệ thành công và thời gian phản hồi chỉ được đưa vào Chương 5 sau khi có kết quả đo thực tế.

## 1.5. Cơ sở công nghệ

### 1.5.1. RESTful API và kiến trúc phân tầng

RESTful API được sử dụng làm giao diện trao đổi giữa React Frontend và Spring Boot Backend. Các endpoint được tổ chức theo tài nguyên, sử dụng phương thức HTTP và mã trạng thái phù hợp. Backend được tổ chức theo kiến trúc phân tầng: Controller xử lý HTTP, Service sở hữu nghiệp vụ và transaction, Repository thực hiện truy vấn dữ liệu.

### 1.5.2. ORM, Flyway và transaction

JPA Hibernate hỗ trợ ánh xạ giữa đối tượng Java và bảng quan hệ. Flyway là thành phần duy nhất quản lý thay đổi cấu trúc cơ sở dữ liệu; Hibernate chỉ kiểm tra sự tương thích bằng chế độ `validate`. Các thao tác ghi nhiều bảng được đặt trong transaction tại Service để bảo đảm nguyên tắc toàn bộ thành công hoặc toàn bộ rollback.

### 1.5.3. JWT và RBAC

JWT (JSON Web Token) được dùng để xác thực request mà không lưu session trên Server. Quyền truy cập được kiểm soát theo vai trò (Role-Based Access Control - RBAC) với `ROLE_ADMIN`, `ROLE_MEMBER` và `ROLE_PT`. Trạng thái `ACTIVE`, `LOCKED`, `DISABLED` được kiểm tra riêng để token cũ không tiếp tục sử dụng được sau khi tài khoản bị khóa.

### 1.5.4. AI Hybrid và fallback

Mô hình AI Hybrid kết hợp tính toán xác định tại Backend với khả năng tạo nội dung của AI. Backend sở hữu số liệu định lượng, whitelist và hậu kiểm; AI chỉ tạo đề xuất có cấu trúc. Khi AI lỗi, hệ thống sử dụng fallback template đã được kiểm tra bằng cùng bộ quy tắc.

## 1.6. Cấu trúc báo cáo

Báo cáo gồm năm chương. Chương 1 giới thiệu đề tài. Chương 2 phân tích yêu cầu. Chương 3 trình bày thiết kế. Chương 4 mô tả quá trình triển khai. Chương 5 trình bày kiểm thử và đánh giá. Phần cuối tổng kết kết quả, hạn chế và hướng phát triển.

# CHƯƠNG 2. PHÂN TÍCH YÊU CẦU HỆ THỐNG

## 2.1. Phân tích bài toán nghiệp vụ

### 2.1.1. Quy trình nghiệp vụ tổng quát

Quy trình sử dụng hệ thống bắt đầu khi người dùng đăng ký tài khoản và hoàn thiện hồ sơ thể trạng. Hội viên lựa chọn gói tập và gửi yêu cầu đăng ký. Sau khi Admin phê duyệt, hội viên được sử dụng các chức năng cao cấp như tạo recommendation, kích hoạt giáo án và ghi nhật ký tập luyện. Dữ liệu từ Workout Log và Body Progress được tổng hợp để theo dõi tiến độ.

### 2.1.2. Quản lý tài khoản và hội viên

Email phải được loại bỏ khoảng trắng đầu cuối và chuyển thành chữ thường trước khi kiểm tra trùng hoặc lưu. Mật khẩu được băm bằng BCrypt, không lưu dưới dạng văn bản thuần. Tài khoản đăng ký công khai chỉ được gán `ROLE_MEMBER`.

Hội viên chỉ được truy cập tài nguyên thuộc tài khoản của mình. Định danh hội viên phải được suy ra từ Principal trong Security Context thay vì nhận trực tiếp từ Client. Admin có quyền khóa hoặc mở khóa tài khoản nhưng không được khóa người dùng chỉ vì subscription đã hết hạn.

### 2.1.3. Quản lý gói tập, giáo án và tiến độ

Một hội viên chỉ được có tối đa một subscription `ACTIVE` và một yêu cầu đăng ký mới `PENDING`. Subscription được xem là hợp lệ khi có trạng thái `ACTIVE` và thỏa mãn `startDate <= currentDate < endDate`.

Recommendation tạo ra Workout Plan ở trạng thái `DRAFT`. Khi hội viên kích hoạt plan mới, plan `ACTIVE` trước đó chuyển thành `ARCHIVED`. Body Progress và Workout Log sử dụng cơ chế cập nhật tại chỗ khi trùng khóa nghiệp vụ nhằm tránh tạo dữ liệu lặp.

## 2.2. Tác nhân hệ thống

Hệ thống xác định ba tác nhân tham gia trực tiếp vào MVP là Anonymous Guest, Member và Admin. Anonymous Guest có thể xem gói tập công khai và đăng ký tài khoản. Member quản lý dữ liệu cá nhân, gói tập, giáo án và tiến độ. Admin quản lý các tài nguyên dùng chung và xử lý yêu cầu của hội viên.

PT được chuẩn bị trong cấu trúc RBAC bằng `ROLE_PT`. Các API và giao diện riêng cho PT thuộc nhóm Should-have nên không tham gia vào luồng hoạt động bắt buộc của phiên bản MVP.

| Tác nhân | Trách nhiệm chính |
|---|---|
| Anonymous Guest | Xem gói tập công khai, đăng ký và bắt đầu luồng đăng nhập |
| Member | Quản lý hồ sơ, subscription, giáo án, nhật ký và tiến độ cá nhân |
| Admin | Quản lý tài khoản, package, subscription và Exercise master data |
| PT | Vai trò mở rộng, chưa có nghiệp vụ bắt buộc trong MVP |

## 2.3. Phân tích Use Case

### 2.3.1. Phạm vi Use Case tổng quát

Mô hình Use Case của MVP tập trung vào ba tác nhân Anonymous Guest, Member và Admin. Anonymous Guest tham gia đăng ký và bắt đầu quá trình đăng nhập. Member thực hiện các nghiệp vụ cá nhân gồm hồ sơ, subscription, recommendation, giáo án, nhật ký và tiến độ. Admin quản lý tài nguyên dùng chung, xử lý subscription và trạng thái tài khoản. PT không tham gia vào Use Case bắt buộc vì phân hệ này thuộc phạm vi Should-have. Phạm vi phân tích chỉ gồm các quan hệ trên, không bổ sung chức năng ngoài 10 Use Case đã chốt.

### 2.3.2. Danh sách Use Case cốt lõi

| Mã | Tên Use Case | Tác nhân chính | FR liên quan | Ưu tiên |
|---|---|---|---|---|
| UC-01 | Đăng ký tài khoản | Anonymous Guest | FR-AUTH-01 | Must-have |
| UC-02 | Đăng nhập | Anonymous Guest, Member, Admin | FR-AUTH-02, FR-AUTH-03 | Must-have |
| UC-03 | Cập nhật hồ sơ thể trạng | Member | FR-PROFILE-02–04, FR-PROGRESS-01–02 | Must-have |
| UC-04 | Đăng ký mới hoặc gửi yêu cầu gia hạn | Member | FR-SUB-04–05, FR-SUB-07–08 | Must-have |
| UC-05 | Admin xác nhận subscription | Admin | FR-SUB-06, FR-SUB-08 | Must-have |
| UC-06 | Admin quản lý bài tập | Admin | FR-EXR-01–03 | Must-have |
| UC-07 | Tạo lịch tập và dinh dưỡng | Member | FR-SUB-07, FR-EXR-06, FR-WORKOUT-01–05, FR-NUTRITION-01–06 | Must-have |
| UC-08 | Ghi nhật ký tập luyện | Member | FR-WORKOUT-06, FR-SUB-07 | Must-have |
| UC-09 | Xem tiến độ tập luyện | Member | FR-WORKOUT-07, FR-PROGRESS-01–04 | Must-have |
| UC-10 | Khóa hoặc mở khóa tài khoản | Admin | FR-AUTH-03, FR-ADMIN-02 | Must-have |

Ba mươi bốn Acceptance Criteria theo cấu trúc Given–When–Then được truy vết từ 10 Use Case xuống API contract và Test Case. Để nội dung không lặp lại toàn bộ kịch bản kiểm thử, phần dưới đây tập trung vào mục tiêu, điều kiện, luồng xử lý và ngoại lệ của từng Use Case.

### 2.3.3. Đặc tả UC-01 – Đăng ký tài khoản

| Thuộc tính | Nội dung |
|---|---|
| Mục tiêu | Tạo tài khoản hội viên để người dùng có thể đăng nhập hệ thống |
| Tác nhân chính | Anonymous Guest |
| Tiền điều kiện | Người dùng chưa đăng nhập và email chưa được sử dụng |
| Hậu điều kiện | Tạo `User` có email chuẩn hóa, password hash, `ACTIVE` và `ROLE_MEMBER` |
| Business Rule | BR-01, BR-02, BR-15, BR-18, BR-20 |
| RESTful API | `POST /api/v1/auth/register` |

**Luồng chính:** Người dùng nhập họ tên, email, mật khẩu và xác nhận mật khẩu. Hệ thống chuẩn hóa email, kiểm tra trùng, kiểm tra chính sách mật khẩu và sự trùng khớp của hai trường mật khẩu. Sau khi băm mật khẩu bằng BCrypt, hệ thống tạo User `ACTIVE`, gán `ROLE_MEMBER` và trả HTTP 201 Created.

**Luồng ngoại lệ:** Email đã tồn tại trả `ACC-001` và HTTP 409. Mật khẩu không đạt yêu cầu hoặc xác nhận mật khẩu không khớp trả `ACC-002` và HTTP 400. Hệ thống không cho Client truyền role hoặc account status.

### 2.3.4. Đặc tả UC-02 – Đăng nhập

| Thuộc tính | Nội dung |
|---|---|
| Mục tiêu | Xác thực người dùng và cấp JWT Access Token |
| Tác nhân chính | Người dùng chưa xác thực có tài khoản Member hoặc Admin |
| Tiền điều kiện | Tài khoản đã tồn tại |
| Hậu điều kiện | Client nhận JWT chứa subject là email, danh sách roles, thời điểm phát hành và hết hạn |
| Business Rule | BR-16, BR-18, BR-20, BR-21 |
| RESTful API | `POST /api/v1/auth/login` |

**Luồng chính:** Người dùng nhập email và mật khẩu. Hệ thống chuẩn hóa email, tải User cùng Roles, kiểm tra trạng thái tài khoản và so khớp mật khẩu với BCrypt hash. Nếu hợp lệ, Backend cấp Access Token và trả HTTP 200.

**Luồng ngoại lệ:** Email hoặc mật khẩu không đúng trả `ACC-007` và HTTP 401 mà không tiết lộ trường nào sai. Tài khoản `LOCKED` trả `ACC-004`; tài khoản `DISABLED` trả `ACC-006`; cả hai trường hợp dùng HTTP 403 và không cấp token.

### 2.3.5. Đặc tả UC-03 – Cập nhật hồ sơ thể trạng

| Thuộc tính | Nội dung |
|---|---|
| Mục tiêu | Lưu hồ sơ thể chất và dinh dưỡng làm đầu vào cho tính toán và recommendation |
| Tác nhân chính | Member |
| Tiền điều kiện | Member đã xác thực bằng JWT |
| Hậu điều kiện | Cập nhật `member_profiles` và collection; sau đó Frontend ghi Body Progress qua API độc lập |
| Business Rule | BR-13, BR-22, BR-23 |
| RESTful API | `PUT /api/v1/member/profile` |

**Luồng chính:** Member nhập thông tin giới tính, ngày sinh, chiều cao, cân nặng, mục tiêu, trình độ, mức hoạt động, số ngày tập, thời lượng buổi tập, thiết bị, nhóm cơ ưu tiên, hạn chế vận động và thông tin dinh dưỡng. Backend Profile kiểm tra dữ liệu, lưu hồ sơ, tính BMI, BMR, TDEE, calories và các chất dinh dưỡng đa lượng rồi trả response. Frontend tiếp tục gọi API Body Progress bằng cân nặng vừa lưu và ngày nghiệp vụ Việt Nam. Hai API có transaction độc lập; lỗi Progress không rollback hoặc làm mất trạng thái Profile đã lưu và có thể retry riêng.

**Luồng ngoại lệ:** Chiều cao hoặc cân nặng không dương; ngày sinh ở tương lai; số ngày tập ngoài 1–7; số bữa ngoài 1–6; enum hoặc collection sai quy định đều trả `VAL-001` và HTTP 400. Hệ thống lấy Member từ Principal, không nhận `memberId` tùy ý từ Client.

### 2.3.6. Đặc tả UC-04 – Đăng ký mới hoặc gửi yêu cầu gia hạn

| Thuộc tính | Nội dung |
|---|---|
| Mục tiêu | Tạo yêu cầu đăng ký mới hoặc yêu cầu gia hạn subscription hiện hành |
| Tác nhân chính | Member |
| Tiền điều kiện | Member đã đăng nhập; package còn hoạt động; thỏa điều kiện riêng của từng nhánh |
| Hậu điều kiện | Tạo Subscription `PENDING` hoặc Renewal Request `PENDING` |
| Business Rule | BR-04, BR-05, BR-13, BR-24, BR-25 |
| RESTful API | `POST /api/v1/member/subscriptions`; `POST /api/v1/member/subscriptions/{activeSubscriptionId}/renewal-requests` |

**Luồng đăng ký mới:** Member chọn package đang hoạt động. Hệ thống kiểm tra Member chưa có subscription hợp lệ và chưa có yêu cầu đăng ký `PENDING`, sau đó tạo Subscription `PENDING` cùng snapshot của package.

**Luồng gia hạn:** Member chọn subscription `ACTIVE` thuộc sở hữu của mình. Hệ thống kiểm tra package khớp và còn hoạt động, sau đó tạo Renewal Request `PENDING` mà không tạo thêm subscription `ACTIVE`.

**Luồng ngoại lệ:** Package không tồn tại trả `SUB-002`; package ngừng hoạt động trả `SUB-003`; đã có subscription hợp lệ trả `SUB-004`; subscription gia hạn không tồn tại hoặc không thuộc Member trả `SUB-005`; yêu cầu `PENDING` trùng trả `SUB-006`.

### 2.3.7. Đặc tả UC-05 – Admin xác nhận subscription

| Thuộc tính | Nội dung |
|---|---|
| Mục tiêu | Phê duyệt đăng ký mới hoặc xử lý yêu cầu gia hạn |
| Tác nhân chính | Admin |
| Tiền điều kiện | Admin đã đăng nhập; request ở trạng thái `PENDING` |
| Hậu điều kiện | Subscription chuyển `ACTIVE` hoặc Renewal Request chuyển `PROCESSED` |
| Business Rule | BR-03, BR-04, BR-05, BR-24, BR-25 |
| RESTful API | `POST /api/v1/admin/subscriptions/{id}/approve` |

**Luồng đăng ký mới:** Service khóa phạm vi subscription của Member, chuẩn hóa các bản ghi `ACTIVE` đã hết hiệu lực sang `EXPIRED`, sau đó kiểm tra không còn subscription hợp lệ trước khi kích hoạt request. `startDate` là ngày phê duyệt; `endDate` là mốc exclusive được tính bằng `startDate + durationDays`.

**Luồng gia hạn:** Service khóa Renewal Request và subscription đích, xác minh subscription còn hiệu lực, cộng `durationDays` vào `endDate`, rồi chuyển request sang `PROCESSED`. Các thay đổi hoàn thành trong cùng transaction.

Hai nhánh phê duyệt dùng chung endpoint nhưng được phân biệt bằng `requestType = NEW_SUBSCRIPTION` hoặc `requestType = RENEWAL`. Nhờ đó, Backend tải đúng loại yêu cầu ngay cả khi Subscription và Renewal Request có giá trị khóa chính trùng nhau.

**Luồng ngoại lệ:** Package đã ngừng hoạt động trả `SUB-003`; phát sinh subscription hợp lệ khác trả `SUB-004`; subscription gia hạn không còn hợp lệ trả `SUB-005`; xung đột khóa hoặc version trả `CON-001`. Transaction lỗi phải rollback toàn bộ.

### 2.3.8. Đặc tả UC-06 – Admin quản lý bài tập

| Thuộc tính | Nội dung |
|---|---|
| Mục tiêu | Tạo, cập nhật và xóa mềm Exercise master data |
| Tác nhân chính | Admin |
| Tiền điều kiện | Admin đã xác thực |
| Hậu điều kiện | Danh mục Exercise được cập nhật nhưng vẫn bảo toàn lịch sử |
| Business Rule | BR-03, BR-14 |
| RESTful API | `POST /api/v1/admin/exercises`; `PUT /api/v1/admin/exercises/{id}`; `DELETE /api/v1/admin/exercises/{id}` |

**Luồng chính:** Khi tạo hoặc cập nhật, Admin cung cấp tên, nhóm cơ, kiểu chuyển động, vùng cơ thể, thiết bị, độ khó, chống chỉ định và hướng dẫn. Backend chuẩn hóa tên, kiểm tra trùng và xác thực enum. Khi xóa, Backend đặt `isActive = false` thay vì xóa vật lý; Exercise bị loại khỏi danh mục hiện hành và whitelist nhưng vẫn có thể xuất hiện trong lịch sử.

**Luồng ngoại lệ:** Exercise không tồn tại trả `EXR-001`; tên đã tồn tại sau chuẩn hóa trả `EXR-002`; metadata sai trả `VAL-001`.

### 2.3.9. Đặc tả UC-07 – Tạo lịch tập và dinh dưỡng

| Thuộc tính | Nội dung |
|---|---|
| Mục tiêu | Tạo recommendation cá nhân hóa có kiểm soát |
| Tác nhân chính | Member |
| Tác nhân phụ | AI Provider bên ngoài |
| Tiền điều kiện | Member đã đăng nhập, hồ sơ đầy đủ và có subscription hợp lệ |
| Hậu điều kiện | Lưu Workout Plan `DRAFT`, AI Recommendation và meal suggestions hợp lệ |
| Business Rule | BR-06–08, BR-09A, BR-09C, BR-10–13, BR-23, BR-25–26 |
| RESTful API | `POST /api/v1/member/recommendations`; `PATCH /api/v1/member/workout-plans/{id}/activate` |

**Luồng chính:** Backend kiểm tra tài khoản, subscription và hồ sơ; tính BMI, BMR, TDEE, calories và các chất dinh dưỡng đa lượng; tạo exercise whitelist theo thiết bị và hạn chế vận động; làm sạch dữ liệu rồi gọi AI bằng JSON Schema. AI chỉ trả `workoutSchedule` và `mealStructure`. Backend kiểm tra toàn bộ exercise ID, số ngày, số bữa, planned values và dietary constraints. Kết quả hợp lệ được lưu trong một transaction với nguồn `AI_GENERATED`.

**Luồng fallback:** Response sai schema, sai whitelist hoặc sai planned values được từ chối toàn bộ và yêu cầu tái sinh tối đa một lần. Timeout, HTTP 429 hoặc 5xx cũng được retry tối đa một lần trong ngân sách tổng 30 giây. Nếu vẫn thất bại, Backend tạo fallback đã lọc và hậu kiểm. Fallback thành công trả HTTP 200 với `AI_TIMEOUT` hoặc `AI_RESPONSE_INVALID`; fallback không an toàn trả `AI-001` và HTTP 502, không lưu dữ liệu một phần.

### 2.3.10. Đặc tả UC-08 – Ghi nhật ký tập luyện

| Thuộc tính | Nội dung |
|---|---|
| Mục tiêu | Lưu kết quả thực tế của bài tập |
| Tác nhân chính | Member |
| Tiền điều kiện | Member có subscription hợp lệ và Workout Plan `ACTIVE` |
| Hậu điều kiện | Tạo mới hoặc cập nhật Workout Log của bài tập trong ngày |
| Business Rule | BR-08, BR-09B, BR-13, BR-19, BR-25, BR-28 |
| RESTful API | `POST /api/v1/member/workout-logs` |

**Luồng chính:** Member nhập số sets, reps, RPE, mức tạ và ngày tập. Backend xác minh chi tiết giáo án thuộc plan `ACTIVE` của Member, exercise ID khớp và ngày không ở tương lai. Dữ liệu hợp lệ được lưu; nếu trùng `(member_id, log_date, exercise_id)`, hệ thống cập nhật bản ghi hiện có.

**Luồng ngoại lệ:** Actual sets ngoài 1–10, reps ngoài 1–100, RPE ngoài 1–10 hoặc mức tạ âm trả `VAL-001`. Không có subscription hợp lệ trả `SUB-001`. Tham chiếu giáo án không tồn tại hoặc không thuộc Member trả `WRK-001`.

### 2.3.11. Đặc tả UC-09 – Xem tiến độ tập luyện

| Thuộc tính | Nội dung |
|---|---|
| Mục tiêu | Cung cấp dữ liệu chuỗi thời gian về cân nặng, mức tạ và tần suất tập |
| Tác nhân chính | Member |
| Tiền điều kiện | Member đã đăng nhập; có hoặc chưa có dữ liệu lịch sử |
| Hậu điều kiện | Trả dữ liệu tiến độ chỉ thuộc Member hiện hành |
| Business Rule | BR-13, BR-22 |
| RESTful API | `POST /api/v1/member/body-progress`; `GET /api/v1/member/body-progress`; `GET /api/v1/member/workout-logs/exercises/{exerciseId}` |

**Luồng chính:** Backend lấy identity từ Principal, truy vấn Body Progress theo thời gian, tổng hợp mức tạ tối đa của bài tập theo ngày và đếm số ngày có workout log trong từng tuần ISO. Dữ liệu được sắp xếp tăng dần theo thời gian để Frontend hiển thị.

**Cập nhật Body Progress:** Một Member chỉ có một bản ghi trong mỗi `recordDate`. Khi ghi lại cùng ngày, Backend sử dụng atomic upsert dựa trên unique key `(member_id, record_date)` để cập nhật cân nặng và `updated_at`.

**Luồng ngoại lệ:** API không nhận `memberId` tùy ý từ Client. Mọi truy vấn phải ràng buộc theo Principal nhằm ngăn truy cập ngang hàng vào dữ liệu của Member khác.

### 2.3.12. Đặc tả UC-10 – Khóa hoặc mở khóa tài khoản

| Thuộc tính | Nội dung |
|---|---|
| Mục tiêu | Cho phép Admin kiểm soát trạng thái sử dụng tài khoản |
| Tác nhân chính | Admin |
| Tiền điều kiện | Admin đã đăng nhập; tài khoản đích tồn tại |
| Hậu điều kiện | `accountStatus` chuyển `LOCKED` hoặc `ACTIVE`; subscription không bị thay đổi |
| Business Rule | BR-03, BR-16, BR-21 |
| RESTful API | `PATCH /api/v1/admin/users/{id}/lock`; `PATCH /api/v1/admin/users/{id}/unlock` |

**Luồng chính:** Admin chọn tài khoản và thực hiện khóa hoặc mở khóa. Khi khóa, lý do được kiểm tra theo API contract và dùng cho nhật ký vận hành; việc thay đổi trạng thái không làm thay đổi subscription. Do JWT stateless, token cũ không bị thu hồi trực tiếp, nhưng request bảo vệ tiếp theo bị `AccountStatusGuard` từ chối nếu trạng thái là `LOCKED` hoặc `DISABLED`.

**Luồng ngoại lệ:** Người dùng chưa xác thực truy cập endpoint bảo vệ nhận `ACC-005` và HTTP 401. Tài khoản khóa nhận `ACC-004`; tài khoản vô hiệu hóa nhận `ACC-006`; thiếu quyền Admin nhận `AUTH-002`. Hết hạn gói tập không phải lý do để khóa tài khoản.

## 2.4. Yêu cầu chức năng

Hệ thống có 44 yêu cầu chức năng được phân chia theo module.

| Nhóm | Mã yêu cầu | Số lượng | Nội dung chính |
|---|---|---:|---|
| Authentication | FR-AUTH-01 đến FR-AUTH-05 | 5 | Đăng ký, đăng nhập, trạng thái tài khoản, RBAC và current user |
| Profile | FR-PROFILE-01 đến FR-PROFILE-04 | 4 | Xem và cập nhật hồ sơ, mục tiêu, dữ liệu dinh dưỡng |
| Subscription | FR-SUB-01 đến FR-SUB-09 | 9 | Package, đăng ký, phê duyệt, hiệu lực, gia hạn và hủy |
| Exercise | FR-EXR-01 đến FR-EXR-06 | 6 | CRUD, soft delete, tìm kiếm và whitelist |
| Workout | FR-WORKOUT-01 đến FR-WORKOUT-07 | 7 | Recommendation, fallback, plan, activation và log |
| Nutrition | FR-NUTRITION-01 đến FR-NUTRITION-06 | 6 | BMI, BMR, TDEE, macro và meal structure |
| Progress | FR-PROGRESS-01 đến FR-PROGRESS-04 | 4 | Cân nặng, cập nhật cùng ngày và dữ liệu biểu đồ |
| Administration | FR-ADMIN-01 đến FR-ADMIN-03 | 3 | Danh sách user, trạng thái tài khoản và thống kê cơ bản |
| **Tổng cộng** | | **44** | |

## 2.5. Phân tích quy trình nghiệp vụ chính

### 2.5.1. Đăng ký và phê duyệt gói tập

Member lựa chọn package đang hoạt động và gửi yêu cầu đăng ký. Trước khi tạo bản ghi, hệ thống kiểm tra Member có subscription hợp lệ hoặc yêu cầu `PENDING` hay không. Nếu đủ điều kiện, subscription được tạo ở trạng thái `PENDING` cùng snapshot tên, thời hạn và giá package.

Khi Admin phê duyệt, Service khóa phạm vi subscription của Member, chuẩn hóa bản ghi `ACTIVE` đã hết hiệu lực sang `EXPIRED`, rồi kích hoạt request mới trong cùng transaction. Thiết kế này kết hợp kiểm tra tại Service với unique constraint ở Database để chống hai subscription `ACTIVE` phát sinh đồng thời.

### 2.5.2. Gia hạn gói tập

Yêu cầu gia hạn được lưu riêng tại `subscription_renewal_requests`. Một subscription chỉ có tối đa một renewal `PENDING`. Khi được xử lý, ngày kết thúc mới bằng ngày kết thúc hiện tại cộng thời hạn package. Subscription đang hoạt động được cập nhật tại chỗ; hệ thống không tạo thêm subscription `ACTIVE` song song.

### 2.5.3. Tạo AI Recommendation

Quá trình tạo recommendation gồm kiểm tra tài khoản, kiểm tra subscription, xác thực hồ sơ, tính chỉ số tại Backend, tạo exercise whitelist, làm sạch prompt, gọi AI, hậu kiểm và lưu dữ liệu. AI response và fallback đều đi qua cùng validator. Workout Plan, AI Recommendation và meal suggestions được lưu trong cùng transaction để tránh dữ liệu một phần.

### 2.5.4. Ghi nhật ký và theo dõi tiến độ

Member chỉ được ghi log cho bài tập thuộc plan `ACTIVE` của mình và phải có subscription hợp lệ. Quyền xem dữ liệu lịch sử không bị chặn khi subscription hết hạn. Workout Log và Body Progress áp dụng update-in-place theo khóa nghiệp vụ để dữ liệu chuỗi thời gian không bị nhân bản.

## 2.6. Quy tắc nghiệp vụ

Các quy tắc nghiệp vụ xác định điều kiện mà dữ liệu và luồng xử lý phải tuân thủ, độc lập với giao diện người dùng. Mỗi quy tắc được truy vết xuống Service, Database Constraint hoặc Test Case tương ứng. Bảng sau tóm lược toàn bộ bộ quy tắc đã chốt.

| Mã | Nội dung áp dụng |
|---|---|
| BR-01 | Email phải duy nhất sau khi loại bỏ khoảng trắng ở hai đầu và chuyển thành chữ thường. |
| BR-02 | Mật khẩu chỉ được lưu dưới dạng BCrypt hash, không lưu văn bản thuần. |
| BR-03 | Chức năng quản trị chỉ dành cho tài khoản có `ROLE_ADMIN`. |
| BR-04 | Mỗi Member có tối đa một subscription `ACTIVE` và một yêu cầu đăng ký mới `PENDING`. |
| BR-05 | Package đã ngừng hoạt động không được dùng để đăng ký mới hoặc gia hạn. |
| BR-06 | Workout Plan do AI hoặc fallback tạo phải có ít nhất một ngày tập. |
| BR-07 | Mỗi Workout Day phải có ít nhất một Exercise. |
| BR-08 | `plannedRpe` nằm trong 6–9, còn `actualRpe` nằm trong 1–10. |
| BR-09A | Thông số kế hoạch gồm sets 1–5, reps 1–30, RPE 6–9 và thời gian nghỉ 30–300 giây. |
| BR-09B | Nhật ký thực tế gồm sets 1–10, reps 1–100, RPE 1–10 và mức tạ không âm. |
| BR-09C | BMI, BMR, TDEE, calories và macro do Backend tính; AI không được quyết định các giá trị này. |
| BR-10 | Mọi Exercise do AI đề xuất phải thuộc whitelist; một ngày tập không được lặp Exercise. |
| BR-11 | AI lỗi hoặc trả dữ liệu không hợp lệ phải chuyển sang fallback đã qua cùng bộ hậu kiểm. |
| BR-12 | Đề xuất AI phải kèm miễn trừ trách nhiệm y tế và chỉ có giá trị tham khảo. |
| BR-13 | Member chỉ được truy cập hồ sơ, nhật ký và tiến độ thuộc tài khoản của mình. |
| BR-14 | Exercise được xóa mềm bằng `isActive = false` để giữ dữ liệu lịch sử. |
| BR-15 | Tài khoản đăng ký công khai được tạo ở trạng thái `ACTIVE` nhưng chưa có subscription. |
| BR-16 | Tài khoản `LOCKED` không được đăng nhập hoặc sử dụng endpoint bảo vệ. |
| BR-17 | Dọn tài khoản chờ xác thực quá 24 giờ chỉ áp dụng nếu bổ sung xác nhận email trong phạm vi Should-have. |
| BR-18 | Mật khẩu dài 8–72 ký tự, có ít nhất một chữ hoa và một chữ số, không có khoảng trắng ở biên. |
| BR-19 | Workout Log trùng `(memberId, logDate, exerciseId)` được cập nhật tại chỗ. |
| BR-20 | Email phải được chuẩn hóa trước mọi thao tác đăng ký, đăng nhập và tìm kiếm. |
| BR-21 | Tài khoản `DISABLED` bị từ chối đăng nhập và mọi request yêu cầu xác thực. |
| BR-22 | Mỗi Member chỉ có một Body Progress trong một ngày; ghi lại cùng ngày dùng atomic upsert. |
| BR-23 | Hồ sơ thể trạng, enum và collection phải vượt qua validation trước khi lưu hoặc gửi sang AI. |
| BR-24 | Gia hạn dùng Renewal Request `PENDING` và cập nhật `endDate`, không tạo subscription `ACTIVE` mới. |
| BR-25 | Subscription chỉ hợp lệ khi `status = ACTIVE` và `startDate <= currentDate < endDate`. |
| BR-26 | Workout Plan chuyển theo `DRAFT → ACTIVE → ARCHIVED`; mỗi Member chỉ có một plan `ACTIVE`. |
| BR-27 | Package phải thỏa quy tắc về tên, thời hạn, giá, mô tả và trạng thái hoạt động. |
| BR-28 | Workout Log chỉ được tham chiếu Exercise thuộc plan `ACTIVE` của chính Member; ngày log không ở tương lai. |

Các giới hạn định lượng được bảo vệ ở nhiều tầng. DTO từ chối request sai ngay tại biên API; Service kiểm tra trạng thái, quyền sở hữu và quan hệ giữa các tài nguyên; Database dùng CHECK, UNIQUE và FOREIGN KEY làm lớp bảo vệ cuối. Việc lặp lại có chủ đích này giảm khả năng dữ liệu không hợp lệ lọt vào hệ thống khi có request đồng thời hoặc khi một luồng Service bị gọi sai cách.

## 2.7. Yêu cầu phi chức năng

| Mã | Yêu cầu chính |
|---|---|
| NFR-01 | API nội bộ phản hồi không quá 800 ms trong môi trường demo |
| NFR-02 | Endpoint AI có tổng ngân sách tối đa 30 giây |
| NFR-03 | Tính toán sinh học hoàn thành trong 50 ms |
| NFR-04 | Circuit Breaker dùng cửa sổ 5, ngưỡng lỗi 50%, trạng thái OPEN 60 giây |
| NFR-05 | Ghi nhiều bảng phải nằm trong một transaction |
| NFR-06 | JWT secret tối thiểu 32 byte và lấy từ environment |
| NFR-07 | Tài nguyên cá nhân được truy vấn theo Principal |
| NFR-08 | Dữ liệu gửi AI phải qua allowlist và sanitizer |
| NFR-09 | Hệ thống có thể khởi chạy bằng `docker compose up --build` |
| NFR-10 | Có dữ liệu mẫu đủ cho luồng demo |
| NFR-11 | Backend tuân thủ Modular Layered Monolith |
| NFR-12 | RESTful API được mô tả bằng SpringDoc OpenAPI |
| NFR-13 | Mỗi lần gọi AI tối đa 15 giây và retry tối đa một lần |
| NFR-14 | Không ghi password, token, secret hoặc hồ sơ nhạy cảm vào log |

## 2.8. Ma trận truy vết yêu cầu

Mỗi yêu cầu chức năng được liên kết với Use Case, Business Rule, API và bảng dữ liệu liên quan. Toàn bộ 44 FR đã được đối chiếu trong hồ sơ đặc tả API để kiểm tra một yêu cầu đã được phân tích, thiết kế và chuẩn bị tiêu chí kiểm thử hay chưa. Bảng sau trình bày các liên kết tiêu biểu:

| Use Case | FR chính | BR tiêu biểu | API | Nhóm dữ liệu |
|---|---|---|---|---|
| UC-01 | FR-AUTH-01 | BR-01, 02, 15, 18, 20 | Register | Auth |
| UC-03 | FR-PROFILE-02–04 | BR-13, 22, 23 | Member Profile | Profile, Progress |
| UC-04–05 | FR-SUB-04–08 | BR-04, 05, 24, 25 | Membership | Membership |
| UC-07 | FR-EXR-06, FR-WORKOUT-01–05, FR-NUTRITION-01–06 | BR-09A, 09C, 10–12, 25–26 | Recommendation | Exercise, Workout, AI |
| UC-08–09 | FR-WORKOUT-06–07, FR-PROGRESS-01–04 | BR-09B, 13, 19, 22, 28 | Workout Log, Progress | Workout, Progress |
| UC-10 | FR-AUTH-03, FR-ADMIN-02 | BR-03, 16, 21 | Admin User Status | Auth |

# CHƯƠNG 3. THIẾT KẾ HỆ THỐNG

## 3.1. Thiết kế kiến trúc tổng thể

Hệ thống gồm React Frontend, Spring Boot Backend, MySQL Database và AI Provider bên ngoài. Frontend gửi yêu cầu HTTP theo hợp đồng RESTful API, kèm Bearer JWT khi truy cập tài nguyên bảo vệ. Backend xử lý bảo mật, nghiệp vụ và truy cập dữ liệu. AI Provider chỉ nhận dữ liệu đã được giới hạn và không kết nối trực tiếp với Database.

Kiến trúc Modular Layered Monolith được lựa chọn vì phù hợp với phạm vi 9 tuần, 32 API contract và các transaction liên quan nhiều bảng. Các module cùng chạy trong một ứng dụng Spring Boot nhưng được phân chia theo miền nghiệp vụ gồm `auth`, `member`, `membership`, `exercise`, `recommendation`, `workout` và `progress`. Package `common` chỉ chứa thành phần kỹ thuật dùng chung.

Controller tiếp nhận request, thực hiện validation đầu vào, lấy Principal và trả DTO. Service sở hữu nghiệp vụ, transaction và kiểm tra quyền sở hữu. Repository thực hiện truy vấn dữ liệu. Policy, Guard và Validator được sử dụng cho các quy tắc cần tái sử dụng. Adapter chỉ xuất hiện tại ranh giới AI Provider.

### 3.1.1. Đánh giá lựa chọn kiến trúc

Ưu điểm của Modular Layered Monolith là quy trình build và triển khai đơn giản, transaction không phải đi qua mạng và cấu trúc module vẫn giữ được ranh giới nghiệp vụ. Hạn chế là các module dùng chung một tiến trình và cơ sở dữ liệu, nên cần kiểm soát phụ thuộc package để tránh liên kết chéo. Với quy mô MVP, lựa chọn này phù hợp hơn microservice, CQRS hoặc Event Sourcing.

## 3.2. Thiết kế cơ sở dữ liệu vật lý

### 3.2.1. Nguyên tắc thiết kế

Database sử dụng MySQL 8 với InnoDB, `utf8mb4` và `utf8mb4_unicode_ci`. Tên bảng, cột, constraint và index dùng `snake_case`; thuộc tính Java dùng `camelCase`. Bảng Entity nghiệp vụ dùng khóa chính `BIGINT AUTO_INCREMENT`; bảng liên kết và collection dùng khóa ghép có ý nghĩa nghiệp vụ.

Enum được lưu bằng `VARCHAR` và ánh xạ bằng `EnumType.STRING`. Audit timestamp sử dụng `TIMESTAMP(6)` theo UTC; ngày nghiệp vụ dùng SQL `DATE` được xác định theo `Asia/Ho_Chi_Minh`. Cả 25 bảng đều có `created_at` và `updated_at`.

### 3.2.2. Phân rã Flyway Migration

| Migration | Nội dung | Số bảng |
|---|---|---:|
| V1 | `users`, `roles`, `user_roles` | 3 |
| V2 | Member Profile và năm collection tables | 6 |
| V3 | Package, subscription, renewal request | 3 |
| V4 | Exercise và bốn collection tables | 5 |
| V5 | Workout plan, day, detail, session và log | 5 |
| V6 | AI recommendation và meal suggestion | 2 |
| V7 | Body Progress | 1 |
| V8 | Seed `ROLE_ADMIN`, `ROLE_MEMBER`, `ROLE_PT` | 0 |
| **Tổng cộng** | | **25** |

Flyway là nguồn sở hữu DDL duy nhất. Migration đã áp dụng không được sửa; thay đổi schema phải tạo migration mới. Hibernate chỉ chạy `validate` để phát hiện sai lệch giữa Entity và schema.

### 3.2.3. Danh sách 25 bảng vật lý

| Phân hệ | Bảng |
|---|---|
| Auth | `users`, `roles`, `user_roles` |
| Profile | `member_profiles`, `member_available_equipment`, `member_target_muscle_groups`, `member_injury_constraints`, `member_food_allergies`, `member_excluded_foods` |
| Membership | `membership_packages`, `member_subscriptions`, `subscription_renewal_requests` |
| Exercise | `exercises`, `exercise_secondary_muscles`, `exercise_equipment`, `exercise_target_body_regions`, `exercise_contraindication_tags` |
| Workout Plan | `workout_plans`, `workout_days`, `workout_plan_exercises` |
| Workout Log | `workout_sessions`, `workout_logs` |
| Progress | `body_progress` |
| AI/Nutrition | `ai_recommendations`, `nutrition_meal_suggestions` |

### 3.2.4. Quan hệ và lực lượng kết hợp

| Nhóm quan hệ | Cardinality | Ý nghĩa thiết kế |
|---|---|---|
| `users` – `member_profiles` | `1 – 0..1` | Một User có thể chưa hoàn thiện hồ sơ; mỗi Member Profile thuộc đúng một User. |
| `users` – `user_roles` – `roles` | `1 – 0..N – 1` | `user_roles` phân rã quan hệ nhiều-nhiều giữa User và Role, đồng thời lưu auditing. |
| `member_profiles` – năm bảng collection | `1 – 0..N` | Thiết bị, nhóm cơ, hạn chế vận động, dị ứng và thực phẩm loại trừ phụ thuộc hoàn toàn vào hồ sơ. |
| `users`/`membership_packages` – `member_subscriptions` | `1 – 0..N` | Mỗi Subscription thuộc một Member và một Package; một Member có thể có nhiều bản ghi theo lịch sử nhưng chỉ một bản hợp lệ tại một thời điểm. |
| `users` – `member_subscriptions` qua người duyệt/người hủy | `0..1 – 0..N` | Người duyệt hoặc hủy là Admin tùy chọn trên mỗi Subscription; một Admin có thể xử lý nhiều bản ghi. |
| `member_subscriptions` – `subscription_renewal_requests` | `1 – 0..N` | Một Subscription có thể được gia hạn nhiều lần theo lịch sử nhưng chỉ có một Renewal Request `PENDING` tại một thời điểm. |
| `exercises` – bốn bảng metadata | `1 – 0..N` | Nhóm cơ phụ, thiết bị, vùng cơ thể và chống chỉ định là các tập giá trị phụ thuộc Exercise. |
| `users` – `workout_plans` – `workout_days` | `1 – 0..N – 1..N` | Member có nhiều giáo án theo lịch sử; giáo án hợp lệ do AI hoặc fallback tạo phải có ít nhất một ngày tập. |
| `workout_days` – `workout_plan_exercises` – `exercises` | `1 – 1..N – 1` | Mỗi ngày có ít nhất một chi tiết bài tập; mỗi chi tiết tham chiếu đúng một Exercise. |
| `users`/`workout_days` – `workout_sessions` | `1 – 0..N` | Mỗi buổi tập thực tế thuộc một Member và được lập từ một Workout Day. |
| `workout_sessions` – `workout_logs` | `1 – 1..N` | Một Session chứa các log thực tế; composite foreign key giữ `member_id` và ngày log khớp Session. |
| `users` – `body_progress` | `1 – 0..N` | Member có chuỗi dữ liệu thể trạng theo ngày, duy nhất theo `(member_id, record_date)`. |
| `users`/`workout_plans` – `ai_recommendations` | `1 – 0..N`; `1 – 0..1` | Member có thể tạo nhiều recommendation; một Workout Plan có tối đa một AI Recommendation do `workout_plan_id` là unique. |
| `ai_recommendations` – `nutrition_meal_suggestions` | `1 – 1..N` | Recommendation hợp lệ phải có số bữa đúng `mealsPerDay`; mỗi bữa thuộc đúng một recommendation. |

Các lực lượng `1..N` đối với Workout Day, Workout Plan Exercise, Workout Log và Meal Suggestion là ràng buộc nghiệp vụ được kiểm tra tại Service hoặc Validator. Khóa ngoại chỉ bảo đảm chiều bản ghi con thuộc về một bản ghi cha, không tự bảo đảm bản ghi cha luôn có ít nhất một bản ghi con.

### 3.2.5. Ràng buộc toàn vẹn quan trọng

Generated column kết hợp unique constraint bảo đảm mỗi Member có tối đa một subscription `ACTIVE`, một yêu cầu đăng ký mới `PENDING` và một Workout Plan `ACTIVE`. Renewal Request có unique key để giới hạn một request `PENDING` trên mỗi subscription.

`body_progress` có unique key `(member_id, record_date)`. `workout_logs` có unique key `(member_id, log_date, exercise_id)`. Composite foreign key giữa Workout Log và Workout Session bảo đảm Member và ngày log không lệch khỏi session sở hữu.

Check constraint bảo vệ enum, trạng thái và giới hạn dữ liệu. Các bảng dễ xung đột gồm `member_subscriptions`, `subscription_renewal_requests` và `workout_plans` có cột `version` phục vụ Optimistic Locking.

Thiết kế vật lý đã được đối chiếu với metadata MySQL 8, ghi nhận đúng 25 bảng, 54 CHECK constraints, 34 khóa ngoại và 18 unique constraints. Các số liệu này được dùng làm baseline để kiểm tra Flyway Migration và ORM Mapping.

### 3.2.6. Bảo toàn lịch sử (History Retention)

User có dữ liệu nghiệp vụ không bị xóa cứng mà chuyển sang `LOCKED` hoặc `DISABLED`. Package và Exercise được vô hiệu hóa bằng `is_active = false`. Workout Plan cũ chuyển sang `ARCHIVED`. Khóa ngoại đến dữ liệu lịch sử dùng `ON DELETE RESTRICT`; `ON DELETE CASCADE` chỉ áp dụng cho bảng liên kết hoặc collection không tồn tại độc lập.

Thiết kế này giữ được subscription, workout plan, workout log, Body Progress và recommendation đã phát sinh. Đổi lại, truy vấn danh mục hiện hành và truy vấn lịch sử cần được tách rõ, đặc biệt khi đọc Exercise đã bị xóa mềm.

## 3.3. Thiết kế ORM Mapping

### 3.3.1. BaseEntity và JPA Auditing

Thiết kế ORM xác định 16 bảng nghiệp vụ được ánh xạ thành Entity kế thừa `BaseEntity` để quản lý `createdAt` và `updatedAt`. `@CreatedDate` ghi thời điểm tạo; `@LastModifiedDate` ghi thời điểm cập nhật. Chín bảng collection không có vòng đời độc lập được thiết kế bằng `@ElementCollection`; timestamp của các bảng này do MySQL điền trong cùng transaction. Phạm vi đã hiện thực tại Ngày 4 mới gồm các Entity thuộc phân hệ Auth và được trình bày riêng ở Chương 4.

### 3.3.2. Danh sách Entity và Element Collection

Danh sách thiết kế gồm 16 Entity: `User`, `Role`, `UserRole`, `MemberProfile`, `MembershipPackage`, `MemberSubscription`, `SubscriptionRenewalRequest`, `Exercise`, `WorkoutPlan`, `WorkoutDay`, `WorkoutPlanExercise`, `WorkoutSession`, `WorkoutLog`, `BodyProgress`, `AiRecommendation` và `NutritionMealSuggestion`.

Chín collection tables gồm năm bảng của Member Profile và bốn bảng metadata của Exercise. Cách ánh xạ này phù hợp với các tập giá trị không có vòng đời độc lập, đồng thời giữ schema chuẩn hóa để lọc theo thiết bị, nhóm cơ và chống chỉ định.

### 3.3.3. Mapping phân hệ Auth

`UserRole` được xây dựng thành Entity riêng thay vì dùng `@ManyToMany` trực tiếp vì bảng `user_roles` có auditing và có khả năng bổ sung metadata cấp quyền trong tương lai. `UserRoleId` là khóa phức hợp gồm `userId` và `roleId`, được ánh xạ bằng `@EmbeddedId` cùng `@MapsId`.

Các quan hệ ưu tiên `FetchType.LAZY`. Riêng luồng Security cần User cùng Roles nên `UserRepository` dùng fetch join để nạp dữ liệu trong một truy vấn, tránh vấn đề truy vấn N+1 (N+1 Select Problem).

### 3.3.4. Fetch, cascade và soft delete

Cascade chỉ được dùng khi vòng đời bản ghi con phụ thuộc hoàn toàn vào bản ghi cha. Dữ liệu lịch sử không dùng cascade remove từ User, Package hoặc Exercise. Exercise và Membership Package dùng soft delete; truy vấn lịch sử cần đọc bản ghi inactive bằng DTO projection phù hợp thay vì phụ thuộc vào filter của danh mục hiện hành.

## 3.4. Thiết kế RESTful API

### 3.4.1. Quy ước chung

Toàn bộ endpoint sử dụng prefix `/api/v1`, trao đổi `application/json` và xác thực bằng Bearer JWT. Timestamp trả theo UTC ISO-8601; ngày nghiệp vụ dùng `yyyy-MM-dd`. Tham số phân trang bắt đầu từ `page = 0`, còn `size` nằm trong khoảng 1–100.

Thiết kế sử dụng `GET` để truy vấn, `POST` để tạo tài nguyên hoặc khởi phát nghiệp vụ, `PUT` để cập nhật đầy đủ, `PATCH` để chuyển trạng thái và `DELETE` cho thao tác vô hiệu hóa tài nguyên. Các endpoint trạng thái như `approve`, `activate`, `lock` và `unlock` phản ánh cách tiếp cận RESTful thực dụng đối với nghiệp vụ có lệnh chuyển trạng thái rõ ràng.

### 3.4.2. Phân nhóm 32 API

| Nhóm | Số API |
|---|---:|
| Auth | 3 |
| Member Profile | 2 |
| Membership | 9 |
| Exercise | 5 |
| Recommendation | 2 |
| Workout Plan | 2 |
| Workout Log | 3 |
| Body Progress | 2 |
| Admin | 4 |
| **Tổng cộng** | **32** |

### 3.4.3. Định dạng phản hồi và mã lỗi

Hợp đồng API quy định phản hồi thành công bằng `ApiResponse<T>`, lỗi bằng `ErrorResponse` và dữ liệu phân trang bằng `PageResponse<T>`. Controller chỉ nhận và trả DTO, không serialize JPA Entity. Registry hiện có 20 mã lỗi và hai mã cảnh báo fallback là `AI_TIMEOUT`, `AI_RESPONSE_INVALID`.

HTTP 400 dành cho validation; 401 cho thiếu hoặc sai thông tin xác thực; 403 cho thiếu quyền, trạng thái tài khoản hoặc điều kiện subscription; 404 cho tài nguyên không tồn tại; 409 cho trùng dữ liệu và xung đột trạng thái. Error Code Registry được giữ ổn định để Frontend xử lý theo mã thay vì phụ thuộc vào nội dung thông báo.

## 3.5. Thiết kế bảo mật

Hệ thống sử dụng Spring Security 6 và JWT stateless. `JwtAuthenticationFilter` xác minh chữ ký, hạn dùng và nạp identity cùng Roles. Filter không quyết định `accountStatus` và không kiểm tra subscription.

`AccountStatusGuard` đọc trạng thái hiện hành của tài khoản. Tài khoản `LOCKED` hoặc `DISABLED` bị từ chối ngay cả khi token được cấp trước đó vẫn còn hạn. RBAC kiểm tra quyền Admin hoặc Member; Service tiếp tục kiểm tra ownership trong truy vấn nghiệp vụ.

`SubscriptionGuard` chỉ áp dụng cho tạo recommendation, kích hoạt giáo án và ghi workout log mới. Điều kiện là `status = ACTIVE` và `startDate <= currentDate < endDate`. Member hết hạn gói vẫn được đăng nhập, xem dữ liệu lịch sử và gửi yêu cầu gia hạn.

JWT secret phải có tối thiểu 32 byte và được lấy từ environment. Token chỉ chứa subject là email, danh sách roles, thời điểm phát hành và hết hạn; không chứa password, dữ liệu thể trạng hoặc recommendation.

Phản hồi bảo mật được chuẩn hóa: token thiếu, sai hoặc hết hạn trả `ACC-005` và HTTP 401; không đủ role trả `AUTH-002`; tài khoản `LOCKED` trả `ACC-004`; tài khoản `DISABLED` trả `ACC-006` và HTTP 403.

## 3.6. Thiết kế trạng thái và kiểm soát đồng thời

### 3.6.1. Vòng đời Subscription

Subscription có bốn trạng thái `PENDING`, `ACTIVE`, `EXPIRED` và `CANCELLED`. Yêu cầu mới bắt đầu từ `PENDING`; sau khi Admin phê duyệt chuyển sang `ACTIVE`. Bản ghi hết hiệu lực chuyển sang `EXPIRED`. Yêu cầu chưa duyệt hoặc subscription đang hoạt động có thể chuyển sang `CANCELLED` theo nghiệp vụ.

Renewal Request có hai trạng thái `PENDING` và `PROCESSED`. Duyệt gia hạn chỉ cập nhật `endDate` của subscription hiện hành, không tạo subscription `ACTIVE` mới.

### 3.6.2. Vòng đời Workout Plan

Workout Plan có ba trạng thái `DRAFT`, `ACTIVE` và `ARCHIVED`. Recommendation tạo plan `DRAFT`. Khi Member kích hoạt plan mới, plan `ACTIVE` trước đó được chuyển sang `ARCHIVED` trong cùng transaction. Unique generated key ở Database bảo đảm chỉ có một plan `ACTIVE` trên mỗi Member.

### 3.6.3. Transaction và cơ chế khóa lạc quan (Optimistic Locking)

Cơ chế khóa lạc quan phát hiện hai request cùng cập nhật một bản ghi dựa trên cột `version`. Với transition liên quan nhiều dòng, Service kết hợp transaction, khóa theo phạm vi Member và unique constraint. Xung đột trả `CON-001` cùng HTTP 409; Client phải tải lại trạng thái trước khi thử lại.

Ưu điểm của cách kết hợp này là vừa phát hiện stale update ở tầng ORM, vừa có lớp bảo vệ cuối tại Database. Hạn chế là Service cần xác định thứ tự lấy khóa nhất quán để giảm nguy cơ chờ khóa kéo dài.

## 3.7. Thiết kế cấu trúc và điều hướng giao diện Web

Frontend chưa được hiện thực ở giai đoạn Ngày 1–5, vì vậy nội dung dưới đây là cấu trúc thông tin logic được suy ra trực tiếp từ phạm vi MVP, ma trận phân quyền và 32 API contract. Thiết kế không ấn định URL hoặc tên component React khi mã nguồn Frontend chưa tồn tại.

### 3.7.1. Phân vùng chức năng

| Phân vùng | Màn hình logic | Chức năng và dữ liệu chính |
|---|---|---|
| Công khai | Thông tin chung, danh sách gói tập, đăng ký, đăng nhập | Xem Package đang hoạt động; tạo tài khoản `ROLE_MEMBER`; nhận JWT khi đăng nhập. |
| Member | Tổng quan cá nhân, hồ sơ thể trạng, gói tập hiện hành, thư viện bài tập, đề xuất AI, giáo án hiện hành, nhật ký tập luyện, tiến độ | Sử dụng dữ liệu của chính Member; các chức năng cao cấp chịu kiểm tra Subscription. |
| Admin | Số liệu tổng hợp, tài khoản, gói tập, yêu cầu Subscription, thư viện bài tập | Quản lý master data, phê duyệt/hủy Subscription và khóa/mở khóa tài khoản. |

Không tạo phân vùng PT trong Sitemap của MVP. `ROLE_PT` chỉ tồn tại trong RBAC để chuẩn bị mở rộng; việc đưa màn hình PT vào sơ đồ hiện tại sẽ làm sai phạm vi đã chốt.

### 3.7.2. Luồng người dùng trọng tâm

**Luồng khởi tạo và sử dụng của Member:** Anonymous Guest xem gói tập hoặc đăng ký tài khoản, sau đó đăng nhập và hoàn thiện hồ sơ thể trạng. Member chọn Package, gửi yêu cầu đăng ký và theo dõi trạng thái `PENDING`. Sau khi Admin phê duyệt, Subscription chuyển sang `ACTIVE`; Member có thể yêu cầu recommendation, xem Workout Plan `DRAFT`, kích hoạt giáo án, ghi Workout Log và xem tiến độ. Luồng này kết nối UC-01, UC-02, UC-03, UC-04, UC-05, UC-07, UC-08 và UC-09.

**Luồng gia hạn:** Member đang có Subscription hợp lệ gửi Renewal Request. Admin xử lý request bằng nhánh `requestType = RENEWAL`; hệ thống cộng `durationDays` vào `endDate` hiện hành và chuyển request sang `PROCESSED`, không tạo thêm Subscription `ACTIVE`.

**Luồng quản trị:** Admin đăng nhập, quản lý Package và Exercise, xử lý yêu cầu Subscription, xem danh sách User và thay đổi trạng thái tài khoản. Khi tài khoản bị khóa, Subscription không thay đổi; request bảo vệ tiếp theo của tài khoản đó bị `AccountStatusGuard` từ chối.

### 3.7.3. Quy tắc điều hướng và trạng thái giao diện

- Người chưa xác thực chỉ truy cập phân vùng công khai. Request tới tài nguyên bảo vệ không có token hợp lệ nhận HTTP 401 với `ACC-005`.
- Member và Admin sử dụng layout riêng theo role. Truy cập sai phân vùng nhận HTTP 403 với `AUTH-002`; giao diện không chỉ dựa vào việc ẩn menu mà phải xử lý response từ Backend.
- Tài khoản `LOCKED` hoặc `DISABLED` không được tiếp tục dùng màn hình bảo vệ, kể cả khi JWT cũ còn hạn.
- Không có Subscription hợp lệ chỉ chặn tạo recommendation, kích hoạt giáo án và ghi log mới. Member vẫn được xem lịch sử, cập nhật hồ sơ và thực hiện luồng đăng ký hoặc gia hạn phù hợp.
- Màn hình Recommendation phải có trạng thái đang xử lý tối đa theo ngân sách 30 giây, phân biệt nguồn `AI_GENERATED` và `FALLBACK_TEMPLATE`, hiển thị `warningCode` khi fallback và luôn hiển thị miễn trừ trách nhiệm y tế.
- Các màn hình danh sách phải có trạng thái tải dữ liệu, rỗng, validation, không có quyền và lỗi hệ thống; phân trang tuân theo `page = 0`, `size = 1–100` của API contract.

## 3.8. Thiết kế miền Member Profile

### 3.8.1. Phạm vi dữ liệu hồ sơ

`MemberProfile` là aggregate lưu dữ liệu nền để cá nhân hóa lịch tập và dinh dưỡng. Phần thông tin sinh học gồm giới tính, ngày sinh, chiều cao và cân nặng. Phần tập luyện gồm mục tiêu, trình độ, mức vận động, số buổi mỗi tuần, thời lượng tối đa, thiết bị sẵn có, nhóm cơ ưu tiên và hạn chế vận động. Phần dinh dưỡng gồm chế độ ăn, số bữa mỗi ngày, thực phẩm gây dị ứng và thực phẩm loại trừ.

Ngày 12 chỉ triển khai persistence và API đọc. Các chỉ số BMI, BMR, TDEE, calories và chất dinh dưỡng đa lượng chưa được ghép vào response vì thuộc Calculator Ngày 13. Member mới chưa có hồ sơ là trạng thái hợp lệ; hệ thống không tự sinh dữ liệu thể trạng giả.

### 3.8.2. Ánh xạ Entity và năm collection table

Một User có tối đa một `MemberProfile`, được bảo vệ bằng unique constraint `uk_member_profiles_user(user_id)`. Quan hệ JPA dùng `@OneToOne(fetch = LAZY)` và không cascade remove User. Entity kế thừa `BaseEntity` để sử dụng timestamp auditing.

Năm tập giá trị không có vòng đời độc lập được ánh xạ bằng `@ElementCollection`: `member_available_equipment`, `member_target_muscle_groups`, `member_injury_constraints`, `member_food_allergies` và `member_excluded_foods`. Các collection dùng `Set` để loại phần tử trùng; enum được lưu bằng chuỗi để giá trị Java khớp trực tiếp với CHECK constraint trong migration V2. Thiết kế này không thêm Entity không cần thiết và vẫn cho phép Database lọc theo thiết bị, nhóm cơ hoặc chống chỉ định.

### 3.8.3. Ownership dựa trên Principal

Endpoint hồ sơ không nhận `memberId` từ path, query hoặc request body. `MemberProfileService` lấy User ID từ `AuthenticatedUserPrincipal` trong Security Context, gọi `AccountStatusGuard` trước nghiệp vụ rồi truy vấn profile bằng chính ID đó. Cách thực hiện này ngăn Client thay ID để đọc hồ sơ của người khác và giữ kiểm tra ownership tại Backend.

`/api/v1/member/**` yêu cầu `ROLE_MEMBER`. Token thiếu, sai hoặc hết hạn trả `ACC-005`; tài khoản `LOCKED` hoặc `DISABLED` bị Guard chặn bằng `ACC-004` hoặc `ACC-006`; role không phù hợp trả `AUTH-002`. Khi profile chưa tồn tại, Backend trả HTTP 404 với `PROF-001` thay vì trả object rỗng hoặc tạo dữ liệu giả.

# CHƯƠNG 4. TRIỂN KHAI HỆ THỐNG

## 4.1. Công nghệ và môi trường triển khai

Backend sử dụng Java 21 và Spring Boot 3.4.3. Các dependency chính đã cấu hình gồm Spring Web, Spring Data JPA, Spring Validation, Spring Security, Spring Boot Actuator, MySQL Connector, Flyway Core, Flyway MySQL, SpringDoc OpenAPI 2.8.8, Lombok và JJWT 0.12.6. Kiểm thử sử dụng JUnit 5, Mockito, Spring Security Test và MockMvc thông qua Spring Boot Test.

Frontend sử dụng React, TypeScript, React Router, TanStack Query và Axios. M1 đã hoàn thành Auth/RBAC shell; Ngày 12 bổ sung Member layout và Profile shell desktop/laptop. Docker và Resilience4j vẫn thuộc các milestone tiếp theo.

### 4.1.1. Phạm vi hiện thực đến hết Ngày 5 (27/07/2026)

| Hạng mục | Trạng thái | Minh chứng hiện có |
|---|---|---|
| Đặc tả yêu cầu và API | Đã hoàn thành thiết kế | 44 FR, 14 NFR, 10 Use Case, 34 Acceptance Criteria và 32 API contract |
| Cơ sở dữ liệu vật lý | Đã hiện thực | Tám Flyway Migration tạo 25 bảng và seed ba role hệ thống |
| ORM | Hiện thực một phần | `BaseEntity`, hai Enum, ba Entity Auth, một Embeddable ID và hai Repository |
| Security/JWT | Đã hoàn thành phần nền | Security Filter Chain, JWT, UserDetails, Account Status Guard và phản hồi 401/403 |
| RESTful API nghiệp vụ | Chưa hiện thực | Chưa có Register, Login, `/users/me` hoặc controller nghiệp vụ |
| Frontend, AI và Docker | Chưa hiện thực | Thuộc các milestone tiếp theo |

Bảng trạng thái trên được dùng để phân biệt kết quả đã có trong mã nguồn với nội dung mới dừng ở mức yêu cầu hoặc thiết kế. Các chức năng chưa hiện thực không được sử dụng làm kết quả đánh giá của giai đoạn này.

## 4.2. Triển khai Persistence Layer

### 4.2.1. Cấu hình Database và Flyway

Datasource nhận thông tin kết nối từ `DB_URL` hoặc các biến `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` và `DB_PASSWORD`. Mật khẩu không có fallback trong source code. Kết nối đặt timezone UTC; Hibernate dùng `ddl-auto=validate` và `open-in-view=false`.

Flyway được bật tại `classpath:db/migration`. Tám migration V1–V8 đã tạo đủ 25 bảng vật lý và seed ba role hệ thống. Các migration đã được chạy thành công trên MySQL 8.0.44; Hibernate khởi tạo `EntityManagerFactory` với schema hiện hành.

### 4.2.2. JPA Auditing và ORM Auth đã triển khai

`JpaAuditingConfiguration` bật `@EnableJpaAuditing`. `BaseEntity` dùng `@MappedSuperclass`, `@CreatedDate` và `@LastModifiedDate` để quản lý timestamp cho các Entity.

Mã nguồn hiện đã ánh xạ `User`, `Role`, `UserRole` và `UserRoleId`; đã có `UserRepository` và `RoleRepository`. Các Entity còn lại mới ở mức thiết kế và không được xem là đã triển khai trong giai đoạn này.

`UserRole` dùng `@EmbeddedId` kết hợp `@MapsId`. Thiết kế này ánh xạ đúng khóa chính `(user_id, role_id)` và vẫn giữ auditing của bảng liên kết.

### 4.2.3. Truy vấn User và Roles

`UserRepository` cung cấp truy vấn fetch join để tải User cùng `userRoles` và `role` trong một lần truy vấn. Cách thực hiện này tránh vấn đề truy vấn N+1 trong `CustomUserDetailsService`, nơi tập quyền luôn cần thiết để tạo `GrantedAuthority`.

### 4.2.4. Kiểm chứng schema

Schema đã được thực thi từ cơ sở dữ liệu trống trên MySQL 8.0.44. Kết quả metadata xác nhận 25 bảng, 54 CHECK constraints, 34 khóa ngoại, 18 unique constraints và ba role được seed. Chín trường hợp kiểm thử âm tại tầng Database đều đạt, gồm chặn hai subscription `ACTIVE`, hai renewal request `PENDING`, hai Workout Plan `ACTIVE`, thông số planned hoặc actual vượt giới hạn, Workout Log sai chủ sở hữu Session, Body Progress trùng ngày, trạng thái AI Recommendation không hợp lệ và Gender ngoài Enum.

Kết quả này cho thấy các ràng buộc quan trọng không chỉ tồn tại trong tài liệu thiết kế mà đã được MySQL thực thi. Tuy nhiên, kiểm thử optimistic locking và transaction nghiệp vụ đầy đủ vẫn cần được bổ sung khi các Entity và Service tương ứng được triển khai.

## 4.3. Triển khai Authentication và Security Foundation

### 4.3.1. Baseline Security tại Ngày 5

Tại baseline Ngày 5, phần Security gồm `SecurityConfiguration`, `JwtService`, `JwtAuthenticationFilter`, `CustomUserDetailsService`, `AccountStatusGuard`, `AccountStatusAccessDeniedException`, `RestAuthenticationEntryPoint`, `RestAccessDeniedHandler`, `ApiResponse` và `ErrorResponse`.

Các API Register, Login, `/users/me` và quản lý trạng thái tài khoản chưa có controller/service trong mã nguồn tại thời điểm lập bản thảo. Vì vậy, chúng chỉ xuất hiện ở Chương 2 và Chương 3 dưới dạng yêu cầu và thiết kế, chưa được mô tả là kết quả triển khai.

### 4.3.2. SecurityFilterChain

`SecurityConfiguration` tắt CSRF, đặt Session Management ở chế độ `STATELESS` và chèn `JwtAuthenticationFilter` trước `UsernamePasswordAuthenticationFilter`. Các đường dẫn `/api/v1/auth/**`, health check, OpenAPI và Swagger UI được truy cập công khai. `/api/v1/admin/**` yêu cầu `ROLE_ADMIN`; `/api/v1/member/**` yêu cầu `ROLE_MEMBER`; các endpoint còn lại yêu cầu người dùng đã xác thực.

`DaoAuthenticationProvider` sử dụng `CustomUserDetailsService` và `BCryptPasswordEncoder(12)`. Password encoder đã được cấu hình nhưng chỉ được sử dụng trong luồng Register/Login khi các service tương ứng được triển khai.

### 4.3.3. JwtService

`JwtService` dùng JJWT 0.12.6 để tạo Access Token. Token chứa `sub` là email, `roles`, `iat` và `exp`. Thời hạn mặc định là 3.600.000 ms. Secret được đọc từ `application.security.jwt.secret`, kiểm tra khi khởi động và phải có tối thiểu 32 byte hiệu lực.

Khóa ký được khởi tạo một lần tại `@PostConstruct`. Service hỗ trợ secret Base64 hợp lệ hoặc chuỗi UTF-8. Token không chứa password hash, dữ liệu thể trạng hoặc dữ liệu AI.

### 4.3.4. JwtAuthenticationFilter

Filter kế thừa `OncePerRequestFilter`, đọc header `Authorization` và bỏ qua request không có tiền tố `Bearer `. Khi token hiện diện, filter trích xuất subject, tải `UserDetails`, xác minh username cùng thời hạn rồi đặt `UsernamePasswordAuthenticationToken` vào `SecurityContextHolder`.

Nếu token sai chữ ký, sai cấu trúc, hết hạn hoặc user không tồn tại, filter xóa Security Context và tiếp tục chuỗi bảo mật. Endpoint bảo vệ sau đó kích hoạt `RestAuthenticationEntryPoint`. Filter không đánh giá `accountStatus`; trách nhiệm này thuộc `AccountStatusGuard`.

### 4.3.5. CustomUserDetailsService

Email được chuẩn hóa bằng `trim()` và `toLowerCase(Locale.ROOT)`. Repository tải User cùng Roles bằng fetch join. Role được ánh xạ thành `SimpleGrantedAuthority`.

Trạng thái `DISABLED` được ánh xạ vào thuộc tính `disabled`, còn `LOCKED` được ánh xạ vào `accountLocked` của Spring Security `UserDetails`. Cấu hình này phục vụ quá trình xác thực bằng password; request dùng JWT vẫn cần `AccountStatusGuard` kiểm tra trạng thái hiện hành.

### 4.3.6. AccountStatusGuard

`AccountStatusGuard` truy vấn User theo email hoặc user ID. Trạng thái `ACTIVE` được tiếp tục xử lý; `LOCKED` và `DISABLED` làm phát sinh `AccountStatusAccessDeniedException` tương ứng với `ACC-004` hoặc `ACC-006`.

Ở baseline Ngày 5, Guard đã có entry point boolean `isAccountActive(String email)` để dùng trong Method Security nhưng chưa được gắn vào endpoint nghiệp vụ. Việc tích hợp sau đó đã hoàn thành trong M1 và tiếp tục được sử dụng tại API Profile Ngày 12.

### 4.3.7. Phản hồi lỗi 401 và 403

`RestAuthenticationEntryPoint` trả HTTP 401, `ACC-005` và thông báo token không hợp lệ hoặc hết hạn. `RestAccessDeniedHandler` trả HTTP 403 với `AUTH-002` cho lỗi thiếu quyền. Khi nhận `AccountStatusAccessDeniedException`, handler trả `ACC-004` hoặc `ACC-006` và bổ sung `details.accountStatus`.

Các phản hồi đều dùng JSON UTF-8, giúp Frontend xử lý lỗi thống nhất thay vì phụ thuộc vào response mặc định của Spring Security.

### 4.3.8. Kết quả kiểm thử hiện có

Báo cáo Surefire được lưu trong workspace từ lần chạy ngày 27/07/2026 ghi nhận kết quả sau:

| Test class | Số test | Kết quả |
|---|---:|---|
| `JwtServiceTest` | 7 | Đạt |
| `JwtAuthenticationFilterTest` | 4 | Đạt |
| `CustomUserDetailsServiceTest` | 3 | Đạt |
| `AccountStatusGuardTest` | 4 | Đạt |
| `SecurityErrorHandlerTest` | 3 | Đạt |
| `SmartGymApiApplicationTests` | 5 | Đạt |
| **Tổng cộng** | **26** | **Không có failure hoặc error** |

Bộ test bao phủ tạo và đọc JWT, token hết hạn, sai chữ ký, secret dưới 32 byte, UserDetails theo ba trạng thái tài khoản, AccountStatusGuard, response lỗi 401/403, request thiếu token, OpenAPI public và Spring context. Ngoài ra, Flyway đã validate đủ tám migration và Hibernate đã kiểm tra schema khi khởi động.

### 4.3.9. Đánh giá baseline Security Ngày 5

Phần nền tảng đã tách được ba trách nhiệm: xác thực token, phân quyền theo role và kiểm tra trạng thái tài khoản. Secret không nằm trong source code; response bảo mật đã có mã lỗi ổn định; truy vấn User cùng Roles tránh N+1.

Giới hạn tại thời điểm Ngày 5 là chưa có endpoint nghiệp vụ để kiểm chứng toàn bộ chuỗi Register, Login, `/users/me`, lock và unlock theo luồng end-to-end. Các giới hạn này đã được xử lý trong các ngày tiếp theo của M1; Profile API Ngày 12 kế thừa principal, JWT, RBAC và Account Status Guard từ nền đã hoàn thiện đó.

## 4.4. Đánh giá kết quả giai đoạn Ngày 1–5

Đến hết Ngày 5, dự án đã hoàn thành chuỗi công việc từ xác định phạm vi, đặc tả yêu cầu, thiết kế cơ sở dữ liệu đến khởi tạo Backend và xây dựng nền bảo mật. Điểm đạt được rõ nhất là các quyết định thiết kế đã có đối chiếu xuống Flyway Migration, ORM Auth và kiểm thử Security thay vì chỉ dừng ở tài liệu.

Việc để Flyway sở hữu DDL, tắt Open Session in View và dùng fetch join trong truy vấn User–Role giúp giảm sai lệch schema và tránh truy vấn N+1 ở luồng xác thực. Cơ chế JWT stateless tách riêng xác thực token, phân quyền và trạng thái tài khoản, nhờ đó token cũ vẫn bị chặn sau khi tài khoản chuyển sang `LOCKED` hoặc `DISABLED` khi Guard được gắn vào endpoint bảo vệ.

Phạm vi hiện tại vẫn có giới hạn: mới ba Entity Auth được hiện thực; chưa có RESTful API nghiệp vụ, giao diện React, `SubscriptionGuard`, Resilience4j hoặc tích hợp AI. Vì vậy, 26 test đang có chỉ chứng minh Backend Foundation, Flyway, JWT và các thành phần Security; chưa đủ để kết luận hệ thống đã vận hành end-to-end. Các số liệu kiểm thử tổng thể, hiệu năng và mức đáp ứng 44 FR chỉ được kết luận sau khi hoàn thành các milestone còn lại.

## 4.5. Hiện thực Profile Persistence Ngày 12

### 4.5.1. Backend Profile read model

Backend đã bổ sung tám enum Profile khớp migration V2 và danh mục enum trong thiết kế cơ sở dữ liệu. `MemberProfile` ánh xạ bảng `member_profiles` cùng năm collection table. Các kiểu `BigDecimal`, `LocalDate`, `Byte`, `Short` và `Instant` được chọn theo kiểu dữ liệu vật lý tương ứng; response DTO chuyển các số nguyên nhỏ sang `Integer` để tạo contract JSON ổn định cho Frontend.

`MemberProfileRepository` cung cấp truy vấn theo `user.id`. `MemberProfileService` chạy trong transaction chỉ đọc để các collection lazy được tải trong phạm vi persistence context, sau đó ánh xạ sang `MemberProfileResponse`, `BioProfileResponse` và `NutritionProfileResponse`. Response không serialize JPA Entity, password hash hoặc association nội bộ.

`MemberProfileController` cung cấp `GET /api/v1/member/profile`. Response thành công dùng envelope `ApiResponse`; trường hợp chưa có profile dùng `ErrorResponse`, HTTP 404 và `PROF-001`. OpenAPI mô tả response 200, 401, 403 và 404 cùng Bearer security requirement.

### 4.5.2. Frontend Profile shell

Frontend bổ sung `MemberLayout` với điều hướng Tổng quan và Hồ sơ. Route `/member/profile` được đặt sau `ProtectedRoute` và lớp `RoleRoute` riêng cho `ROLE_MEMBER`; `ROLE_PT` có thể dùng khu vực Member chung nhưng không thể mở Profile trực tiếp.

API client gọi `/member/profile` qua Axios instance dùng base URL `/api/v1` và tự gắn Bearer token từ auth session. Client kiểm tra cấu trúc response trước khi sử dụng và phân biệt `PROF-001`, `ACC-004`, `ACC-005`, `ACC-006`, `NETWORK-001` và `SYS-001`.

Trang Profile có loading state, empty state “Chưa hoàn thiện hồ sơ”, form cập nhật, lỗi kết nối và nút thử lại. Sau khi lưu thành công, giao diện hiển thị các chỉ số BMI, BMR, TDEE và mục tiêu dinh dưỡng do Backend tính toán; không tự tính hoặc hiển thị dữ liệu giả. Từ Ngày 14, Frontend tiếp tục gửi cân nặng hiện tại sang `POST /api/v1/member/body-progress` bằng một request độc lập và có nút thử lại nếu request ghi tiến trình thất bại.

### 4.5.3. Kết quả targeted test

Theo giới hạn của ngày phát triển feature, dự án chỉ chạy các test mục tiêu thay vì full regression M1 hoặc manual localhost.

| Nhóm kiểm thử | Số test | Kết quả |
|---|---:|---|
| `MemberProfileControllerTest` | 7 | Đạt |
| `MemberProfileTest` | 4 | Đạt |
| `MemberProfileServiceTest` | 5 | Đạt |
| `MemberProfilePageTest` và `MemberLayoutTest` | 10 | Đạt |
| Kiểm tra route bảo vệ bổ sung | 6 | Đạt |

Tổng cộng 16 targeted backend test và 16 frontend/component-route test đều không có failure. Trong quá trình chạy Backend, Flyway validate đủ tám migration, schema ở version 8 và Hibernate khởi tạo `EntityManagerFactory` thành công trên MySQL 8.0.44.

Kết quả cho thấy persistence mapping, ownership theo Principal, error contract và kết nối contract giữa Frontend với Backend đã hoàn thành cho phạm vi đọc Profile. Full regression, production build và kiểm thử thủ công localhost được dành cho ngày local QA/fix của M2 theo kế hoạch.

## 4.6. Hiện thực Profile Update và Calculator Ngày 13

### 4.6.1. Thiết kế module Calculator

`BiometricCalculationService` được triển khai như một service stateless, không
truy cập Repository và nhận `Clock` từ cấu hình hệ thống. Tuổi được tính theo
ngày nghiệp vụ `Asia/Ho_Chi_Minh`, không phụ thuộc timezone mặc định của JVM.
Các công thức sử dụng BMI, Mifflin-St Jeor, hệ số hoạt động 1.2/1.375/1.55/1.725,
điều chỉnh BULK/CUT/MAINTAIN, protein 2.2 g/kg, chất béo 25% calories và
carbohydrate là phần calories còn lại chia 4. Mọi kết quả được làm tròn hai
chữ số; carbohydrate âm bị từ chối.

### 4.6.2. API cập nhật Profile

`PUT /api/v1/member/profile` nhận toàn bộ `MemberProfileUpsertRequest`. Service
chuẩn hóa và sanitize collection, tạo mới hoặc cập nhật đúng Profile thuộc
Principal hiện hành, rồi trả `MemberProfileResponse` kèm
`calculatedTargets`. Các chỉ tiêu chỉ được tính trong response, không được lưu
vào bảng `member_profiles`; dữ liệu `BodyProgress` được để sang Ngày 14.
`AccountStatusGuard` chạy trước truy vấn/ghi và Security chỉ cho phép
`ROLE_MEMBER` truy cập endpoint.

### 4.6.3. Giao diện Profile Form

Frontend sử dụng React Hook Form kết hợp Zod để quản lý form chỉnh sửa Profile.
CTA tạo Profile đã hoạt động; Profile hiện có được điền sẵn. Client gọi cùng
contract `/api/v1/member/profile`, hiển thị lỗi validation theo field, khóa
nút khi đang lưu, cập nhật React Query cache sau thành công và hiển thị các
chỉ tiêu do Backend trả về thay vì tự tạo số liệu giả.

### 4.6.4. Kết quả kiểm thử Ngày 13

Backend targeted có **53 test đạt**, gồm controller, service, entity,
Calculator và integration test xác nhận transaction rollback trên MySQL;
Frontend targeted có **16 test đạt** cho form, API contract,
validation, cache update và chống submit lặp. Không có failure hoặc error.
Backend đồng thời validate đủ 8 Flyway migration và khởi tạo Hibernate thành
công trên MySQL 8.0.44. Full regression, production build và manual localhost
được dành cho ngày Local QA M2, đúng phạm vi kế hoạch.

## 4.7. Hiện thực Body Progress và tích hợp M2 Ngày 14

### 4.7.1. Thiết kế dữ liệu và atomic upsert

`BodyProgress` lưu cân nặng theo Member và ngày nghiệp vụ. Bảng
`body_progress` sử dụng khóa duy nhất `(member_id, record_date)` với tên
`uk_body_progress_member_date`, nhờ đó mỗi Member chỉ có tối đa một bản ghi
trong một ngày. Repository thực thi một câu lệnh MySQL nguyên tử `INSERT ... ON
DUPLICATE KEY UPDATE`; cách này tránh race condition của quy trình kiểm tra rồi
insert khi hai request cùng đến. Body Progress không cascade xóa từ User hoặc
Profile nhằm bảo toàn lịch sử.

### 4.7.2. Service, ownership và timezone nghiệp vụ

Service lấy Member ID từ `AuthenticatedUserPrincipal`, không nhận ID chủ sở hữu
từ client. `AccountStatusGuard` và role `ROLE_MEMBER` được kiểm tra trước nghiệp
vụ. `Clock` được chuyển sang `Asia/Ho_Chi_Minh` để từ chối `recordDate` trong
tương lai theo ngày nghiệp vụ, không phụ thuộc timezone mặc định của JVM. POST
thực hiện upsert trong transaction; GET chỉ tải lịch sử của Member hiện hành và
sắp xếp `recordDate` tăng dần.

### 4.7.3. Tích hợp Profile và giao diện

Profile và Body Progress vẫn là hai API độc lập. Sau khi PUT Profile thành
công, frontend dùng cân nặng trong response và ngày Việt Nam để gọi POST Body
Progress. Nếu bước thứ hai thất bại, Profile không bị báo là rollback hoặc mất
dữ liệu; giao diện hiển thị lỗi riêng và cho phép retry cùng payload. Trang
Progress đồng thời cung cấp form ghi nhận, lịch sử, widget cân nặng mới nhất và
các trạng thái loading, empty, error, success. Lịch sử được sắp xếp tăng dần;
bản ghi đầu tiên là cân nặng ban đầu, bản ghi cuối là cân nặng hiện tại. Mỗi
dòng lịch sử hiển thị mức tăng/giảm so với cân nặng ban đầu. Khoảng cách tuyệt
đối tới `targetWeightKg` được đặt bằng chữ xanh ngay dưới cân nặng hiện tại;
khối cân nặng mục tiêu chỉ hiển thị con số đích để tránh lặp thông tin.

### 4.7.4. Kết quả kiểm thử Ngày 14

Năm lớp test Backend cung cấp 18 trường hợp cho Entity, native upsert, Service,
Controller và Integration trên MySQL. Full regression đạt 271 test, Flyway
validate tám migration và Hibernate khởi tạo thành công. Frontend targeted
Profile/Progress đạt 30 test, bao phủ validation, cache, chuỗi Profile →
Progress và retry độc lập. Full Frontend regression đạt 75 test và production
build thành công. OpenAPI hiện thực 10 operation M1–M2; Postman có 29 request
Auth/RBAC/Admin/Profile/Progress và không chứa credential thật. Kiểm thử thủ
công localhost được dành cho gate M2 Ngày 15.

## 4.8. Hoàn thiện Profile và Body Progress sau Ngày 14

Trong vòng local QA ngày 06–07/08/2026, Profile được mở rộng để hỗ trợ tối đa
hai mục tiêu, cân nặng đích, ghi chú hạn chế vận động tự nhập và danh sách thực
phẩm phổ biến. Kết quả BMI được phân loại; BMR, TDEE và lượng calo mục tiêu được
trình bày theo ý nghĩa nghiệp vụ thay vì chỉ hiển thị số.

Body Progress bổ sung khối lượng cơ và khối lượng mỡ tùy chọn. Giao diện phân
biệt rõ ba mốc: cân nặng ban đầu lấy từ bản ghi `recordDate` sớm nhất, cân nặng
hiện tại lấy từ bản ghi mới nhất và cân nặng mục tiêu lấy từ Profile. Thông báo
sau khi lưu và từng dòng lịch sử dùng chênh lệch so với cân nặng ban đầu. Dòng
`Còn cách mục tiêu X.XX kg` hoặc `Đã đạt cân nặng mục tiêu` nằm ngay dưới cân
nặng hiện tại; khi đạt mục tiêu hệ thống hiển thị lời chúc mừng và hiệu ứng.

Flyway V9–V10 được validate trên MySQL, full Backend regression đạt 274 test.
Frontend đạt 80 test, kiểm tra TypeScript không còn lỗi và Vite production build
thành công. Các tài liệu phạm vi, yêu cầu chức năng, business rule, use case,
API, database, ORM và kiến trúc đã được đồng bộ với cách tính baseline này.
