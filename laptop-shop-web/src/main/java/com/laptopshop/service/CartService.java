package com.laptopshop.service;

import com.laptopshop.dto.CartItemDTO;
import com.laptopshop.entity.Account;
import com.laptopshop.entity.CartItemEntity;
import com.laptopshop.entity.Laptop;
import com.laptopshop.exception.BusinessException;
import com.laptopshop.repository.AccountRepository;
import com.laptopshop.repository.CartItemRepository;
import com.laptopshop.repository.LaptopRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class CartService {

    public static final String SESSION_CART_KEY = "cart";

    private final CartItemRepository cartItemRepository;
    private final LaptopRepository laptopRepository;
    private final AccountRepository accountRepository;

    public CartService(CartItemRepository cartItemRepository,
                       LaptopRepository laptopRepository,
                       AccountRepository accountRepository) {
        this.cartItemRepository = cartItemRepository;
        this.laptopRepository = laptopRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    @SuppressWarnings("unchecked")
    public void addItem(Integer laptopId, int quantity, Integer accountId, HttpSession session) {
        Laptop laptop = laptopRepository.findByIdWithDetails(laptopId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy sản phẩm."));
        if (!Boolean.TRUE.equals(laptop.getIsActive()) || laptop.getStockQuantity() <= 0) {
            throw new BusinessException("Sản phẩm hiện đã hết hàng hoặc ngừng kinh doanh.");
        }
        int qtyToAdd = Math.max(1, quantity);
        if (accountId != null) {
            CartItemEntity existing = cartItemRepository.findByAccount_IdAndLaptop_Id(accountId, laptopId).orElse(null);
            int cur = existing != null ? existing.getQuantity() : 0;
            int next = Math.min(laptop.getStockQuantity(), cur + qtyToAdd);
            if (existing == null) {
                Account acc = accountRepository.getReferenceById(accountId);
                existing = new CartItemEntity(acc, laptop, next);
            } else {
                existing.setQuantity(next);
            }
            cartItemRepository.save(existing);
        }
        if (session != null) {
            Map<Integer, Integer> cart = (Map<Integer, Integer>) session.getAttribute(SESSION_CART_KEY);
            if (cart == null) cart = new LinkedHashMap<>();
            int next = Math.min(laptop.getStockQuantity(), cart.getOrDefault(laptopId, 0) + qtyToAdd);
            cart.put(laptopId, next);
            session.setAttribute(SESSION_CART_KEY, cart);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @SuppressWarnings("unchecked")
    public Optional<String> updateQuantity(Integer laptopId, int quantity, Integer accountId, HttpSession session) {
        String warning = null;
        if (quantity <= 0) {
            removeItem(laptopId, accountId, session);
            return Optional.empty();
        }
        Laptop laptop = laptopRepository.findByIdWithDetails(laptopId).orElse(null);
        if (laptop == null) {
            removeItem(laptopId, accountId, session);
            return Optional.empty();
        }
        int capped = quantity;
        if (quantity > laptop.getStockQuantity()) {
            capped = Math.max(0, laptop.getStockQuantity());
            warning = "Sản phẩm '" + laptop.getName() + "' chỉ còn " + laptop.getStockQuantity() + " chiếc trong kho.";
        }
        if (accountId != null) {
            if (capped <= 0) {
                cartItemRepository.deleteByAccount_IdAndLaptop_Id(accountId, laptopId);
            } else {
                CartItemEntity item = cartItemRepository.findByAccount_IdAndLaptop_Id(accountId, laptopId).orElse(null);
                if (item != null) {
                    item.setQuantity(capped);
                    cartItemRepository.save(item);
                } else {
                    cartItemRepository.save(new CartItemEntity(accountRepository.getReferenceById(accountId), laptop, capped));
                }
            }
        }
        if (session != null) {
            Map<Integer, Integer> cart = (Map<Integer, Integer>) session.getAttribute(SESSION_CART_KEY);
            if (cart != null) {
                if (capped <= 0) cart.remove(laptopId);
                else cart.put(laptopId, capped);
                session.setAttribute(SESSION_CART_KEY, cart);
            }
        }
        return Optional.ofNullable(warning);
    }

    @Transactional(rollbackFor = Exception.class)
    @SuppressWarnings("unchecked")
    public void removeItem(Integer laptopId, Integer accountId, HttpSession session) {
        if (accountId != null) {
            cartItemRepository.deleteByAccount_IdAndLaptop_Id(accountId, laptopId);
        }
        if (session != null) {
            Map<Integer, Integer> cart = (Map<Integer, Integer>) session.getAttribute(SESSION_CART_KEY);
            if (cart != null) {
                cart.remove(laptopId);
                session.setAttribute(SESSION_CART_KEY, cart);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void clearCart(Integer accountId, HttpSession session) {
        if (accountId != null) {
            cartItemRepository.deleteByAccount_Id(accountId);
        }
        if (session != null) {
            session.removeAttribute(SESSION_CART_KEY);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @SuppressWarnings("unchecked")
    public void mergeSessionCartToDb(Integer accountId, HttpSession session) {
        if (accountId == null || session == null) return;
        Map<Integer, Integer> sessionCart = (Map<Integer, Integer>) session.getAttribute(SESSION_CART_KEY);
        if (sessionCart != null && !sessionCart.isEmpty()) {
            Account account = accountRepository.findById(accountId).orElse(null);
            if (account != null) {
                for (Map.Entry<Integer, Integer> e : sessionCart.entrySet()) {
                    laptopRepository.findById(e.getKey()).ifPresent(laptop -> {
                        if (Boolean.TRUE.equals(laptop.getIsActive()) && laptop.getStockQuantity() > 0) {
                            CartItemEntity existing = cartItemRepository.findByAccount_IdAndLaptop_Id(accountId, laptop.getId()).orElse(null);
                            int merged = Math.min(laptop.getStockQuantity(),
                                    (existing != null ? existing.getQuantity() : 0) + Math.max(1, e.getValue()));
                            if (existing == null) {
                                cartItemRepository.save(new CartItemEntity(account, laptop, merged));
                            } else {
                                existing.setQuantity(merged);
                                cartItemRepository.save(existing);
                            }
                        }
                    });
                }
            }
        }
        syncDbToSession(accountId, session);
    }

    @Transactional(readOnly = true)
    public void syncDbToSession(Integer accountId, HttpSession session) {
        if (accountId == null || session == null) return;
        Map<Integer, Integer> map = new LinkedHashMap<>();
        for (CartItemEntity entity : cartItemRepository.findByAccount_IdOrderByIdAsc(accountId)) {
            if (entity.getLaptop() != null) {
                map.put(entity.getLaptop().getId(), entity.getQuantity());
            }
        }
        session.setAttribute(SESSION_CART_KEY, map);
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<CartItemDTO> getCartItems(Integer accountId, HttpSession session) {
        List<CartItemDTO> list = new ArrayList<>();
        if (accountId != null) {
            List<CartItemEntity> dbItems = cartItemRepository.findByAccount_IdOrderByIdAsc(accountId);
            if (!dbItems.isEmpty()) {
                for (CartItemEntity e : dbItems) {
                    if (e.getLaptop() != null) {
                        list.add(new CartItemDTO(e.getLaptop(), e.getQuantity()));
                    }
                }
                return list;
            }
        }
        if (session != null) {
            Map<Integer, Integer> cart = (Map<Integer, Integer>) session.getAttribute(SESSION_CART_KEY);
            if (cart != null) {
                for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
                    laptopRepository.findByIdWithDetails(entry.getKey())
                            .ifPresent(l -> list.add(new CartItemDTO(l, entry.getValue())));
                }
            }
        }
        return list;
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public int getCartCount(Integer accountId, HttpSession session) {
        if (accountId != null) {
            int dbCount = cartItemRepository.findByAccount_IdOrderByIdAsc(accountId)
                    .stream().mapToInt(CartItemEntity::getQuantity).sum();
            if (dbCount > 0) return dbCount;
        }
        if (session == null) return 0;
        Object cartObj = session.getAttribute(SESSION_CART_KEY);
        if (cartObj instanceof Map<?, ?> cart) {
            return cart.values().stream()
                    .filter(Integer.class::isInstance)
                    .mapToInt(Integer.class::cast)
                    .sum();
        }
        return 0;
    }

    public BigDecimal calculateSubtotal(List<CartItemDTO> items) {
        return items.stream()
                .map(CartItemDTO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
