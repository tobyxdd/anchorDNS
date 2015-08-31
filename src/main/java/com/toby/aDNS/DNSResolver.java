package com.toby.aDNS;

import org.xbill.DNS.Message;
import org.xbill.DNS.SimpleResolver;

import java.io.IOException;
import java.net.UnknownHostException;

public class DNSResolver implements Runnable {

    private Message qmsg, rmsg;
    private SimpleResolver resolver;

    public DNSResolver(int timeout, Message msg, String server) throws UnknownHostException {
        resolver = new SimpleResolver(server);
        resolver.setTimeout(timeout);
        qmsg = msg;
    }

    @Override
    public void run() {
        try {
            rmsg = resolver.send(qmsg);
        } catch (IOException e) {
            SimpleLog.log("An error occurred while resolving: " + e.toString());
        }
    }

    public Message getRmsg() {
        return rmsg;
    }
}
