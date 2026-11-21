package com.laptopshop.exception;

// Thành viên phụ trách: TV5 - Vũ Gia Bảo
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
