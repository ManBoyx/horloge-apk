package fr.manboyx.horloge;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

import fr.manboyx.horloge.logique.Alarme;
import fr.manboyx.horloge.logique.Chrono;
import fr.manboyx.horloge.logique.ListeAlarmes;
import fr.manboyx.horloge.logique.Minuteur;
import fr.manboyx.horloge.logique.Villes;

import java.util.ArrayList;
import java.util.List;

/** Seule classe qui lit ou écrit la mémoire de l'appli (préférences). */
final class Depot {

    private static final String ALARMES = "alarmes";
    private static final String MINUTEUR = "minuteur";
    private static final String CHRONO = "chrono";
    private static final String CHRONO_DEMARRAGE = "chrono_demarrage";
    private static final String VILLES = "villes";
    private static final String SECONDES = "secondes";
    private static final String ONGLET = "onglet";
    private static final String REPETITION_ID = "repetition_id";
    private static final String REPETITION_A = "repetition_a";

    private final Context contexte;
    private final SharedPreferences prefs;

    Depot(Context contexte) {
        this.contexte = contexte.getApplicationContext();
        // Stockage « protégé par l'appareil » : lisible dès le démarrage du
        // téléphone, avant le premier déverrouillage, pour que les alarmes
        // sonnent même après un redémarrage nocturne. Il ne contient que des
        // heures d'alarme et des réglages d'affichage.
        this.prefs = this.contexte.createDeviceProtectedStorageContext()
                .getSharedPreferences("horloge", Context.MODE_PRIVATE);
    }

    // Une valeur du mauvais type (mémoire abîmée) est traitée comme absente.

    private String texte(String cle) {
        try {
            return prefs.getString(cle, null);
        } catch (ClassCastException e) {
            return null;
        }
    }

    private int entier(String cle, int defaut) {
        try {
            return prefs.getInt(cle, defaut);
        } catch (ClassCastException e) {
            return defaut;
        }
    }

    private long entierLong(String cle) {
        try {
            return prefs.getLong(cle, 0);
        } catch (ClassCastException e) {
            return 0;
        }
    }

    // ---- Alarmes ----

    List<Alarme> alarmes() {
        return ListeAlarmes.lire(texte(ALARMES));
    }

    Alarme alarme(int id) {
        for (Alarme a : alarmes()) {
            if (a.id == id) {
                return a;
            }
        }
        return null;
    }

    private void ecrireAlarmes(List<Alarme> alarmes) {
        prefs.edit().putString(ALARMES, ListeAlarmes.ecrire(alarmes)).apply();
    }

    /** Ajoute l'alarme, ou remplace celle qui porte le même identifiant. */
    void enregistrer(Alarme alarme) {
        List<Alarme> alarmes = alarmes();
        for (int i = 0; i < alarmes.size(); i++) {
            if (alarmes.get(i).id == alarme.id) {
                alarmes.set(i, alarme);
                ecrireAlarmes(alarmes);
                return;
            }
        }
        alarmes.add(alarme);
        ecrireAlarmes(alarmes);
    }

    void supprimer(int id) {
        List<Alarme> alarmes = alarmes();
        for (int i = 0; i < alarmes.size(); i++) {
            if (alarmes.get(i).id == id) {
                alarmes.remove(i);
                break;
            }
        }
        ecrireAlarmes(alarmes);
    }

    // ---- Répétition (« Répéter dans 10 min ») ----

    int repetitionId() {
        return entier(REPETITION_ID, 0);
    }

    /** Instant de la répétition en attente, ou 0. */
    long repetitionA() {
        return entierLong(REPETITION_A);
    }

    void ecrireRepetition(int idAlarme, long instant) {
        prefs.edit().putInt(REPETITION_ID, idAlarme).putLong(REPETITION_A, instant).apply();
    }

    void effacerRepetition() {
        prefs.edit().remove(REPETITION_ID).remove(REPETITION_A).apply();
    }

    // ---- Minuteur ----

    Minuteur minuteur() {
        return Minuteur.depuisTexte(texte(MINUTEUR));
    }

    void ecrireMinuteur(Minuteur minuteur) {
        prefs.edit().putString(MINUTEUR, minuteur.versTexte()).apply();
    }

    // ---- Chrono ----

    /**
     * Le chrono se repère sur le temps écoulé depuis le démarrage du
     * téléphone : s'il a redémarré depuis, l'état mémorisé ne vaut plus rien.
     */
    Chrono chrono(long maintenant) {
        if (entier(CHRONO_DEMARRAGE, -1) != compteurDemarrages()) {
            return Chrono.VIDE;
        }
        return Chrono.depuisTexte(texte(CHRONO), maintenant);
    }

    void ecrireChrono(Chrono chrono) {
        prefs.edit()
                .putString(CHRONO, chrono.versTexte())
                .putInt(CHRONO_DEMARRAGE, compteurDemarrages())
                .apply();
    }

    private int compteurDemarrages() {
        return Settings.Global.getInt(contexte.getContentResolver(), Settings.Global.BOOT_COUNT, 0);
    }

    // ---- Heures du monde ----

    List<Villes.Ville> villes() {
        List<Villes.Ville> villes = new ArrayList<>();
        String brut = texte(VILLES);
        if (brut == null || brut.isEmpty()) {
            return villes;
        }
        for (String cle : brut.split("\n")) {
            Villes.Ville v = Villes.parCle(cle);
            if (v != null && !villes.contains(v)) {
                villes.add(v);
            }
        }
        return villes;
    }

    void ecrireVilles(List<Villes.Ville> villes) {
        StringBuilder sortie = new StringBuilder();
        for (Villes.Ville v : villes) {
            if (sortie.length() > 0) {
                sortie.append('\n');
            }
            sortie.append(v.cle());
        }
        prefs.edit().putString(VILLES, sortie.toString()).apply();
    }

    // ---- Affichage ----

    boolean secondes() {
        try {
            return prefs.getBoolean(SECONDES, false);
        } catch (ClassCastException e) {
            return false;
        }
    }

    void ecrireSecondes(boolean affichees) {
        prefs.edit().putBoolean(SECONDES, affichees).apply();
    }

    int onglet() {
        int onglet = entier(ONGLET, 0);
        return onglet < 0 || onglet > 3 ? 0 : onglet;
    }

    void ecrireOnglet(int onglet) {
        prefs.edit().putInt(ONGLET, onglet).apply();
    }
}
