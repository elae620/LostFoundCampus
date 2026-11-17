package com.example.lostfoundcampus.ui.publish;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;

import com.example.lostfoundcampus.data.Mappers;
import com.example.lostfoundcampus.data.model.Announcement;
import com.example.lostfoundcampus.data.model.AnnouncementType;
import com.example.lostfoundcampus.data.model.CampusLocation;
import com.example.lostfoundcampus.data.model.Category;
import com.example.lostfoundcampus.data.model.ColorSwatch;
import com.example.lostfoundcampus.data.model.ImageAnalysis;
import com.example.lostfoundcampus.data.remote.dto.PhotoAnalysisDto;

import java.util.ArrayList;
import java.util.List;

/**
 * Brouillon de l'annonce, partagé (portée activité) entre Publier, l'aperçu photo,
 * l'analyse IA et la sélection du lieu, pour survivre aux allers-retours de navigation.
 */
public class PublishViewModel extends ViewModel {

    /** Une photo du brouillon : locale (à envoyer) ou déjà dans Supabase Storage (modification). */
    public static final class PhotoItem {
        @Nullable
        public final Uri localUri;
        @Nullable
        public final String remotePath;
        @Nullable
        public ImageAnalysis analysis;

        PhotoItem(@Nullable Uri localUri, @Nullable String remotePath) {
            this.localUri = localUri;
            this.remotePath = remotePath;
        }
    }

    public AnnouncementType type = AnnouncementType.LOST;
    @Nullable
    public String editingId;

    public final List<PhotoItem> photos = new ArrayList<>();
    /** Photos déjà en ligne retirées pendant la modification (supprimées après enregistrement). */
    public final List<String> removedRemotePaths = new ArrayList<>();
    /** Fichier cible de l'appareil photo en cours. */
    @Nullable
    public Uri cameraTarget;
    /** Photo prise, en attente dans l'aperçu (onglet « J'ai trouvé »). */
    @Nullable
    public Uri pendingPhoto;

    /** Suggestions retenues (photo la plus sûre). */
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
    /** Recherche de position en cours. */
    public boolean locating;
    @Nullable
    public Long date;

    @Nullable
    public String aiTitle;
    @Nullable
    public Category aiCategory;
    @Nullable
    public ColorSwatch aiColor;

    public void reset(AnnouncementType newType) {
        editingId = null;
        photos.clear();
        removedRemotePaths.clear();
        cameraTarget = null;
        pendingPhoto = null;
        analysis = null;
        title = "";
        category = null;
        color = null;
        description = "";
        location = null;
        locationCaptured = false;
        locating = false;
        date = null;
        aiTitle = null;
        aiCategory = null;
        aiColor = null;
        type = newType;
    }

    public void loadForEdit(@NonNull Announcement a, @NonNull List<PhotoAnalysisDto> analyses) {
        reset(a.type);
        editingId = a.id;
        for (String path : a.photoPaths) photos.add(new PhotoItem(null, path));
        for (PhotoAnalysisDto d : analyses) {
            if (d.photoIndex >= 0 && d.photoIndex < photos.size()) {
                photos.get(d.photoIndex).analysis = new ImageAnalysis(d.labels, "",
                        d.labelConfidence == null ? 0 : d.labelConfidence, d.detectedTexts,
                        d.dominantColor == null ? null : Mappers.enumOr(ColorSwatch.class, d.dominantColor, null),
                        a.category, a.title);
            }
        }
        title = a.title;
        category = a.category;
        color = a.color;
        description = a.description == null ? "" : a.description;
        location = a.location;
        date = a.eventDate;
    }

    public boolean isEditing() {
        return editingId != null;
    }

    /** Change d'onglet ; « J'ai perdu » oublie la position capturée automatiquement. */
    public void setType(AnnouncementType newType) {
        type = newType;
        if (newType == AnnouncementType.LOST && locationCaptured) {
            location = null;
            locationCaptured = false;
        }
    }

    /** « J'ai trouvé » sans lieu : il faut capturer la position. */
    public boolean needsCapturedLocation() {
        return type == AnnouncementType.FOUND && location == null && !isEditing();
    }

    public void setCapturedLocation(@NonNull CampusLocation captured) {
        location = captured;
        locationCaptured = true;
    }

    public void setLocation(@NonNull CampusLocation newLocation) {
        location = newLocation;
        locationCaptured = false;
    }

    public int remainingSlots(int max) {
        return Math.max(0, max - photos.size());
    }

    public void addLocalPhotos(List<Uri> uris, int max) {
        for (Uri u : uris) {
            if (photos.size() >= max) break;
            photos.add(new PhotoItem(u, null));
        }
    }

    public void removePhoto(int index) {
        if (index < 0 || index >= photos.size()) return;
        PhotoItem removed = photos.remove(index);
        if (removed.remotePath != null) removedRemotePaths.add(removed.remotePath);
    }

    /** Photos à analyser : la photo prise en attente, sinon les photos locales pas encore analysées. */
    public List<Uri> analysisTargets() {
        List<Uri> out = new ArrayList<>();
        if (pendingPhoto != null) {
            out.add(pendingPhoto);
            return out;
        }
        for (PhotoItem p : photos) if (p.localUri != null && p.analysis == null) out.add(p.localUri);
        return out;
    }

    @Nullable
    public Uri previewSource() {
        if (pendingPhoto != null) return pendingPhoto;
        for (PhotoItem p : photos) if (p.localUri != null) return p.localUri;
        return null;
    }

    /** Range une analyse avec sa photo ; la photo prise en attente rejoint la liste. */
    public void attachAnalysis(@NonNull Uri uri, @Nullable ImageAnalysis result, int max) {
        if (uri.equals(pendingPhoto)) {
            if (photos.size() < max) {
                PhotoItem item = new PhotoItem(uri, null);
                item.analysis = result;
                photos.add(item);
            }
            pendingPhoto = null;
            return;
        }
        for (PhotoItem p : photos) {
            if (uri.equals(p.localUri)) p.analysis = result;
        }
    }

    /**
     * Applique les suggestions de la photo la plus sûre aux champs vides ou encore égaux
     * à une ancienne suggestion (jamais à une saisie de l'utilisateur, RG03).
     */
    public void applyBestSuggestions() {
        ImageAnalysis best = null;
        for (PhotoItem p : photos) {
            if (p.analysis != null && (best == null || p.analysis.confidence > best.confidence)) best = p.analysis;
        }
        if (best == null) return;
        analysis = best;
        if (title.trim().isEmpty() || title.equals(aiTitle)) {
            title = best.suggestedTitle;
            aiTitle = best.suggestedTitle;
        }
        if (category == null || category == aiCategory) {
            category = best.suggestedCategory;
            aiCategory = best.suggestedCategory;
        }
        if (best.dominantColor != null && (color == null || color == aiColor)) {
            color = best.dominantColor;
            aiColor = best.dominantColor;
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
}
