package com.example.chatserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 * 
 * @author Chris Coslor
 *
 */
public class ChatConnectionProcessorTest {

    String listString = "joe@1.2.3.4 99\r\nfred@5.6.7.8 999\r\nsam@9.a.b.c 9999\r\n";

    List<Chatter> chatters;
    ChatServer chatServer = new ChatServer();
    ChatConnectionProcessor testProcessor = new ChatConnectionProcessor(null, null);

    @Before
    public void setup() {
        if (chatters == null) {
            chatters = new ArrayList<Chatter>();
            chatters.add(new Chatter("joe", "1.2.3.4", 99));
            chatters.add(new Chatter("fred", "5.6.7.8", 999));
            chatters.add(new Chatter("sam", "9.a.b.c", 9999));
        }
    }

    @Test
    public void testWriteChatterList() throws IOException {
        CharArrayWriter testWriter = new CharArrayWriter();
        ChatterHelper.writeChatters(new BufferedWriter(testWriter), chatters);

        assertEquals(testWriter.toString().trim(), listString.trim());
    }

    @Test
    public void testReadChatter() {
        Chatter chatter = ChatterHelper.readChatter("joe@1.2.3.4 99");
        assertTrue(chatter.getNickname().equals("joe")
            && chatter.getAddress().getHostName().equals("1.2.3.4")
            && chatter.getAddress().getPort() == 99);
    }

}
