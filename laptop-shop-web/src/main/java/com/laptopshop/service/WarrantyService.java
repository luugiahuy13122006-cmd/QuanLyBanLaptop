package com.laptopshop.service;

import com.laptopshop.entity.*;
import com.laptopshop.exception.BusinessException;
import com.laptopshop.exception.ResourceNotFoundException;
import com.laptopshop.repository.LaptopRepository;
import com.laptopshop.repository.ProductSerialRepository;
import com.laptopshop.repository.WarrantyClaimRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class WarrantyService {

    private final ProductSerialRepository serialRepository;
    private final WarrantyClaimRepository claimRepository;
    private final LaptopRepository laptopRepository;
    private final AuditLogService auditLogService;

    public WarrantyService(ProductSerialRepository serialRepository,
                           WarrantyClaimRepository claimRepository,
                           LaptopRepository laptopRepository,
                           AuditLogService auditLogService) {
        this.serialRepository = serialRepository;
        this.claimRepository = claimRepository;
        this.laptopRepository = laptopRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<ProductSerial> lookupWarranty(String query) {
        if (query == null || query.isBlank()) return List.of();
        String q = query.trim();
        Optional<ProductSerial> bySerial = serialRepository.findBySerialNumberIgnoreCase(q);
        if (bySerial.isPresent()) {
            return List.of(bySerial.get());
        }
        return serialRepository.findByOrderDetail_Order_OrderCodeIgnoreCase(q);
    }

    @Transactional(readOnly = true)
    public List<WarrantyClaim> getClaimsForSerial(String serialNumber) {
        if (serialNumber == null || serialNumber.isBlank()) return List.of();
        return claimRepository.findBySerialNumberIgnoreCaseOrderByCreatedAtDesc(serialNumber.trim());
    }

    @Transactional(rollbackFor = Exception.class)
    public void allocateSerialsForCompletedOrder(Order order) {
        if (order == null || order.getDetails() == null) return;
        for (OrderDetail detail : order.getDetails()) {
            Laptop laptop = detail.getLaptop();
            if (laptop == null) continue;
            int qty = detail.getQuantity() != null ? detail.getQuantity() : 0;
            if (qty <= 0) continue;

            // Đã cấp trước đó chưa
            List<ProductSerial> existingForOrder = serialRepository.findByOrderDetail_Order_OrderCodeIgnoreCase(order.getOrderCode())
                    .stream().filter(s -> s.getOrderDetail() != null && s.getOrderDetail().getId().equals(detail.getId())).toList();
            int remaining = qty - existingForOrder.size();
            if (remaining <= 0) continue;

            int months = laptop.getWarrantyMonths() != null ? laptop.getWarrantyMonths() : 12;
            LocalDate warrantyEnd = LocalDate.now().plusMonths(months);

            List<ProductSerial> available = serialRepository.findByLaptop_IdAndStatusOrderByIdAsc(
                    laptop.getId(), SerialStatus.IN_STOCK, PageRequest.of(0, remaining));
            for (ProductSerial s : available) {
                s.setStatus(SerialStatus.SOLD);
                s.setOrderDetail(detail);
                s.setWarrantyEndDate(warrantyEnd);
                serialRepository.save(s);
                remaining--;
            }
            for (int i = 0; i < remaining; i++) {
                String generatedSn = String.format("SN-%s-%s-%02d",
                        laptop.getSku(), order.getOrderCode(), existingForOrder.size() + i + 1);
                while (serialRepository.existsBySerialNumberIgnoreCase(generatedSn)) {
                    generatedSn += "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
                }
                ProductSerial ps = new ProductSerial(laptop, generatedSn);
                ps.setStatus(SerialStatus.SOLD);
                ps.setOrderDetail(detail);
                ps.setWarrantyEndDate(warrantyEnd);
                serialRepository.save(ps);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ProductSerial addSerialToStock(Integer laptopId, String serialNumber) {
        Laptop laptop = laptopRepository.findById(laptopId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Laptop ID: " + laptopId));
        String sn = serialNumber != null ? serialNumber.trim().toUpperCase() : "";
        if (sn.isEmpty()) throw new BusinessException("Số Serial không được để trống.");
        if (serialRepository.existsBySerialNumberIgnoreCase(sn)) {
            throw new BusinessException("Số Serial '" + sn + "' đã tồn tại!");
        }
        return serialRepository.save(new ProductSerial(laptop, sn));
    }

    @Transactional(rollbackFor = Exception.class)
    public WarrantyClaim submitClaim(String serialNumber, String customerName, String phone, String issueDescription) {
        String sn = serialNumber != null ? serialNumber.trim().toUpperCase() : "";
        ProductSerial serial = serialRepository.findBySerialNumberIgnoreCase(sn)
                .orElseThrow(() -> new BusinessException("Không tìm thấy số Serial '" + sn + "' trong hệ thống."));
        if (!serial.isUnderWarranty()) {
            throw new BusinessException("Sản phẩm Serial '" + sn + "' đã hết hạn bảo hành hoặc chưa kích hoạt.");
        }
        WarrantyClaim claim = new WarrantyClaim();
        claim.setSerialNumber(sn);
        claim.setCustomerName(customerName.trim());
        claim.setPhone(phone.trim());
        claim.setIssueDescription(issueDescription.trim());
        claim.setStatus(WarrantyStatus.RECEIVED);
        serial.setStatus(SerialStatus.WARRANTY_CLAIM);
        serialRepository.save(serial);
        return claimRepository.save(claim);
    }

    @Transactional(rollbackFor = Exception.class)
    public WarrantyClaim updateClaimStatus(Integer id, WarrantyStatus status, String note) {
        WarrantyClaim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu bảo hành ID: " + id));
        claim.setStatus(status);
        claim.setTechnicianNote(note);
        claim.setUpdatedAt(LocalDateTime.now());
        if (status == WarrantyStatus.COMPLETED || status == WarrantyStatus.REJECTED) {
            serialRepository.findBySerialNumberIgnoreCase(claim.getSerialNumber()).ifPresent(s -> {
                s.setStatus(SerialStatus.SOLD);
                serialRepository.save(s);
            });
        }
        auditLogService.logAction("UPDATE_WARRANTY", "WarrantyClaim", String.valueOf(id), status.name());
        return claimRepository.save(claim);
    }

    @Transactional(readOnly = true)
    public Page<ProductSerial> listSerials(Pageable pageable) {
        return serialRepository.findAllByOrderByIdDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Page<WarrantyClaim> listClaims(Pageable pageable) {
        return claimRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
}
