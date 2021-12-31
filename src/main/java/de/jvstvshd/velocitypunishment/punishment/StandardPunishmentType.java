package de.jvstvshd.velocitypunishment.punishment;

public enum StandardPunishmentType implements PunishmentType {

    BAN(false, "BAN"),
    PERMANENT_BAN(true, "PERMANENT_BAN"),
    MUTE(false, "MUTE"),
    PERMANENT_MUTE(true, "PERMANENT_MUTE"),
    KICK(false, "KICK");

    private final boolean isPermanent;
    private final String typeString;

    StandardPunishmentType(boolean isPermanent, String typeString) {
        this.isPermanent = isPermanent;
        this.typeString = typeString;
    }

    public boolean isPermanent() {
        return isPermanent;
    }

    @Override
    public String getName() {
        return typeString;
    }
}
