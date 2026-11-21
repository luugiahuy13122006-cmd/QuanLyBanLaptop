package com.laptopshop.exception;

// Thành viên phụ trách: TV5 - Vũ Gia Bảo (xử lý ngoại lệ nghiệp vụ toàn hệ thống)
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
