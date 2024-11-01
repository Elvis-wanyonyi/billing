package com.wolfcode.MikrotikHotspot.repository;

import com.wolfcode.MikrotikHotspot.entity.Routers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RouterRepository extends JpaRepository<Routers, Long> {
    Routers findByRouterName(String routerName);

    void deleteByRouterName(String routerName);
}
