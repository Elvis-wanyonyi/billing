package com.wolfcode.MikrotikHotspot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "Routers")
public class Routers {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String routerName;
    private String routerIPAddress;
    private String username;
    private String password;
    private String dnsName;
    private String description;
}
