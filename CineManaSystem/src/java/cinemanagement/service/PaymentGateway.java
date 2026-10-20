package cinemanagement.service;

import java.math.BigDecimal;

import cinemanagement.util.TicketCodeGenerator;

public final class PaymentGateway {
    public record Result(boolean success, String transactionCode, String message) {
    }

    private PaymentGateway() {
    }

    public static Result charge(String method, BigDecimal amount) {
        String prefix = "SIM-" + method.replace(" ", "").toUpperCase();
        return new Result(true, TicketCodeGenerator.transactionCode(prefix), "OK");
    }
}
