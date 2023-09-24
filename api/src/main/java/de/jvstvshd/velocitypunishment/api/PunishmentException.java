package de.jvstvshd.velocitypunishment.api;

public class PunishmentException extends Exception {

    public PunishmentException() {
        super();
    }

    public PunishmentException(String message) {
        super(message);
    }

    public PunishmentException(String message, Throwable cause) {
        super(message, cause);
    }

    public PunishmentException(Throwable cause) {
        super(cause);
    }

    public PunishmentException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
