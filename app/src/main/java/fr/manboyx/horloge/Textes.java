package fr.manboyx.horloge;

import android.content.Context;
import android.text.format.DateFormat;

import java.util.Calendar;

/** Textes qui dépendent des réglages du téléphone (format 12 ou 24 heures). */
final class Textes {

    private Textes() {
    }

    static String heure(Context contexte, int heure, int minute) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, heure);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        return DateFormat.getTimeFormat(contexte).format(c.getTime());
    }
}
