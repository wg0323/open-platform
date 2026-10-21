package com.bike.openapi.security;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * IP 白名单校验，支持精确 IP 与 IPv4 CIDR。
 */
public final class IpWhitelistChecker {

    private static final String COMMA = ",";
    private static final String SLASH = "/";
    private static final int IPV4_BITS = 32;

    private IpWhitelistChecker() {
    }

    /**
     * 判断客户端 IP 是否命中白名单，白名单为空视为不限制。
     *
     * @param clientIp  客户端 IP
     * @param whitelist 逗号分隔的白名单
     * @return 是否放行
     */
    public static boolean matches(String clientIp, String whitelist) {
        if (whitelist == null || whitelist.trim().isEmpty()) {
            return true;
        }
        for (String rule : whitelist.split(COMMA)) {
            String trimmed = rule.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.contains(SLASH)) {
                if (matchesCidr(clientIp, trimmed)) {
                    return true;
                }
            } else if (trimmed.equals(clientIp)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesCidr(String clientIp, String rule) {
        String[] parts = rule.split(SLASH);
        if (parts.length != 2) {
            return false;
        }
        try {
            InetAddress client = InetAddress.getByName(clientIp);
            InetAddress network = InetAddress.getByName(parts[0]);
            if (!(client instanceof Inet4Address) || !(network instanceof Inet4Address)) {
                return false;
            }
            int prefix = Integer.parseInt(parts[1]);
            if (prefix < 0 || prefix > IPV4_BITS) {
                return false;
            }
            int mask = prefix == 0 ? 0 : -1 << (IPV4_BITS - prefix);
            return (toInt(client) & mask) == (toInt(network) & mask);
        } catch (UnknownHostException | NumberFormatException exception) {
            return false;
        }
    }

    private static int toInt(InetAddress address) {
        byte[] bytes = address.getAddress();
        int value = 0;
        for (byte item : bytes) {
            value = (value << 8) | (item & 0xFF);
        }
        return value;
    }
}
