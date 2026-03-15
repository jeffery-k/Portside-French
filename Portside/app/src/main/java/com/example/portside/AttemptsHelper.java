package com.example.portside;

public class AttemptsHelper {
    private static final double RECENCY_WEIGHT_RATIO = 1.4;

    public static String createUpdatedAttempts(String attempts, boolean correct) {
        String nextAttempt = correct ? "1" : "0";
        return attempts.substring(1) + nextAttempt;
    }

    public static double getSuccess(String attempts) {
        double success = 0;
        double total = 0;
        double weight = 1;
        for (char attempt : attempts.toCharArray()) {
            total += weight;
            if (attempt == '1') {
                success += weight;
            }

            weight = weight * RECENCY_WEIGHT_RATIO;
        }
        return success / total;
    }
}
