package com.example.lostfoundcampus.data.model;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Announcement {

    public final String id;
    public AnnouncementType type;
    public AnnouncementStatus status;
    public String title;
    public Category category;
    public String description;
    @Nullable
    public ColorSwatch color;
    public CampusLocation location;
    public long eventDate;
    public final long createdAt;
    /** Étape 1 : drawables d'exemple (1 à 5). Étape 2 : photoUrls dans Storage. */
    public final List<Integer> photoRes = new ArrayList<>();
    public final String authorId;
    @Nullable
    public ImageAnalysis analysis;

    public Announcement(String id, AnnouncementType type, AnnouncementStatus status, String title,
                        Category category, String description, @Nullable ColorSwatch color,
                        CampusLocation location, long eventDate, long createdAt,
                        List<Integer> photos, String authorId) {
        this.id = id;
        this.type = type;
        this.status = status;
        this.title = title;
        this.category = category;
        this.description = description;
        this.color = color;
        this.location = location;
        this.eventDate = eventDate;
        this.createdAt = createdAt;
        this.photoRes.addAll(photos);
        this.authorId = authorId;
    }

    public boolean isActive() {
        return status == AnnouncementStatus.ACTIVE;
    }

    @DrawableRes
    public int coverPhoto() {
        return photoRes.isEmpty() ? 0 : photoRes.get(0);
    }

    public boolean hasDescription() {
        return description != null && !description.trim().isEmpty();
    }
}
