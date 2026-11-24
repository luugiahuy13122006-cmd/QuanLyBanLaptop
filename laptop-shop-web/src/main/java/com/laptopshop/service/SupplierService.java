package com.laptopshop.service;

import com.laptopshop.entity.ImportReceipt;
import com.laptopshop.entity.Supplier;
import com.laptopshop.exception.BusinessException;
import com.laptopshop.exception.ResourceNotFoundException;
import com.laptopshop.repository.ImportReceiptRepository;
import com.laptopshop.repository.SupplierRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final ImportReceiptRepository receiptRepository;

    public SupplierService(SupplierRepository supplierRepository, ImportReceiptRepository receiptRepository) {
        this.supplierRepository = supplierRepository;
        this.receiptRepository = receiptRepository;
    }

    @Transactional(readOnly = true)
    public Page<Supplier> search(String keyword, Pageable pageable) {
        return supplierRepository.findByNameContainingIgnoreCaseOrderByIdDesc(keyword == null ? "" : keyword.trim(), pageable);
    }

    @Transactional(readOnly = true)
    public List<Supplier> findAllActive() {
        return supplierRepository.findByIsActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Supplier getById(Integer id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhà cung cấp ID: " + id));
    }

    @Transactional
    public Supplier save(Supplier form) {
        String name = form.getName() != null ? form.getName().trim() : "";
        if (name.isEmpty()) throw new BusinessException("Tên nhà cung cấp không được để trống.");
        if (form.getId() == null && supplierRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException("Nhà cung cấp '" + name + "' đã tồn tại.");
        }
        if (form.getId() != null && supplierRepository.existsByNameIgnoreCaseAndIdNot(name, form.getId())) {
            throw new BusinessException("Tên nhà cung cấp '" + name + "' đã tồn tại.");
        }
        Supplier target = form.getId() != null ? getById(form.getId()) : new Supplier();
        target.setName(name);
        target.setTaxCode(form.getTaxCode());
        target.setContactName(form.getContactName());
        target.setPhone(form.getPhone());
        target.setEmail(form.getEmail());
        target.setAddress(form.getAddress());
        target.setIsActive(form.getIsActive() != null ? form.getIsActive() : true);
        return supplierRepository.save(target);
    }

    @Transactional(rollbackFor = Exception.class)
    public ImportReceipt recordPayablePayment(Integer receiptId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException("Số tiền thanh toán phải lớn hơn 0.");
        }
        ImportReceipt receipt = receiptRepository.findByIdForUpdate(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu nhập ID: " + receiptId));
        BigDecimal currentPaid = receipt.getPaidAmount() != null ? receipt.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal nextPaid = currentPaid.add(amount);
        if (nextPaid.compareTo(receipt.getTotalAmount()) > 0) {
            throw new BusinessException("Tổng đã trả vượt quá tổng giá trị phiếu nhập (" + receipt.getTotalAmount() + ").");
        }
        receipt.setPaidAmount(nextPaid);
        receipt.setPayableStatus(nextPaid.compareTo(receipt.getTotalAmount()) >= 0 ? "PAID" : "PARTIAL");
        return receiptRepository.save(receipt);
    }
}
