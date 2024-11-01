package com.wolfcode.MikrotikHotspot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateVoucher {

    private String packageType;
    private VoucherStatus status;
    private String redeemedBy;
}

