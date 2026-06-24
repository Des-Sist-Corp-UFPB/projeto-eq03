package com.cristiane.salon.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogMasker {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("(?i)\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");
    private static final Pattern CPF_PATTERN = Pattern.compile("\\b(\\d{3})\\.?(\\d{3})\\.?(\\d{3})-?(\\d{2})\\b");

    public static String maskEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return email;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (local.length() <= 2) {
            return local.substring(0, 1) + "***" + domain;
        }
        return local.substring(0, 2) + "***" + local.substring(local.length() - 1) + domain;
    }

    public static String maskCpf(String cpf) {
        if (cpf == null || cpf.trim().isEmpty()) {
            return cpf;
        }
        String digits = cpf.replaceAll("\\D", "");
        if (digits.length() == 11) {
            return "***.***.***-" + digits.substring(9);
        }
        return "***";
    }

    public static String sanitizeJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return json;
        }

        // 1. Mask raw emails (including inside JSON fields) first to prevent double-masking
        String result = maskRawEmails(json);

        // 2. Mask "number":"..." (which might be CPF) in JSON
        Matcher numberMatcher = Pattern.compile("(?i)(\"number\"\\s*:\\s*\")([^\"]+)(\")").matcher(result);
        StringBuffer sb = new StringBuffer();
        while (numberMatcher.find()) {
            String num = numberMatcher.group(2);
            String masked = num.length() == 11 ? maskCpf(num) : "***";
            numberMatcher.appendReplacement(sb, numberMatcher.group(1) + masked + numberMatcher.group(3));
        }
        numberMatcher.appendTail(sb);
        result = sb.toString();

        // 3. Also mask raw CPFs in the JSON string
        result = maskRawCpfs(result);

        return result;
    }

    public static String maskRawEmails(String text) {
        if (text == null) return null;
        Matcher m = EMAIL_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, maskEmail(m.group()));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static String maskRawCpfs(String text) {
        if (text == null) return null;
        Matcher m = CPF_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String cpfDigits = m.group(1) + m.group(2) + m.group(3) + m.group(4);
            m.appendReplacement(sb, maskCpf(cpfDigits));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
