package com.wolfcode.MikrotikHotspot.entity;

import com.wolfcode.MikrotikHotspot.dto.VoucherStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "voucher")
public class Voucher {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;
    private String voucherCode;
    private String packageType;
    @Enumerated(EnumType.STRING)
    private VoucherStatus status;
    private LocalDateTime createdAt;
    private String redeemedBy;
    private String ipAddress;
    private LocalDateTime expiryDate;
    private String routerName;
}
