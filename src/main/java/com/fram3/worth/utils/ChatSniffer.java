package com.fram3.worth.utils;

import com.fram3.worth.Chat;
import com.fram3.worth.User;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;

/**
 * ChatSniffer modella il task che sniffa i messaggi inviati sulla chat,
 * salvandoli nella lista dei messaggi della Chat per consultarli in seguito
 *
 * @author Francesco Amodeo
 * @version 1.0
 */
public class ChatSniffer implements Runnable {

    /** chat di cui sniffare i messaggi */
    private final Chat chat;

    /** user a cui interessano i messaggi */
    private final User user;

    /** socket per collegarsi al gruppo multicast */
    MulticastSocket multicastSocket;

    public ChatSniffer(Chat chat, User user) {
        this.chat = chat;
        this.user = user;
    }

    @Override
    public void run() {
        try {
            multicastSocket = new MulticastSocket(chat.getPort());
            multicastSocket.joinGroup(chat.getAddress());
            multicastSocket.setSoTimeout(1000);
            while (user.isOnline() && !Thread.currentThread().isInterrupted()) {
                try {
                    byte[] buffer = new byte[8192];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    multicastSocket.receive(packet);
                    String received = new String(packet.getData());
                    chat.getMessages().add(received.trim());
                } catch (SocketTimeoutException ignored) {
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (multicastSocket != null) {
                    multicastSocket.leaveGroup(chat.getAddress());
                    multicastSocket.close();
                }
            } catch (IOException e) { e.printStackTrace(); }
        }
    }
}
