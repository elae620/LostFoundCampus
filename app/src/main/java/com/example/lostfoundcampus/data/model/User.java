package com.example.lostfoundcampus.data.model;

import androidx.annotation.DrawableRes;

public class User {

    public final String uid;
    public String fullName;
    public final String universityEmail;
    /** Rôle affiché sous le nom (ex. « Étudiante »). */
    public final String role;
    /** Étape 1 : drawable d'exemple (0 = initiales). Étape 2 : photoUrl. */
    @DrawableRes
    public int photoRes;

    public User(String uid, String fullName, String universityEmail, String role, @DrawableRes int photoRes) {
        this.uid = uid;
        this.fullName = fullName;
        this.universityEmail = universityEmail;
        this.role = role;
        this.photoRes = photoRes;
    }

    public String firstName() {
        int space = fullName.indexOf(' ');
        return space > 0 ? fullName.substring(0, space) : fullName;
    }
}
