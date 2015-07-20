package com.toby.aDNS;

import org.xbill.DNS.Message;
import org.xbill.DNS.Record;

import java.util.HashMap;

public class DNSMessageCache {

    public static boolean enabled=true;

    private DNSMessageCache() {
    }

    private static HashMap<Integer, Message> cacheMap = new HashMap<Integer, Message>();

    public static void put(Integer hash, Message msg) {
        if(enabled) cacheMap.put(hash, msg);
    }

    public static Message get(Integer hash) {
        if(enabled) return cacheMap.get(hash);else return null;
    }

    public static void clear() {
        cacheMap.clear();
    }
}
