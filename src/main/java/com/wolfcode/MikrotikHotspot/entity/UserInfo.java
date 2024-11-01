package com.wolfcode.MikrotikHotspot.entity;

import com.wolfcode.MikrotikHotspot.dto.LoginBy;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "user_info")
public class UserInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;
    private String checkoutRequestID;
    private String macAddress;
    private LocalDateTime createTime;
    private String packageType;
    private String ipAddress;
    private String router;
    private String mpesaReceiptNumber;
    private String phoneNumber;
    private int amount;
    private String userName;
    private String password;
    @Enumerated(EnumType.STRING)
    private LoginBy loginBy;

}
