package com.toby.aDNS;

import org.xbill.DNS.Message;

import java.util.concurrent.ConcurrentHashMap;

public class DNSMessageCache {

    public static boolean enabled = true;

    private DNSMessageCache() {
    }

    private static ConcurrentHashMap<Integer, Message> cacheMap = new ConcurrentHashMap<Integer, Message>();

    public static void put(Integer hash, Message msg) {
        if (enabled) cacheMap.put(hash, msg);
    }

    public static Message get(Integer hash) {
        if (enabled) return cacheMap.get(hash);
        else return null;
    }

    public static void clear() {
        cacheMap.clear();
    }
}
