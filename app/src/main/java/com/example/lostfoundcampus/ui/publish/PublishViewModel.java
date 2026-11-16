package com.example.lostfoundcampus.ui.publish;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;

import com.example.lostfoundcampus.data.FakeRepository;
import com.example.lostfoundcampus.data.model.Announcement;
import com.example.lostfoundcampus.data.model.AnnouncementStatus;
import com.example.lostfoundcampus.data.model.AnnouncementType;
import com.example.lostfoundcampus.data.model.CampusLocation;
import com.example.lostfoundcampus.data.model.Category;
import com.example.lostfoundcampus.data.model.ColorSwatch;
import com.example.lostfoundcampus.data.model.ImageAnalysis;

import java.util.ArrayList;
import java.util.List;

/**
 * Brouillon de l'annonce, partagé (portée activité) entre Publier, l'aperçu photo,
 * l'analyse IA et la sélection du lieu, pour survivre aux allers-retours de navigation.
 */
public class PublishViewModel extends ViewModel {

    public AnnouncementType type = AnnouncementType.LOST;
    @Nullable
    public String editingId;

    public final List<Integer> photos = new ArrayList<>();
    /** Photo prise, en attente d'analyse dans l'aperçu (onglet « J'ai trouvé »). */
    @Nullable
    public Integer pendingPhoto;

    @Nullable
    public ImageAnalysis analysis;

    public String title = "";
    @Nullable
    public Category category;
    @Nullable
    public ColorSwatch color;
    public String description = "";
    @Nullable
    public CampusLocation location;
    /** Lieu prérempli par la position capturée (onglet « J'ai trouvé »). */
    public boolean locationCaptured;
    @Nullable
    public Long date;

    /** Valeurs proposées par l'IA, pour signaler « Suggestion IA : à vérifier » tant qu'elles sont intactes. */
    @Nullable
    public String aiTitle;
    @Nullable
    public Category aiCategory;
    @Nullable
    public ColorSwatch aiColor;

    public void reset(AnnouncementType newType) {
        editingId = null;
        photos.clear();
        pendingPhoto = null;
        analysis = null;
        title = "";
        category = null;
        color = null;
        description = "";
        location = null;
        locationCaptured = false;
        date = null;
        aiTitle = null;
        aiCategory = null;
        aiColor = null;
        setType(newType);
    }

    public void loadForEdit(@NonNull Announcement a) {
        reset(a.type);
        editingId = a.id;
        photos.addAll(a.photoRes);
        analysis = a.analysis;
        title = a.title;
        category = a.category;
        color = a.color;
        description = a.description == null ? "" : a.description;
        location = a.location;
        locationCaptured = false;
        date = a.eventDate;
    }

    public boolean isEditing() {
        return editingId != null;
    }

    /** Change d'onglet. « J'ai trouvé » préremplit le lieu avec la position capturée. */
    public void setType(AnnouncementType newType) {
        type = newType;
        if (newType == AnnouncementType.FOUND && location == null) {
            // ÉTAPE 1 : position d'exemple fixe. Étape 2 : FusedLocationProviderClient.
            location = FakeRepository.get().campusCenter();
            locationCaptured = true;
        } else if (newType == AnnouncementType.LOST && locationCaptured) {
            location = null;
            locationCaptured = false;
        }
    }

    public void setLocation(@NonNull CampusLocation newLocation) {
        location = newLocation;
        locationCaptured = false;
    }

    public int remainingSlots(int max) {
        return Math.max(0, max - photos.size());
    }

    public void addPhotos(List<Integer> newPhotos, int max) {
        for (Integer p : newPhotos) {
            if (photos.size() >= max) break;
            photos.add(p);
        }
    }

    public void removePhoto(int index) {
        if (index >= 0 && index < photos.size()) photos.remove(index);
    }

    /** Photo montrée pendant l'analyse : la photo prise, sinon la première de la galerie. */
    @DrawableRes
    public int analysisSource() {
        if (pendingPhoto != null) return pendingPhoto;
        return photos.isEmpty() ? 0 : photos.get(0);
    }

    /**
     * Fin d'analyse : la photo prise rejoint la liste et les suggestions remplissent
     * les champs vides ou encore égaux à une ancienne suggestion (jamais une saisie de l'utilisateur).
     */
    public void applyAnalysis(@NonNull ImageAnalysis result, int max) {
        if (pendingPhoto != null) {
            if (photos.size() < max) photos.add(pendingPhoto);
            pendingPhoto = null;
        }
        analysis = result;
        if (title.trim().isEmpty() || title.equals(aiTitle)) {
            title = result.suggestedTitle;
            aiTitle = result.suggestedTitle;
        }
        if (category == null || category == aiCategory) {
            category = result.suggestedCategory;
            aiCategory = result.suggestedCategory;
        }
        if (color == null || color == aiColor) {
            color = result.dominantColor;
            aiColor = result.dominantColor;
        }
    }

    public boolean titleIsAiSuggestion() {
        return aiTitle != null && aiTitle.equals(title);
    }

    public boolean categoryIsAiSuggestion() {
        return aiCategory != null && aiCategory == category;
    }

    public boolean colorIsAiSuggestion() {
        return aiColor != null && aiColor == color;
    }

    /** Crée ou met à jour l'annonce dans le dépôt et renvoie son identifiant. */
    @NonNull
    public String save(@NonNull FakeRepository repo) {
        if (location == null || category == null || date == null) {
            throw new IllegalStateException("Formulaire incomplet");
        }
        Announcement existing = repo.announcement(editingId);
        if (existing != null) {
            existing.type = type;
            existing.title = title.trim();
            existing.category = category;
            existing.color = color;
            existing.description = description.trim();
            existing.location = location;
            existing.eventDate = date;
            existing.photoRes.clear();
            existing.photoRes.addAll(photos);
            existing.analysis = analysis;
            repo.save(existing);
            return existing.id;
        }
        Announcement created = new Announcement(repo.newAnnouncementId(), type, AnnouncementStatus.ACTIVE,
                title.trim(), category, description.trim(), color, location, date,
                System.currentTimeMillis(), photos, repo.currentUser().uid);
        if (analysis != null) analysis.userConfirmed = true;
        created.analysis = analysis;
        repo.save(created);
        return created.id;
    }
}
