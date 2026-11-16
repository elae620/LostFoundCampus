package com.example.lostfoundcampus.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.lostfoundcampus.data.model.Announcement;
import com.example.lostfoundcampus.data.model.AnnouncementStatus;
import com.example.lostfoundcampus.data.model.AnnouncementType;
import com.example.lostfoundcampus.data.model.CampusLocation;
import com.example.lostfoundcampus.data.model.Category;
import com.example.lostfoundcampus.data.model.Conversation;
import com.example.lostfoundcampus.data.model.Match;
import com.example.lostfoundcampus.data.model.Message;
import com.example.lostfoundcampus.data.model.User;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ÉTAPE 1 — dépôt en mémoire. Son interface publique préfigure les dépôts Firebase
 * (Auth, Firestore, Storage) de l'étape 2 : les écrans n'auront presque pas à changer.
 */
public final class FakeRepository {

    public enum ProfileFilter { ALL, LOST, FOUND, RETURNED }

    private static FakeRepository instance;

    final Map<String, User> users = new LinkedHashMap<>();
    final Map<String, Announcement> announcements = new LinkedHashMap<>();
    final Map<String, Conversation> conversations = new LinkedHashMap<>();
    final List<Match> matches = new ArrayList<>();

    private String currentUserId = FakeData.ME;
    private String password = FakeData.DEMO_PASSWORD;
    private int idCounter = 0;

    private FakeRepository() {
        FakeData.seed(this);
    }

    @NonNull
    public static synchronized FakeRepository get() {
        if (instance == null) instance = new FakeRepository();
        return instance;
    }

    // ---------------------------------------------------------------- Session

    /** Connexion simulée : e-mail universitaire valide + mot de passe de démo. */
    public boolean signIn(String email, String pass) {
        User me = users.get(FakeData.ME);
        return me != null && me.universityEmail.equalsIgnoreCase(email.trim()) && password.equals(pass);
    }

    public void register(String fullName) {
        User me = currentUser();
        me.fullName = fullName.trim();
    }

    public void signOut() {
        // Rien à nettoyer en étape 1 : la session est simulée.
    }

    @NonNull
    public User currentUser() {
        User u = users.get(currentUserId);
        if (u == null) throw new IllegalStateException("Utilisateur courant absent");
        return u;
    }

    public boolean isCurrentUser(String uid) {
        return currentUserId.equals(uid);
    }

    @Nullable
    public User user(String uid) {
        return users.get(uid);
    }

    public boolean checkPassword(String candidate) {
        return password.equals(candidate);
    }

    public void changePassword(String newPassword) {
        password = newPassword;
    }

    public static String demoEmail() {
        return FakeData.DEMO_EMAIL;
    }

    // ---------------------------------------------------------------- Annonces

    @NonNull
    public List<Announcement> feed(@Nullable AnnouncementType type, @Nullable Category category, @Nullable String query) {
        String q = normalize(query);
        List<Announcement> out = new ArrayList<>();
        for (Announcement a : announcements.values()) {
            if (!a.isActive()) continue;
            if (type != null && a.type != type) continue;
            if (category != null && a.category != category) continue;
            if (!q.isEmpty()) {
                String haystack = normalize(a.title + " " + a.description + " "
                        + (a.location.precision == null ? "" : a.location.precision));
                if (!haystack.contains(q)) continue;
            }
            out.add(a);
        }
        Collections.sort(out, (x, y) -> Long.compare(y.createdAt, x.createdAt));
        return out;
    }

    @Nullable
    public Announcement announcement(@Nullable String id) {
        return id == null ? null : announcements.get(id);
    }

    @NonNull
    public List<Announcement> myAnnouncements(ProfileFilter filter) {
        List<Announcement> out = new ArrayList<>();
        for (Announcement a : announcements.values()) {
            if (!a.authorId.equals(currentUserId)) continue;
            switch (filter) {
                case LOST:
                    if (a.type != AnnouncementType.LOST || a.status == AnnouncementStatus.RETURNED) continue;
                    break;
                case FOUND:
                    if (a.type != AnnouncementType.FOUND || a.status == AnnouncementStatus.RETURNED) continue;
                    break;
                case RETURNED:
                    if (a.status != AnnouncementStatus.RETURNED) continue;
                    break;
                default:
                    break;
            }
            out.add(a);
        }
        Collections.sort(out, (x, y) -> Long.compare(y.createdAt, x.createdAt));
        return out;
    }

