package com.wolfcode.MikrotikHotspot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientResponse{

    private String mpesaReceiptNumber;
    private String ipAddress;
    private String macAddress;
    private String phoneNumber;
    private String packageType;
    private int amount;
    private LocalDateTime createTime ;
}
