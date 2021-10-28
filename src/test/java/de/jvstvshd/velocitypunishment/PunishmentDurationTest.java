package de.jvstvshd.velocitypunishment;

import de.jvstvshd.velocitypunishment.punishment.PunishmentDuration;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class PunishmentDurationTest {

    private final String string = "12d";

    @Test
    public void parse() {
        PunishmentDuration punishmentDuration = PunishmentDuration.permanent();
        System.out.println(punishmentDuration.expiration());
        System.out.println(Timestamp.from(punishmentDuration.expiration().toInstant(OffsetDateTime.now().getOffset())));
        System.out.println(punishmentDuration.timestampExpiration());
        System.out.println("MAx: " + Timestamp.from(Instant.MAX));
        DateTimeFormatter format = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        System.out.println(punishmentDuration.expiration().format(format));
    }
}
