package fr.manboyx.horloge;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import fr.manboyx.horloge.logique.Alarme;
import fr.manboyx.horloge.logique.Format;
import fr.manboyx.horloge.logique.ListeAlarmes;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Onglet Réveil : liste des alarmes, ajout, modification, suppression. */
final class EcranReveil extends Ecran {

    private static final String[] INITIALES = {"L", "M", "M", "J", "V", "S", "D"};
    private static final String[] NOMS_JOURS = {
        "lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche"
    };

    private final List<Alarme> alarmes = new ArrayList<>();
    private final Adaptateur adaptateur = new Adaptateur();
    private final TextView avertissement;

    EcranReveil(MainActivity activite, ViewGroup parent) {
        super(activite, parent, R.layout.ecran_reveil);
        avertissement = vue.findViewById(R.id.avertissement);

        ListView liste = vue.findViewById(R.id.alarmes);
        liste.setEmptyView(vue.findViewById(R.id.alarmes_vide));
        liste.setAdapter(adaptateur);
        liste.setOnItemClickListener((p, v, position, id) -> modifier(alarmes.get(position)));

        vue.findViewById(R.id.ajouter_alarme).setOnClickListener(v -> ajouter());
    }

    @Override
    void afficher() {
        recharger();
    }

    private void recharger() {
        alarmes.clear();
        alarmes.addAll(depot.alarmes());
        Collections.sort(alarmes, new Comparator<Alarme>() {
            @Override
            public int compare(Alarme a, Alarme b) {
                return (a.heure * 60 + a.minute) - (b.heure * 60 + b.minute);
            }
        });
        adaptateur.notifyDataSetChanged();
        avertir();
    }

    private void ajouter() {
        new TimePickerDialog(activite, (selecteur, heure, minute) ->
                enregistrer(new Alarme(ListeAlarmes.prochainId(depot.alarmes()),
                        heure, minute, 0, "", true, true)),
                7, 0, DateFormat.is24HourFormat(activite)).show();
    }

    private void enregistrer(Alarme alarme) {
        depot.enregistrer(alarme);
        Planificateur.toutProgrammer(activite);
        recharger();
        if (alarme.active) {
            ZonedDateTime maintenant = ZonedDateTime.now();
            long attente = alarme.prochaine(maintenant).toInstant().toEpochMilli()
                    - maintenant.toInstant().toEpochMilli();
            Toast.makeText(activite, "Sonnera " + Format.dansCombien(attente), Toast.LENGTH_SHORT).show();
            activite.demanderNotifications();
        }
    }

    private void supprimer(Alarme alarme) {
        Planificateur.annuler(activite, alarme.id);
        depot.supprimer(alarme.id);
        Planificateur.toutProgrammer(activite);
        recharger();
    }

