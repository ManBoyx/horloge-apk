package fr.manboyx.horloge;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import fr.manboyx.horloge.logique.Format;
import fr.manboyx.horloge.logique.Minuteur;

/**
 * Onglet Minuteur. La fin est confiée à Android (Planificateur) : l'écran ne
 * fait qu'afficher le temps restant, une fois par seconde et seulement quand
 * il est visible.
 */
final class EcranMinuteur extends Ecran {

    private final Handler principal = new Handler(Looper.getMainLooper());
    private final View reglage;
    private final View decompte;
    private final TextView reste;
    private final Button pause;
    private final NumberPicker heures;
    private final NumberPicker minutes;
    private final NumberPicker secondes;
    private final Runnable battement = this::rafraichir;

    EcranMinuteur(MainActivity activite, ViewGroup parent) {
        super(activite, parent, R.layout.ecran_minuteur);
        reglage = vue.findViewById(R.id.reglage);
        decompte = vue.findViewById(R.id.decompte);
        reste = vue.findViewById(R.id.reste);
        pause = vue.findViewById(R.id.pause);
        heures = vue.findViewById(R.id.choix_heures);
        minutes = vue.findViewById(R.id.choix_minutes);
        secondes = vue.findViewById(R.id.choix_secondes);

        heures.setMaxValue(23);
        minutes.setMaxValue(59);
        secondes.setMaxValue(59);
        long duree = depot.minuteur().dureeMs / 1000;
        heures.setValue((int) Math.min(23, duree / 3600));
        minutes.setValue((int) ((duree % 3600) / 60));
        secondes.setValue((int) (duree % 60));

        vue.findViewById(R.id.demarrer).setOnClickListener(v -> demarrer());
        pause.setOnClickListener(v -> {
            Minuteur m = depot.minuteur();
            long maintenant = System.currentTimeMillis();
            enregistrer(m.etat == Minuteur.Etat.PAUSE ? m.reprendre(maintenant) : m.pause(maintenant));
        });
        vue.findViewById(R.id.annuler).setOnClickListener(v -> enregistrer(depot.minuteur().arreter()));
    }

    private void demarrer() {
        long duree = heures.getValue() * 3_600_000L + minutes.getValue() * 60_000L
                + secondes.getValue() * 1_000L;
        if (duree <= 0) {
            Toast.makeText(activite, "Choisissez une durée.", Toast.LENGTH_SHORT).show();
            return;
        }
        enregistrer(depot.minuteur().demarrer(duree, System.currentTimeMillis()));
        activite.demanderNotifications();
    }

    private void enregistrer(Minuteur minuteur) {
        depot.ecrireMinuteur(minuteur);
        Planificateur.toutProgrammer(activite);
        rafraichir();
    }

    @Override
    void afficher() {
        rafraichir();
    }

    @Override
    void masquer() {
        principal.removeCallbacks(battement);
    }

    private void rafraichir() {
        principal.removeCallbacks(battement);
        // Relu à chaque fois : à la fin, c'est RecepteurAlarme qui arrête le minuteur.
        Minuteur m = depot.minuteur();
        long maintenant = System.currentTimeMillis();
        boolean actif = m.etat != Minuteur.Etat.ARRET;

        reglage.setVisibility(actif ? View.GONE : View.VISIBLE);
        decompte.setVisibility(actif ? View.VISIBLE : View.GONE);
        if (!actif) {
            return;
        }

        long restant = m.reste(maintenant);
        reste.setText(Format.duree(restant));
        pause.setText(m.etat == Minuteur.Etat.PAUSE ? "Reprendre" : "Pause");
        if (m.etat == Minuteur.Etat.EN_COURS) {
            // Prochain passage juste après le changement de seconde affichée.
            long attente = restant <= 0 ? 500 : (restant - 1) % 1000 + 30;
            principal.postDelayed(battement, attente);
        }
    }
}
