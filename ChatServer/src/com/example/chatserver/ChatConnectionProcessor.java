package com.example.chatserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Worker that does the actual processing for incoming connections
 * 
 * @author Chris Coslor
 * 
 */
public class ChatConnectionProcessor implements Runnable {

    private final Logger log = Logger.getLogger(getClass().getName());
    /**
	 * 
	 */
    private final ChatServer chatServer;
    private Socket socket;

    public ChatConnectionProcessor(ChatServer chatServer, Socket socket) {
        this.chatServer = chatServer;
        this.socket = socket;
    }

    @Override
    public void run() {
        log.info("ChatConnectionProcessor got a connection!");

        try {
            InputStream inStream = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                socket.getOutputStream()));

            String input = reader.readLine();

            log.fine("Read line:" + input);

            Chatter newChatter = ChatterHelper.readChatter(input);
            if (newChatter != null) {
                writer.write("OK\n");
                writer.flush();

                Map<String, Chatter> chatters = chatServer.getChatters();
                List<Chatter> chatterList = new ArrayList<Chatter>(chatters.values());
                ChatterHelper.writeChatters(writer, chatterList);
                synchronized (chatters) {
                    chatters.put(newChatter.getNickname(), newChatter);
                }
                log.info("Chatters:" + chatters.values());
            }
            else {
                writer.write("ERROR: Bad input format\n");
                log.severe("Got bad input:" + input);
            }
            writer.flush();
            socket.close();

        }
        catch (IOException ie) {
            log.warning(ie.getMessage());
        }

    }

}