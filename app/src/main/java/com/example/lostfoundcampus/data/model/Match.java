package com.example.lostfoundcampus.data.model;

public class Match {

    public final String lostAnnouncementId;
    public final String foundAnnouncementId;
    public final int score;

    public Match(String lostAnnouncementId, String foundAnnouncementId, int score) {
        this.lostAnnouncementId = lostAnnouncementId;
        this.foundAnnouncementId = foundAnnouncementId;
        this.score = score;
    }

    public String otherId(String announcementId) {
        return announcementId.equals(lostAnnouncementId) ? foundAnnouncementId : lostAnnouncementId;
    }
}
