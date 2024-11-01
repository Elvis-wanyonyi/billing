package com.wolfcode.MikrotikHotspot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateVouchers {

    private String routerName;
    private int quantity;
    private String packageType;
    private List<VoucherRequest> voucherRequests;

}
