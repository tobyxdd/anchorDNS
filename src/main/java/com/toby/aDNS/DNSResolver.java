package com.toby.aDNS;

import org.xbill.DNS.ExtendedResolver;
import org.xbill.DNS.Message;

import java.io.IOException;
import java.net.UnknownHostException;

public class DNSResolver implements Runnable {

    private Message qmsg, rmsg;
    private ExtendedResolver resolver;

    public DNSResolver(int timeout, Message msg, String server) throws UnknownHostException {
        resolver = new ExtendedResolver(server.split(","));
        resolver.setTimeout(timeout);
        qmsg = msg;
    }

    @Override
    public void run() {
        try {
            rmsg = resolver.send(qmsg);
        } catch (IOException e) {
            SimpleLog.log("Failed to resolve " + qmsg.getQuestion().getName() + ": " + e.toString());
        }
    }

    public Message getRmsg() {
        return rmsg;
    }
}
