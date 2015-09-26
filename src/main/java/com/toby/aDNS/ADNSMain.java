package com.toby.aDNS;

import org.apache.commons.cli.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class ADNSMain {

    private static final String
            ddefDNS = "114.114.114.114,114.114.115.115", daltDNS = "8.8.8.8,8.8.4.4";

    public static void main(String[] args) throws UnknownHostException {
        SimpleLog.log("anchorDNS 3.0 - By Toby Huang");
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
            if (commandLine.hasOption("n")) {
                DNSMessageCache.enabled = false;
                SimpleLog.log("Cache disabled.");
            }
            String defDNS = commandLine.getOptionValue("d", ddefDNS), altDNS = commandLine.getOptionValue("a", daltDNS);
            SimpleLog.log("Using DNS " + defDNS + "/" + altDNS);
            String serverName = commandLine.getOptionValue("i", "127.0.0.1");
            String serverPort = commandLine.getOptionValue("p", "53");
            Runnable ar = new ADNSServer(defDNS, altDNS, cidrs.toArray(new String[cidrs.size()]), Integer.parseInt(commandLine.getOptionValue("t", "2")), commandLine.hasOption("f"), serverName, Integer.parseInt(serverPort));
            Thread art = new Thread(ar);
            art.start();
            SimpleLog.log("Up and running on " + serverName + ":" + serverPort);
        } catch (Exception e) {
            SimpleLog.log("Initialization error: " + e.toString());
        }
    }

    private static Options createOptions() {
        Options options = new Options();
        options.addOption(Option.builder("i").longOpt("ip").hasArg().desc("Specify the listening IP. Default: 127.0.0.1").build());
        options.addOption(Option.builder("p").longOpt("port").hasArg().desc("Specify the listening port. Default: 53").build());
        options.addOption(Option.builder("d").longOpt("defaultDNS").hasArg().desc("Specify the default DNS server. Default: " + ddefDNS).build());
        options.addOption(Option.builder("a").longOpt("alternativeDNS").hasArg().desc("Specify the alternative DNS server. Default: " + daltDNS).build());
        options.addOption(Option.builder("c").longOpt("cidr").hasArg().desc("Specify the CIDR list. Default: ChinaCIDR.txt").build());
        options.addOption(Option.builder("t").longOpt("timeout").hasArg().desc("Specify the DNS time out (sec). Default: 2").build());
        options.addOption(Option.builder("n").longOpt("nocache").desc("Disable results cache.").build());
        options.addOption(Option.builder("f").longOpt("fallback").desc("Use alternative DNS when default DNS failed.").build());
        options.addOption(Option.builder("h").longOpt("help").desc("Show this help message.").build());
        return options;
    }
}
