package com.example.chatserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatterHelper {

    private static Pattern inputPattern = Pattern.compile("(.*)@(.*) ([0-9]*)");

    public static void writeChatters(BufferedWriter writer, List<Chatter> chatters)
        throws IOException {
        for (Chatter chatter : chatters) {
            writeChatter(writer, chatter);
        }

    }

    public static Chatter readChatter(String input) {
        Chatter newChatter = null;
        try {
            Matcher matcher = inputPattern.matcher(input);
            if (matcher.find()) {
                String nickname = matcher.group(1);
                String hostname = matcher.group(2);
                int port = Integer.parseInt(matcher.group(3));
                newChatter = new Chatter(nickname, hostname, port);
            }
        }
        catch (NumberFormatException ignored) {
        }
        return newChatter;
    }

    public static void writeChatter(BufferedWriter writer, Chatter chatter) throws IOException {

        InetSocketAddress address = chatter.getAddress();
        //TODO refactor to use a formatter that doesn't create a bunch of strings
        writer.write(chatter.getNickname() + "@" + address.getHostName() + " " + address.getPort()
            + "\r\n");
        writer.flush();

    }

    public static List<Chatter> readChatters(BufferedReader reader) throws IOException {
        List<Chatter> chatters = new ArrayList<Chatter>();
        while (true) {
            String input = reader.readLine();
            if (input == null || input.length() == 0) {
                break;
            }
            Chatter chatter = readChatter(input);
            if (chatter != null) {
                chatters.add(chatter);
            }
        }
        return chatters;
    }

}
