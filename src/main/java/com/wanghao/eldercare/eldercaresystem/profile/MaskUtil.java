package com.wanghao.eldercare.eldercaresystem.profile;

public final class MaskUtil {

    private MaskUtil() {
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }
        String p = phone.trim();
        if (p.length() < 7) {
            return "***";
        }
        return p.substring(0, 3) + "****" + p.substring(p.length() - 4);
    }

    public static String maskIdNumber(String idNumber) {
        if (idNumber == null || idNumber.isBlank()) {
            return idNumber;
        }
        String s = idNumber.trim();
        if (s.length() <= 5) {
            return "***";
        }
        return s.substring(0, 3) + "*".repeat(s.length() - 5) + s.substring(s.length() - 2);
    }
}
