package com.toby.aDNS;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SimpleLog {
    private SimpleLog(){}
    private static DateTimeFormatter dtFormatter=DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss");
    public static void log(String argStr)
    {
        System.out.println("["+ LocalDateTime.now().format(dtFormatter)+"] "+argStr);
    }
}
