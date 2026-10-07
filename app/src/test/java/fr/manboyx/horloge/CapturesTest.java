package fr.manboyx.horloge;

import static org.junit.Assume.assumeFalse;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlertDialog;
import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.app.Activity;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.RemoteViews;

import fr.manboyx.horloge.logique.Alarme;
import fr.manboyx.horloge.logique.Villes;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowAlarmManager;
import org.robolectric.shadows.ShadowAlertDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Écrit des images des écrans et des widgets, pour contrôler la mise en page
 * sans téléphone. Ne fait rien sans la propriété « captures.dir ».
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "fr-rFR-w360dp-h740dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class CapturesTest {

    private Application appli;
    private Depot depot;
    private String dossier;

    @Before
    public void preparer() {
        dossier = System.getProperty("captures.dir", "");
        assumeFalse(dossier.isEmpty());
        appli = RuntimeEnvironment.getApplication();
        depot = new Depot(appli);
        ShadowAlarmManager.setCanScheduleExactAlarms(true);
        ServiceSonnerie.titreEnCours = null;
        // Un téléphone français en format 24 heures, réglé sur l'heure réelle :
        // l'horloge analogique lit la vraie horloge de la machine, pas celle
        // du simulateur, et les deux doivent concorder sur les images.
        Settings.System.putString(appli.getContentResolver(), Settings.System.TIME_12_24, "24");
        SystemClock.setCurrentTimeMillis(Clock.systemUTC().millis());

        List<Villes.Ville> villes = new ArrayList<>();
        villes.addAll(Villes.chercher("tokyo"));
        villes.addAll(Villes.chercher("new york"));
        villes.addAll(Villes.chercher("calcutta"));
        depot.ecrireVilles(villes);
        depot.enregistrer(new Alarme(1, 7, 30, 31, "Travail", true, true));
        depot.enregistrer(new Alarme(2, 9, 15, 96, "", true, false));
        depot.enregistrer(new Alarme(3, 22, 45, 0, "Médicament", false, true));
    }

    private void capturer(View vue, String nom) throws IOException {
        shadowOf(Looper.getMainLooper()).idle();
        Bitmap image = Bitmap.createBitmap(vue.getWidth(), vue.getHeight(), Bitmap.Config.ARGB_8888);
        vue.draw(new Canvas(image));
        File fichier = new File(dossier, nom + ".png");
        try (FileOutputStream sortie = new FileOutputStream(fichier)) {
            image.compress(Bitmap.CompressFormat.PNG, 100, sortie);
        }
    }

    private void lesQuatreOnglets(String suffixe) throws IOException {
        MainActivity a = Robolectric.buildActivity(MainActivity.class).setup().get();
        View ecran = a.getWindow().getDecorView();
        a.findViewById(R.id.onglet_horloge).performClick();
        capturer(ecran, "1-horloge" + suffixe);

        a.findViewById(R.id.onglet_reveil).performClick();
        capturer(ecran, "2-reveil" + suffixe);
        ListView liste = a.findViewById(R.id.alarmes);
        liste.performItemClick(liste.getChildAt(0), 0, 1);
        shadowOf(Looper.getMainLooper()).idle();
        AlertDialog dialogue = ShadowAlertDialog.getLatestAlertDialog();
        capturer(dialogue.getWindow().getDecorView(), "3-modifier" + suffixe);
        dialogue.dismiss();

        a.findViewById(R.id.onglet_minuteur).performClick();
        capturer(ecran, "4-minuteur" + suffixe);
        ((NumberPicker) a.findViewById(R.id.choix_minutes)).setValue(12);
        ((NumberPicker) a.findViewById(R.id.choix_secondes)).setValue(30);
        a.findViewById(R.id.demarrer).performClick();
        capturer(ecran, "5-decompte" + suffixe);
        a.findViewById(R.id.annuler).performClick();

        a.findViewById(R.id.onglet_chrono).performClick();
        a.findViewById(R.id.droite).performClick();
        for (int i = 0; i < 3; i++) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(21_300 + i * 4_000));
            a.findViewById(R.id.gauche).performClick();
        }
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(7_400));
        capturer(ecran, "6-chrono" + suffixe);
    }

    @Test
    public void ecransClairs() throws IOException {
        lesQuatreOnglets("-clair");
    }

    @Test
    @Config(qualifiers = "+night")
    public void ecransSombres() throws IOException {
        lesQuatreOnglets("-sombre");
    }

    @Test
    @Config(qualifiers = "fr-rFR-w320dp-h568dp-xhdpi")
    public void petitEcran() throws IOException {
        lesQuatreOnglets("-petit");
    }

    @Test
    @Config(qualifiers = "+night")
    public void sonnerie() throws IOException {
        ServiceSonnerie.titreEnCours = "Travail";
        ServiceSonnerie.reveilEnCours = true;
        SonnerieActivity ecran = Robolectric.buildActivity(SonnerieActivity.class).setup().get();
        capturer(ecran.getWindow().getDecorView(), "7-sonnerie");
    }

    @Test
    public void widgets() throws IOException {
        Planificateur.toutProgrammer(appli);
        float d = appli.getResources().getDisplayMetrics().density;
        int[][] tailles = {{150, 70}, {220, 80}, {150, 150}, {220, 150}};
        int[] misesEnPage = {R.layout.widget_numerique, R.layout.widget_date,
            R.layout.widget_analogique, R.layout.widget_reveil};
        String[] noms = {"numerique", "date", "analogique", "reveil"};
        for (int i = 0; i < misesEnPage.length; i++) {
            // Une vraie fenêtre, comme un écran d'accueil : c'est en y étant
            // rattachées que les horloges système se mettent à l'heure.
            Activity hote = Robolectric.buildActivity(Activity.class).setup().get();
            FrameLayout accueil = new FrameLayout(hote);
            // Fond imitant un fond d'écran, pour juger la lisibilité.
            accueil.setBackgroundColor(Color.rgb(74, 110, 150));
            int marge = Math.round(12 * d);
            accueil.setPadding(marge, marge, marge, marge);
            RemoteViews vues = new RemoteViews(appli.getPackageName(), misesEnPage[i]);
            if (misesEnPage[i] == R.layout.widget_reveil) {
                vues.setTextViewText(R.id.widget_prochain, "Réveil jeu. 07:30");
            }
            accueil.addView(vues.apply(appli, accueil));
            int largeur = Math.round(tailles[i][0] * d) + 2 * marge;
            int hauteur = Math.round(tailles[i][1] * d) + 2 * marge;
            hote.setContentView(accueil, new FrameLayout.LayoutParams(largeur, hauteur));
            shadowOf(Looper.getMainLooper()).idle();
            capturer(accueil, "8-widget-" + noms[i]);
        }
    }
}
