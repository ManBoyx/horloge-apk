package fr.manboyx.horloge;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import fr.manboyx.horloge.logique.Chrono;
import fr.manboyx.horloge.logique.Format;

/**
 * Onglet Chrono. Le temps est calculé à partir d'un instant de départ : le
 * rafraîchissement (10 fois par seconde) n'existe que lorsque l'onglet est
 * visible et que le chrono tourne.
 */
final class EcranChrono extends Ecran {

    private static final long PAS_MS = 100;

    private final Handler principal = new Handler(Looper.getMainLooper());
    private final TextView temps;
    private final Button gauche;
    private final Button droite;
    private final Adaptateur adaptateur = new Adaptateur();
    private Chrono chrono = Chrono.VIDE;

    private final Runnable battement = new Runnable() {
        @Override
        public void run() {
            temps.setText(Format.chrono(chrono.ecoule(maintenant())));
            if (chrono.enCours) {
                principal.postDelayed(this, PAS_MS);
            }
        }
    };

    EcranChrono(MainActivity activite, ViewGroup parent) {
        super(activite, parent, R.layout.ecran_chrono);
        temps = vue.findViewById(R.id.temps);
        gauche = vue.findViewById(R.id.gauche);
        droite = vue.findViewById(R.id.droite);
        ((ListView) vue.findViewById(R.id.tours)).setAdapter(adaptateur);

        droite.setOnClickListener(v -> {
            chrono = chrono.enCours ? chrono.pause(maintenant()) : chrono.demarrer(maintenant());
            enregistrer();
        });
        gauche.setOnClickListener(v -> {
            chrono = chrono.enCours ? chrono.tour(maintenant()) : Chrono.VIDE;
            enregistrer();
        });
    }

    /** Temps depuis le démarrage du téléphone : ne recule jamais, même si l'heure est changée. */
    private static long maintenant() {
        return SystemClock.elapsedRealtime();
    }

    @Override
    void afficher() {
        chrono = depot.chrono(maintenant());
        rafraichir();
    }

    @Override
    void masquer() {
        principal.removeCallbacks(battement);
    }

    private void enregistrer() {
        depot.ecrireChrono(chrono);
        rafraichir();
    }

    private void rafraichir() {
        principal.removeCallbacks(battement);
        boolean entame = chrono.ecoule(maintenant()) > 0;

        droite.setText(chrono.enCours ? "Pause" : entame ? "Reprendre" : "Démarrer");
        gauche.setText(chrono.enCours ? "Tour" : "Remise à zéro");
        boolean gaucheUtile = chrono.enCours || entame;
        gauche.setEnabled(gaucheUtile);
        gauche.setAlpha(gaucheUtile ? 1f : 0.4f);

        adaptateur.notifyDataSetChanged();
        battement.run();
    }

    /** Tours, du plus récent au plus ancien. */
    private final class Adaptateur extends BaseAdapter {

        @Override
        public int getCount() {
            return chrono.tours.size();
        }

        @Override
        public Object getItem(int position) {
            return chrono.tours.get(indice(position));
        }

        @Override
        public long getItemId(int position) {
            return indice(position);
        }

        private int indice(int position) {
            return chrono.tours.size() - 1 - position;
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }

        @Override
        public View getView(int position, View recyclee, ViewGroup parent) {
            View ligne = recyclee != null ? recyclee
                    : LayoutInflater.from(activite).inflate(R.layout.ligne_tour, parent, false);
            int i = indice(position);
            long total = chrono.tours.get(i);
            long precedent = i == 0 ? 0 : chrono.tours.get(i - 1);
            ((TextView) ligne.findViewById(R.id.numero)).setText("Tour " + (i + 1));
            ((TextView) ligne.findViewById(R.id.ecart)).setText("+" + Format.chrono(total - precedent));
            ((TextView) ligne.findViewById(R.id.total)).setText(Format.chrono(total));
            return ligne;
        }
    }
}
