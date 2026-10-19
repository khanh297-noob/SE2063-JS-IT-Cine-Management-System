package cinemanagement.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.io.IOException;

/**
 * Đặt UTF-8 cho mọi request/response. Thiếu filter này thì dữ liệu tiếng Việt
 * gửi từ form (tên phim, địa chỉ chi nhánh...) sẽ bị lỗi font khi lưu.
 *
 * Khai báo trong web.xml (không dùng @WebFilter) để luôn chạy TRƯỚC mọi filter khác,
 * vd AuthFilter: setCharacterEncoding chỉ có tác dụng trước lần getParameter() đầu tiên.
 */
public class EncodingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        chain.doFilter(request, response);
    }
}
