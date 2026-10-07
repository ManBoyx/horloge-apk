package fr.manboyx.horloge;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Un onglet. L'écran n'est rafraîchi qu'entre afficher() et masquer() :
 * hors de cette fenêtre, il ne consomme rien.
 */
abstract class Ecran {

    final MainActivity activite;
    final Depot depot;
    final View vue;

    Ecran(MainActivity activite, ViewGroup parent, int miseEnPage) {
        this.activite = activite;
        this.depot = activite.depot();
        this.vue = LayoutInflater.from(activite).inflate(miseEnPage, parent, false);
    }

    /** L'onglet devient visible. */
    void afficher() {
    }

    /** L'onglet n'est plus visible (autre onglet, appli en arrière-plan). */
    void masquer() {
    }
}
