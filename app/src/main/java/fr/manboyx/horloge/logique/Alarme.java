package fr.manboyx.horloge.logique;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;

/** Une alarme. Immuable : les méthodes avec…() renvoient une copie modifiée. */
public final class Alarme {

    /** Tous les jours cochés. */
    public static final int TOUS_LES_JOURS = 127;

    public final int id;
    public final int heure;
    public final int minute;
    /** Masque de bits : bit 0 = lundi … bit 6 = dimanche. 0 = sonne une seule fois. */
    public final int jours;
    public final String nom;
    public final boolean vibreur;
    public final boolean active;

    public Alarme(int id, int heure, int minute, int jours, String nom, boolean vibreur, boolean active) {
        this.id = id;
        this.heure = heure;
        this.minute = minute;
        this.jours = jours;
        this.nom = nom == null ? "" : nom;
        this.vibreur = vibreur;
        this.active = active;
    }

    public boolean repete() {
        return jours != 0;
    }

    public boolean jourCoche(DayOfWeek jour) {
        return (jours & (1 << (jour.getValue() - 1))) != 0;
    }

    /**
     * Prochaine sonnerie strictement après « maintenant », dans le fuseau de
     * « maintenant ». Une heure qui n'existe pas (passage à l'heure d'été)
     * est décalée à l'heure valide suivante ; une heure qui existe deux fois
     * (passage à l'heure d'hiver) ne compte qu'à son premier passage.
     */
    public ZonedDateTime prochaine(ZonedDateTime maintenant) {
        LocalDate aujourdhui = maintenant.toLocalDate();
        LocalTime heureDuJour = LocalTime.of(heure, minute);
        for (int i = 0; i <= 7; i++) {
            LocalDate jour = aujourdhui.plusDays(i);
            if (repete() && !jourCoche(jour.getDayOfWeek())) {
                continue;
            }
            ZonedDateTime candidate = ZonedDateTime.of(jour, heureDuJour, maintenant.getZone());
            if (candidate.isAfter(maintenant)) {
                return candidate;
            }
        }
        // Inatteignable : en huit jours, tout jour de la semaine revient.
        return ZonedDateTime.of(aujourdhui.plusDays(8), heureDuJour, maintenant.getZone());
    }

    public Alarme avecHeure(int h, int m) {
        return new Alarme(id, h, m, jours, nom, vibreur, active);
    }

    public Alarme avecJours(int j) {
        return new Alarme(id, heure, minute, j, nom, vibreur, active);
    }

    public Alarme avecNom(String n) {
        return new Alarme(id, heure, minute, jours, n, vibreur, active);
    }

    public Alarme avecVibreur(boolean v) {
        return new Alarme(id, heure, minute, jours, nom, v, active);
    }

    public Alarme avecActive(boolean a) {
        return new Alarme(id, heure, minute, jours, nom, vibreur, a);
    }
}
