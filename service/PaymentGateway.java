package cinemanagement.service;

import java.math.BigDecimal;

import cinemanagement.util.TicketCodeGenerator;

/**
 * Cong thanh toan GIA LAP - luon thanh cong.
 * Phan Payment Management (VNPay HMAC-SHA512, IPN callback) se thay the lop nay:
 * khi do BookingService.confirm() nen tao Booking 'Pending' -> redirect VNPay -> callback moi chuyen 'Confirmed'.
 */
public final class PaymentGateway {

    public record Result(boolean success, String transactionCode, String message) {
    }

    private PaymentGateway() {
    }

    public static Result charge(String method, BigDecimal amount, boolean atCounter) {
        String prefix = atCounter ? "POS-" + method.replace(" ", "").toUpperCase()
                : "SIM-" + method.replace(" ", "").toUpperCase();
        return new Result(true, TicketCodeGenerator.transactionCode(prefix), "OK");
    }
}
