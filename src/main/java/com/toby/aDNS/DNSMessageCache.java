package com.toby.aDNS;

import org.xbill.DNS.Message;

import java.util.HashMap;

public class DNSMessageCache {
    private DNSMessageCache() {
    }

    private static HashMap<Integer, Message> cacheMap = new HashMap<Integer, Message>();

    public static void put(Integer hash, Message msg) {
        //cacheMap.put(hash, msg);
    }

    public static Message get(Integer hash) {
        return null;
        //return cacheMap.get(hash);
    }

    public static void clear() {
        cacheMap.clear();
    }
}
