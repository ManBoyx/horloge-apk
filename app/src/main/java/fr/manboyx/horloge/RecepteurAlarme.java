package fr.manboyx.horloge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import fr.manboyx.horloge.logique.Alarme;
import fr.manboyx.horloge.logique.Minuteur;

/** Appelé par Android à l'heure d'une alarme, d'une répétition ou de la fin du minuteur. */
public class RecepteurAlarme extends BroadcastReceiver {

    @Override
    public void onReceive(Context contexte, Intent intent) {
        String action = intent.getAction();
        Depot depot = new Depot(contexte);

        if (Planificateur.ACTION_ALARME.equals(action)) {
            Alarme a = depot.alarme(intent.getIntExtra(Planificateur.EXTRA_ID, 0));
            if (a != null && a.active) {
                if (!a.repete()) {
                    // Une alarme sans jour coché ne sonne qu'une fois.
                    depot.enregistrer(a.avecActive(false));
                }
                ServiceSonnerie.sonner(contexte, a.id, titre(a), a.vibreur, true);
            }
        } else if (Planificateur.ACTION_REPETITION.equals(action)) {
            int id = depot.repetitionId();
            depot.effacerRepetition();
            Alarme a = depot.alarme(id);
            ServiceSonnerie.sonner(contexte, id, a == null ? "Réveil" : titre(a),
                    a == null || a.vibreur, true);
        } else if (Planificateur.ACTION_MINUTEUR.equals(action)) {
            Minuteur m = depot.minuteur();
            if (m.etat == Minuteur.Etat.EN_COURS) {
                depot.ecrireMinuteur(m.arreter());
                ServiceSonnerie.sonner(contexte, 0, "Minuteur terminé", true, false);
            }
        }

        // Programme la sonnerie suivante et met à jour le widget.
        Planificateur.toutProgrammer(contexte);
    }

    private static String titre(Alarme a) {
        return a.nom.trim().isEmpty() ? "Réveil" : a.nom.trim();
    }
}
