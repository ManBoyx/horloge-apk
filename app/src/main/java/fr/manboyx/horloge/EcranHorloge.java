package fr.manboyx.horloge;

import android.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextClock;
import android.widget.TextView;

import fr.manboyx.horloge.logique.Format;
import fr.manboyx.horloge.logique.Villes;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Onglet Horloge : heure locale et heures du monde. Tout l'affichage repose
 * sur TextClock, que le système met à jour une fois par minute (ou par
 * seconde si les secondes sont affichées) et seulement quand il est visible.
 */
final class EcranHorloge extends Ecran {

    private final TextClock heure;
    private final List<Villes.Ville> villes = new ArrayList<>();
    private final Adaptateur adaptateur = new Adaptateur();

    EcranHorloge(MainActivity activite, ViewGroup parent) {
        super(activite, parent, R.layout.ecran_horloge);

        heure = vue.findViewById(R.id.heure);
        heure.setOnClickListener(v -> {
            depot.ecrireSecondes(!depot.secondes());
            appliquerFormat();
        });
        appliquerFormat();

        ListView liste = vue.findViewById(R.id.villes);
        liste.setEmptyView(vue.findViewById(R.id.villes_vide));
        liste.setAdapter(adaptateur);
        liste.setOnItemLongClickListener((p, v, position, id) -> {
            confirmerRetrait(villes.get(position));
            return true;
        });

        vue.findViewById(R.id.ajouter_ville).setOnClickListener(v -> choisirVille());
    }

    @Override
    void afficher() {
        villes.clear();
        villes.addAll(depot.villes());
        // Recalcule aussi les décalages : ils changent aux changements d'heure.
        adaptateur.notifyDataSetChanged();
    }

    private void appliquerFormat() {
        boolean secondes = depot.secondes();
        heure.setFormat24Hour(secondes ? "HH:mm:ss" : "HH:mm");
        heure.setFormat12Hour(secondes ? "h:mm:ss a" : "h:mm a");
    }

    private void confirmerRetrait(final Villes.Ville ville) {
        new AlertDialog.Builder(activite)
                .setMessage("Retirer " + ville.nom + " ?")
                .setPositiveButton("Retirer", (d, w) -> {
                    villes.remove(ville);
                    depot.ecrireVilles(villes);
                    adaptateur.notifyDataSetChanged();
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void choisirVille() {
        View contenu = LayoutInflater.from(activite).inflate(R.layout.dialogue_ville, null);
        EditText recherche = contenu.findViewById(R.id.recherche);
        ListView resultats = contenu.findViewById(R.id.resultats);

        final List<Villes.Ville> trouvees = new ArrayList<>(Villes.TOUTES);
        final ArrayAdapter<String> noms = new ArrayAdapter<>(activite,
                android.R.layout.simple_list_item_1, new ArrayList<String>());
        remplir(noms, trouvees);
        resultats.setAdapter(noms);

        final AlertDialog dialogue = new AlertDialog.Builder(activite)
                .setTitle("Ajouter une ville")
                .setView(contenu)
                .setNegativeButton("Annuler", null)
                .create();

        recherche.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int debut, int nombre, int apres) {
            }

            @Override
            public void onTextChanged(CharSequence s, int debut, int avant, int nombre) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                trouvees.clear();
                trouvees.addAll(Villes.chercher(s.toString()));
                remplir(noms, trouvees);
            }
        });

        resultats.setOnItemClickListener((p, v, position, id) -> {
            Villes.Ville choisie = trouvees.get(position);
            if (!villes.contains(choisie)) {
                villes.add(choisie);
                depot.ecrireVilles(villes);
                adaptateur.notifyDataSetChanged();
            }
            dialogue.dismiss();
        });

        dialogue.show();
    }

    private static void remplir(ArrayAdapter<String> noms, List<Villes.Ville> villes) {
        noms.clear();
        for (Villes.Ville v : villes) {
            noms.add(v.nom + ", " + v.pays);
        }
    }

    private final class Adaptateur extends BaseAdapter {

        @Override
        public int getCount() {
            return villes.size();
        }

        @Override
        public Object getItem(int position) {
            return villes.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View recyclee, ViewGroup parent) {
            View ligne = recyclee != null ? recyclee
                    : LayoutInflater.from(activite).inflate(R.layout.ligne_ville, parent, false);
            Villes.Ville ville = villes.get(position);
            int decalage = Villes.decalageMinutes(ville.zone, ZoneId.systemDefault(), Instant.now());

            ((TextView) ligne.findViewById(R.id.nom)).setText(ville.nom);
            ((TextView) ligne.findViewById(R.id.detail))
                    .setText(ville.pays + " · " + Format.decalage(decalage));
            TextClock horloge = ligne.findViewById(R.id.heure_ville);
            horloge.setFormat24Hour("HH:mm");
            horloge.setFormat12Hour("h:mm a");
            horloge.setTimeZone(ville.zone);
            return ligne;
        }
    }
}
