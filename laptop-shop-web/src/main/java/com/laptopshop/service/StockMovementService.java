package com.laptopshop.service;

import com.laptopshop.entity.Laptop;
import com.laptopshop.entity.StockMovement;
import com.laptopshop.entity.StockMovementType;
import com.laptopshop.repository.StockMovementRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockMovementService {

    private final StockMovementRepository stockMovementRepository;

    public StockMovementService(StockMovementRepository stockMovementRepository) {
        this.stockMovementRepository = stockMovementRepository;
    }

    public StockMovement record(Laptop laptop, StockMovementType type, int delta,
                                int before, int after, String referenceCode,
                                String reason, Integer actorAccountId) {
        StockMovement movement = new StockMovement(laptop, type, delta, before, after,
                referenceCode, reason, actorAccountId);
        return stockMovementRepository.save(movement);
    }

    @Transactional(readOnly = true)
    public Page<StockMovement> listMovements(Integer laptopId, StockMovementType type, Pageable pageable) {
        if (laptopId != null) {
            return stockMovementRepository.findByLaptop_IdOrderByCreatedAtDesc(laptopId, pageable);
        }
        if (type != null) {
            return stockMovementRepository.findByMovementTypeOrderByCreatedAtDesc(type, pageable);
        }
        return stockMovementRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
}
