package com.example.portside;

public class AttemptsHelper {
    private static final double RECENCY_WEIGHT_RATIO = 1.4;

    public static String createUpdatedAttempts(String attempts, boolean correct) {
        String nextAttempt = correct ? "1" : "0";
        return nextAttempt + attempts.substring(0, attempts.length() - 1);
    }

    public static String createDecayedAttempts(String attempts, int count) {
        return attempts.substring(0, attempts.length() - count) + "0".repeat(count);
    }

    public static double getSuccess(String attempts) {
        double success = 0;
        double total = 0;
        double weight = 1;
        char[] attemptsArray = attempts.toCharArray();
        for (int i = attemptsArray.length - 1; i >= 0; i--) {
            char attempt = attemptsArray[i];
            total += weight;
            if (attempt == '1') {
                success += weight;
            }

            weight = weight * RECENCY_WEIGHT_RATIO;
        }
        return success / total;
    }
}
