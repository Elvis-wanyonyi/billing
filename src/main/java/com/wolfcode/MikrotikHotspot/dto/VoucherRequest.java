package com.wolfcode.MikrotikHotspot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VoucherRequest {

    private String voucherCode;
    private VoucherStatus status;
    private String redeemedBy;

}
