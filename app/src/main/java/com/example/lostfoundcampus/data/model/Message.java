package com.example.lostfoundcampus.data.model;

public class Message {

    public enum State { SENDING, SENT }

    public final String id;
    public final String text;
    public final long sentAt;
    public final String senderId;
    public State state;
    public boolean isRead;

    public Message(String id, String text, long sentAt, String senderId, State state, boolean isRead) {
        this.id = id;
        this.text = text;
        this.sentAt = sentAt;
        this.senderId = senderId;
        this.state = state;
        this.isRead = isRead;
    }
}
