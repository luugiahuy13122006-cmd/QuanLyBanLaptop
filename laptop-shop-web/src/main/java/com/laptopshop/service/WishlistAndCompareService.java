package com.laptopshop.service;

import com.laptopshop.entity.Account;
import com.laptopshop.entity.Laptop;
import com.laptopshop.entity.WishlistItem;
import com.laptopshop.exception.BusinessException;
import com.laptopshop.repository.AccountRepository;
import com.laptopshop.repository.LaptopRepository;
import com.laptopshop.repository.WishlistRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class WishlistAndCompareService {

    public static final String SESSION_COMPARE_KEY = "compareIds";
    public static final String SESSION_RECENT_KEY = "recentLaptopIds";
    public static final int MAX_COMPARE = 4;
    public static final int MAX_RECENT = 8;

    private final WishlistRepository wishlistRepository;
    private final AccountRepository accountRepository;
    private final LaptopRepository laptopRepository;

    public WishlistAndCompareService(WishlistRepository wishlistRepository,
                                     AccountRepository accountRepository,
                                     LaptopRepository laptopRepository) {
        this.wishlistRepository = wishlistRepository;
        this.accountRepository = accountRepository;
        this.laptopRepository = laptopRepository;
    }

    @Transactional
    public boolean toggleWishlist(Integer accountId, Integer laptopId) {
        if (accountId == null) throw new BusinessException("Vui lòng đăng nhập để lưu sản phẩm yêu thích.");
        if (wishlistRepository.existsByAccount_IdAndLaptop_Id(accountId, laptopId)) {
            wishlistRepository.deleteByAccount_IdAndLaptop_Id(accountId, laptopId);
            return false;
        }
        Account acc = accountRepository.getReferenceById(accountId);
        Laptop laptop = laptopRepository.findById(laptopId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy sản phẩm."));
        wishlistRepository.save(new WishlistItem(acc, laptop));
        return true;
    }

    @Transactional(readOnly = true)
    public List<WishlistItem> getWishlist(Integer accountId) {
        if (accountId == null) return List.of();
        return wishlistRepository.findByAccount_IdOrderByCreatedAtDesc(accountId);
    }

    @Transactional(readOnly = true)
    public boolean isInWishlist(Integer accountId, Integer laptopId) {
        return accountId != null && wishlistRepository.existsByAccount_IdAndLaptop_Id(accountId, laptopId);
    }

    @SuppressWarnings("unchecked")
    public List<Integer> getCompareIds(HttpSession session) {
        if (session == null) return List.of();
        Object obj = session.getAttribute(SESSION_COMPARE_KEY);
        if (obj instanceof List<?> list) {
            List<Integer> res = new ArrayList<>();
            for (Object o : list) if (o instanceof Integer i) res.add(i);
            return res;
        }
        return List.of();
    }

    public List<Integer> addToCompare(Integer laptopId, HttpSession session) {
        List<Integer> current = new ArrayList<>(getCompareIds(session));
        if (!current.contains(laptopId)) {
            if (current.size() >= MAX_COMPARE) {
                throw new BusinessException("Chỉ được so sánh tối đa " + MAX_COMPARE + " laptop cùng lúc.");
            }
            current.add(laptopId);
            if (session != null) session.setAttribute(SESSION_COMPARE_KEY, current);
        }
        return current;
    }

    public List<Integer> removeFromCompare(Integer laptopId, HttpSession session) {
        List<Integer> current = new ArrayList<>(getCompareIds(session));
        current.remove(laptopId);
        if (session != null) session.setAttribute(SESSION_COMPARE_KEY, current);
        return current;
    }

    @Transactional(readOnly = true)
    public List<Laptop> getCompareLaptops(HttpSession session) {
        List<Integer> ids = getCompareIds(session);
        if (ids.isEmpty()) return List.of();
        List<Laptop> list = new ArrayList<>();
        for (Integer id : ids) {
            laptopRepository.findByIdWithDetails(id).ifPresent(list::add);
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    public void recordRecentlyViewed(Integer laptopId, HttpSession session) {
        if (session == null || laptopId == null) return;
        LinkedList<Integer> recent = new LinkedList<>();
        Object obj = session.getAttribute(SESSION_RECENT_KEY);
        if (obj instanceof List<?> l) {
            for (Object o : l) if (o instanceof Integer i) recent.add(i);
        }
        recent.remove(laptopId);
        recent.addFirst(laptopId);
        while (recent.size() > MAX_RECENT) {
            recent.removeLast();
        }
        session.setAttribute(SESSION_RECENT_KEY, new ArrayList<>(recent));
    }

    @Transactional(readOnly = true)
    public List<Laptop> getRecentlyViewedLaptops(HttpSession session, Integer excludeId) {
        if (session == null) return List.of();
        Object obj = session.getAttribute(SESSION_RECENT_KEY);
        if (!(obj instanceof List<?> raw)) return List.of();
        List<Laptop> out = new ArrayList<>();
        for (Object o : raw) {
            if (o instanceof Integer id && !id.equals(excludeId)) {
                laptopRepository.findByIdWithDetails(id)
                        .filter(l -> Boolean.TRUE.equals(l.getIsActive()))
                        .ifPresent(out::add);
                if (out.size() >= 4) break;
            }
        }
        return out;
    }
}
