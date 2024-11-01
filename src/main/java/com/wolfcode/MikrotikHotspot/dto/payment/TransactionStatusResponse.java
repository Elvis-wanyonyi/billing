package com.wolfcode.MikrotikHotspot.dto.payment;

import lombok.Data;

@Data
public class TransactionStatusResponse {
    private String responseCode;
    private String responseDescription;
    private String resultCode;
    private String resultDescription;
}
