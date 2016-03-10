package com.toby.aDNS;

import org.xbill.DNS.ExtendedResolver;
import org.xbill.DNS.Message;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.Callable;

public class DNSResolver implements Callable<Message> {

    private Message qmsg, rmsg;
    private ExtendedResolver resolver;

    public DNSResolver(int timeout, Message msg, String server) throws UnknownHostException {
        resolver = new ExtendedResolver(server.split(","));
        resolver.setTimeout(timeout);
        qmsg = msg;
    }

    @Override
    public Message call() throws Exception {
        try {
            rmsg = resolver.send(qmsg);
            return rmsg;
        } catch (IOException e) {
            SimpleLog.log("Failed to resolve " + qmsg.getQuestion().getName() + ": " + e.toString());
            return null;
        }
    }

}
