package com.example.chatserver;

import java.io.Serializable;
import java.net.InetSocketAddress;

/** Chatter represents a chat client.
 * 
 * @author Chris Coslor
 *
 */
public class Chatter implements Serializable {

    private String nickname;
    private InetSocketAddress address;

    public Chatter(String nickname, String hostname, int port) {
        setNickname(nickname);
        setAddress(hostname, port);
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public void setAddress(InetSocketAddress address) {
        this.address = address;
    }

    public void setAddress(String hostname, int port) {
        setAddress(new InetSocketAddress(hostname, port));
    }

    @Override
    public String toString() {
        return "Chatter nickname=" + nickname + " address=" + address;
    }
}
