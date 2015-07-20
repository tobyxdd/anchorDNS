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
    public static void main(String[] args) {
        SimpleLog.log("anchorDNS - By Toby Huang");
        Options options = createOptions();
        try {
            CommandLine commandLine = new DefaultParser().parse(options, args);
            BufferedReader reader = new BufferedReader(new FileReader(commandLine.getOptionValue("c")));
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
                boolean rm = commandLine.hasOption("r");
                if (rm) SimpleLog.log("Reverse mode enabled.");
                if(commandLine.hasOption("n"))
                {
                    DNSMessageCache.enabled=false;
                    SimpleLog.log("Cache disabled.");
                }
                String defDNS = commandLine.getOptionValue("d"), altDNS = commandLine.getOptionValue("a");
                b.group(group).channel(NioDatagramChannel.class).handler(new ADNSServer(defDNS, altDNS, cidrs.toArray(new String[cidrs.size()]),
                        rm, commandLine.hasOption("t") ? Integer.parseInt(commandLine.getOptionValue("t")) : 2));
                SimpleLog.log("Up and running. (" + defDNS + "/" + altDNS + ")");
                b.bind("127.0.0.1", 53).sync().channel().closeFuture().await();
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
        options.addOption(Option.builder("d").longOpt("defaultDNS").hasArg().desc("Specify the default DNS server.").required().build());
        options.addOption(Option.builder("a").longOpt("alternativeDNS").hasArg().desc("Specify the alternative DNS server.").required().build());
        options.addOption(Option.builder("r").longOpt("reverse").desc("Check the alternative DNS first.").build());
        options.addOption(Option.builder("c").longOpt("cidr").hasArg().desc("Specify the CIDR list.").required().build());
        options.addOption(Option.builder("t").longOpt("timeout").hasArg().desc("Specify the DNS time out (sec). Default: 2").build());
        options.addOption(Option.builder("n").longOpt("nocache").desc("Disable results cache.").build());
        options.addOption(Option.builder("h").longOpt("help").desc("Show this help message.").build());
        return options;
    }
}
