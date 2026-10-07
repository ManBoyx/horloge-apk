package fr.manboyx.horloge.logique;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Mise en forme des durées, des décalages horaires et des jours. */
public final class Format {

    private static final String[] JOURS = {"lun.", "mar.", "mer.", "jeu.", "ven.", "sam.", "dim."};
    private static final int SEMAINE = 31;
    private static final int WEEK_END = 96;

    private Format() {
    }

    private static String deux(long n) {
        return n < 10 ? "0" + n : String.valueOf(n);
    }

    private static String hms(long secondes) {
        long h = secondes / 3600;
        long m = (secondes % 3600) / 60;
        long s = secondes % 60;
        return (h > 0 ? h + ":" : "") + deux(m) + ":" + deux(s);
    }

    /** Décompte : arrondi à la seconde supérieure, pour n'afficher 00:00 qu'à la fin. */
    public static String duree(long ms) {
        return hms(ms <= 0 ? 0 : (ms + 999) / 1000);
    }

    /** Chronomètre, au dixième de seconde. */
    public static String chrono(long ms) {
        long positif = Math.max(0, ms);
        return hms(positif / 1000) + "," + (positif % 1000) / 100;
    }

    /** Décalage d'une ville par rapport à l'heure locale. */
    public static String decalage(int minutes) {
        if (minutes == 0) {
            return "Même heure";
        }
        int absolu = Math.abs(minutes);
        String texte = (minutes > 0 ? "+" : "−") + absolu / 60 + " h";
        return absolu % 60 == 0 ? texte : texte + " " + deux(absolu % 60);
    }

    /** Jours de répétition d'une alarme (bit 0 = lundi). */
    public static String jours(int masque) {
        if (masque == 0) {
            return "Une fois";
        }
        if (masque == Alarme.TOUS_LES_JOURS) {
            return "Tous les jours";
        }
        if (masque == SEMAINE) {
            return "Du lundi au vendredi";
        }
        if (masque == WEEK_END) {
            return "Week-end";
        }
        StringBuilder sortie = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            if ((masque & (1 << i)) != 0) {
                if (sortie.length() > 0) {
                    sortie.append(", ");
                }
                sortie.append(JOURS[i]);
            }
        }
        return sortie.toString();
    }

    /** Jour abrégé : « lun. », « mar. »… */
    public static String jourCourt(java.time.DayOfWeek jour) {
        return JOURS[jour.getValue() - 1];
    }

    /** « Aujourd'hui », « Demain », sinon le jour abrégé. */
    public static String jourRelatif(LocalDate cible, LocalDate aujourdhui) {
        long ecart = ChronoUnit.DAYS.between(aujourdhui, cible);
        if (ecart == 0) {
            return "Aujourd'hui";
        }
        if (ecart == 1) {
            return "Demain";
        }
        return jourCourt(cible.getDayOfWeek());
    }

    /** Délai avant une sonnerie, pour le message de confirmation. */
    public static String dansCombien(long ms) {
        long minutes = Math.max(0, ms) / 60_000;
        if (minutes < 1) {
            return "dans moins d'une minute";
        }
        long jours = minutes / (24 * 60);
        long heures = (minutes / 60) % 24;
        long reste = minutes % 60;
        StringBuilder sortie = new StringBuilder("dans");
        if (jours > 0) {
            sortie.append(' ').append(jours).append(" j");
        }
        if (heures > 0) {
            sortie.append(' ').append(heures).append(" h");
        }
        if (jours == 0 && reste > 0) {
            sortie.append(' ').append(reste).append(" min");
        }
        return sortie.toString();
    }
}
