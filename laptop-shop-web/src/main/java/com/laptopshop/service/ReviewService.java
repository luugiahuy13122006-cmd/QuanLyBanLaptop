package com.laptopshop.service;

import com.laptopshop.entity.*;
import com.laptopshop.exception.BusinessException;
import com.laptopshop.exception.ResourceNotFoundException;
import com.laptopshop.repository.CustomerRepository;
import com.laptopshop.repository.LaptopRepository;
import com.laptopshop.repository.OrderRepository;
import com.laptopshop.repository.ProductReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReviewService {

    private final ProductReviewRepository reviewRepository;
    private final LaptopRepository laptopRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    public ReviewService(ProductReviewRepository reviewRepository,
                         LaptopRepository laptopRepository,
                         CustomerRepository customerRepository,
                         OrderRepository orderRepository) {
        this.reviewRepository = reviewRepository;
        this.laptopRepository = laptopRepository;
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductReview> getApprovedReviews(Integer laptopId) {
        return reviewRepository.findByLaptop_IdAndApprovedTrueOrderByCreatedAtDesc(laptopId);
    }

    @Transactional(readOnly = true)
    public double getAverageRating(Integer laptopId) {
        Double avg = reviewRepository.getAverageRating(laptopId);
        return avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
    }

    @Transactional(readOnly = true)
    public long getReviewCount(Integer laptopId) {
        return reviewRepository.countByLaptop_IdAndApprovedTrue(laptopId);
    }

    @Transactional(rollbackFor = Exception.class)
    public ProductReview submitReview(Integer laptopId, Integer accountId, int rating, String title, String content) {
        if (rating < 1 || rating > 5) {
            throw new BusinessException("Số sao đánh giá phải từ 1 đến 5.");
        }
        if (content == null || content.isBlank()) {
            throw new BusinessException("Vui lòng nhập nội dung đánh giá.");
        }
        Customer customer = customerRepository.findByAccountId(accountId)
                .orElseThrow(() -> new BusinessException("Chỉ tài khoản khách hàng mới có thể gửi đánh giá."));
        Laptop laptop = laptopRepository.findById(laptopId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy laptop."));

        if (reviewRepository.existsByLaptop_IdAndCustomer_Id(laptopId, customer.getId())) {
            throw new BusinessException("Bạn đã đánh giá sản phẩm này rồi.");
        }

        boolean verifiedBuyer = orderRepository.findByCustomerIdOrderByOrderDateDesc(customer.getId()).stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
                .anyMatch(o -> o.getDetails().stream()
                        .anyMatch(d -> d.getLaptop() != null && d.getLaptop().getId().equals(laptopId)));

        if (!verifiedBuyer) {
            throw new BusinessException("Chỉ khách hàng đã mua và hoàn thành đơn hàng chứa sản phẩm này mới có thể đánh giá.");
        }

        ProductReview review = new ProductReview();
        review.setLaptop(laptop);
        review.setCustomer(customer);
        review.setRating(rating);
        review.setTitle(title != null ? title.trim() : null);
        review.setContent(content.trim());
        review.setVerifiedPurchase(true);
        review.setApproved(false); // chờ kiểm duyệt hoặc admin bật duyệt
        return reviewRepository.save(review);
    }

    @Transactional(readOnly = true)
    public Page<ProductReview> listAllForModeration(Pageable pageable) {
        return reviewRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional
    public ProductReview moderate(Integer reviewId, boolean approve, String shopReply) {
        ProductReview r = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá ID: " + reviewId));
        r.setApproved(approve);
        if (shopReply != null && !shopReply.isBlank()) {
            r.setShopReply(shopReply.trim());
        }
        return reviewRepository.save(r);
    }
}
