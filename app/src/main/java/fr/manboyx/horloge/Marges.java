package fr.manboyx.horloge;

import android.app.Activity;
import android.graphics.Insets;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;

/** Place réservée aux barres système, à l'encoche et au clavier. */
final class Marges {

    private Marges() {
    }

    /**
     * À partir d'Android 11, l'écran s'affiche bord à bord (imposé à partir
     * d'Android 15) et la vue racine reçoit les marges en remplissage. Avant,
     * le système s'en charge.
     */
    static void gerer(Activity activite, View racine) {
        if (Build.VERSION.SDK_INT < 30) {
            return;
        }
        final int gauche = racine.getPaddingLeft();
        final int haut = racine.getPaddingTop();
        final int droite = racine.getPaddingRight();
        final int bas = racine.getPaddingBottom();
        activite.getWindow().setDecorFitsSystemWindows(false);
        racine.setOnApplyWindowInsetsListener((v, marges) -> {
            Insets m = marges.getInsets(WindowInsets.Type.systemBars()
                    | WindowInsets.Type.displayCutout()
                    | WindowInsets.Type.ime());
            v.setPadding(gauche + m.left, haut + m.top, droite + m.right, bas + m.bottom);
            return WindowInsets.CONSUMED;
        });
    }
}
