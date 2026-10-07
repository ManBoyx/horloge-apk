package fr.manboyx.horloge.logique;

/**
 * État du minuteur. Immuable. Seuls des instants sont mémorisés : rien n'a
 * besoin de tourner pour que le décompte avance.
 */
public final class Minuteur {

    public enum Etat { ARRET, EN_COURS, PAUSE }

    private static final long DUREE_PAR_DEFAUT_MS = 5 * 60_000L;

    public static final Minuteur ARRET_INITIAL =
            new Minuteur(Etat.ARRET, DUREE_PAR_DEFAUT_MS, 0, 0);

    public final Etat etat;
    /** Durée choisie par l'utilisateur. */
    public final long dureeMs;
    /** Instant de fin (horloge murale), valable en cours de décompte. */
    public final long finMs;
    /** Temps restant, valable en pause. */
    public final long resteMs;

    private Minuteur(Etat etat, long dureeMs, long finMs, long resteMs) {
        this.etat = etat;
        this.dureeMs = dureeMs;
        this.finMs = finMs;
        this.resteMs = resteMs;
    }

    public Minuteur demarrer(long duree, long maintenant) {
        if (duree <= 0) {
            return arreter();
        }
        return new Minuteur(Etat.EN_COURS, duree, maintenant + duree, 0);
    }

    public Minuteur pause(long maintenant) {
        if (etat != Etat.EN_COURS) {
            return this;
        }
        long reste = finMs - maintenant;
        if (reste <= 0) {
            return arreter();
        }
        return new Minuteur(Etat.PAUSE, dureeMs, 0, reste);
    }

    public Minuteur reprendre(long maintenant) {
        if (etat != Etat.PAUSE) {
            return this;
        }
        return new Minuteur(Etat.EN_COURS, dureeMs, maintenant + resteMs, 0);
    }

    public Minuteur arreter() {
        return new Minuteur(Etat.ARRET, dureeMs, 0, 0);
    }

    public long reste(long maintenant) {
        switch (etat) {
            case EN_COURS:
                return Math.max(0, finMs - maintenant);
            case PAUSE:
                return resteMs;
            default:
                return dureeMs;
        }
    }

    public boolean termine(long maintenant) {
        return etat == Etat.EN_COURS && maintenant >= finMs;
    }

    public String versTexte() {
        return etat.name() + ";" + dureeMs + ";" + finMs + ";" + resteMs;
    }

    /** Un texte illisible redonne l'état initial. */
    public static Minuteur depuisTexte(String texte) {
        if (texte == null) {
            return ARRET_INITIAL;
        }
        String[] champs = texte.split(";");
        if (champs.length != 4) {
            return ARRET_INITIAL;
        }
        try {
            Etat etat = Etat.valueOf(champs[0]);
            long duree = Long.parseLong(champs[1]);
            long fin = Long.parseLong(champs[2]);
            long reste = Long.parseLong(champs[3]);
            if (duree <= 0 || fin < 0 || reste < 0) {
                return ARRET_INITIAL;
            }
            return new Minuteur(etat, duree, fin, reste);
        } catch (IllegalArgumentException e) {
            return ARRET_INITIAL;
        }
    }
}
