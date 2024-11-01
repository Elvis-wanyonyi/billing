package com.wolfcode.MikrotikHotspot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
public class PaymentSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tempRequestId;
    private String checkoutRequestID;

    private String ip;
    private String mac;
    private String packageType;
    private String routerName;
    private String phoneNumber;
    private String amount;
    @Enumerated(EnumType.STRING)
    private Status status;

}
