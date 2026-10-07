package fr.manboyx.horloge;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import fr.manboyx.horloge.logique.Alarme;
import fr.manboyx.horloge.logique.Minuteur;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Confie à Android les prochaines sonneries. Rien ne tourne dans l'appli en
 * attendant : le système la relance au moment voulu, via RecepteurAlarme.
 */
final class Planificateur {

    static final String ACTION_ALARME = "fr.manboyx.horloge.ALARME";
    static final String ACTION_REPETITION = "fr.manboyx.horloge.REPETITION";
    static final String ACTION_MINUTEUR = "fr.manboyx.horloge.MINUTEUR";
    static final String EXTRA_ID = "id";

    static final long REPETITION_MS = 10 * 60_000L;

    // Les alarmes utilisent leur identifiant (1 et plus) comme code de demande.
    private static final int CODE_REPETITION = -1;
    private static final int CODE_MINUTEUR = -2;
    private static final int CODE_OUVRIR = -3;

    private Planificateur() {
    }

    /**
     * Reprogramme tout d'après la mémoire : alarmes actives, répétition en
     * attente, fin du minuteur. Sans danger à appeler plusieurs fois.
     */
    static void toutProgrammer(Context contexte) {
        AlarmManager gestionnaire = contexte.getSystemService(AlarmManager.class);
        Depot depot = new Depot(contexte);
        ZonedDateTime maintenant = ZonedDateTime.now();
        long instant = maintenant.toInstant().toEpochMilli();

        for (Alarme a : depot.alarmes()) {
            PendingIntent intention = intention(contexte, ACTION_ALARME, a.id, a.id);
            if (a.active) {
                reveil(contexte, gestionnaire, a.prochaine(maintenant).toInstant().toEpochMilli(), intention);
            } else {
                gestionnaire.cancel(intention);
            }
        }

        PendingIntent repetition = intention(contexte, ACTION_REPETITION, CODE_REPETITION, 0);
        long repetitionA = depot.repetitionA();
        if (repetitionA > instant) {
            reveil(contexte, gestionnaire, repetitionA, repetition);
        } else {
            gestionnaire.cancel(repetition);
            if (repetitionA != 0) {
                depot.effacerRepetition();
            }
        }

        PendingIntent finMinuteur = intention(contexte, ACTION_MINUTEUR, CODE_MINUTEUR, 0);
        Minuteur minuteur = depot.minuteur();
        if (minuteur.etat == Minuteur.Etat.EN_COURS && minuteur.finMs > instant) {
            exact(contexte, gestionnaire, minuteur.finMs, finMinuteur);
        } else {
            gestionnaire.cancel(finMinuteur);
        }

        Widgets.rafraichirReveil(contexte);
    }

    /** À appeler avant de supprimer une alarme de la mémoire. */
    static void annuler(Context contexte, int idAlarme) {
        contexte.getSystemService(AlarmManager.class)
                .cancel(intention(contexte, ACTION_ALARME, idAlarme, idAlarme));
    }

    /** Prochaine sonnerie de réveil (alarme ou répétition), ou null. */
    static ZonedDateTime prochaine(Context contexte) {
        Depot depot = new Depot(contexte);
        ZonedDateTime maintenant = ZonedDateTime.now();
        ZonedDateTime plusTot = null;
        for (Alarme a : depot.alarmes()) {
            if (a.active) {
                ZonedDateTime p = a.prochaine(maintenant);
                if (plusTot == null || p.isBefore(plusTot)) {
                    plusTot = p;
                }
            }
        }
        long repetitionA = depot.repetitionA();
        if (repetitionA > maintenant.toInstant().toEpochMilli()) {
            ZonedDateTime r = Instant.ofEpochMilli(repetitionA).atZone(ZoneId.systemDefault());
            if (plusTot == null || r.isBefore(plusTot)) {
                plusTot = r;
            }
        }
        return plusTot;
    }

    /** Faux seulement sur Android 12 et plus, si l'utilisateur a retiré l'autorisation. */
    static boolean exactesAutorisees(Context contexte) {
        return Build.VERSION.SDK_INT < 31
                || contexte.getSystemService(AlarmManager.class).canScheduleExactAlarms();
    }

    private static PendingIntent intention(Context contexte, String action, int code, int idAlarme) {
        Intent intent = new Intent(contexte, RecepteurAlarme.class)
                .setAction(action)
                .putExtra(EXTRA_ID, idAlarme);
        return PendingIntent.getBroadcast(contexte, code, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static PendingIntent ouvrirAppli(Context contexte) {
        Intent intent = new Intent(contexte, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(contexte, CODE_OUVRIR, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** Réveil : annoncé au système (icône d'alarme, sortie du mode veille). */
    private static void reveil(Context contexte, AlarmManager gestionnaire, long instant,
            PendingIntent intention) {
        try {
            if (exactesAutorisees(contexte)) {
                gestionnaire.setAlarmClock(
                        new AlarmManager.AlarmClockInfo(instant, ouvrirAppli(contexte)), intention);
                return;
            }
        } catch (SecurityException e) {
            // Autorisation retirée entre la vérification et l'appel : repli ci-dessous.
        }
        gestionnaire.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, instant, intention);
    }

    /** Fin du minuteur : exacte, sans icône d'alarme dans la barre d'état. */
    private static void exact(Context contexte, AlarmManager gestionnaire, long instant,
            PendingIntent intention) {
        try {
            if (exactesAutorisees(contexte)) {
                gestionnaire.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, instant, intention);
                return;
            }
        } catch (SecurityException e) {
            // Même repli que pour le réveil.
        }
        gestionnaire.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, instant, intention);
    }
}
