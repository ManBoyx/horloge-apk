package fr.manboyx.horloge;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.widget.RemoteViews;

import fr.manboyx.horloge.logique.Format;

import java.time.ZonedDateTime;

/**
 * Les quatre widgets. Ils n'utilisent que TextClock et AnalogClock : c'est
 * l'écran d'accueil qui fait avancer l'heure, sans jamais réveiller l'appli.
 * Seul le texte du prochain réveil est écrit par l'appli, quand une alarme
 * change.
 */
public final class Widgets {

    private Widgets() {
    }

    /** Met à jour le texte du widget « prochain réveil », s'il est posé. */
    static void rafraichirReveil(Context contexte) {
        try {
            AppWidgetManager gestionnaire = AppWidgetManager.getInstance(contexte);
            if (gestionnaire == null) {
                return;
            }
            int[] ids = gestionnaire.getAppWidgetIds(new ComponentName(contexte, AvecReveil.class));
            if (ids.length > 0) {
                dessiner(contexte, gestionnaire, ids, R.layout.widget_reveil);
            }
        } catch (RuntimeException e) {
            // Service des widgets indisponible (téléphone pas encore déverrouillé
            // après un démarrage) : le widget sera redessiné au déverrouillage.
        }
    }

    private static void dessiner(Context contexte, AppWidgetManager gestionnaire, int[] ids,
            int miseEnPage) {
        RemoteViews vues = new RemoteViews(contexte.getPackageName(), miseEnPage);
        vues.setOnClickPendingIntent(R.id.widget_racine, Planificateur.ouvrirAppli(contexte));
        if (miseEnPage == R.layout.widget_reveil) {
            vues.setTextViewText(R.id.widget_prochain, prochainReveil(contexte));
        }
        gestionnaire.updateAppWidget(ids, vues);
    }

    /**
     * Jour et heure absolus (« mer. 07:30 ») plutôt que « demain » : le texte
     * reste juste après minuit sans qu'il faille le redessiner.
     */
    private static String prochainReveil(Context contexte) {
        ZonedDateTime prochaine = Planificateur.prochaine(contexte);
        if (prochaine == null) {
            return contexte.getString(R.string.aucun_reveil);
        }
        return "Réveil " + Format.jourCourt(prochaine.getDayOfWeek()) + " "
                + Textes.heure(contexte, prochaine.getHour(), prochaine.getMinute());
    }

    private abstract static class Base extends AppWidgetProvider {

        abstract int miseEnPage();

        @Override
        public void onUpdate(Context contexte, AppWidgetManager gestionnaire, int[] ids) {
            dessiner(contexte, gestionnaire, ids, miseEnPage());
        }
    }

    public static final class Numerique extends Base {
        @Override
        int miseEnPage() {
            return R.layout.widget_numerique;
        }
    }

    public static final class AvecDate extends Base {
        @Override
        int miseEnPage() {
            return R.layout.widget_date;
        }
    }

    public static final class Analogique extends Base {
        @Override
        int miseEnPage() {
            return R.layout.widget_analogique;
        }
    }

    public static final class AvecReveil extends Base {
        @Override
        int miseEnPage() {
            return R.layout.widget_reveil;
        }
    }
}
