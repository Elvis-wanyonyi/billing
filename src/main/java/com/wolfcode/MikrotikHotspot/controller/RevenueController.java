package com.wolfcode.MikrotikHotspot.controller;

import com.wolfcode.MikrotikHotspot.dto.UserRevenue;
import com.wolfcode.MikrotikHotspot.service.RevenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/revenue")
@RequiredArgsConstructor
public class RevenueController {

    private final RevenueService revenueService;


    @GetMapping
    public ResponseEntity<Integer> getAllRevenue() {
        int totalRevenue = revenueService.calculateAllRevenue();
        return ResponseEntity.ok(totalRevenue);
    }

    @GetMapping("/today")
    public ResponseEntity<Integer> getTodayRevenue() {
        int todayRevenue = revenueService.calculateTodayRevenue();
        return ResponseEntity.ok(todayRevenue);
    }

    @GetMapping("/month")
    public ResponseEntity<Integer> getThisMonthRevenue() {
        int monthRevenue = revenueService.calculateThisMonthRevenue();
        return ResponseEntity.ok(monthRevenue);
    }

    @GetMapping("/filter")
    public ResponseEntity<Integer> getCustomRevenue(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        int customRevenue = revenueService.calculateCustomRevenue(start, end);
        return ResponseEntity.ok(customRevenue);
    }

    @GetMapping("/by-router")
    public ResponseEntity<Integer> getRevenueByRouter(@RequestParam("router") String router) {
        int routerRevenue = revenueService.calculateRevenueByRouter(router);
        return ResponseEntity.ok(routerRevenue);
    }

    @GetMapping("/hourly-distribution")
    public ResponseEntity<Map<Integer, Integer>> getHourlyRevenueDistribution(
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        Map<Integer, Integer> hourlyDistribution = revenueService.getHourlyRevenueDistribution(date);
        return ResponseEntity.ok(hourlyDistribution);
    }

    @GetMapping("/daily-trend")
    public ResponseEntity<Map<LocalDate, Integer>> getDailyRevenueTrend(
            @RequestParam("days") int days) {
        Map<LocalDate, Integer> dailyRevenueTrend = revenueService.getDailyRevenueTrend(days);
        return ResponseEntity.ok(dailyRevenueTrend);
    }

    @GetMapping("/by-package")
    public ResponseEntity<Map<String, Integer>> getRevenueByPackage() {
        Map<String, Integer> revenueByPackage = revenueService.calculateRevenueByPackage();
        return ResponseEntity.ok(revenueByPackage);
    }

}
