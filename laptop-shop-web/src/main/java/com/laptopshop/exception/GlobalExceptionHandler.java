package com.example.laptopshop.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

/** Bắt lỗi toàn hệ thống và hiển thị trang lỗi thân thiện thay vì trang lỗi mặc định. */
@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Ví dụ /san-pham/abc (id không phải số). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ModelAndView badParam(MethodArgumentTypeMismatchException e) {
        return view("error/404", HttpStatus.BAD_REQUEST, "Đường dẫn hoặc tham số không hợp lệ.");
    }

    /** Lỗi kết nối / truy vấn SQL Server. */
    @ExceptionHandler(DataAccessException.class)
    public ModelAndView database(DataAccessException e) {
        log.error("Lỗi cơ sở dữ liệu", e);
        return view("error/500", HttpStatus.INTERNAL_SERVER_ERROR,
                "Không truy cập được cơ sở dữ liệu. Vui lòng thử lại sau.");
    }

    /** Mọi lỗi runtime còn lại. */
    @ExceptionHandler(RuntimeException.class)
    public ModelAndView runtime(RuntimeException e) {
        // Để Spring Security / Spring MVC tự xử lý các lỗi này (403, 404...)
        if (e instanceof AccessDeniedException || e instanceof ResponseStatusException) {
            throw e;
        }
        log.error("Lỗi không mong muốn", e);
        return view("error/500", HttpStatus.INTERNAL_SERVER_ERROR,
                "Hệ thống gặp sự cố. Vui lòng thử lại sau.");
    }

    private ModelAndView view(String name, HttpStatus status, String message) {
        ModelAndView mav = new ModelAndView(name);
        mav.setStatus(status);
        mav.addObject("message", message);
        return mav;
    }
}
