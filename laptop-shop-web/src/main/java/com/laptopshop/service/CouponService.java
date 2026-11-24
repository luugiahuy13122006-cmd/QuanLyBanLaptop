package com.laptopshop.service;

import com.laptopshop.entity.Coupon;
import com.laptopshop.entity.DiscountType;
import com.laptopshop.exception.BusinessException;
import com.laptopshop.exception.ResourceNotFoundException;
import com.laptopshop.repository.CouponRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class CouponService {

    private final CouponRepository couponRepository;
    private final AuditLogService auditLogService;

    public CouponService(CouponRepository couponRepository, AuditLogService auditLogService) {
        this.couponRepository = couponRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public Page<Coupon> searchCoupons(String keyword, Pageable pageable) {
        String kw = keyword == null ? "" : keyword.trim();
        return couponRepository.findByCodeContainingIgnoreCaseOrderByIdDesc(kw, pageable);
    }

    @Transactional(readOnly = true)
    public Coupon getById(Integer id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mã giảm giá ID: " + id));
    }

    @Transactional
    public Coupon saveCoupon(Coupon form) {
        String code = form.getCode() != null ? form.getCode().trim().toUpperCase() : "";
        if (code.isEmpty()) throw new BusinessException("Mã giảm giá không được để trống.");
        if (form.getId() == null && couponRepository.existsByCodeIgnoreCase(code)) {
            throw new BusinessException("Mã '" + code + "' đã tồn tại!");
        }
        if (form.getId() != null && couponRepository.existsByCodeIgnoreCaseAndIdNot(code, form.getId())) {
            throw new BusinessException("Mã '" + code + "' đã được dùng bởi voucher khác!");
        }
        Coupon target = form.getId() != null ? getById(form.getId()) : new Coupon();
        target.setCode(code);
        target.setDescription(form.getDescription());
        target.setDiscountType(form.getDiscountType() != null ? form.getDiscountType() : DiscountType.PERCENT);
        target.setDiscountValue(form.getDiscountValue() != null ? form.getDiscountValue() : BigDecimal.ZERO);
        target.setMaxDiscountAmount(form.getMaxDiscountAmount());
        target.setMinOrderAmount(form.getMinOrderAmount() != null ? form.getMinOrderAmount() : BigDecimal.ZERO);
        target.setMaxUsage(form.getMaxUsage() != null ? form.getMaxUsage() : 100);
        target.setStartsAt(form.getStartsAt());
        target.setExpiresAt(form.getExpiresAt());
        target.setIsActive(form.getIsActive() != null ? form.getIsActive() : true);
        Coupon saved = couponRepository.save(target);
        auditLogService.logAction("SAVE_COUPON", "Coupon", saved.getCode(), "Lưu mã giảm giá");
        return saved;
    }

    @Transactional(readOnly = true)
    public BigDecimal previewDiscount(String rawCode, BigDecimal subtotal) {
        if (rawCode == null || rawCode.isBlank()) return BigDecimal.ZERO;
        Coupon coupon = couponRepository.findByCodeIgnoreCase(rawCode.trim())
                .orElseThrow(() -> new BusinessException("Mã giảm giá '" + rawCode.trim().toUpperCase() + "' không tồn tại."));
        return computeAndValidateDiscount(coupon, subtotal);
    }

    @Transactional(rollbackFor = Exception.class)
    public BigDecimal consumeCoupon(String rawCode, BigDecimal subtotal) {
        Coupon coupon = couponRepository.findByCodeForUpdate(rawCode.trim())
                .orElseThrow(() -> new BusinessException("Mã giảm giá '" + rawCode.trim().toUpperCase() + "' không tồn tại."));
        BigDecimal discount = computeAndValidateDiscount(coupon, subtotal);
        coupon.setUsedCount(coupon.getUsedCount() + 1);
        couponRepository.save(coupon);
        return discount;
    }

    @Transactional(rollbackFor = Exception.class)
    public void releaseCoupon(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) return;
        couponRepository.findByCodeForUpdate(rawCode.trim()).ifPresent(c -> {
            if (c.getUsedCount() != null && c.getUsedCount() > 0) {
                c.setUsedCount(c.getUsedCount() - 1);
                couponRepository.save(c);
            }
        });
    }

    private BigDecimal computeAndValidateDiscount(Coupon coupon, BigDecimal subtotal) {
        LocalDateTime now = LocalDateTime.now();
        if (!Boolean.TRUE.equals(coupon.getIsActive())) {
            throw new BusinessException("Mã giảm giá hiện đang tạm khóa.");
        }
        if (coupon.getStartsAt() != null && now.isBefore(coupon.getStartsAt())) {
            throw new BusinessException("Mã giảm giá chưa đến thời gian áp dụng.");
        }
        if (coupon.getExpiresAt() != null && now.isAfter(coupon.getExpiresAt())) {
            throw new BusinessException("Mã giảm giá đã hết hạn sử dụng.");
        }
        if (coupon.getUsedCount() >= coupon.getMaxUsage()) {
            throw new BusinessException("Mã giảm giá đã hết lượt sử dụng.");
        }
        BigDecimal sub = subtotal != null ? subtotal : BigDecimal.ZERO;
        if (sub.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new BusinessException("Đơn hàng cần tối thiểu " + coupon.getMinOrderAmount().setScale(0, RoundingMode.HALF_UP) + " đ để áp dụng mã này.");
        }
        BigDecimal discount;
        if (coupon.getDiscountType() == DiscountType.PERCENT) {
            discount = sub.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            if (coupon.getMaxDiscountAmount() != null && coupon.getMaxDiscountAmount().signum() > 0
                    && discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
                discount = coupon.getMaxDiscountAmount();
            }
        } else {
            discount = coupon.getDiscountValue();
        }
        return discount.min(sub).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
