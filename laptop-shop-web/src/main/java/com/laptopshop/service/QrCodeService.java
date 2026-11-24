package com.laptopshop.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.Map;

@Service
public class QrCodeService {

    private static final String HMAC_KEY = "QL_LAPTOP_VIETQR_SECRET_KEY_2026";
    private final QrTunnelService qrTunnelService;

    public QrCodeService(QrTunnelService qrTunnelService) {
        this.qrTunnelService = qrTunnelService;
    }

    public String createToken(Integer orderId, String orderCode, String transactionCode) {
        try {
            String payload = orderId + "|" + (orderCode != null ? orderCode : "") + "|"
                    + (transactionCode != null ? transactionCode : "");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(HMAC_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 16 && i < digest.length; i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tạo chữ ký QR", e);
        }
    }

    public boolean verifyToken(Integer orderId, String orderCode, String transactionCode, String token) {
        if (token == null || token.isBlank()) return false;
        String expected = createToken(orderId, orderCode, transactionCode);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                token.trim().getBytes(StandardCharsets.UTF_8)
        );
    }

    public byte[] generateQrPng(String content, int size) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = Map.of(
                    EncodeHintType.CHARACTER_SET, "UTF-8",
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                    EncodeHintType.MARGIN, 1
            );
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tạo mã QR", e);
        }
    }

    public boolean isPublicTunnelActive() {
        return qrTunnelService.getPublicBaseUrl() != null;
    }

    public String buildMobilePayUrl(HttpServletRequest request, Integer orderId, String token) {
        String ctx = request.getContextPath() != null ? request.getContextPath() : "";
        String publicBase = qrTunnelService.getPublicBaseUrl();
        if (publicBase != null) {
            return publicBase + ctx + "/qr-pay/" + orderId + "/" + token;
        }
        String scheme = request.getScheme();
        String host = request.getServerName();
        if (isLoopbackHost(host)) {
            String lanIp = detectLanIpv4();
            if (lanIp != null) {
                host = lanIp;
            }
        }
        int port = request.getServerPort();
        boolean defaultPort = ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443);
        String portPart = defaultPort ? "" : ":" + port;
        return scheme + "://" + host + portPart + ctx + "/qr-pay/" + orderId + "/" + token;
    }

    private static boolean isLoopbackHost(String host) {
        if (host == null) return true;
        String h = host.trim().toLowerCase();
        return h.equals("localhost") || h.equals("127.0.0.1") || h.equals("::1") || h.equals("[::1]") || h.equals("0.0.0.0");
    }

    private static String detectLanIpv4() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            InetAddress local = socket.getLocalAddress();
            if (local instanceof Inet4Address && !local.isLoopbackAddress() && !local.isLinkLocalAddress()) {
                return local.getHostAddress();
            }
        } catch (Exception ignored) {
        }
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces != null && ifaces.hasMoreElements()) {
                NetworkInterface nif = ifaces.nextElement();
                if (!nif.isUp() || nif.isLoopback() || nif.isVirtual()) continue;
                String name = (nif.getDisplayName() + " " + nif.getName()).toLowerCase();
                if (name.contains("vmware") || name.contains("virtualbox") || name.contains("hyper-v")
                        || name.contains("vethernet") || name.contains("docker") || name.contains("wsl")) {
                    continue;
                }
                Enumeration<InetAddress> addrs = nif.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address && addr.isSiteLocalAddress() && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
