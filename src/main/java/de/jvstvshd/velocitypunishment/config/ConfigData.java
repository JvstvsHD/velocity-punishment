package de.jvstvshd.velocitypunishment.config;

import java.util.Locale;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class ConfigData {

    private DataBaseData dataBaseData = new DataBaseData();
    private Locale defaultLanguage = Locale.ENGLISH;
    private boolean forceUsingDefaultLanguage = false;

    public DataBaseData getDataBaseData() {
        return dataBaseData;
    }

    public Locale getDefaultLanguage() {
        return defaultLanguage;
    }

    public boolean isForceUsingDefaultLanguage() {
        return forceUsingDefaultLanguage;
    }
}
