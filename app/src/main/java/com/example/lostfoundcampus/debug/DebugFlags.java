package com.example.lostfoundcampus.debug;

import androidx.annotation.Nullable;

/**
 * ÉTAPE 1 — interrupteur de démonstration « mode auteur » du détail d'annonce.
 * Pour le retirer : supprimer cette classe et la vue debugAuthorCard de
 * fragment_announcement_detail.xml (voir AnnouncementDetailFragment.bindDebugSwitch).
 */
public final class DebugFlags {

    /** null = comportement réel (auteur si authorId == utilisateur courant). */
    @Nullable
    private static Boolean forcedAuthorMode = null;

    private DebugFlags() {
    }

    public static boolean isAuthor(boolean realValue) {
        return forcedAuthorMode != null ? forcedAuthorMode : realValue;
    }

    public static void setForcedAuthorMode(@Nullable Boolean value) {
        forcedAuthorMode = value;
    }
}
