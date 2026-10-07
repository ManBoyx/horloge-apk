package fr.manboyx.horloge;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

/** Écran de sonnerie, affiché par-dessus l'écran verrouillé. */
public class SonnerieActivity extends Activity {

    private final BroadcastReceiver finSonnerie = new BroadcastReceiver() {
        @Override
        public void onReceive(Context contexte, Intent intent) {
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle etat) {
        super.onCreate(etat);
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            afficherSurEcranVerrouille();
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activite_sonnerie);
        Marges.gerer(this, findViewById(R.id.racine_sonnerie));

        findViewById(R.id.arreter).setOnClickListener(v -> commander(ServiceSonnerie.ACTION_ARRETER));
        findViewById(R.id.repeter).setOnClickListener(v -> commander(ServiceSonnerie.ACTION_REPETER));
    }

    @SuppressWarnings("deprecation")
    private void afficherSurEcranVerrouille() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }

    private void commander(String action) {
        startService(ServiceSonnerie.commande(this, action));
        finish();
    }

    // Le drapeau RECEIVER_NOT_EXPORTED n'existe qu'à partir d'Android 13 ;
    // avant, l'enregistrement simple est la seule forme disponible.
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filtre = new IntentFilter(ServiceSonnerie.ACTION_FINIE);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(finSonnerie, filtre, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(finSonnerie, filtre);
        }

        String titre = ServiceSonnerie.titreEnCours;
        if (titre == null) {
            // La sonnerie s'est arrêtée entre-temps.
            finish();
            return;
        }
        ((TextView) findViewById(R.id.titre_sonnerie)).setText(titre);
        findViewById(R.id.repeter).setVisibility(
                ServiceSonnerie.reveilEnCours ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onStop() {
        unregisterReceiver(finSonnerie);
        super.onStop();
    }
}
