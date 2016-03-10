package com.toby.aDNS;

import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class ADNSMain {

    private static final String cidrlistURL = "http://www.ipdeny.com/ipblocks/data/aggregated/cn-aggregated.zone";

    private static final String
            ddefDNS = "119.29.29.29,182.254.116.116,223.5.5.5,223.6.6.6",
            daltDNS = "8.8.8.8,8.8.4.4,208.67.222.222,208.67.220.220",
            dserverName = "127.0.0.1", dserverPort = "53", dtimeout = "2";

    public static void main(String[] args) throws UnknownHostException {
        SimpleLog.log("anchorDNS 3.1 - By Toby Huang");
        Options options = createOptions();
        try {
            CommandLine commandLine = new DefaultParser().parse(options, args);
            if (commandLine.hasOption("h")) {
                new HelpFormatter().printHelp("anchorDNS", options);
                return;
            }
            File clfile = new File("ChinaCIDR.txt");
            if (commandLine.getOptionValue("c") == null && !clfile.exists()) {
                SimpleLog.log("CIDR list not found, downloading from " + cidrlistURL);
                FileUtils.copyURLToFile(new URL(cidrlistURL), clfile, 10000, 10000);
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
            String serverName = commandLine.getOptionValue("i", dserverName);
            String serverPort = commandLine.getOptionValue("p", dserverPort);
            Runnable ar = new ADNSServer(defDNS, altDNS, cidrs.toArray(new String[cidrs.size()]), Integer.parseInt(commandLine.getOptionValue("t", dtimeout)),
                    commandLine.hasOption("f"), serverName, Integer.parseInt(serverPort));
            Thread art = new Thread(ar);
            art.start();
            SimpleLog.log("Up and running on " + serverName + ":" + serverPort);
        } catch (Exception e) {
            SimpleLog.log("Initialization error: " + e.toString());
        }
    }

    private static Options createOptions() {
        Options options = new Options();
        options.addOption(Option.builder("i").longOpt("ip").hasArg().desc("Specify the listening IP. Default: " + dserverName).build());
        options.addOption(Option.builder("p").longOpt("port").hasArg().desc("Specify the listening port. Default: " + dserverPort).build());
        options.addOption(Option.builder("d").longOpt("defaultDNS").hasArg().desc("Specify the default DNS server. Default: " + ddefDNS).build());
        options.addOption(Option.builder("a").longOpt("alternativeDNS").hasArg().desc("Specify the alternative DNS server. Default: " + daltDNS).build());
        options.addOption(Option.builder("c").longOpt("cidr").hasArg().desc("Specify the CIDR list. Default: Download from IPdeny").build());
        options.addOption(Option.builder("t").longOpt("timeout").hasArg().desc("Specify the DNS time out (sec). Default: " + dtimeout).build());
        options.addOption(Option.builder("n").longOpt("nocache").desc("Disable results cache.").build());
        options.addOption(Option.builder("f").longOpt("fallback").desc("Use alternative DNS when default DNS failed.").build());
        options.addOption(Option.builder("h").longOpt("help").desc("Show this help message.").build());
        return options;
    }
}
