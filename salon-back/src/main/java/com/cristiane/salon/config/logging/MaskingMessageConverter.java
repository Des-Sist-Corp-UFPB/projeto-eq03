package com.cristiane.salon.config.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MaskingMessageConverter extends CompositeConverter<ILoggingEvent> {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("(?i)\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");
    private static final Pattern CPF_PATTERN = Pattern.compile("\\b(\\d{3})\\.?(\\d{3})\\.?(\\d{3})-?(\\d{2})\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(\\b|\\()(\\d{2})\\)?\\s*(9?\\d{4})-?(\\d{4})\\b");
    
    // Detects names in contexts like "Cliente: José Silva" or in the JSON format "clientName":"José Silva"
    private static final Pattern CONTEXT_NAME_PATTERN = Pattern.compile(
            "(?i)(usuário|cliente|profissional|nome|payerName|clientName|employeeName)\\s*(?:logado|do agendamento)?\\s*(?::|=|\"\\s*:\\s*\")\\s*([A-ZÀ-ÿ][a-zà-ÿ]+(?:\\s+[A-ZÀ-ÿ][a-zà-ÿ]+)+)"
    );
    private static final Pattern JSON_NAME_PATTERN = Pattern.compile("(?i)\"(clientName|employeeName|payerName|name)\"\\s*:\\s*\"([^\"]+)\"");

    @Override
    protected String transform(ILoggingEvent event, String in) {
        if (in == null || in.isEmpty()) {
            return in;
        }
        return maskMessage(in);
    }

    public String maskMessage(String message) {
        String result = message;
        result = maskEmails(result);
        result = maskPhones(result);
        result = maskCpfs(result);
        result = maskJsonNames(result);
        result = maskContextNames(result);
        return result;
    }

    private String maskEmails(String text) {
        Matcher matcher = EMAIL_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, Matcher.quoteReplacement(maskEmailValue(matcher.group())));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String maskEmailValue(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) return email;
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (local.length() <= 2) {
            return local.substring(0, 1) + "***" + domain;
        }
        return local.substring(0, 2) + "***" + local.substring(local.length() - 1) + domain;
    }

    private String maskCpfs(String text) {
        Matcher matcher = CPF_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "***.***.***-" + matcher.group(4));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String maskPhones(String text) {
        Matcher matcher = PHONE_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String prefix = matcher.group(1);
            String lastFour = matcher.group(4);
            if ("(".equals(prefix)) {
                matcher.appendReplacement(sb, "() *****-" + lastFour);
            } else {
                matcher.appendReplacement(sb, "*****" + lastFour);
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String maskJsonNames(String text) {
        Matcher matcher = JSON_NAME_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2);
            matcher.appendReplacement(sb, "\"" + key + "\":\"" + maskNameValue(value) + "\"");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String maskContextNames(String text) {
        Matcher matcher = CONTEXT_NAME_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String label = matcher.group(1);
            String name = matcher.group(2);
            String matchedGroup = matcher.group();
            String separator = matchedGroup.substring(label.length(), matchedGroup.length() - name.length());
            matcher.appendReplacement(sb, label + separator + maskNameValue(name));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String maskNameValue(String name) {
        if (name == null || name.trim().isEmpty()) {
            return name;
        }
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.length() > 0) {
                sb.append(part.charAt(0));
                for (int j = 1; j < part.length(); j++) {
                    sb.append('*');
                }
            }
            if (i < parts.length - 1) {
                sb.append(' ');
            }
        }
        return sb.toString();
    }
}
