package com.toby.aDNS;

import org.apache.commons.net.util.SubnetUtils;
import org.xbill.DNS.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;

public class LookupHandler implements Runnable {

    private String defaultDNS_IP, alternativeDNS_IP;
    private String[] CIDRs;
    private int timeout;
    private boolean useFB;
    private DatagramSocket socket;
    private DatagramPacket packet;

    public LookupHandler(String defaultDNS_IP, String alternativeDNS_IP, String[] CIDRs, int timeout, boolean useFB, DatagramPacket packet, DatagramSocket socket) {
        this.defaultDNS_IP = defaultDNS_IP;
        this.alternativeDNS_IP = alternativeDNS_IP;
        this.CIDRs = CIDRs;
        this.timeout = timeout;
        this.useFB = useFB;
        this.socket = socket;
        this.packet = packet;
    }

    @Override
    public void run() {
        try {
            Message dnsMsg = new Message(packet.getData());
            Record record = dnsMsg.getQuestion();
            if (record != null) {
                SimpleLog.log("Resolving " + record.getName() + " (" + Type.string(record.getType()) + ")");
                Message cm = DNSMessageCache.get(record.hashCode());
                if (cm != null) {
                    SimpleLog.log("Using cached result for " + record.getName());
                    cm.getHeader().setID(dnsMsg.getHeader().getID());
                    writeResult(record, cm, false);
                    return;
                }
                Message ddnsResult, adnsResult;
                DNSResolver ddnsRunnable = new DNSResolver(timeout, dnsMsg, defaultDNS_IP), adnsRunnable = new DNSResolver(timeout, dnsMsg, alternativeDNS_IP);
                Thread ddnsThread = new Thread(ddnsRunnable);
                Thread adnsThread = new Thread(adnsRunnable);
                ddnsThread.start();
                adnsThread.start();
                ddnsThread.join();
                ddnsResult = ddnsRunnable.getRmsg();
                if (ddnsResult != null) {
                    Record[] ddnsRecords = ddnsResult.getSectionArray(Section.ANSWER);
                    if (useAltDNS(ddnsRecords)) {
                        SimpleLog.log("Using alternative DNS for " + record.getName());
                        adnsThread.join();
                        adnsResult = adnsRunnable.getRmsg();
                        writeResult(record, adnsResult, true);
                    } else {
                        SimpleLog.log("Using default DNS for " + record.getName());
                        writeResult(record, ddnsResult, true);
                    }
                } else if (useFB) {
                    SimpleLog.log("Falling back to alternative DNS for " + record.getName());
                    adnsThread.join();
                    adnsResult = adnsRunnable.getRmsg();
                    writeResult(record, adnsResult, true);
                }
            }
        } catch (Exception e) {
            SimpleLog.log("Lookup error: " + e.toString());
        }
    }

    private void writeResult(Record question, Message dnsResult, boolean writeCache) throws IOException {
        if (dnsResult != null) {
            if (writeCache) DNSMessageCache.put(question.hashCode(), dnsResult);
            byte[] rb = dnsResult.toWire();
            socket.send(new DatagramPacket(rb, rb.length, packet.getAddress(), packet.getPort()));
        }
    }

    private boolean useAltDNS(Record[] records) {
        if (records.length == 0) return true;
        ArrayList<ARecord> aRecords = new ArrayList<ARecord>();
        for (Record r : records) {
            if (r instanceof ARecord) aRecords.add((ARecord) r);
        }
        if (aRecords.size() == 0) return false;
        for (String cidr : CIDRs) {
            SubnetUtils su = new SubnetUtils(cidr);
            for (ARecord ar : aRecords) {
                if (su.getInfo().isInRange(ar.getAddress().getHostAddress())) return false;
            }
        }
        return true;
    }
}
