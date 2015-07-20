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

public class ADNSServer extends SimpleChannelInboundHandler<DatagramPacket> {

    private String defaultDNS_IP, alternativeDNS_IP;
    private String[] CIDRs;
    private boolean reverse;

    private SimpleResolver defResolver, altResolver;

    public ADNSServer(String defaultDNS_IP, String alternativeDNS_IP, String[] CIDRs, boolean reverse, int timeout) throws UnknownHostException {
        this.defaultDNS_IP = defaultDNS_IP;
        this.alternativeDNS_IP = alternativeDNS_IP;
        this.CIDRs = CIDRs;
        this.reverse = reverse;
        defResolver = new SimpleResolver(defaultDNS_IP);
        altResolver = new SimpleResolver(alternativeDNS_IP);
        defResolver.setTCP(false);
        defResolver.setTimeout(timeout);
        altResolver.setTCP(false);
        altResolver.setTimeout(timeout);
    }

    private Message resolve(Message dnsQuestion, boolean useAlt) {
        try {
            Message msg;
            if (useAlt)
                msg = altResolver.send(dnsQuestion);
            else
                msg = defResolver.send(dnsQuestion);
            return msg;
        } catch (IOException e) {
            return null;
        }

    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
        byte[] bytes = new byte[msg.content().readableBytes()];
        msg.content().readBytes(bytes);
        Message dnsMsg = new Message(bytes);
        Record record = dnsMsg.getQuestion();
        if (record != null) {
            SimpleLog.log("Resolving " + record.getName() + " (" + Type.string(record.getType()) + ") via " + (reverse ? alternativeDNS_IP : defaultDNS_IP));
            Message cm = DNSMessageCache.get(record.hashCode());
            if (cm != null) {
                SimpleLog.log("Using cached result.");
                writeResult(ctx, record, cm, msg, false);
                return;
            }
            Message dnsResult;
            dnsResult = resolve(dnsMsg, reverse);
            if (dnsResult != null) {
                Record[] resultRecords = dnsResult.getSectionArray(Section.ANSWER);
                if (resultRecords.length != 0) {
                    boolean useAlt = !reverse;
                    SubnetUtils subnetUtils;
                    ol:
                    for (Record rr : resultRecords) {
                        if (rr instanceof ARecord) {
                            String ipaddr = ((ARecord) rr).getAddress().getHostAddress();
                            for (String cidrblock : CIDRs) {
                                subnetUtils = new SubnetUtils(cidrblock);
                                if (subnetUtils.getInfo().isInRange(ipaddr)) {
                                    if (!reverse)
                                        useAlt = false;
                                    else
                                        useAlt = true;
                                    break ol;
                                }
                            }
                        }
                    }
                    if (!useAlt) {
                        SimpleLog.log("Resolved IPs are " + (reverse ? "not " : "") + "included in CIDRs.");
                        writeResult(ctx, record, dnsResult, msg, true);
                    } else {
                        SimpleLog.log("Resolved IPs are " + (reverse ? "" : "not ") + "included in CIDRs. Switching to " + (reverse ? defaultDNS_IP : alternativeDNS_IP));
                        Message altDNSResult;
                        altDNSResult = resolve(dnsMsg, !reverse);
                        if (altDNSResult != null) writeResult(ctx, record, altDNSResult, msg, true);
                    }
                } else {
                    SimpleLog.log("No record for " + record.getName());
                    writeResult(ctx, record, dnsResult, msg, true);
                }
            } else {
                SimpleLog.log("Lookup failed. Switching to " + (reverse ? defaultDNS_IP : alternativeDNS_IP));
                Message altDNSResult;
                altDNSResult = resolve(dnsMsg, !reverse);
                if (altDNSResult != null) writeResult(ctx, record, altDNSResult, msg, true);
            }
        }
    }

    private void writeResult(ChannelHandlerContext ctx, Record question, Message dnsResult, DatagramPacket msg, boolean writeCache) {
        if (writeCache) DNSMessageCache.put(question.hashCode(), dnsResult);
        ctx.write(new DatagramPacket(Unpooled.copiedBuffer(dnsResult.toWire()), msg.sender()));
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (!(cause instanceof SocketTimeoutException)) cause.printStackTrace();
        else SimpleLog.log("Lookup timed out.");
    }
}
