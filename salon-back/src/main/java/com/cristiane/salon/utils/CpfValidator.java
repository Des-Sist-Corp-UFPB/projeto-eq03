package com.cristiane.salon.utils;

public class CpfValidator {
    public static boolean isValid(String cpf) {
        if (cpf == null) {
            return false;
        }
        String clean = cpf.replaceAll("\\D", "");
        if (clean.length() != 11) {
            return false;
        }

        // Rejeita CPFs conhecidos com todos os dígitos iguais
        if (clean.equals("00000000000") || clean.equals("11111111111") ||
            clean.equals("22222222222") || clean.equals("33333333333") ||
            clean.equals("44444444444") || clean.equals("55555555555") ||
            clean.equals("66666666666") || clean.equals("77777777777") ||
            clean.equals("88888888888") || clean.equals("99999999999")) {
            return false;
        }

        try {
            // Calcula o primeiro dígito verificador
            int sum = 0;
            for (int i = 0; i < 9; i++) {
                sum += (clean.charAt(i) - '0') * (10 - i);
            }
            int r1 = 11 - (sum % 11);
            int d1 = (r1 == 10 || r1 == 11) ? 0 : r1;

            if (d1 != (clean.charAt(9) - '0')) {
                return false;
            }

            // Calcula o segundo dígito verificador
            sum = 0;
            for (int i = 0; i < 10; i++) {
                sum += (clean.charAt(i) - '0') * (11 - i);
            }
            int r2 = 11 - (sum % 11);
            int d2 = (r2 == 10 || r2 == 11) ? 0 : r2;

            return d2 == (clean.charAt(10) - '0');
        } catch (Exception e) {
            return false;
        }
    }
}
