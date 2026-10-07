package fr.manboyx.horloge;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

/** Écran principal : quatre onglets, dont un seul existe à l'écran à la fois. */
public class MainActivity extends Activity {

    private static final int DEMANDE_NOTIFICATIONS = 7;
    private static final int[] ONGLETS = {
        R.id.onglet_horloge, R.id.onglet_reveil, R.id.onglet_minuteur, R.id.onglet_chrono
    };

    private final Ecran[] ecrans = new Ecran[ONGLETS.length];
    private Depot depot;
    private FrameLayout contenu;
    private int courant = -1;
    private boolean visible;

    @Override
    protected void onCreate(Bundle etat) {
        super.onCreate(etat);
        depot = new Depot(this);
        setContentView(R.layout.activite_principale);
        Marges.gerer(this, findViewById(R.id.racine));
        contenu = findViewById(R.id.contenu);

        for (int i = 0; i < ONGLETS.length; i++) {
            final int index = i;
            findViewById(ONGLETS[i]).setOnClickListener(v -> choisir(index));
        }
        choisir(depot.onglet());

        // Rattrape une sonnerie perdue (appli mise à jour, données restaurées).
        Planificateur.toutProgrammer(this);
    }

    Depot depot() {
        return depot;
    }

    private Ecran creer(int index) {
        switch (index) {
            case 1:
                return new EcranReveil(this, contenu);
            case 2:
                return new EcranMinuteur(this, contenu);
            case 3:
                return new EcranChrono(this, contenu);
            default:
                return new EcranHorloge(this, contenu);
        }
    }

    private void choisir(int index) {
        if (index == courant) {
            return;
        }
        if (courant >= 0 && visible) {
            ecrans[courant].masquer();
        }
        courant = index;
        if (ecrans[index] == null) {
            ecrans[index] = creer(index);
        }
        contenu.removeAllViews();
        contenu.addView(ecrans[index].vue);
        for (int i = 0; i < ONGLETS.length; i++) {
            View onglet = findViewById(ONGLETS[i]);
            onglet.setSelected(i == index);
        }
        depot.ecrireOnglet(index);
        if (visible) {
            ecrans[index].afficher();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        visible = true;
        ecrans[courant].afficher();
    }

    @Override
    protected void onStop() {
        visible = false;
        ecrans[courant].masquer();
        super.onStop();
    }

    /** Vrai si les notifications de l'appli sont actives. */
    boolean notificationsAutorisees() {
        return getSystemService(NotificationManager.class).areNotificationsEnabled();
    }

    /** Demande l'autorisation des notifications si elle manque (Android 13 et plus). */
    void demanderNotifications() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS},
                    DEMANDE_NOTIFICATIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] autorisations, int[] reponses) {
        super.onRequestPermissionsResult(code, autorisations, reponses);
        if (code == DEMANDE_NOTIFICATIONS && visible) {
            // L'avertissement de l'onglet Réveil dépend de la réponse.
            ecrans[courant].masquer();
            ecrans[courant].afficher();
        }
    }
}
