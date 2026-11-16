package com.example.lostfoundcampus.data;

import com.example.lostfoundcampus.R;
import com.example.lostfoundcampus.data.model.Announcement;
import com.example.lostfoundcampus.data.model.AnnouncementStatus;
import com.example.lostfoundcampus.data.model.AnnouncementType;
import com.example.lostfoundcampus.data.model.CampusLocation;
import com.example.lostfoundcampus.data.model.Category;
import com.example.lostfoundcampus.data.model.ColorSwatch;
import com.example.lostfoundcampus.data.model.Conversation;
import com.example.lostfoundcampus.data.model.Match;
import com.example.lostfoundcampus.data.model.Message;
import com.example.lostfoundcampus.data.model.User;

import java.util.Arrays;
import java.util.Collections;

/**
 * ÉTAPE 1 UNIQUEMENT — données factices locales qui simulent les documents Firestore.
 * Les textes ci-dessous sont des données (comme le contenu d'une base), pas des textes
 * d'interface : ils seront remplacés par Firestore à l'étape 2 et ce fichier supprimé.
 */
final class FakeData {

    static final String ME = "u_amine";
    static final String SARA = "u_sara";
    static final String YOUSSEF = "u_youssef";
    static final String LINA = "u_lina";

    /** Identifiants de démonstration acceptés par l'écran de connexion simulé. */
    static final String DEMO_EMAIL = "amine.bensalah@universite.tn";
    static final String DEMO_PASSWORD = "campus2026";

    /** Position d'exemple fixe sur le campus (étape 1). */
    static final double CAMPUS_LAT = 36.8368;
    static final double CAMPUS_LON = 10.1475;

    private static final long DAY = 24L * 60 * 60 * 1000;
    private static final long MINUTE = 60L * 1000;

    private FakeData() {
    }

