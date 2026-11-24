package com.laptopshop.service;

import com.laptopshop.entity.CodeSequence;
import com.laptopshop.repository.CodeSequenceRepository;
import com.laptopshop.repository.ImportReceiptRepository;
import com.laptopshop.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Predicate;

@Service
public class CodeGeneratorService {

    public static final String ORDER_SEQ = "DON_HANG";
    public static final String RECEIPT_SEQ = "PHIEU_NHAP";

    private final CodeSequenceRepository sequenceRepository;
    private final OrderRepository orderRepository;
    private final ImportReceiptRepository receiptRepository;

    public CodeGeneratorService(CodeSequenceRepository sequenceRepository,
                                OrderRepository orderRepository,
                                ImportReceiptRepository receiptRepository) {
        this.sequenceRepository = sequenceRepository;
        this.orderRepository = orderRepository;
        this.receiptRepository = receiptRepository;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public String nextOrderCode() {
        return next(ORDER_SEQ, "DH", orderRepository::existsByOrderCodeIgnoreCase);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public String nextReceiptCode() {
        return next(RECEIPT_SEQ, "PN", receiptRepository::existsByReceiptCodeIgnoreCase);
    }

    private String next(String sequenceName, String prefix, Predicate<String> exists) {
        CodeSequence seq = sequenceRepository.findByNameForUpdate(sequenceName)
                .orElseGet(() -> sequenceRepository.saveAndFlush(new CodeSequence(sequenceName, 0)));
        String code;
        do {
            seq.setCurrentValue(seq.getCurrentValue() + 1);
            code = String.format("%s%03d", prefix, seq.getCurrentValue());
        } while (exists.test(code));
        sequenceRepository.save(seq);
        return code;
    }
}
