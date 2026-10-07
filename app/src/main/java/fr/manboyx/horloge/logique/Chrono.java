package fr.manboyx.horloge.logique;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * État du chronomètre. Immuable. Les instants viennent d'une horloge qui ne
 * recule jamais tant que le téléphone reste allumé (temps depuis le
 * démarrage) ; après un redémarrage du téléphone, un chrono en cours est
 * remis à zéro.
 */
public final class Chrono {

    public static final int TOURS_MAX = 99;

    public static final Chrono VIDE = new Chrono(false, 0, 0, Collections.<Long>emptyList());

    public final boolean enCours;
    /** Temps écoulé à chaque tour, depuis le départ. */
    public final List<Long> tours;

    private final long debut;
    private final long cumul;

    private Chrono(boolean enCours, long debut, long cumul, List<Long> tours) {
        this.enCours = enCours;
        this.debut = debut;
        this.cumul = cumul;
        this.tours = Collections.unmodifiableList(tours);
    }

    public long ecoule(long maintenant) {
        return cumul + (enCours ? Math.max(0, maintenant - debut) : 0);
    }

    public Chrono demarrer(long maintenant) {
        if (enCours) {
            return this;
        }
        return new Chrono(true, maintenant, cumul, tours);
    }

    public Chrono pause(long maintenant) {
        if (!enCours) {
            return this;
        }
        return new Chrono(false, 0, ecoule(maintenant), tours);
    }

    public Chrono tour(long maintenant) {
        if (!enCours || tours.size() >= TOURS_MAX) {
            return this;
        }
        List<Long> nouveaux = new ArrayList<>(tours);
        nouveaux.add(ecoule(maintenant));
        return new Chrono(true, debut, cumul, nouveaux);
    }

    public String versTexte() {
        StringBuilder sortie = new StringBuilder();
        sortie.append(enCours ? 1 : 0).append(';').append(debut).append(';').append(cumul).append(';');
        for (int i = 0; i < tours.size(); i++) {
            if (i > 0) {
                sortie.append(',');
            }
            sortie.append(tours.get(i));
        }
        return sortie.toString();
    }

    /** Un texte illisible, ou un départ situé dans le futur, redonne un chrono vide. */
    public static Chrono depuisTexte(String texte, long maintenant) {
        if (texte == null) {
            return VIDE;
        }
        String[] champs = texte.split(";", 4);
        if (champs.length != 4 || !(champs[0].equals("0") || champs[0].equals("1"))) {
            return VIDE;
        }
        try {
            boolean enCours = champs[0].equals("1");
            long debut = Long.parseLong(champs[1]);
            long cumul = Long.parseLong(champs[2]);
            if (debut < 0 || cumul < 0 || (enCours && maintenant < debut)) {
                return VIDE;
            }
            List<Long> tours = new ArrayList<>();
            if (!champs[3].isEmpty()) {
                for (String t : champs[3].split(",")) {
                    long valeur = Long.parseLong(t);
                    if (valeur < 0 || tours.size() >= TOURS_MAX) {
                        return VIDE;
                    }
                    tours.add(valeur);
                }
            }
            return new Chrono(enCours, debut, cumul, tours);
        } catch (NumberFormatException e) {
            return VIDE;
        }
    }
}
