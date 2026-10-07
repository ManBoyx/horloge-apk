package fr.manboyx.horloge.logique;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;

import org.junit.Test;

public class FormatTest {

    @Test
    public void dureeArrondieALaSecondeSuperieure() {
        assertEquals("00:00", Format.duree(0));
        assertEquals("00:01", Format.duree(1));
        assertEquals("00:01", Format.duree(1_000));
        assertEquals("00:02", Format.duree(1_001));
        assertEquals("05:00", Format.duree(300_000));
        assertEquals("59:59", Format.duree(3_599_000));
        assertEquals("1:00:00", Format.duree(3_600_000));
        assertEquals("12:03:04", Format.duree(12 * 3_600_000L + 3 * 60_000 + 4_000));
        assertEquals("00:00", Format.duree(-50));
    }

    @Test
    public void chronoAuDixieme() {
        assertEquals("00:00,0", Format.chrono(0));
        assertEquals("00:00,0", Format.chrono(99));
        assertEquals("00:00,1", Format.chrono(100));
        assertEquals("02:03,4", Format.chrono(123_456));
        assertEquals("1:00:00,0", Format.chrono(3_600_000));
        assertEquals("00:00,0", Format.chrono(-10));
    }

    @Test
    public void decalage() {
        assertEquals("Même heure", Format.decalage(0));
        assertEquals("+6 h", Format.decalage(360));
        assertEquals("−9 h", Format.decalage(-540));
        assertEquals("+5 h 30", Format.decalage(330));
        assertEquals("−3 h 30", Format.decalage(-210));
        assertEquals("+0 h 45", Format.decalage(45));
        assertEquals("+5 h 05", Format.decalage(305));
    }

    @Test
    public void jours() {
        assertEquals("Une fois", Format.jours(0));
        assertEquals("Tous les jours", Format.jours(127));
        assertEquals("Du lundi au vendredi", Format.jours(31));
        assertEquals("Week-end", Format.jours(96));
        assertEquals("lun.", Format.jours(1));
        assertEquals("lun., mer., dim.", Format.jours(1 | 4 | 64));
    }

    @Test
    public void jourRelatif() {
        LocalDate mercredi = LocalDate.of(2026, 10, 7);
        assertEquals("Aujourd'hui", Format.jourRelatif(mercredi, mercredi));
        assertEquals("Demain", Format.jourRelatif(mercredi.plusDays(1), mercredi));
        assertEquals("ven.", Format.jourRelatif(mercredi.plusDays(2), mercredi));
        assertEquals("mar.", Format.jourRelatif(mercredi.plusDays(6), mercredi));
        assertEquals("Demain", Format.jourRelatif(LocalDate.of(2027, 1, 1), LocalDate.of(2026, 12, 31)));
    }

    @Test
    public void dansCombien() {
        assertEquals("dans moins d'une minute", Format.dansCombien(0));
        assertEquals("dans moins d'une minute", Format.dansCombien(59_000));
        assertEquals("dans 1 min", Format.dansCombien(60_000));
        assertEquals("dans 12 min", Format.dansCombien(12 * 60_000 + 30_000));
        assertEquals("dans 7 h", Format.dansCombien(7 * 3_600_000L));
        assertEquals("dans 7 h 30 min", Format.dansCombien(7 * 3_600_000L + 30 * 60_000));
        assertEquals("dans 2 j 3 h", Format.dansCombien(51 * 3_600_000L + 20 * 60_000));
        assertEquals("dans 6 j", Format.dansCombien(6 * 24 * 3_600_000L + 5 * 60_000));
    }
}
