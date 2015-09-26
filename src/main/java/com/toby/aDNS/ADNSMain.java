package com.toby.aDNS;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.apache.commons.cli.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class ADNSMain {

    private static final String ddefDNS = "114.114.114.114,114.114.115.115", daltDNS = "8.8.8.8,8.8.4.4";

    public static void main(String[] args) throws UnknownHostException {
        SimpleLog.log("anchorDNS 2.2 - By Toby Huang");
        Options options = createOptions();
        try {
            CommandLine commandLine = new DefaultParser().parse(options, args);
            if (commandLine.hasOption("h")) {
                new HelpFormatter().printHelp("anchorDNS", options);
                return;
            }
            BufferedReader reader = new BufferedReader(new FileReader(commandLine.getOptionValue("c", "ChinaCIDR.txt")));
            ArrayList<String> cidrs = new ArrayList<String>();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.length() > 0) cidrs.add(line);
            }
            reader.close();
            EventLoopGroup group = new NioEventLoopGroup();
            try {
                Bootstrap b = new Bootstrap();
                if (commandLine.hasOption("n")) {
                    DNSMessageCache.enabled = false;
                    SimpleLog.log("Cache disabled.");
                }
                String defDNS = commandLine.getOptionValue("d", ddefDNS), altDNS = commandLine.getOptionValue("a", daltDNS);
                b.group(group).channel(NioDatagramChannel.class).handler(new ADNSServer(defDNS, altDNS, cidrs.toArray(new String[cidrs.size()]),
                        Integer.parseInt(commandLine.getOptionValue("t", "2")), commandLine.hasOption("f")));
                SimpleLog.log("Using DNS " + defDNS + "/" + altDNS);
                String serverName = commandLine.getOptionValue("i", "127.0.0.1");
                String serverPort = commandLine.getOptionValue("p", "53");
                SimpleLog.log("Up and running on " + serverName + ":" + serverPort);
                b.bind(serverName, Integer.parseInt(serverPort)).sync().channel().closeFuture().await();
            } catch (InterruptedException e) {
                SimpleLog.log("Interrupted.");
            } catch (UnknownHostException e) {
                SimpleLog.log("DNS server(s) error.");
            } catch (NumberFormatException e) {
                SimpleLog.log("Number format error.");
            } finally {
                group.shutdownGracefully();
            }
        } catch (MissingOptionException e) {
            SimpleLog.log("Missing required option(s).");
            new HelpFormatter().printHelp("anchorDNS", options);
        } catch (MissingArgumentException e) {
            SimpleLog.log("Missing required argument(s).");
            new HelpFormatter().printHelp("anchorDNS", options);
        } catch (ParseException e) {
            SimpleLog.log("Bad argument(s).");
        } catch (FileNotFoundException e) {
            SimpleLog.log("CIDRs file not found.");
        } catch (IOException e) {
            SimpleLog.log("CIDRs file IO exception.");
        }
    }

    private static Options createOptions() {
        Options options = new Options();
        options.addOption(Option.builder("i").longOpt("ip").hasArg().desc("Specify the listening IP." +
                "   Default: 127.0.0.1").build());
        options.addOption(Option.builder("p").longOpt("port").hasArg().desc("Specify the listening port." +
                "   Default: 53").build());
        options.addOption(Option.builder("d").longOpt("defaultDNS").hasArg().desc("Specify the default DNS server." +
                "   Default: " + ddefDNS).build());
        options.addOption(Option.builder("a").longOpt("alternativeDNS").hasArg().desc("Specify the alternative DNS server." +
                "   Default: " + daltDNS).build());
        options.addOption(Option.builder("c").longOpt("cidr").hasArg().desc("Specify the CIDR list." +
                "   Default: ChinaCIDR.txt").build());
        options.addOption(Option.builder("t").longOpt("timeout").hasArg().desc("Specify the DNS time out (sec)." +
                "   Default: 2").build());
        options.addOption(Option.builder("n").longOpt("nocache").desc("Disable results cache.").build());
        options.addOption(Option.builder("f").longOpt("fallback").desc("Use alternative DNS when default DNS failed.").build());
        options.addOption(Option.builder("h").longOpt("help").desc("Show this help message.").build());
        return options;
    }
}
