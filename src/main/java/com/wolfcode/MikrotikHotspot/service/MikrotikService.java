package com.wolfcode.MikrotikHotspot.service;


import com.wolfcode.MikrotikHotspot.dto.*;
import com.wolfcode.MikrotikHotspot.entity.Routers;
import com.wolfcode.MikrotikHotspot.entity.UserInfo;
import com.wolfcode.MikrotikHotspot.entity.UserSession;
import com.wolfcode.MikrotikHotspot.entity.Voucher;
import com.wolfcode.MikrotikHotspot.repository.HotspotUserInfo;
import com.wolfcode.MikrotikHotspot.repository.RouterRepository;
import com.wolfcode.MikrotikHotspot.repository.UserSessionRepository;
import com.wolfcode.MikrotikHotspot.repository.VoucherRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.legrange.mikrotik.MikrotikApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MikrotikService {

    private final MikrotikClient mikroTikClient;
    private final HotspotUserInfo hotspotUserInfo;
    private final VoucherRepository voucherRepository;
    private final RouterRepository routerRepository;
    private final UserSessionRepository userSessionRepository;


    public void connectUser(String ipAddress, String packageType, String macAddress,
                            String phoneNumber, String mpesaReceiptNumber, String amount, String routerName, String checkoutRequestID) {
        try {
            String username = generateUsername(macAddress, phoneNumber);
            String password = generatePassword();
            String profile = mapPackageToProfile(packageType);
            String uptimeLimit = mapPackageToUptime(packageType);

            log.info("User Created: username{} password{} ipAddress{} macAddress{}",
                    username, password, ipAddress, macAddress);

            int intAmount = Integer.parseInt(amount);
            UserInfo userInfo = UserInfo.builder()
                    .checkoutRequestID(checkoutRequestID)
                    .mpesaReceiptNumber(mpesaReceiptNumber)
                    .phoneNumber(phoneNumber)
                    .router(routerName)
                    .macAddress(macAddress)
                    .ipAddress(ipAddress)
                    .packageType(packageType)
                    .amount(intAmount)
                    .createTime(LocalDateTime.now())
                    .userName(username)
                    .password(password)
                    .build();
            hotspotUserInfo.save(userInfo);

            mikroTikClient.createHotspotUser(username, password, ipAddress,
                    macAddress, profile, uptimeLimit, routerName);

            Duration sessionLimit = Duration.ofHours(mapPackageToSessionLimit(packageType));
            LocalDateTime sessionStartTime = LocalDateTime.now();
            LocalDateTime sessionEndTime = sessionStartTime.plus(sessionLimit);

            UserSession userSession = UserSession.builder()
                    .routerName(routerName)
                    .username(username)
                    .sessionStartTime(LocalDateTime.now())
                    .sessionEndTime(sessionEndTime)
                    .build();
            userSessionRepository.save(userSession);


        } catch (MikrotikApiException e) {
            e.printStackTrace();
        }
    }

    public UserCredentials getUserCredentials(String checkoutRequestID) {
        UserInfo user = hotspotUserInfo.findUserByCheckoutRequestID(checkoutRequestID);
        if (user == null) {
            throw new IllegalArgumentException("Failed to confirm the payment");
        }
        return UserCredentials.builder()
                .username(user.getUserName())
                .password(user.getPassword())
                .build();

    }

    public String generateUsername(String macAddress, String phoneNumber) {
        String mac = macAddress.substring(0, 4);
        return phoneNumber + mac;
    }

    public String generatePassword() {
        return UUID.randomUUID().toString().substring(0, 4);
    }

    public String mapPackageToProfile(String packageType) {
        return switch (packageType) {
            case "1hr" -> "1hr";
            case "3hrs" -> "3hrs";
            case "1day" -> "1day";
            case "weekly" -> "weekly";
            case "30days" -> "30days";
            default -> throw new IllegalArgumentException("Package not found contact admin: " + packageType);
        };
    }

    public String mapPackageToUptime(String packageType) {
        return switch (packageType) {
            case "1hr" -> "01:00:00";
            case "3hrs" -> "03:00:00";
            case "1day" -> "24:00:00";
            case "weekly" -> "168:00:00";
            case "30days" -> "720:00:00";
            default -> throw new IllegalArgumentException("Package not found contact admin: " + packageType);
        };
    }

    public void createHotspotVoucher(CreateVouchers voucherRequests) {

        for (int i = 0; i < voucherRequests.getQuantity(); i++) {
            String voucherCode = UUID.randomUUID().toString().substring(0, 4);

            Voucher voucher = Voucher.builder()
                    .voucherCode(voucherCode)
                    .packageType(voucherRequests.getPackageType())
                    .createdAt(LocalDateTime.now())
                    .status(VoucherStatus.ACTIVE)
                    .redeemedBy(null)
                    .ipAddress(null)
                    .routerName(voucherRequests.getRouterName())
                    .build();
            voucherRepository.save(voucher);
        }
    }


    public Map<String, String> loginWithMpesaCode(MpesaCodeRequest mpesaCodeRequest) {

        UserInfo userInfo = hotspotUserInfo.findByMpesaReceiptNumberIgnoreCase(mpesaCodeRequest.getCode());
        if (userInfo == null) {
            throw new IllegalArgumentException("Code not found contact admin:");
        } else if (!userInfo.getMacAddress().equals(mpesaCodeRequest.getMacAddress())) {
            throw new IllegalArgumentException("MAC not found contact admin:");
        }

        String username = userInfo.getUserName();
        String password = userInfo.getPassword();

        Map<String, String> response = new HashMap<>();
        response.put("username", username);
        response.put("password", password);

        return response;
    }

    public Map<String, String> redeemVoucher(@Valid RedeemVoucher redeemVoucher) throws MikrotikApiException {
        Voucher voucher = voucherRepository.findByVoucherCodeIgnoreCase(redeemVoucher.getVoucherCode());
        if (voucher == null || voucher.getStatus() == VoucherStatus.EXPIRED) {
            throw new IllegalArgumentException("Voucher not found or expired, contact admin: ");
        }

        UserInfo user = hotspotUserInfo.findUserByUserName(redeemVoucher.getVoucherCode());
        if (user != null && user.getMacAddress().equals(redeemVoucher.getMacAddress())) {
            String code = redeemVoucher.getVoucherCode();
            Map<String, String> response = new HashMap<>();
            response.put("username", code);
            response.put("password", code);

            return response;
        }

        Duration sessionLimit = Duration.ofHours(mapPackageToSessionLimit(voucher.getPackageType()));
        LocalDateTime sessionStartTime = LocalDateTime.now();
        LocalDateTime sessionEndTime = sessionStartTime.plus(sessionLimit);

        voucher.setRedeemedBy(redeemVoucher.getMacAddress());
        voucher.setIpAddress(redeemVoucher.getIpAddress());
        voucher.setStatus(VoucherStatus.USED);
        voucher.setExpiryDate(sessionEndTime);
        voucherRepository.save(voucher);

        String profile = mapPackageToProfile(voucher.getPackageType());
        String uptimeLimit = mapPackageToUptime(voucher.getPackageType());

        mikroTikClient.redeemVoucher(redeemVoucher.getVoucherCode(), redeemVoucher.getIpAddress(),
                redeemVoucher.getMacAddress(), profile, uptimeLimit, voucher.getRouterName());

        UserSession userSession = UserSession.builder()
                .routerName(voucher.getRouterName())
                .username(voucher.getVoucherCode())
                .sessionStartTime(LocalDateTime.now())
                .sessionEndTime(sessionEndTime)
                .build();
        userSessionRepository.save(userSession);


        UserInfo userInfo = UserInfo.builder()
                .checkoutRequestID(null)
                .macAddress(redeemVoucher.getMacAddress())
                .ipAddress(redeemVoucher.getIpAddress())
                .userName(voucher.getVoucherCode())
                .password(voucher.getVoucherCode())
                .packageType(voucher.getPackageType())
                .loginBy(LoginBy.VOUCHER)
                .createTime(LocalDateTime.now())
                .amount(0)
                .build();
        hotspotUserInfo.save(userInfo);

        String voucherCode = voucher.getVoucherCode();
        Map<String, String> response = new HashMap<>();
        response.put("username", voucherCode);
        response.put("password", voucherCode);

        return response;
    }

    public Page<ClientResponse> getAllClients(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createTime").descending());
        Page<UserInfo> userPage = hotspotUserInfo.findAll(pageable);

        return userPage.map(this::convertToClientResponse);
    }

    private ClientResponse convertToClientResponse(UserInfo userInfo) {
        return new ClientResponse(
                userInfo.getMpesaReceiptNumber(),
                userInfo.getIpAddress(),
                userInfo.getMacAddress(),
                userInfo.getPhoneNumber(),
                userInfo.getPackageType(),
                userInfo.getAmount(),
                userInfo.getCreateTime()
        );
    }

    public int getTotalActiveClients(String routerName) throws MikrotikApiException {
        List<ClientResponse> activeClients = mikroTikClient.getTotalActiveClients(routerName);

        return activeClients.size();
    }

    public List<ActiveUsersResponse> getAllActiveClients(String routerName) throws MikrotikApiException {
        return mikroTikClient.getAllActiveClients(routerName);
    }

    public int getTotalConnectedUsers(String routerName) throws MikrotikApiException {
        List<ClientResponse> connectedUsers = mikroTikClient.getTotalConnectedUsers(routerName);
        return connectedUsers.size();
    }

    public List<RouterClientResponse> getConnectedUsers(String routerName) throws MikrotikApiException {
        return mikroTikClient.getConnectedUsers(routerName);
    }

    public Map<String, String> getRouterHealth(String routerName) throws MikrotikApiException {
        return mikroTikClient.getRouterHealth(routerName);
    }

    public Map<String, Object> getRouterTraffic(String routerInterface, String routerName) throws MikrotikApiException {
        return mikroTikClient.getRouterTraffic(routerInterface, routerName);
    }

    public Map<String, String> getRouterSystemAlerts(String routerName) throws MikrotikApiException {
        return mikroTikClient.getRouterSystemAlerts(routerName);
    }

    public Map<String, Object> viewRouterLogs(String routerName) throws MikrotikApiException {
        return mikroTikClient.viewRouterLogs(routerName);
    }

    public void changeRouterSystemSettings(String action, String routerName) throws MikrotikApiException {
        mikroTikClient.changeRouterSystemSettings(action, routerName);
    }


    public void deleteVoucher(String voucherCode) {
        Voucher voucher = voucherRepository.findByVoucherCodeIgnoreCase(voucherCode);
        if (voucher != null) {
            voucherRepository.delete(voucher);
        } else {
            throw new IllegalArgumentException("Voucher not found : " + voucherCode);
        }
    }

    public List<Voucher> getAllVouchers() {
        return voucherRepository.findAll();
    }


    public VoucherResponse editVoucher(String voucherCode, UpdateVoucher updateVoucher) {
        Voucher voucher = voucherRepository.findByVoucherCodeIgnoreCase(voucherCode);

        if (voucher != null) {
            voucher.setPackageType(updateVoucher.getPackageType());
            voucher.setStatus(updateVoucher.getStatus());
            voucher.setRedeemedBy(updateVoucher.getRedeemedBy());
            voucherRepository.save(voucher);

            return VoucherResponse.builder()
                    .voucherCode(voucherCode)
                    .redeemedBy(voucher.getRedeemedBy())
                    .packageType(voucher.getRedeemedBy())
                    .status(voucher.getStatus())
                    .createdAt(voucher.getCreatedAt())
                    .build();
        } else {
            throw new IllegalArgumentException("Voucher not found : " + voucherCode);
        }

    }

    public void addRouter(RouterRequest routerRequest) {
        Routers router = Routers.builder()
                .routerName(routerRequest.getRouterName())
                .routerIPAddress(routerRequest.getRouterIPAddress())
                .username(routerRequest.getUsername())
                .password(routerRequest.getPassword())
                .dnsName(routerRequest.getDnsName())
                .description(routerRequest.getDescription())
                .build();
        routerRepository.save(router);
    }

    public void updateRouter(String routerName, RouterRequest routerRequest) {
        Routers router = routerRepository.findByRouterName(routerName);
        if (router != null) {
            router.setRouterName(routerRequest.getRouterName());
            router.setRouterIPAddress(routerRequest.getRouterIPAddress());
            router.setUsername(routerRequest.getUsername());
            router.setPassword(routerRequest.getPassword());
            router.setDnsName(routerRequest.getDnsName());
            router.setDescription(routerRequest.getDescription());
            routerRepository.save(router);
        } else {
            throw new IllegalArgumentException("Router not found : " + routerName);
        }
    }

    public void deleteRouter(String routerName) {
        routerRepository.deleteByRouterName(routerName);
    }

    public List<Routers> getAllRouters() {
        return routerRepository.findAll();
    }

    public void deleteUser(String routerName, String username) throws MikrotikApiException {
        userSessionRepository.deleteByUsername(username);

        mikroTikClient.deleteUser(routerName, username);
    }

    @Scheduled(fixedRate = 30 * 60 * 1000)
    public void removeExpiredUsers() {
        System.out.println("Checking for Expired Users");
        List<UserSession> expiredSessions = userSessionRepository.findExpiredSessions(LocalDateTime.now());

        for (UserSession session : expiredSessions) {
            try {
                mikroTikClient.removeExpiredUser(session.getRouterName(), session.getUsername());

                System.out.println("removed expired user: " + session.getUsername());
                userSessionRepository.delete(session);
            } catch (MikrotikApiException e) {
                System.err.println("Error removing expired user: " + session.getUsername());
            }
        }
    }

    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void removeExpiredVouchers() {
        System.out.println("Checking for Expired Vouchers");
        List<Voucher> vouchers = voucherRepository.findExpiredVouchers(LocalDateTime.now());

        for (Voucher voucher : vouchers) {
            try {
                voucher.setStatus(VoucherStatus.EXPIRED);
                voucherRepository.save(voucher);
            } catch (Exception e) {
                System.err.println("Error removing expired voucher: " + voucher.getVoucherCode());
            }
        }
    }

    public long mapPackageToSessionLimit(String packageType) {
        return switch (packageType) {
            case "1hr" -> 1L;
            case "3hrs" -> 3L;
            case "1day" -> 24L;
            case "weekly" -> 168L;
            case "30days" -> 720L;
            default -> throw new IllegalArgumentException("Package not found: " + packageType);
        };
    }
}