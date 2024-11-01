package com.wolfcode.MikrotikHotspot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouterClientResponse {

    private String name;
    private String macAddress;
    private String ipAddress;
    private String profile;

}