    private void modifier(final Alarme alarme) {
        View contenu = LayoutInflater.from(activite).inflate(R.layout.dialogue_alarme, null);
        final Button heure = contenu.findViewById(R.id.heure_dialogue);
        final EditText nom = contenu.findViewById(R.id.nom_alarme);
        final CheckBox vibreur = contenu.findViewById(R.id.vibreur);
        LinearLayout jours = contenu.findViewById(R.id.jours);

        final int[] choisie = {alarme.heure, alarme.minute};
        heure.setText(Textes.heure(activite, choisie[0], choisie[1]));
        heure.setOnClickListener(v -> new TimePickerDialog(activite, (selecteur, h, m) -> {
            choisie[0] = h;
            choisie[1] = m;
            heure.setText(Textes.heure(activite, h, m));
        }, choisie[0], choisie[1], DateFormat.is24HourFormat(activite)).show());

        final ToggleButton[] boutons = new ToggleButton[7];
        float densite = activite.getResources().getDisplayMetrics().density;
        for (int i = 0; i < 7; i++) {
            ToggleButton b = new ToggleButton(activite);
            b.setTextOn(INITIALES[i]);
            b.setTextOff(INITIALES[i]);
            b.setChecked((alarme.jours & (1 << i)) != 0);
            b.setContentDescription(NOMS_JOURS[i]);
            b.setBackgroundResource(R.drawable.fond_jour);
            b.setTextColor(activite.getColorStateList(R.color.texte_jour));
            b.setStateListAnimator(null);
            b.setPadding(0, 0, 0, 0);
            b.setMinWidth(0);
            b.setMinimumWidth(0);
            LinearLayout.LayoutParams place =
                    new LinearLayout.LayoutParams(0, Math.round(40 * densite), 1f);
            place.setMarginEnd(i < 6 ? Math.round(4 * densite) : 0);
            jours.addView(b, place);
            boutons[i] = b;
        }

        nom.setText(alarme.nom);
        vibreur.setChecked(alarme.vibreur);

        new AlertDialog.Builder(activite)
                .setTitle("Modifier le réveil")
                .setView(contenu)
                .setPositiveButton("Enregistrer", (d, w) -> {
                    int masque = 0;
                    for (int i = 0; i < 7; i++) {
                        if (boutons[i].isChecked()) {
                            masque |= 1 << i;
                        }
                    }
                    enregistrer(alarme.avecHeure(choisie[0], choisie[1])
                            .avecJours(masque)
                            .avecNom(nom.getText().toString().trim())
                            .avecVibreur(vibreur.isChecked())
                            .avecActive(true));
                })
                .setNeutralButton("Supprimer", (d, w) -> supprimer(alarme))
                .setNegativeButton("Annuler", null)
                .show();
    }

    /** Signale ce qui empêcherait un réveil de sonner correctement. */
    private void avertir() {
        if (alarmes.isEmpty()) {
            avertissement.setVisibility(View.GONE);
        } else if (!activite.notificationsAutorisees()) {
            montrerAvertissement("Notifications désactivées : l'écran de sonnerie ne pourra pas "
                    + "s'afficher. Touchez ici pour les autoriser.",
                    new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, activite.getPackageName()));
        } else if (Build.VERSION.SDK_INT >= 31 && !Planificateur.exactesAutorisees(activite)) {
            montrerAvertissement("Alarmes exactes refusées : le réveil peut sonner en retard. "
                    + "Touchez ici pour les autoriser.",
                    new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:" + activite.getPackageName())));
        } else {
            avertissement.setVisibility(View.GONE);
        }
    }

    private void montrerAvertissement(String texte, final Intent reglages) {
        avertissement.setText(texte);
        avertissement.setVisibility(View.VISIBLE);
        avertissement.setOnClickListener(v -> {
            try {
                activite.startActivity(reglages);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(activite, "Ouvrez les réglages du téléphone, puis Applications.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private final class Adaptateur extends BaseAdapter {

        @Override
        public int getCount() {
            return alarmes.size();
        }

        @Override
        public Object getItem(int position) {
            return alarmes.get(position);
        }

        @Override
        public long getItemId(int position) {
            return alarmes.get(position).id;
        }

        @Override
        public View getView(int position, View recyclee, ViewGroup parent) {
            View ligne = recyclee != null ? recyclee
                    : LayoutInflater.from(activite).inflate(R.layout.ligne_alarme, parent, false);
            final Alarme alarme = alarmes.get(position);

            TextView heure = ligne.findViewById(R.id.heure_alarme);
            TextView detail = ligne.findViewById(R.id.detail_alarme);
            heure.setText(Textes.heure(activite, alarme.heure, alarme.minute));
            detail.setText(alarme.nom.isEmpty() ? Format.jours(alarme.jours)
                    : Format.jours(alarme.jours) + " · " + alarme.nom);
            heure.setAlpha(alarme.active ? 1f : 0.45f);
            detail.setAlpha(alarme.active ? 1f : 0.45f);

            Switch actif = ligne.findViewById(R.id.actif);
            // La ligne est recyclée : on retire l'ancien écouteur avant de la régler.
            actif.setOnCheckedChangeListener(null);
            actif.setChecked(alarme.active);
            actif.setOnCheckedChangeListener((bouton, coche) -> enregistrer(alarme.avecActive(coche)));
            return ligne;
        }
    }
}
