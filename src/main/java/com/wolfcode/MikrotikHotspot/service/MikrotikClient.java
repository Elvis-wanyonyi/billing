package com.wolfcode.MikrotikHotspot.service;


import com.wolfcode.MikrotikHotspot.dto.ActiveUsersResponse;
import com.wolfcode.MikrotikHotspot.dto.ClientResponse;
import com.wolfcode.MikrotikHotspot.dto.RouterClientResponse;
import com.wolfcode.MikrotikHotspot.entity.Routers;
import com.wolfcode.MikrotikHotspot.repository.RouterRepository;
import me.legrange.mikrotik.ApiConnection;
import me.legrange.mikrotik.ApiConnectionException;
import me.legrange.mikrotik.MikrotikApiException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class MikrotikClient {

    private final RouterRepository routerRepository;
    private ApiConnection connection;

    public MikrotikClient(RouterRepository routerRepository) {
        this.routerRepository = routerRepository;
    }


    public void connectRouter(String routerName) throws MikrotikApiException {
        Routers routerConfig = routerRepository.findByRouterName(routerName);

        if (routerConfig == null) {
            throw new MikrotikApiException("Invalid router name: " + routerName);
        }

        connection = ApiConnection.connect(routerConfig.getRouterIPAddress());
        connection.login(routerConfig.getUsername(), routerConfig.getPassword());
    }

    public void disconnect() throws ApiConnectionException {
        if (connection != null) {
            connection.close();
        }
    }

    public void createHotspotUser(String username, String password, String ipAddress,
                                  String macAddress, String profile, String uptimeLimit, String routerName)
            throws MikrotikApiException {
        connectRouter(routerName);
        String command = String.format(
                "/ip/hotspot/user/add name=%s password=%s address=%s mac-address=%s profile=%s limit-uptime=%s",
                username, password, ipAddress, macAddress, profile, uptimeLimit
        );
        System.out.println(command);
        connection.execute(command);
    }

    public void redeemVoucher(String voucherCode,String ipAddress,String macAddress,
                              String profile, String uptimeLimit, String routerName)
            throws MikrotikApiException {
        connectRouter(routerName);
        String command = String.format(
                "/ip/hotspot/user/add name=%s password=%s address=%s mac-address=%s profile=%s limit-uptime=%s",
                voucherCode, voucherCode, ipAddress, macAddress, profile, uptimeLimit
        );
        System.out.println("Executing command : " + command);

        connection.execute(command);
    }

    public List<ClientResponse> getTotalActiveClients(String routerName) throws MikrotikApiException {
        connectRouter(routerName);
        String command = "/ip/hotspot/active/print";
        List<Map<String, String>> response = connection.execute(command);

        List<ClientResponse> activeClients = new ArrayList<>();
        for (Map<String, String> clientData : response) {
            ClientResponse activeClient = new ClientResponse();
            activeClient.setIpAddress(clientData.get("address"));
            activeClient.setMacAddress(clientData.get("mac-address"));
            activeClients.add(activeClient);
        }
        return activeClients;
    }

    public List<ActiveUsersResponse> getAllActiveClients(String routerName) throws MikrotikApiException {
        connectRouter(routerName);
        String command = "/ip/hotspot/active/print";
        List<Map<String, String>> response = connection.execute(command);

        List<ActiveUsersResponse> activeClients = new ArrayList<>();
        for (Map<String, String> clientData : response) {
            ActiveUsersResponse activeClient = new ActiveUsersResponse();

            activeClient.setName(clientData.get("user")); // username
            activeClient.setIpAddress(clientData.get("address")); // IP address
            activeClient.setMacAddress(clientData.get("mac-address")); // MAC address
            activeClient.setUptime(clientData.get("uptime")); // Session uptime
          //  activeClient.setRxRateTxRate(clientData.get("limit-uptime")); // Session limit (if available)
            activeClients.add(activeClient);
        }
        return activeClients;

    }

    public List<ClientResponse> getTotalConnectedUsers(String routerName) throws MikrotikApiException {
        connectRouter(routerName);
        String command = "/ip/hotspot/user/print";
        List<Map<String, String>> response = connection.execute(command);

        List<ClientResponse> connectedUsers = new ArrayList<>();
        for (Map<String, String> clientData : response) {
            ClientResponse connectedUser = new ClientResponse();
            connectedUser.setIpAddress(clientData.get("address"));
            connectedUser.setMacAddress(clientData.get("mac-address"));
            connectedUsers.add(connectedUser);
        }
        return connectedUsers;
    }

    public List<RouterClientResponse> getConnectedUsers(String routerName) throws MikrotikApiException {
        connectRouter(routerName);
        String command = "/ip/hotspot/user/print";
        List<Map<String, String>> response = connection.execute(command);

        List<RouterClientResponse> connectedUsers = new ArrayList<>();
        for (Map<String, String> clientData : response) {
            RouterClientResponse connectedUser = new RouterClientResponse();
            connectedUser.setName(clientData.getOrDefault("name", "Unknown"));
            connectedUser.setProfile(clientData.getOrDefault("profile", "Unknown"));
            connectedUser.setIpAddress(clientData.getOrDefault("address", "No IP Assigned"));
            connectedUser.setMacAddress(clientData.getOrDefault("mac-address", "No MAC Assigned"));

            connectedUsers.add(connectedUser);
        }
        return connectedUsers;
    }

    public Map<String, String> getRouterHealth(String routerName) throws MikrotikApiException {
        connectRouter(routerName);
        Map<String, String> healthData = new HashMap<>();
        List<Map<String, String>> results = connection.execute("/system/resource/print");
        if (!results.isEmpty()) {
            Map<String, String> result = results.getFirst();
            healthData.put("uptime", result.get("uptime"));
            healthData.put("cpuLoad", result.get("cpu-load") + "%");
            healthData.put("memoryUsage", result.get("free-memory") + " / " + result.get("total-memory"));
        }
        System.out.println(healthData);
        return healthData;
    }

    public Map<String, Object> getRouterTraffic(String routerInterface, String routerName) throws MikrotikApiException {
        connectRouter(routerName);
        Map<String, Object> trafficData = new HashMap<>();
        List<Map<String, String>> interfaceResults = connection.execute("/interface/print");

        boolean interfaceFound = false;
        for (Map<String, String> interfaceData : interfaceResults) {
            String interfaceName = interfaceData.get("name");
            if (interfaceName.equals(routerInterface)) {
                trafficData.put("txBytes", interfaceData.get("tx-byte"));
                trafficData.put("rxBytes", interfaceData.get("rx-byte"));
                interfaceFound = true;
                break;
            }
        }
        if (!interfaceFound) {
            throw new IllegalArgumentException("Invalid router interface: " + routerInterface);
        }
        System.out.println(trafficData);
        return trafficData;
    }

    public Map<String, String> getRouterSystemAlerts(String routerName) throws MikrotikApiException {
        connectRouter(routerName);
        Map<String, String> alerts = new HashMap<>();
        List<Map<String, String>> logEntries = connection.execute("/log/print");

        for (Map<String, String> logEntry : logEntries) {
            String message = logEntry.get("message");
            if (message.contains("login failure")) {
                alerts.put("unauthorizedAccess", "Unauthorized login attempt detected.");
            }
        }

        alerts.put("downtime", "No downtime detected in the logs.");

        return alerts;
    }

    public Map<String, Object> viewRouterLogs(String routerName) throws MikrotikApiException {
        connectRouter(routerName);
        Map<String, Object> logs = new HashMap<>();
        List<Map<String, String>> logEntries = connection.execute("/log/print");

        for (int i = 0; i < logEntries.size(); i++) {
            logs.put("log" + (i + 1), logEntries.get(i).get("message"));
        }

        return logs;
    }

    public void changeRouterSystemSettings(String action, String routerName) throws MikrotikApiException {
        connectRouter(routerName);
        try {
            if ("reboot".equalsIgnoreCase(action)) {
                connection.execute("/system/reboot");
            } else if ("shutdown".equalsIgnoreCase(action)) {
                connection.execute("/system/shutdown");
            } else {
                System.out.println("Invalid action specified: " + action);
            }
        } catch (MikrotikApiException e) {
            System.err.println("Error executing action '" + action + "' on router '" + routerName + "': " + e.getMessage());
            throw e;
        }
    }

    public void deleteUser(String routerName, String username) throws MikrotikApiException {
        connectRouter(routerName);
        try {
            String findCommand = "/ip/hotspot/user/print where name=" + username;
            List<Map<String, String>> users = connection.execute(findCommand);

            if (users.isEmpty()) {
                throw new MikrotikApiException("User not found: " + username);
            }
            String userId = users.getFirst().get(".id");

            String deleteCommand = "/ip/hotspot/user/remove .id=" + userId;
            connection.execute(deleteCommand);

        } catch (MikrotikApiException e) {
            throw new MikrotikApiException("Failed to remove hotspot user: " + username, e);
        }
    }

    public void removeExpiredUser(String routerName, String username) throws MikrotikApiException {
        connectRouter(routerName);
        try {
            String findCommand = "/ip/hotspot/user/print where name=" + username;
            List<Map<String, String>> users = connection.execute(findCommand);

            if (users.isEmpty()) {
                throw new MikrotikApiException("User not found: " + username);
            }
            String userId = users.get(0).get(".id");

            String removeCommand = "/ip/hotspot/user/remove .id=" + userId;
            connection.execute(removeCommand);

        } catch (MikrotikApiException e) {
            throw new MikrotikApiException("Failed to remove expired hotspot user: " + username, e);
        }
    }


}