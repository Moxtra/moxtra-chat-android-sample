package com.moxtra.moxiechat.model;

import java.util.ArrayList;
import java.util.List;

public class DummyData {

    public static final List<MoxieUser> USERS = new ArrayList<>();
    public static final List<String> UNIQUE_IDS = new ArrayList<>();

    static {
        USERS.add(new MoxieUser("amy@example.com", "Amy", "Tate", "FA01.png"));
        USERS.add(new MoxieUser("bob@example.com", "Bob", "Turner", "A01.png"));
        USERS.add(new MoxieUser("ted@example.com", "Ted", "Packman", "A02.png"));
        USERS.add(new MoxieUser("cavin@example.com", "Cavin", "Page", "A03.png"));
        USERS.add(new MoxieUser("emily@example.com", "Emily", "Pedsy", "FB01.png"));
        USERS.add(new MoxieUser("cindy@example.com", "Cindy", "Pettis", "FB02.png"));
        for (MoxieUser moxieUser : USERS) {
            UNIQUE_IDS.add(moxieUser.uniqueId);
        }
    }

    public static MoxieUser findByUniqueId(String uniqueId) {
        for (MoxieUser moxieUser : USERS) {
            if (moxieUser.uniqueId.equals(uniqueId)) {
                return moxieUser;
            }
        }
        return null;
    }

    public static List<MoxieUser> getUserListForSelect(MoxieUser moxieUser) {
        List<MoxieUser> moxieUserList = new ArrayList<>();
        for (MoxieUser u : USERS) {
            if (u.uniqueId.equals(moxieUser.uniqueId)) {
                continue;
            }
            moxieUserList.add(u);
        }
        return moxieUserList;
    }

}