    static void seed(FakeRepository repo) {
        long now = System.currentTimeMillis();

        repo.users.put(ME, new User(ME, "Amine Ben Salah", DEMO_EMAIL, "Étudiant", 0));
        repo.users.put(SARA, new User(SARA, "Sara Mansour", "sara.mansour@universite.tn", "Étudiante", 0));
        repo.users.put(YOUSSEF, new User(YOUSSEF, "Youssef K.", "youssef.k@universite.tn", "Étudiant", 0));
        repo.users.put(LINA, new User(LINA, "Lina B.", "lina.b@universite.tn", "Étudiante", 0));

        Announcement iphone = new Announcement("a_iphone", AnnouncementType.LOST, AnnouncementStatus.ACTIVE,
                "iPhone 13 noir", Category.PHONES,
                "Coque transparente, écran légèrement fissuré dans le coin supérieur droit. Perdu près des tables de travail au rez-de-chaussée.",
                ColorSwatch.BLACK, loc(0.0004, -0.0006, "Bibliothèque centrale"),
                now - DAY, now - 2 * DAY,
                Arrays.asList(R.drawable.sample_iphone, R.drawable.sample_phone_case), ME);

        Announcement keys = new Announcement("a_keys", AnnouncementType.FOUND, AnnouncementStatus.ACTIVE,
                "Clés avec porte-clés rouge", Category.KEYS,
                "Trois clés sur un anneau métallique.",
                ColorSwatch.RED, loc(-0.0011, 0.0009, "Restaurant universitaire"),
                now, now - 2 * 60 * MINUTE,
                Collections.singletonList(R.drawable.sample_keys), YOUSSEF);

        Announcement backpack = new Announcement("a_backpack", AnnouncementType.LOST, AnnouncementStatus.ACTIVE,
                "Sac à dos bleu", Category.BAGS, "",
                ColorSwatch.BLUE, loc(0.0015, 0.0012, "Amphithéâtre B"),
                now - 3 * DAY, now - 3 * DAY,
                Collections.singletonList(R.drawable.sample_backpack), LINA);

        Announcement phoneFound = new Announcement("a_phone_found", AnnouncementType.FOUND, AnnouncementStatus.ACTIVE,
                "Smartphone noir, coque claire", Category.PHONES,
                "Trouvé sur une table de travail, écran verrouillé.",
                ColorSwatch.BLACK, loc(0.0005, -0.0004, "Bibliothèque centrale"),
                now - 8 * 60 * MINUTE, now - 8 * 60 * MINUTE,
                Arrays.asList(R.drawable.sample_phone_found, R.drawable.sample_iphone), SARA);

        Announcement phoneCase = new Announcement("a_case", AnnouncementType.FOUND, AnnouncementStatus.ACTIVE,
                "Étui de téléphone bleu", Category.PHONES,
                "Étui vide, laissé dans le couloir du premier étage.",
                ColorSwatch.BLUE, loc(-0.0006, -0.0013, "Bâtiment B"),
                now - 7 * DAY, now - 7 * DAY,
                Collections.singletonList(R.drawable.sample_phone_case), YOUSSEF);

        Announcement card = new Announcement("a_card", AnnouncementType.FOUND, AnnouncementStatus.ACTIVE,
                "Carte étudiante", Category.CARDS,
                "Carte au nom d’un étudiant de 2e année, déposée à l’accueil.",
                ColorSwatch.WHITE, loc(0.0001, 0.0002, "Accueil du bâtiment principal"),
                now - 4 * DAY, now - 4 * DAY,
                Collections.singletonList(R.drawable.sample_student_card), LINA);

        Announcement wallet = new Announcement("a_wallet", AnnouncementType.LOST, AnnouncementStatus.RETURNED,
                "Portefeuille marron", Category.WALLETS,
                "Portefeuille en cuir avec une carte de bus.",
                ColorSwatch.BROWN, loc(-0.0003, 0.0007, "Cafétéria"),
                now - 13 * DAY, now - 13 * DAY,
                Collections.singletonList(R.drawable.sample_wallet), ME);

        for (Announcement a : Arrays.asList(iphone, keys, backpack, phoneFound, phoneCase, card, wallet)) {
            repo.announcements.put(a.id, a);
        }

        repo.matches.add(new Match(iphone.id, phoneFound.id, 95));
        repo.matches.add(new Match(iphone.id, phoneCase.id, 70));
        repo.matches.add(new Match(iphone.id, card.id, 18));
        repo.matches.add(new Match(backpack.id, keys.id, 12));

        Conversation withSara = new Conversation("c_sara", ME, SARA, iphone.id, now - 15 * MINUTE);
        withSara.messages.add(new Message("m1", "Bonjour, je pense avoir trouvé votre iPhone près des tables de travail.",
                now - 40 * MINUTE, SARA, Message.State.SENT, true));
        withSara.messages.add(new Message("m2", "Bonjour Sara, merci beaucoup ! Pouvez-vous me décrire la coque ?",
                now - 35 * MINUTE, ME, Message.State.SENT, true));
        withSara.messages.add(new Message("m3", "Une coque transparente avec un petit autocollant bleu sur le dos.",
                now - 20 * MINUTE, SARA, Message.State.SENT, false));
        withSara.messages.add(new Message("m4", "Parfait, c’est bien le mien. Quand puis-je le récupérer ?",
                now - 15 * MINUTE, ME, Message.State.SENDING, true));

        Conversation withYoussef = new Conversation("c_youssef", ME, YOUSSEF, keys.id, now - DAY);
        withYoussef.messages.add(new Message("m5", "Bonjour, ces clés sont peut-être les miennes.",
                now - DAY - 30 * MINUTE, ME, Message.State.SENT, true));
        withYoussef.messages.add(new Message("m6", "Merci, je passe récupérer les clés demain.",
                now - DAY, YOUSSEF, Message.State.SENT, true));

        Conversation withLina = new Conversation("c_lina", ME, LINA, backpack.id, now - 5 * DAY);

        repo.conversations.put(withSara.id, withSara);
        repo.conversations.put(withYoussef.id, withYoussef);
        repo.conversations.put(withLina.id, withLina);
    }

    private static CampusLocation loc(double dLat, double dLon, String precision) {
        return new CampusLocation(CAMPUS_LAT + dLat, CAMPUS_LON + dLon, precision);
    }
}
