package com.laptopshop.controller;

// Trang thông tin cửa hàng: giới thiệu, liên hệ, hướng dẫn mua hàng/thanh toán, các chính sách (bảo hành, đổi trả, vận chuyển, bảo mật).
// Nội dung bám sát nghiệp vụ thực tế của hệ thống (chỉ thanh toán chuyển khoản VietQR, đơn giữ chỗ có thời hạn...).

import com.laptopshop.config.ShopProperties;
import com.laptopshop.exception.ResourceNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class InfoPageController {

    public record Section(String heading, List<String> paragraphs, List<String> bullets) {
        static Section of(String heading, List<String> paragraphs, List<String> bullets) {
            return new Section(heading, paragraphs, bullets);
        }
        static Section text(String heading, String... paragraphs) {
            return new Section(heading, List.of(paragraphs), List.of());
        }
        static Section list(String heading, String intro, String... bullets) {
            return new Section(heading, intro == null ? List.of() : List.of(intro), List.of(bullets));
        }
    }

    public record InfoPage(String slug, String title, String icon, String intro, List<Section> sections) {
    }

    private final ShopProperties props;

    public InfoPageController(ShopProperties props) {
        this.props = props;
    }

    @GetMapping("/thong-tin/{slug}")
    public String show(@PathVariable String slug, Model model) {
        Map<String, InfoPage> pages = buildPages();
        InfoPage page = pages.get(slug);
        if (page == null) {
            throw new ResourceNotFoundException("Không tìm thấy trang thông tin: " + slug);
        }
        model.addAttribute("page", page);
        model.addAttribute("pages", new ArrayList<>(pages.values()));
        model.addAttribute("activeSlug", slug);
        return "store/info-page";
    }

    private Map<String, InfoPage> buildPages() {
        ShopProperties.ShopInfo shop = props.getShop();
        ShopProperties.PaymentConfig pay = props.getPayment();
        int expire = pay.getExpireMinutes();
        String name = shop.getName();

        Map<String, InfoPage> pages = new LinkedHashMap<>();

        pages.put("gioi-thieu", new InfoPage("gioi-thieu", "Giới thiệu " + name, "bi-info-circle",
                shop.getSlogan() + ". " + name + " là cửa hàng chuyên laptop chính hãng, đồng hành cùng học sinh - sinh viên, dân văn phòng, nhà sáng tạo nội dung và game thủ.",
                List.of(
                        Section.text("Chúng tôi là ai?",
                                name + " ra đời với một mục tiêu đơn giản: giúp mọi người chọn được chiếc laptop đúng nhu cầu, đúng ngân sách, mua nhanh và yên tâm sau bán hàng.",
                                "Mỗi sản phẩm đều có thông số rõ ràng, giá niêm yết minh bạch, tồn kho cập nhật theo thời gian thực để bạn luôn biết máy còn hàng hay không."),
                        Section.list("Danh mục sản phẩm", "Chúng tôi phân phối đầy đủ các thương hiệu và phân khúc phổ biến:",
                                "Thương hiệu: Dell, HP, Lenovo, ASUS, Acer, Apple MacBook, MSI, Gigabyte và nhiều hãng khác.",
                                "Phân khúc: văn phòng, gaming, đồ họa, mỏng nhẹ, cao cấp, sinh viên, doanh nhân, 2 trong 1, workstation.",
                                "Mọi sản phẩm đều chính hãng, nguyên seal, có hóa đơn và bảo hành điện tử."),
                        Section.list("Vì sao chọn " + name + "?", null,
                                "Thanh toán linh hoạt: chuyển khoản VietQR xác nhận tức thì hoặc trả tiền mặt khi nhận hàng (COD).",
                                "Kiểm tra hàng nguyên seal trước khi thanh toán khi chọn giao hàng thu tiền mặt (COD).",
                                "Hệ thống giữ tồn kho chính xác ngay cả khi nhiều khách đặt cùng lúc, tránh tình trạng bán quá số lượng.",
                                "Đội ngũ tư vấn hỗ trợ trong giờ làm việc: " + shop.getWorkingHours() + "."),
                        Section.text("Thông tin liên hệ",
                                "Địa chỉ: " + shop.getAddress() + ". Hotline: " + shop.getPhone() + ". Email: " + shop.getEmail() + ".")
                )));

        pages.put("lien-he", new InfoPage("lien-he", "Liên hệ & góp ý", "bi-headset",
                "Chúng tôi luôn sẵn sàng lắng nghe. Hãy liên hệ qua các kênh dưới đây để được tư vấn chọn máy, hỗ trợ đơn hàng hoặc tiếp nhận góp ý.",
                List.of(
                        Section.list("Kênh liên hệ chính thức", null,
                                "Cửa hàng: " + shop.getAddress(),
                                "Hotline tư vấn - hỗ trợ đơn hàng: " + shop.getPhone(),
                                "Email chăm sóc khách hàng: " + shop.getEmail(),
                                "Giờ làm việc: " + shop.getWorkingHours()),
                        Section.list("Bạn cần hỗ trợ về?", null,
                                "Tư vấn chọn laptop theo nhu cầu và ngân sách: gọi hotline hoặc đến trực tiếp cửa hàng.",
                                "Đơn hàng, thanh toán: vào mục \"Đơn hàng của tôi\" để xem trạng thái; liên hệ hotline nếu cần hỗ trợ thêm.",
                                "Bảo hành, đổi trả: mang theo sản phẩm, hóa đơn/mã đơn hàng đến cửa hàng hoặc gọi hotline để được hướng dẫn.",
                                "Góp ý, khiếu nại dịch vụ: gửi email, chúng tôi phản hồi trong vòng 24 giờ làm việc.")
                )));

        pages.put("huong-dan-mua-hang", new InfoPage("huong-dan-mua-hang", "Hướng dẫn mua hàng", "bi-bag-check",
                "Mua laptop tại " + name + " chỉ với 6 bước đơn giản, toàn bộ quy trình diễn ra trực tuyến.",
                List.of(
                        Section.of("Các bước đặt hàng", List.of(), List.of(
                                "Bước 1 - Tìm sản phẩm: dùng ô tìm kiếm hoặc thanh danh mục (Gaming, Văn phòng, Đồ họa, Mỏng nhẹ...). Có thể lọc theo thương hiệu, khoảng giá và chỉ xem sản phẩm giảm giá.",
                                "Bước 2 - Thêm vào giỏ hàng: mở trang chi tiết để xem cấu hình, chọn số lượng rồi bấm \"Thêm vào giỏ\" hoặc \"Mua ngay\".",
                                "Bước 3 - Đăng nhập / Đăng ký: bạn cần có tài khoản để đặt hàng và theo dõi đơn. Đăng ký miễn phí chỉ mất một phút.",
                                "Bước 4 - Đặt hàng & chọn thanh toán: nhập họ tên, SĐT, địa chỉ nhận hàng và chọn Chuyển khoản ngân hàng (VietQR) hoặc Tiền mặt khi nhận hàng (COD).",
                                "Bước 5 - Thanh toán / Xác nhận đơn: nếu chọn VietQR, quét mã QR trên điện thoại và bấm \"Xác nhận đã chuyển khoản\"; nếu chọn COD, đơn chuyển sang chờ cửa hàng xác nhận và bạn trả tiền mặt khi nhận máy.",
                                "Bước 6 - Theo dõi đơn hàng: vào \"Đơn hàng của tôi\" để xem trạng thái xử lý và giao hàng.")),
                        Section.text("Lưu ý",
                                "Số lượng trong giỏ không được vượt quá tồn kho hiện có. Khi nhiều khách cùng đặt một sản phẩm, hệ thống ưu tiên theo thứ tự đặt hàng thành công.")
                )));

        pages.put("huong-dan-thanh-toan", new InfoPage("huong-dan-thanh-toan", "Hướng dẫn thanh toán (VietQR & COD)", "bi-qr-code-scan",
                name + " hỗ trợ linh hoạt hai hình thức thanh toán chính: Chuyển khoản ngân hàng qua VietQR và Trả tiền mặt khi nhận hàng (COD).",
                List.of(
                        Section.of("Cách 1: Chuyển khoản ngân hàng (VietQR)", List.of(), List.of(
                                "Sau khi bấm \"Đặt hàng & Quét mã QR\", màn hình hiển thị mã QR kèm số tài khoản, số tiền và nội dung chuyển khoản (mã đơn hàng).",
                                "Dùng camera điện thoại quét mã QR để mở trang xác nhận thanh toán.",
                                "Thực hiện chuyển khoản đúng số tiền & nội dung rồi bấm \"Xác nhận đã chuyển khoản\" trên điện thoại.",
                                "Màn hình máy tính tự động chuyển sang \"Thanh toán thành công\" ngay lập tức.")),
                        Section.of("Cách 2: Tiền mặt khi nhận hàng (COD)", List.of(), List.of(
                                "Chọn phương thức \"Thanh toán tiền mặt khi nhận hàng (COD)\" ở bước đặt hàng.",
                                "Đơn hàng được giữ tồn kho ngay và chuyển sang trạng thái Chờ xác nhận.",
                                "Khi nhân viên giao hàng giao máy tới địa chỉ của bạn, bạn kiểm tra tình trạng niêm phong bên ngoài rồi thanh toán tiền mặt trực tiếp cho nhân viên giao hàng.",
                                "Sau khi giao & thu tiền thành công, trạng thái đơn chuyển sang Hoàn thành và thanh toán được ghi nhận là Thành công.")),
                        Section.list("Thông tin tài khoản nhận tiền (khi chọn VietQR)", null,
                                "Ngân hàng: " + pay.getBankName(),
                                "Số tài khoản: " + pay.getAccountNumber(),
                                "Chủ tài khoản: " + pay.getAccountName(),
                                "Nội dung chuyển khoản: mã đơn hàng hiển thị trên màn hình thanh toán."),
                        Section.list("Thời hạn thanh toán đối với đơn VietQR", "Để đảm bảo công bằng cho các khách hàng khác:",
                                "Đơn chuyển khoản được giữ tồn kho trong " + expire + " phút kể từ lúc đặt nếu chưa thanh toán.",
                                "Quá thời hạn mà chưa chuyển khoản, đơn tự động hủy và sản phẩm được trả lại kho. (Đơn COD không bị giới hạn 30 phút này).")
                )));

        pages.put("chinh-sach-bao-hanh", new InfoPage("chinh-sach-bao-hanh", "Chính sách bảo hành", "bi-shield-check",
                "Tất cả laptop tại " + name + " đều là hàng chính hãng và được bảo hành theo chính sách của nhà sản xuất.",
                List.of(
                        Section.list("Thời hạn & phạm vi bảo hành", null,
                                "Thời hạn bảo hành từ 12 đến 24 tháng tùy hãng và dòng sản phẩm, ghi rõ trên phiếu bảo hành/hóa đơn.",
                                "Bảo hành áp dụng cho lỗi phần cứng do nhà sản xuất: bo mạch, màn hình, bàn phím, pin, ổ cứng, RAM...",
                                "Thời gian bảo hành tính từ ngày giao hàng thành công."),
                        Section.list("Trường hợp không được bảo hành", null,
                                "Sản phẩm hết thời hạn bảo hành hoặc không có thông tin mua hàng tại cửa hàng.",
                                "Máy rơi vỡ, vào nước, cháy nổ, biến dạng do tác động bên ngoài.",
                                "Tự ý tháo máy, sửa chữa, thay linh kiện tại nơi không được ủy quyền.",
                                "Tem bảo hành bị rách, mờ hoặc có dấu hiệu chỉnh sửa."),
                        Section.text("Quy trình tiếp nhận",
                                "Mang sản phẩm cùng hóa đơn/mã đơn hàng đến " + shop.getAddress() + " hoặc gọi " + shop.getPhone() + " để được hướng dẫn. Thời gian xử lý thông thường từ 3 đến 15 ngày làm việc tùy mức độ lỗi và linh kiện.")
                )));

        pages.put("chinh-sach-doi-tra", new InfoPage("chinh-sach-doi-tra", "Chính sách đổi trả & hoàn tiền", "bi-arrow-repeat",
                "Chúng tôi cam kết đổi mới cho sản phẩm lỗi do nhà sản xuất và hoàn tiền nhanh chóng, minh bạch.",
                List.of(
                        Section.list("Đổi trả trong 10 ngày", null,
                                "Sản phẩm lỗi do nhà sản xuất được đổi máy mới tương đương trong 10 ngày đầu kể từ khi nhận hàng.",
                                "Sản phẩm phải còn đầy đủ phụ kiện, hộp, tem, không trầy xước, không có dấu hiệu va đập/vào nước.",
                                "Cần có hóa đơn hoặc mã đơn hàng để đối chiếu."),
                        Section.list("Hủy đơn & hoàn tiền", null,
                                "Đơn chưa thanh toán: hủy miễn phí, không phát sinh chi phí.",
                                "Đơn đã thanh toán nhưng chưa giao: có thể hủy, tiền được hoàn lại về tài khoản đã chuyển trong 3 - 5 ngày làm việc.",
                                "Đơn đã giao: hoàn tiền sau khi cửa hàng kiểm tra và xác nhận sản phẩm đủ điều kiện đổi trả.",
                                "Hoàn tiền thực hiện bằng chuyển khoản ngân hàng, cùng hình thức khách đã thanh toán."),
                        Section.text("Trường hợp không áp dụng đổi trả",
                                "Sản phẩm đã kích hoạt/sử dụng không đúng cách, hư hỏng do người dùng, hoặc quá thời hạn 10 ngày. Khi đó sản phẩm vẫn được hưởng chính sách bảo hành chính hãng.")
                )));

        pages.put("chinh-sach-van-chuyen", new InfoPage("chinh-sach-van-chuyen", "Chính sách vận chuyển", "bi-truck",
                "Miễn phí giao hàng toàn quốc cho mọi đơn hàng, đóng gói chống sốc và bảo hiểm trong suốt quá trình vận chuyển.",
                List.of(
                        Section.list("Thời gian giao hàng", null,
                                "Nội thành TP. Hồ Chí Minh và Hà Nội: 1 - 2 ngày làm việc.",
                                "Các tỉnh thành khác: 2 - 5 ngày làm việc tùy khu vực.",
                                "Đơn chuyển khoản (VietQR) được xử lý giao ngay sau khi xác nhận thanh toán; đơn COD được xác nhận và giao trực tiếp tới địa chỉ nhận hàng."),
                        Section.list("Kiểm tra hàng khi nhận", null,
                                "Vui lòng kiểm tra tình trạng bên ngoài của kiện hàng, mở hộp và đối chiếu sản phẩm trước khi ký nhận.",
                                "Nên quay video quá trình mở hộp để làm bằng chứng nếu có sai sót.",
                                "Nếu phát hiện hư hỏng/sai sản phẩm, từ chối nhận và liên hệ hotline " + shop.getPhone() + " ngay."),
                        Section.text("Phí vận chuyển & thanh toán khi nhận",
                                "Miễn phí giao hàng toàn quốc, không phụ thu. Nếu chọn COD, khách hàng thanh toán đúng giá trị đơn hàng bằng tiền mặt khi nhận sản phẩm.")
                )));

        pages.put("chinh-sach-bao-mat", new InfoPage("chinh-sach-bao-mat", "Chính sách bảo mật", "bi-lock",
                name + " tôn trọng và bảo vệ thông tin cá nhân của khách hàng khi sử dụng website.",
                List.of(
                        Section.list("Thông tin chúng tôi thu thập", null,
                                "Họ tên, số điện thoại, email và địa chỉ nhận hàng khi bạn đăng ký tài khoản hoặc đặt hàng.",
                                "Lịch sử đơn hàng và trạng thái thanh toán để phục vụ hỗ trợ và bảo hành."),
                        Section.list("Cách chúng tôi bảo vệ dữ liệu", null,
                                "Mật khẩu được mã hóa một chiều (BCrypt), không ai kể cả quản trị viên có thể xem mật khẩu của bạn.",
                                "Phân quyền chặt chẽ: khách hàng chỉ xem được đơn hàng của chính mình; nhân viên chỉ truy cập chức năng được cấp.",
                                "Không bán hoặc chia sẻ thông tin cá nhân cho bên thứ ba vì mục đích thương mại."),
                        Section.text("Quyền của bạn",
                                "Bạn có thể xem và cập nhật họ tên, email, số điện thoại, địa chỉ tại \"Hồ sơ cá nhân\". Mọi thay đổi đều yêu cầu nhập mật khẩu hiện tại để xác nhận. Cần xóa dữ liệu, vui lòng liên hệ " + shop.getEmail() + ".")
                )));

        return pages;
    }
}