    @NonNull
    public String newAnnouncementId() {
        idCounter++;
        return "a_new_" + System.currentTimeMillis() + "_" + idCounter;
    }

    public void save(@NonNull Announcement announcement) {
        announcements.put(announcement.id, announcement);
    }

    public void markReturned(String id) {
        Announcement a = announcements.get(id);
        if (a != null && a.authorId.equals(currentUserId)) {
            a.status = AnnouncementStatus.RETURNED;
        }
    }

    public void updateLocation(String id, CampusLocation location) {
        Announcement a = announcements.get(id);
        if (a != null && a.authorId.equals(currentUserId)) {
            a.location = location;
        }
    }

    @NonNull
    public CampusLocation campusCenter() {
        return new CampusLocation(FakeData.CAMPUS_LAT, FakeData.CAMPUS_LON, null);
    }

    // ---------------------------------------------------------------- Correspondances

    /** Correspondances actives au-dessus du seuil, triées par score décroissant. */
    @NonNull
    public List<Match> matchesFor(String announcementId, int threshold) {
        List<Match> out = new ArrayList<>();
        for (Match m : matches) {
            if (!m.lostAnnouncementId.equals(announcementId) && !m.foundAnnouncementId.equals(announcementId)) continue;
            if (m.score < threshold) continue;
            Announcement other = announcements.get(m.otherId(announcementId));
            if (other == null || !other.isActive()) continue;
            out.add(m);
        }
        Collections.sort(out, (x, y) -> Integer.compare(y.score, x.score));
        return out;
    }

    // ---------------------------------------------------------------- Messagerie

    @NonNull
    public List<Conversation> conversations(@Nullable String query) {
        String q = normalize(query);
        List<Conversation> out = new ArrayList<>();
        for (Conversation c : conversations.values()) {
            if (!q.isEmpty()) {
                User other = users.get(c.otherParticipant(currentUserId));
                Announcement a = announcement(c.relatedAnnouncementId);
                String haystack = normalize((other == null ? "" : other.fullName) + " " + (a == null ? "" : a.title));
                if (!haystack.contains(q)) continue;
            }
            out.add(c);
        }
        Collections.sort(out, (x, y) -> Long.compare(y.lastMessageAt, x.lastMessageAt));
        return out;
    }

    @Nullable
    public Conversation conversation(@Nullable String id) {
        return id == null ? null : conversations.get(id);
    }

    /** Ouvre la conversation existante liée à l'annonce, ou en crée une vide. */
    @NonNull
    public Conversation openConversation(String otherUserId, String announcementId) {
        for (Conversation c : conversations.values()) {
            if (c.participantIds.contains(otherUserId) && announcementId.equals(c.relatedAnnouncementId)) {
                return c;
            }
        }
        idCounter++;
        Conversation c = new Conversation("c_new_" + idCounter, currentUserId, otherUserId, announcementId,
                System.currentTimeMillis());
        conversations.put(c.id, c);
        return c;
    }

    @NonNull
    public Message sendMessage(String conversationId, String text) {
        Conversation c = conversations.get(conversationId);
        if (c == null) throw new IllegalArgumentException("Conversation inconnue");
        idCounter++;
        long now = System.currentTimeMillis();
        Message m = new Message("m_new_" + idCounter, text.trim(), now, currentUserId, Message.State.SENDING, true);
        c.messages.add(m);
        c.lastMessageAt = now;
        return m;
    }

    public void markRead(String conversationId) {
        Conversation c = conversations.get(conversationId);
        if (c == null) return;
        for (Message m : c.messages) m.isRead = true;
    }

    // ---------------------------------------------------------------- Outils

    @NonNull
    static String normalize(@Nullable String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s.trim().toLowerCase(Locale.FRENCH), Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}", "");
    }
}
