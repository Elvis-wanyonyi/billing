package com.wolfcode.MikrotikHotspot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RouterRequest {

    private String routerName;
    private String routerIPAddress;
    private String username;
    private String password;
    private String dnsName;
    private String description;

}
