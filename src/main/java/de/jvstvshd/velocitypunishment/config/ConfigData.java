package de.jvstvshd.velocitypunishment.config;

import java.util.Locale;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class ConfigData {

    private DataBaseData dataBaseData = new DataBaseData();
    private Locale forcedLanguage = null;

    public DataBaseData getDataBaseData() {
        return dataBaseData;
    }

    public Locale getForcedLanguage() {
        return forcedLanguage;
    }
}
