package com.wolfcode.MikrotikHotspot.service;

import com.wolfcode.MikrotikHotspot.dto.UserRevenue;
import com.wolfcode.MikrotikHotspot.repository.HotspotUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RevenueService {

    private final HotspotUserInfo hotspotUserInfo;


    public int calculateAllRevenue() {
        return hotspotUserInfo.sumAllRevenue();
    }

    public int calculateTodayRevenue() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);
        return hotspotUserInfo.sumRevenueBetween(todayStart, todayEnd);
    }

    public int calculateThisMonthRevenue() {
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = monthStart.plusMonths(1);
        return hotspotUserInfo.sumRevenueBetween(monthStart, monthEnd);
    }

    public int calculateCustomRevenue(LocalDateTime start, LocalDateTime end) {
        return hotspotUserInfo.sumRevenueBetween(start, end);
    }

    public int calculateRevenueByRouter(String router) {
        return hotspotUserInfo.sumRevenueByRouter(router);
    }

    public Map<String, Integer> calculateRevenueByPackage() {
        return hotspotUserInfo.sumRevenueByPackageType();
    }


    public Map<LocalDate, Integer> getDailyRevenueTrend(int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        return hotspotUserInfo.findDailyRevenueTrend(startDate);
    }

    public Map<Integer, Integer> getHourlyRevenueDistribution(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        return hotspotUserInfo.findHourlyRevenueDistribution(start, end);
    }


}
