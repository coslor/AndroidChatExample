package com.example.androidchat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.conn.util.InetAddressUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.chatserver.Chatter;
import com.example.chatserver.ChatterHelper;

/**
 * AndroidChatActivity is an android-based "chat" app. It uses a central server for lookups,
 * 	and communicates with other instances directly.
 *    
 * @author Chris Coslor
 *
 */
public class AndroidChatActivity extends Activity {

    private static final int CHAT_LISTENER_TIMEOUT = 1000;

	private static final int REGISTRATION_DELAY = 10000;

	private static final int SERVER_CONNECT_TIMEOUT = 10000;

	private final String TAG = getClass().getName();

    private static final int MAX_SCROLL_BUFFER = 4096;

    private ServerSocket listenerSocket = null;

    // FIXME Ask for a nickname
    private String myNickname;

    Thread listenerThread;

    private Chatter myChatter;
    private Chatter friend;

    private String myAddress = null;

    private Timer registrationTimer = new Timer();

    // FIXME ask for the server address
    private InetSocketAddress serverAddress = null;

    private boolean keepListeningForFriend = true;
    private boolean keepConnectingToServer = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Allow the use of the progress bar
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.activity_main);

        // The app only works with wifi; communication across networks is too
        // painful
        if (!wifiIsOn()) {
            showFatalDialog("Sorry, wifi must be turned on! Exiting!");
            return;
        }
        

        myNickname = getSerialNumber();
        setTitle(myNickname);

        addTextToScrollView("AndroidChat starting\n");

        addTextToScrollView("Looking for friends...\n");
        
        //Show spinning progress bar while we look
        showProgressBar(true);
        askForServerAddress();

        Button b = (Button) findViewById(R.id.sendButton);

        b.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                EditText chatBox = (EditText) findViewById(R.id.chatBox);
                String message = chatBox.getText().toString();
                sendMessage(message);
                chatBox.setText("");
            }
        });
    }

    /** Show (or hide) a spinning circle in the title bar **/
    private void showProgressBar(final boolean on) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                setProgressBarIndeterminate(on);
                setProgressBarIndeterminateVisibility(on);
                setProgressBarVisibility(on);
            }});
    }

    private void askForServerAddress() {

    	final EditText input = new EditText(this);

    	new AlertDialog.Builder(this)
        .setTitle("Server Address")
        .setMessage("Please enter the address of the chat server")
        .setView(input)
        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String serverHostname = input.getText().toString();
                setupNetworkingInNonGuiThread(serverHostname);
                //showProgressBar(false);
            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing.
            }
        }).show();
    }
    
    private void setupNetworkingInNonGuiThread(final String serverHostname) {
        // Run our network setup stuff in a non-GUI thread
        new AsyncTask<Object, Object, Object>() {
            @Override
            protected Object doInBackground(Object... nothing) {
                setupNetworking(serverHostname);
                return null;
            }
        }.execute();
    }

    /** Show an error dialog and leave the app if wifi is off **/
    private void showFatalDialog(String fatalMessage) {
        // FIXME get strings from props
        new AlertDialog.Builder(this).setTitle("ERROR")
            .setMessage(fatalMessage)
            .setPositiveButton("Close", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                    moveTaskToBack(true);
                }
            }).show();
    }

    
    private void sendMessage(final String message) {
        addTextToScrollView("You:" + message + "\n");

        new AsyncTask<Object, Object, Object>() {
            @Override
            protected Object doInBackground(Object... nothing) {
                try {
                    sendNetworkMessageToFriend(message);
                }
                catch (IOException ie) {
                    Log.e(TAG, "Error sending message to friend", ie);
                    addTextToScrollView(friend.getNickname()
                        + " is a bad friend. Disconnecting and choosing another one.\n");
                    setFriend(null);
                    showProgressBar(true);
                }
                return null;
            }
        }.execute();

    }

    public void displayMessage(String nickname, String message) {
        addTextToScrollView(nickname + ":" + message + "\n");
    }

    /** Add a line of text to the chat history box and scroll it to the bottom **/
    private void addTextToScrollView(String text) {
        //TODO deal with color here
        final TextView textView = (TextView) findViewById(R.id.chatHistory);
        textView.setMovementMethod(new ScrollingMovementMethod());
        String currentText = textView.getText().toString();
        currentText += text;

        // Don't let our text grow without bound
        int startingOffset = currentText.length() - MAX_SCROLL_BUFFER;
        if (startingOffset > 0) {
            currentText = currentText.substring(startingOffset);
        }

        final String finalText = currentText;

        // After we add to the text, scroll the view to the bottom
        final ScrollView scrollView = (ScrollView) findViewById(R.id.scrollView);

        // addTextToScrollView() may or may not have been called from the GUI
        // thread
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(finalText);

                // Voodoo to get the ScrollView to scroll down to the bottom
                // each time
                scrollView.post(new Runnable() {

                    @Override
                    public void run() {
                        scrollView.fullScroll(ScrollView.FOCUS_DOWN);

                    }
                });
            }
        });

    }

    /** Start listening for incoming chat connections **/
    private void setupSocketListener() {
        Log.d(TAG, "Setting up listener");

        try {
            listenerSocket = new ServerSocket(0); // OS, pick us a socket
            listenerSocket.setSoTimeout(CHAT_LISTENER_TIMEOUT);

            myChatter = new Chatter(myNickname, getLocalIpAddress(), listenerSocket.getLocalPort());

            Thread listenerThread = new Thread(new MessageListener());
            listenerThread.start();

            Log.i(TAG,
                "Now listening at " + getLocalIpAddress() + ":" + listenerSocket.getLocalPort());

        }
        catch (IOException ie) {
            Log.e(TAG, "Error processing socket", ie);
        }

    }

    /**
     * Send my info to the chat server & get back a list of the other chatters.
     * Pick one (that isn't me) for my friend.
     **/
    private void registerWithServer() throws IOException {

        Log.d(TAG, "Trying to register with server");

        Socket clientSocket = null;
        try {

            // If I haven't set up my listener socket yet,
            // don't bother to register
            if (myChatter != null && serverAddress != null) {

            	Log.d(TAG, "Connecting to the server...");
            	
                clientSocket = new Socket();
                clientSocket.connect(serverAddress, SERVER_CONNECT_TIMEOUT);
                
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    clientSocket.getOutputStream()));

                Log.d(TAG,
                        "Sending my registration info for my nickname " + myChatter.getNickname());
                
                ChatterHelper.writeChatter(writer, myChatter);
                writer.flush();

                BufferedReader reader = new BufferedReader(new InputStreamReader(
                    clientSocket.getInputStream()));

                String response = reader.readLine();
                Log.d(TAG, "Got response:" + response);

                Log.d(TAG, "Reading the chat list");
                //TODO This is way slower than it should be. Fix it.
                List<Chatter> chatters = ChatterHelper.readChatters(reader);

                Log.d(TAG, "Got " + chatters.size() + " chatters");

                if (friend == null) {
                    // FIXME let the user choose a friend
                    Iterator<Chatter> iter = chatters.iterator();
                    while (iter.hasNext()) {
                        Chatter chatter = iter.next();
                        if (!chatter.getNickname().equals(myNickname)) {
                            setFriend(chatter);
                            addTextToScrollView("Current friend is:" + friend.getNickname() + "\n");
                            showProgressBar(false);
                            break;
                        }
                    }
                } // friend == null

            }// myChatter != null
        }// try

        catch (Throwable t) {
        	Log.e(TAG, "Unable to connect to server", t);
        	runOnUiThread(new Runnable() {

				@Override
				public void run() {
		        	showFatalDialog("Unable to connect to server! Exiting.");
				}
        		
        	});
        	
            keepConnectingToServer = false;
        }
        finally {
            // Whether or not we could connect to the server, try again in 10
            // secs
            if (keepConnectingToServer) {
                scheduleNextRegistration(REGISTRATION_DELAY);
            }
            if (clientSocket != null) {
                clientSocket.close();
            }
        }

    }

    /**
     * Set a Chatter to be our Friend. Make an outgoing connection to that
     * client. Should only be run from a non-main thread.
     **/
    private void setFriend(Chatter chatter) {

        friend = chatter;

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                setEditingEnabled(friend != null);
            }
        });

    }

    /** Disable (gray out) or enable the chat box and send buttons **/
    private void setEditingEnabled(boolean enabled) {
        final EditText chatBox = (EditText) findViewById(R.id.chatBox);
        chatBox.setEnabled(enabled);
        final Button sendButton = (Button) findViewById(R.id.sendButton);
        sendButton.setEnabled(enabled);

    }

    /** Schedule a task to run a period of time from now, to register again **/
    private void scheduleNextRegistration(long delay) {
        registrationTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                try {
                    registerWithServer();
                }
                catch (IOException ie) {
                    Log.e(TAG, "Unable to register in timer task", ie);
                }
            }
        }, delay);
    }

    private boolean wifiIsOn() {
        WifiManager wim = (WifiManager) getSystemService(WIFI_SERVICE);
        return wim.isWifiEnabled();

    }

    private String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en
                .hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr
                    .hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();

                    String ipv4 = inetAddress.getHostAddress();
                    // for getting IPV4 format
                    if (!inetAddress.isLoopbackAddress() && InetAddressUtils.isIPv4Address(ipv4)) {
                        return ipv4;
                    }
                }
            }
        }
        catch (Exception ex) {
            Log.e("IP Address", ex.toString());
        }
        return null;
    }

    private void sendNetworkMessageToFriend(String message) throws IOException {
        if (friend == null) {
            Log.w(TAG, "No friend yet!");
            return;
        }

        Socket friendSocket = new Socket(friend.getAddress().getAddress(), friend.getAddress()
            .getPort());

        BufferedWriter friendWriter = new BufferedWriter(new OutputStreamWriter(
            friendSocket.getOutputStream()));

        Log.d(TAG, "Sending message '" + message + "' to " + friend.getAddress());
        friendWriter.write(message + "\n");
        friendWriter.flush();
        friendSocket.close();

    }

    // TODO deal properly with the Activity lifecycle.

    @Override
    protected void onStop() {
        super.onPause();
        Log.d(TAG, "onStop()");

        keepListeningForFriend = false;
        keepConnectingToServer = false;

        // TODO unregister from server

        try {
            if (listenerSocket != null) {
                listenerSocket.close();
            }
        }
        catch (IOException ie) {
            Log.w(TAG, ie);
        }
    }

    private String getSerialNumber() {
        String serial = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE))
            .getDeviceId();
        if (serial == null) {
            serial = android.os.Build.SERIAL;
        }
        return serial;
    }

    private void setupNetworking(String serverHostname) {
        try {
            serverAddress = new InetSocketAddress(serverHostname, 666);

            myAddress = getLocalIpAddress();

            addTextToScrollView("My address is " + myAddress + "\n");

            setupSocketListener();
            registerWithServer();
        }
        catch (Exception e) {
            Log.e(TAG, "Unable to register with server", e);
            addTextToScrollView("Unable to connect to server!\n");
        }
    }

    /**
     * Listens for incoming chat messages & displays them to the screen when
     * they come in
     **/
    private final class MessageListener implements Runnable {
        @Override
        public void run() {
            while (keepListeningForFriend) {
                Socket socket = null;

                try {
                    socket = listenerSocket.accept();
                    // If the sender isn't our friend, when we don't want to
                    // hear from them
                    InetSocketAddress remoteAddress = (InetSocketAddress) socket
                        .getRemoteSocketAddress();

                    if (friend != null) {
                        Log.d(TAG, "Got incoming connection from " + remoteAddress + " friend is "
                            + friend.getAddress());
                    }

                    BufferedReader reader = new BufferedReader(new InputStreamReader(
                        socket.getInputStream()));
                    
                    while (true) {
                        String input = reader.readLine();
                        if (input == null) {
                            break;
                        }
                        
                        String friendNickname = (friend == null ? "Unknown" : friend.getNickname());
                        displayMessage(friendNickname, input);
                    }
                    
                    if (socket != null) {
                        socket.close();
                    }
                }
                // It's normal to time out --just keep going
                catch (SocketTimeoutException ignored) {
                }
                catch (IOException ie) {
                    Log.e(TAG, "Error in thread reading input", ie);
                }
                finally {
                    if (socket != null) {
                        try {
                            socket.close();
                        }
                        catch (IOException ignored) {
                        }
                    }
                }// finally

            }// keepListeningForFriend
        }// run
    } //MessageListener

}
