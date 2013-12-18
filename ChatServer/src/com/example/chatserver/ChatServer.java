package com.example.chatserver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Implements a chat registration server for the Android chat client example.
 * The protocol looks like this: 1) The client accesses the server via port
 * LISTENER_PORT 2) The client sends a "\n"-delimited string containing its
 * client info: nickname@hostname port 3) if the info is sane, the server sends
 * a list of all other registered clients.
 * 
 * The same chat client can register multiple times; later registrations
 * overwrite older ones. This is to enable changing IP addresses of mobile
 * devices.
 * 
 * ChatServer currently doesn't store the registrations between invocations.
 * 
 * @author Chris Coslor
 * 
 */
public class ChatServer {

    private final Logger log = Logger.getLogger(getClass().getName());

    @SuppressWarnings("rawtypes")
    HashMap m;
    private Map<String, Chatter> chatterMap = Collections
        .synchronizedMap(new HashMap<String, Chatter>());

    private static final int LISTENER_PORT = 666;
    boolean stopListening = false;

    private ExecutorService threadPool = Executors.newFixedThreadPool(10);

    private void listenForConnections(int port) throws IOException {
        ServerSocket serverSocket = null;

        try {

            serverSocket = new ServerSocket(port);
            log.info("Server listening on IP " + InetAddress.getLocalHost().getHostAddress());

            // TODO establish an outside signal to shut down the "service"
            while (!stopListening) {
                try {
                    Socket socket = serverSocket.accept();
                    threadPool.submit(new ChatConnectionProcessor(this, socket));

                }
                catch (SocketTimeoutException ignored) {
                }
            }
        }
        finally {
            // Wait for all threads to gracefully exit
            try {
                threadPool.awaitTermination(10, TimeUnit.SECONDS);
            }
            catch (InterruptedException ignored) {

            }

            if (serverSocket != null) {
                serverSocket.close();
            }
        }
    }

    public static void main(String[] args) throws IOException {

        final ChatServer chatServer = new ChatServer();

        // Since we don't have an unregistration mechanism, let's just clear out
        // all of the chatters every so often. They'll just reregister anyway.
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                chatServer.clearChatters();
            }
        }, 0, 30000);

        // TODO Our chatters should unregister AND their registrations should
        // expire after so long

        chatServer.listenForConnections(LISTENER_PORT);

    }

    public Map<String, Chatter> getChatters() {
        return chatterMap;
    }

    public void clearChatters() {
        log.info("Clearing chatters");

        synchronized (chatterMap) {
            chatterMap.clear();
        }
    }

}
