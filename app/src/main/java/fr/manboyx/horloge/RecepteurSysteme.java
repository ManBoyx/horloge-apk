package fr.manboyx.horloge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import fr.manboyx.horloge.logique.Minuteur;

import java.util.Arrays;
import java.util.List;

/**
 * Redémarrage du téléphone, changement d'heure ou de fuseau, mise à jour de
 * l'appli : Android oublie alors les sonneries programmées, ou leur heure
 * change. On reprogramme tout.
 */
public class RecepteurSysteme extends BroadcastReceiver {

    private static final List<String> ACTIONS = Arrays.asList(
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED");

    @Override
    public void onReceive(Context contexte, Intent intent) {
        if (!ACTIONS.contains(intent.getAction())) {
            return;
        }
        Depot depot = new Depot(contexte);
        Minuteur m = depot.minuteur();
        if (m.termine(System.currentTimeMillis())) {
            // La fin est passée pendant que le téléphone était éteint.
            depot.ecrireMinuteur(m.arreter());
        }
        Planificateur.toutProgrammer(contexte);
    }
}
