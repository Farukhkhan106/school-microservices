package com.successacademy.staffservice.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffPaymentRequest {

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @JsonAlias({"paidAmount", "paymentAmount"})
    private BigDecimal amount;

    private String paymentMode; // CASH, BANK_TRANSFER, UPI, CHEQUE
    private String paymentReference;
    private LocalDate paymentDate;
    private String remarks;

    public void setPaidAmount(BigDecimal paidAmount) {
        if (this.amount == null) {
            this.amount = paidAmount;
        }
    }
}
