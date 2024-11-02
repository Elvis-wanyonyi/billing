package com.wolfcode.MikrotikHotspot.repository;

import com.wolfcode.MikrotikHotspot.entity.UserInfo;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Repository
public interface HotspotUserInfo extends JpaRepository<UserInfo, Long> {

    UserInfo findByMpesaReceiptNumberIgnoreCase(String code);

    UserInfo findUserByCheckoutRequestID(String checkoutRequestId);

    @Query("SELECT COALESCE(SUM(u.amount), 0) FROM UserInfo u")
    int sumAllRevenue();

    @Query("SELECT COALESCE(SUM(u.amount), 0) FROM UserInfo u WHERE u.createTime BETWEEN :start AND :end")
    int sumRevenueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(u.amount), 0) FROM UserInfo u WHERE u.router = :router")
    int sumRevenueByRouter(@Param("router") String router);

    @Query("SELECT EXTRACT(HOUR FROM u.createTime), COALESCE(SUM(u.amount), 0) FROM UserInfo u WHERE u.createTime BETWEEN :start AND :end GROUP BY EXTRACT(HOUR FROM u.createTime) ORDER BY EXTRACT(HOUR FROM u.createTime)")
    Map<Integer, Integer> findHourlyRevenueDistribution(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT CAST(u.createTime AS LocalDate), COALESCE(SUM(u.amount), 0) FROM UserInfo u WHERE u.createTime >= :startDate GROUP BY CAST(u.createTime AS LocalDate) ORDER BY CAST(u.createTime AS LocalDate) DESC")
    Map<LocalDate, Integer> findDailyRevenueTrend(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT u.packageType, COALESCE(SUM(u.amount), 0) FROM UserInfo u GROUP BY u.packageType")
    Map<String, Integer> sumRevenueByPackageType();


    UserInfo findUserByUserName(@NotNull(message = "Enter Voucher") String voucherCode);
}
