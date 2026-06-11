package pl.wspa.medivisit.util;

import pl.wspa.medivisit.model.User;

/** Przechowuje aktualnie zalogowanego uzytkownika. */
public final class Session {

    private static User currentUser;

    private Session() {
    }

    public static User getUser() {
        return currentUser;
    }

    public static void setUser(User user) {
        currentUser = user;
    }

    public static void logout() {
        currentUser = null;
    }
}
