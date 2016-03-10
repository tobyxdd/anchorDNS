package com.toby.aDNS;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SimpleLog {

    public static boolean showVerbose = false;

    private SimpleLog() {
    }

    private static DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void log(String argStr) {
        log(argStr, false);
    }

    public static void log(String argStr, boolean isVerbose) {
        if (!isVerbose || (isVerbose && showVerbose))
            System.out.println("[" + LocalDateTime.now().format(dtFormatter) + "] " + argStr);
    }
}
