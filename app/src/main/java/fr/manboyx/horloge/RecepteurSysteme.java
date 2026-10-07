package fr.manboyx.horloge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import fr.manboyx.horloge.logique.Minuteur;

/**
 * Redémarrage du téléphone, changement d'heure ou de fuseau, mise à jour de
 * l'appli : Android oublie alors les sonneries programmées, ou leur heure
 * change. On reprogramme tout.
 */
public class RecepteurSysteme extends BroadcastReceiver {

    @Override
    public void onReceive(Context contexte, Intent intent) {
        Depot depot = new Depot(contexte);
        Minuteur m = depot.minuteur();
        if (m.termine(System.currentTimeMillis())) {
            // La fin est passée pendant que le téléphone était éteint.
            depot.ecrireMinuteur(m.arreter());
        }
        Planificateur.toutProgrammer(contexte);
    }
}
