package com.laptopshop.service;

import com.laptopshop.dto.ImportReceiptForm;
import com.laptopshop.entity.ImportReceipt;
import com.laptopshop.entity.ImportReceiptStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ImportReceiptService {
    ImportReceipt createReceipt(ImportReceiptForm form, Integer accountId);
    Page<ImportReceipt> getReceipts(ImportReceiptStatus status, String keyword, Pageable pageable);
    ImportReceipt getReceiptById(Integer id);
    ImportReceipt approveReceipt(Integer id, Integer adminAccountId);
    ImportReceipt rejectReceipt(Integer id, Integer adminAccountId, String reason);
}
