package com.airportrouteplanner.webapp.airport;

import org.springframework.stereotype.Component;

@Component
public class UtcOffsetFormatter {

    public String format(int offsetMinutes) {
        if (offsetMinutes == 0) {
            return "UTC+0";
        }
        String sign = offsetMinutes < 0 ? "-" : "+";
        int absoluteMinutes = Math.abs(offsetMinutes);
        int hours = absoluteMinutes / 60;
        int minutes = absoluteMinutes % 60;
        return minutes == 0
                ? "UTC" + sign + hours
                : "UTC" + sign + hours + ":" + String.format(java.util.Locale.ROOT, "%02d", minutes);
    }
}
