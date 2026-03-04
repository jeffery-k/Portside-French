package com.example.portside;

public class AttemptsHelper {
    public static String createUpdatedAttempts(String attempts, boolean correct) {
        String nextAttempt = correct ? "1" : "0";
        return attempts.substring(1) + nextAttempt;
    }

    public static double getSuccess(String attempts) {
        double success = 0;
        double total = 0;
        for (char attempt : attempts.toCharArray()) {
            total++;
            if (attempt == '1') {
                success++;
            }
        }
        return success / total;
    }
}
