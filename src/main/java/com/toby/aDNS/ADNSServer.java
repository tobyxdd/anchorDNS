package com.toby.aDNS;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class ADNSServer implements Runnable {

    private String defaultDNS_IP, alternativeDNS_IP;
    private String[] CIDRs;
    private int timeout;
    private boolean useFB;
    private DatagramSocket socket;

    private final int bufSize = 512;

    public ADNSServer(String defaultDNS_IP, String alternativeDNS_IP, String[] CIDRs, int timeout, boolean useFB, String serverName, int serverPort) throws SocketException {
        this.defaultDNS_IP = defaultDNS_IP;
        this.alternativeDNS_IP = alternativeDNS_IP;
        this.CIDRs = CIDRs;
        this.timeout = timeout;
        this.useFB = useFB;
        socket = new DatagramSocket(null);
        socket.bind(new InetSocketAddress(serverName, serverPort));
    }

    @Override
    public void run() {
        byte[] buf = new byte[512];
        while (true) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);
                new Thread(new LookupHandler(defaultDNS_IP, alternativeDNS_IP, CIDRs, timeout, useFB, packet, socket)).start();
            } catch (IOException e) {
                SimpleLog.log("Socket error: " + e.toString());
            }
        }
    }
}
