package com.toby.aDNS;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.apache.commons.net.util.SubnetUtils;
import org.xbill.DNS.*;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class ADNSServer extends SimpleChannelInboundHandler<DatagramPacket> {

    private String defaultDNS_IP, alternativeDNS_IP;
    private String[] CIDRs;
    private int timeout;

    public ADNSServer(String defaultDNS_IP, String alternativeDNS_IP, String[] CIDRs, int timeout) throws UnknownHostException {
        this.defaultDNS_IP = defaultDNS_IP;
        this.alternativeDNS_IP = alternativeDNS_IP;
        this.CIDRs = CIDRs;
        this.timeout = timeout;
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
        byte[] bytes = new byte[msg.content().readableBytes()];
        msg.content().readBytes(bytes);
        Message dnsMsg = new Message(bytes);
        Record record = dnsMsg.getQuestion();
        if (record != null) {
            SimpleLog.log("Resolving " + record.getName() + " (" + Type.string(record.getType()) + ")");
            Message cm = DNSMessageCache.get(record.hashCode());
            if (cm != null) {
                SimpleLog.log("Using cached result for " + record.getName());
                cm.getHeader().setID(dnsMsg.getHeader().getID());
                writeResult(ctx, record, cm, msg, false);
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
                    writeResult(ctx, record, adnsResult, msg, true);
                } else {
                    SimpleLog.log("Using default DNS for " + record.getName());
                    writeResult(ctx, record, ddnsResult, msg, true);
                }
            } else {
                SimpleLog.log("Falling back to alternative DNS for " + record.getName());
                adnsThread.join();
                adnsResult = adnsRunnable.getRmsg();
                writeResult(ctx, record, adnsResult, msg, false);
            }
        }
    }

    private void writeResult(ChannelHandlerContext ctx, Record question, Message dnsResult, DatagramPacket msg, boolean writeCache) {
        if (writeCache) DNSMessageCache.put(question.hashCode(), dnsResult);
        if (dnsResult != null) ctx.write(new DatagramPacket(Unpooled.copiedBuffer(dnsResult.toWire()), msg.sender()));
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        SimpleLog.log("An error occurred: " + cause.toString());
    }

    private boolean useAltDNS(Record[] records) {
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
