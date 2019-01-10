package com.moxtra.moxiechat.model;

public class MoxieUser {

    public final String uniqueId;
    public final String firstName;
    public final String lastName;
    public final String avatarPath;

    public MoxieUser(String uniqueId, String firstName, String lastName, String avatarPath) {
        this.uniqueId = uniqueId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.avatarPath = avatarPath;
    }

}
