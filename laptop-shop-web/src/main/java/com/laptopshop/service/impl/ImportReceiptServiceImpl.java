package com.laptopshop.service.impl;

import com.laptopshop.dto.ImportItemRequest;
import com.laptopshop.dto.ImportReceiptForm;
import com.laptopshop.entity.*;
import com.laptopshop.exception.BusinessException;
import com.laptopshop.exception.ResourceNotFoundException;
import com.laptopshop.repository.*;
import com.laptopshop.service.AuditLogService;
import com.laptopshop.service.CodeGeneratorService;
import com.laptopshop.service.ImportReceiptService;
import com.laptopshop.service.StockMovementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ImportReceiptServiceImpl implements ImportReceiptService {

    private static final Logger log = LoggerFactory.getLogger(ImportReceiptServiceImpl.class);

    private final ImportReceiptRepository receiptRepository;
    private final LaptopRepository laptopRepository;
    private final AccountRepository accountRepository;
    private final CodeGeneratorService codeGenerator;
    private final StockMovementService stockMovementService;
    private final AuditLogService auditLogService;

    public ImportReceiptServiceImpl(ImportReceiptRepository receiptRepository,
                                    LaptopRepository laptopRepository,
                                    AccountRepository accountRepository,
                                    CodeGeneratorService codeGenerator,
                                    StockMovementService stockMovementService,
                                    AuditLogService auditLogService) {
        this.receiptRepository = receiptRepository;
        this.laptopRepository = laptopRepository;
        this.accountRepository = accountRepository;
        this.codeGenerator = codeGenerator;
        this.stockMovementService = stockMovementService;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportReceipt createReceipt(ImportReceiptForm form, Integer accountId) {
        Account creator = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản ID: " + accountId));
        String roleName = creator.getRole() != null ? creator.getRole().getRoleName() : "";
        if (!"ADMIN".equalsIgnoreCase(roleName) && !"STAFF".equalsIgnoreCase(roleName)) {
            throw new BusinessException("Chỉ Quản trị viên hoặc Nhân viên mới được lập phiếu nhập kho.");
        }

        ImportReceipt receipt = new ImportReceipt();
        receipt.setSupplier(form.getSupplier().trim());
        receipt.setNote(form.getNote() != null && !form.getNote().isBlank() ? form.getNote().trim() : null);
        receipt.setStatus(ImportReceiptStatus.PENDING);
        receipt.setCreatedBy(creator);
        receipt.setCreatedAt(LocalDateTime.now());

        Set<Integer> seen = new HashSet<>();
        for (ImportItemRequest item : form.getItems()) {
            if (!seen.add(item.getLaptopId())) {
                throw new BusinessException("Laptop bị trùng trong phiếu nhập. Hãy gộp thành một dòng.");
            }
            Laptop laptop = laptopRepository.findById(item.getLaptopId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Laptop ID: " + item.getLaptopId()));
            if (!Boolean.TRUE.equals(laptop.getIsActive())) {
                throw new BusinessException("Laptop '" + laptop.getName() + "' đã ngừng kinh doanh, không thể lập phiếu nhập.");
            }
            receipt.addDetail(new ImportReceiptDetail(laptop, item.getQuantity(), item.getUnitCost()));
        }
        receipt.recalculateTotal();
        receipt.setReceiptCode(codeGenerator.nextReceiptCode());

        ImportReceipt saved = receiptRepository.save(receipt);
        auditLogService.logAction(creator.getId(), creator.getUsername(),
                "CREATE_IMPORT", "ImportReceipt", saved.getReceiptCode(),
                "Lập phiếu nhập từ NCC " + saved.getSupplier() + ", tổng " + saved.getTotalAmount());
        log.info("[IMPORT | THREAD: {}] {} tạo phiếu nhập {} (chờ duyệt)",
                Thread.currentThread().getName(), creator.getUsername(), saved.getReceiptCode());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ImportReceipt> getReceipts(ImportReceiptStatus status, String keyword, Pageable pageable) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return receiptRepository.filterReceipts(status, kw, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ImportReceipt getReceiptById(Integer id) {
        return receiptRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu nhập ID: " + id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportReceipt approveReceipt(Integer id, Integer adminAccountId) {
        Account admin = requireAdmin(adminAccountId);
        ImportReceipt receipt = receiptRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu nhập ID: " + id));
        if (receipt.getStatus() != ImportReceiptStatus.PENDING) {
            throw new BusinessException("Phiếu nhập này đã được xử lý (" + receipt.getStatus().getDisplayName() + ").");
        }
        String threadName = Thread.currentThread().getName();
        List<ImportReceiptDetail> sorted = receipt.getDetails().stream()
                .sorted(Comparator.comparing((ImportReceiptDetail d) -> d.getLaptop().getId()))
                .toList();
        for (ImportReceiptDetail d : sorted) {
            Laptop laptop = laptopRepository.findByIdForUpdate(d.getLaptop().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Laptop ID: " + d.getLaptop().getId()));
            int oldStock = laptop.getStockQuantity() != null ? laptop.getStockQuantity() : 0;
            int qty = d.getQuantity();
            int newStock = oldStock + qty;

            // Tính lại giá nhập bình quân gia quyền
            BigDecimal oldCost = laptop.getPurchasePrice() != null ? laptop.getPurchasePrice() : BigDecimal.ZERO;
            if (newStock > 0 && d.getUnitCost() != null) {
                BigDecimal totalOldVal = oldCost.multiply(BigDecimal.valueOf(oldStock));
                BigDecimal totalNewVal = d.getUnitCost().multiply(BigDecimal.valueOf(qty));
                BigDecimal weightedCost = totalOldVal.add(totalNewVal)
                        .divide(BigDecimal.valueOf(newStock), 2, RoundingMode.HALF_UP);
                laptop.setPurchasePrice(weightedCost);
            }

            laptop.setStockQuantity(newStock);
            laptop.setUpdatedAt(LocalDateTime.now());
            laptopRepository.save(laptop);

            stockMovementService.record(laptop, StockMovementType.IMPORT, qty, oldStock, newStock,
                    receipt.getReceiptCode(), "Duyệt phiếu nhập từ " + receipt.getSupplier(), admin.getId());
            log.info("[IMPORT | THREAD: {}] Cộng kho laptop ID={} +{} => {} (giá vốn BQ={})",
                    threadName, laptop.getId(), qty, newStock, laptop.getPurchasePrice());
        }
        receipt.setStatus(ImportReceiptStatus.APPROVED);
        receipt.setApprovedBy(admin);
        receipt.setApprovedAt(LocalDateTime.now());
        receipt.setRejectReason(null);
        ImportReceipt saved = receiptRepository.save(receipt);
        auditLogService.logAction(admin.getId(), admin.getUsername(),
                "APPROVE_IMPORT", "ImportReceipt", saved.getReceiptCode(),
                "Duyệt phiếu nhập và cập nhật tồn kho + giá vốn bình quân");
        return saved;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportReceipt rejectReceipt(Integer id, Integer adminAccountId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Vui lòng nhập lý do từ chối phiếu nhập.");
        }
        Account admin = requireAdmin(adminAccountId);
        ImportReceipt receipt = receiptRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu nhập ID: " + id));
        if (receipt.getStatus() != ImportReceiptStatus.PENDING) {
            throw new BusinessException("Phiếu nhập này đã được xử lý (" + receipt.getStatus().getDisplayName() + ").");
        }
        String clean = reason.trim();
        receipt.setStatus(ImportReceiptStatus.REJECTED);
        receipt.setApprovedBy(admin);
        receipt.setApprovedAt(LocalDateTime.now());
        receipt.setRejectReason(clean.length() > 500 ? clean.substring(0, 500) : clean);
        ImportReceipt saved = receiptRepository.save(receipt);
        auditLogService.logAction(admin.getId(), admin.getUsername(),
                "REJECT_IMPORT", "ImportReceipt", saved.getReceiptCode(),
                "Từ chối phiếu nhập: " + receipt.getRejectReason());
        return saved;
    }

    private Account requireAdmin(Integer accountId) {
        Account acc = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản ID: " + accountId));
        if (acc.getRole() == null || !"ADMIN".equalsIgnoreCase(acc.getRole().getRoleName())) {
            throw new BusinessException("Chỉ Quản lý (ADMIN) mới được duyệt hoặc từ chối phiếu nhập.");
        }
        return acc;
    }
}
