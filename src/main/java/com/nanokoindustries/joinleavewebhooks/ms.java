package com.nanokoindustries.joinleavewebhooks;

// Loosely from https://github.com/vercel/ms/blob/main/src/index.ts

public class ms {
    public static final Integer second = 1000;
    public static final Integer minute = second * 60;
    public static final Integer hour = minute * 60;
    public static final Integer day = hour * 24;
    public static final Integer week = day * 7;
    public static final double year = day * 365.25;

    public static String turnIntoPluralWord(Long milliseconds, Long millisecondsAbs, Integer timeValue, String timeName) {
        boolean isPlural = millisecondsAbs >= timeValue * 1.5;
        String endingChar = "";
        if (isPlural) {
            endingChar = "s";
        }

        return String.format("%s %s%s", Math.round((float) milliseconds / timeValue), timeName, endingChar);
    }

    public static String format(Long milliseconds) {
        Long millisecondsAbs = Math.abs(milliseconds);

        if (millisecondsAbs >= day) {
            return turnIntoPluralWord(milliseconds, millisecondsAbs, day, "day");
        } else if (millisecondsAbs >= hour) {
            return turnIntoPluralWord(milliseconds, millisecondsAbs, hour, "hour");
        } else if (millisecondsAbs >= minute) {
            return turnIntoPluralWord(milliseconds, millisecondsAbs, minute, "minute");
        } else if (millisecondsAbs >= second) {
            return turnIntoPluralWord(milliseconds, millisecondsAbs, second, "second");
        } else {
            return String.format("%s ms", milliseconds);
        }
    }
}
