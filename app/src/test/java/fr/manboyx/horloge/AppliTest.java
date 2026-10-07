package fr.manboyx.horloge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.RemoteViews;
import android.widget.TextClock;
import android.widget.TextView;

import fr.manboyx.horloge.logique.Alarme;
import fr.manboyx.horloge.logique.Minuteur;
import fr.manboyx.horloge.logique.Villes;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlarmManager;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowDialog;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Vérifie, dans un Android simulé, que l'appli démarre, que chaque écran et
 * chaque widget s'affichent, et que les sonneries sont bien confiées au système.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class AppliTest {

    private Application appli;
    private Depot depot;
    private AlarmManager alarmes;

    @Before
    public void preparer() {
        appli = RuntimeEnvironment.getApplication();
        depot = new Depot(appli);
        alarmes = appli.getSystemService(AlarmManager.class);
        ServiceSonnerie.titreEnCours = null;
        ServiceSonnerie.reveilEnCours = false;
        // Cas normal : l'appli déclare USE_EXACT_ALARM, accordée d'office.
        ShadowAlarmManager.setCanScheduleExactAlarms(true);
    }

    private static void laisserTourner() {
        shadowOf(Looper.getMainLooper()).idle();
    }

    private static void attendre(long ms) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms));
    }

    private MainActivity lancer() {
        MainActivity a = Robolectric.buildActivity(MainActivity.class).setup().get();
        laisserTourner();
        return a;
    }

    private static void onglet(MainActivity a, int id) {
        a.findViewById(id).performClick();
        laisserTourner();
    }

    private static String texte(View parent, int id) {
        return ((TextView) parent.findViewById(id)).getText().toString();
    }

    private int nombreDAlarmesProgrammees() {
        return shadowOf(alarmes).getScheduledAlarms().size();
    }

    // ---- Démarrage et onglets ----

    @Test
    public void demarreSurLHorlogePuisParcourtLesQuatreOnglets() {
        MainActivity a = lancer();
        assertNotNull(a.findViewById(R.id.heure));
        assertTrue(a.findViewById(R.id.onglet_horloge).isSelected());

        onglet(a, R.id.onglet_reveil);
        assertNotNull(a.findViewById(R.id.alarmes));
        assertNull(a.findViewById(R.id.heure));
        assertTrue(a.findViewById(R.id.onglet_reveil).isSelected());
        assertFalse(a.findViewById(R.id.onglet_horloge).isSelected());

        onglet(a, R.id.onglet_minuteur);
        assertNotNull(a.findViewById(R.id.reglage));

        onglet(a, R.id.onglet_chrono);
        assertNotNull(a.findViewById(R.id.temps));

        onglet(a, R.id.onglet_horloge);
        assertNotNull(a.findViewById(R.id.heure));
    }

    @Test
    public void rouvreSurLeDernierOngletUtilise() {
        onglet(lancer(), R.id.onglet_chrono);
        MainActivity b = lancer();
        assertNotNull(b.findViewById(R.id.temps));
        assertTrue(b.findViewById(R.id.onglet_chrono).isSelected());
    }

    @Test
    public void demarreMalgreUneMemoireAbimee() {
        appli.createDeviceProtectedStorageContext()
                .getSharedPreferences("horloge", Context.MODE_PRIVATE).edit()
                .putString("alarmes", "n'importe quoi\n;;;;")
                .putInt("minuteur", 12)
                .putString("chrono", "1;x;y;z")
                .putString("onglet", "trois")
                .putString("secondes", "oui")
                .putString("villes", "Atlantide|Nulle part\n\n")
                .putString("repetition_a", "bientôt")
                .commit();
        MainActivity a = lancer();
        for (int id : new int[] {R.id.onglet_reveil, R.id.onglet_minuteur, R.id.onglet_chrono,
                R.id.onglet_horloge}) {
            onglet(a, id);
        }
        assertTrue(depot.alarmes().isEmpty());
        assertEquals(Minuteur.Etat.ARRET, depot.minuteur().etat);
    }

    // ---- Horloge ----

    @Test
    public void unAppuiSurLHeureAfficheLesSecondes() {
        MainActivity a = lancer();
        TextClock heure = a.findViewById(R.id.heure);
        assertEquals("HH:mm", heure.getFormat24Hour().toString());
        heure.performClick();
        assertEquals("HH:mm:ss", heure.getFormat24Hour().toString());
        assertTrue(depot.secondes());
        heure.performClick();
        assertEquals("HH:mm", heure.getFormat24Hour().toString());
    }

    @Test
    public void heuresDuMondeAffichageAjoutEtRetrait() {
        List<Villes.Ville> tokyo = new ArrayList<>(Villes.chercher("tokyo"));
        depot.ecrireVilles(tokyo);
        MainActivity a = lancer();
        ListView liste = a.findViewById(R.id.villes);
        assertEquals(1, liste.getAdapter().getCount());
        View ligne = liste.getAdapter().getView(0, null, liste);
        assertEquals("Tokyo", texte(ligne, R.id.nom));
        assertTrue(texte(ligne, R.id.detail).startsWith("Japon · "));
        assertEquals("Asia/Tokyo", ((TextClock) ligne.findViewById(R.id.heure_ville)).getTimeZone());

        a.findViewById(R.id.ajouter_ville).performClick();
        laisserTourner();
        AlertDialog dialogue = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialogue);
        ((EditText) dialogue.findViewById(R.id.recherche)).setText("montre");
        laisserTourner();
        ListView resultats = dialogue.findViewById(R.id.resultats);
        assertEquals(1, resultats.getAdapter().getCount());
        assertEquals("Montréal, Canada", resultats.getAdapter().getItem(0));
        resultats.performItemClick(null, 0, 0);
        laisserTourner();
        assertEquals(2, depot.villes().size());
        assertEquals(2, liste.getAdapter().getCount());

        // Appui long : demande de retrait, puis confirmation.
        liste.getOnItemLongClickListener().onItemLongClick(liste, null, 0, 0);
        laisserTourner();
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        laisserTourner();
        assertEquals(1, depot.villes().size());
        assertEquals("Montréal", depot.villes().get(0).nom);
    }

    // ---- Chrono ----

    @Test
    public void chronoDemarreCompteTourPauseEtRemiseAZero() {
        MainActivity a = lancer();
        onglet(a, R.id.onglet_chrono);
        Button gauche = a.findViewById(R.id.gauche);
        Button droite = a.findViewById(R.id.droite);
        assertEquals("Démarrer", droite.getText().toString());
        assertFalse(gauche.isEnabled());
        assertEquals("00:00,0", texte(a.getWindow().getDecorView(), R.id.temps));

        droite.performClick();
        attendre(350);
        assertTrue(depot.chrono(SystemClock.elapsedRealtime()).enCours);
        assertEquals("00:00,3", texte(a.getWindow().getDecorView(), R.id.temps));
        assertEquals("Pause", droite.getText().toString());
        assertEquals("Tour", gauche.getText().toString());

        gauche.performClick();
        laisserTourner();
        assertEquals(1, depot.chrono(SystemClock.elapsedRealtime()).tours.size());
        ListView tours = a.findViewById(R.id.tours);
        assertEquals("Tour 1", texte(tours.getAdapter().getView(0, null, tours), R.id.numero));

        droite.performClick();
        laisserTourner();
        String fige = texte(a.getWindow().getDecorView(), R.id.temps);
        attendre(1000);
        assertEquals("en pause, l'affichage ne bouge plus", fige, texte(a.getWindow().getDecorView(), R.id.temps));
        assertEquals("Reprendre", droite.getText().toString());

        gauche.performClick();
        laisserTourner();
        assertEquals("00:00,0", texte(a.getWindow().getDecorView(), R.id.temps));
        assertEquals(0, tours.getAdapter().getCount());
    }

    private static boolean tacheEnAttente() {
        return shadowOf(Looper.getMainLooper()).getNextScheduledTaskTime().compareTo(Duration.ZERO) > 0;
    }

    @Test
    public void chronoNeSeRafraichitPlusQuandLOngletEstQuitte() {
        MainActivity a = lancer();
        onglet(a, R.id.onglet_chrono);
        a.findViewById(R.id.droite).performClick();
        attendre(200);
        assertTrue("le chrono affiché se rafraîchit", tacheEnAttente());
        onglet(a, R.id.onglet_horloge);
        assertFalse("onglet quitté : plus aucune tâche en attente", tacheEnAttente());
        assertTrue("le chrono continue pourtant de compter", depot.chrono(SystemClock.elapsedRealtime()).enCours);
    }

    @Test
    public void rienNeTourneQuandLAppliPasseEnArrierePlan() {
        ActivityController<MainActivity> controleur = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity a = controleur.get();
        onglet(a, R.id.onglet_chrono);
        a.findViewById(R.id.droite).performClick();
        attendre(200);
        assertTrue(tacheEnAttente());
        controleur.pause().stop();
        assertFalse("appli en arrière-plan : plus aucune tâche en attente", tacheEnAttente());
        // Au retour, l'affichage reprend là où en est réellement le chrono.
        attendre(5_000);
        controleur.start().resume();
        laisserTourner();
        assertEquals("00:05,2", texte(a.getWindow().getDecorView(), R.id.temps));
    }

    @Test
    public void minuteurNeSeRafraichitPlusQuandLOngletEstQuitte() {
        MainActivity a = lancer();
        onglet(a, R.id.onglet_minuteur);
        ((NumberPicker) a.findViewById(R.id.choix_minutes)).setValue(2);
        a.findViewById(R.id.demarrer).performClick();
        laisserTourner();
        assertTrue("le décompte affiché se rafraîchit", tacheEnAttente());
        onglet(a, R.id.onglet_horloge);
        assertFalse("onglet quitté : plus aucune tâche en attente", tacheEnAttente());
        assertEquals(Minuteur.Etat.EN_COURS, depot.minuteur().etat);
    }

    // ---- Minuteur ----

    @Test
    public void minuteurDemarreDecomptePauseEtAnnulation() {
        MainActivity a = lancer();
        onglet(a, R.id.onglet_minuteur);
        ((NumberPicker) a.findViewById(R.id.choix_heures)).setValue(0);
        ((NumberPicker) a.findViewById(R.id.choix_minutes)).setValue(1);
        ((NumberPicker) a.findViewById(R.id.choix_secondes)).setValue(0);
        int avant = nombreDAlarmesProgrammees();

        a.findViewById(R.id.demarrer).performClick();
        laisserTourner();
        assertEquals(View.GONE, a.findViewById(R.id.reglage).getVisibility());
        assertEquals(View.VISIBLE, a.findViewById(R.id.decompte).getVisibility());
        assertEquals(Minuteur.Etat.EN_COURS, depot.minuteur().etat);
        assertEquals("01:00", texte(a.getWindow().getDecorView(), R.id.reste));
        assertEquals("la fin est confiée au système", avant + 1, nombreDAlarmesProgrammees());

        Button pause = a.findViewById(R.id.pause);
        pause.performClick();
        laisserTourner();
        assertEquals(Minuteur.Etat.PAUSE, depot.minuteur().etat);
        assertEquals("Reprendre", pause.getText().toString());
        assertEquals(avant, nombreDAlarmesProgrammees());

        pause.performClick();
        laisserTourner();
        assertEquals(Minuteur.Etat.EN_COURS, depot.minuteur().etat);

        a.findViewById(R.id.annuler).performClick();
        laisserTourner();
        assertEquals(Minuteur.Etat.ARRET, depot.minuteur().etat);
        assertEquals(View.VISIBLE, a.findViewById(R.id.reglage).getVisibility());
        assertEquals(avant, nombreDAlarmesProgrammees());
    }

    @Test
    public void minuteurSansDureeNeDemarrePas() {
        MainActivity a = lancer();
        onglet(a, R.id.onglet_minuteur);
        ((NumberPicker) a.findViewById(R.id.choix_heures)).setValue(0);
        ((NumberPicker) a.findViewById(R.id.choix_minutes)).setValue(0);
        ((NumberPicker) a.findViewById(R.id.choix_secondes)).setValue(0);
        a.findViewById(R.id.demarrer).performClick();
        laisserTourner();
        assertEquals(Minuteur.Etat.ARRET, depot.minuteur().etat);
    }

    @Test
    public void finDuMinuteurArreteLeMinuteurEtLanceLaSonnerie() {
        depot.ecrireMinuteur(Minuteur.ARRET_INITIAL.demarrer(1_000, System.currentTimeMillis() - 2_000));
        new RecepteurAlarme().onReceive(appli, new Intent(Planificateur.ACTION_MINUTEUR));
        assertEquals(Minuteur.Etat.ARRET, depot.minuteur().etat);
        assertEquals(ServiceSonnerie.ACTION_SONNER, shadowOf(appli).getNextStartedService().getAction());
    }

    // ---- Réveil ----

    @Test
    public void reveilAffichageInterrupteurModificationSuppression() {
        depot.enregistrer(new Alarme(1, 7, 30, 31, "Travail", true, true));
        MainActivity a = lancer();
        assertNotNull("programmé dès le lancement", alarmes.getNextAlarmClock());

        onglet(a, R.id.onglet_reveil);
        ListView liste = a.findViewById(R.id.alarmes);
        assertEquals(1, liste.getAdapter().getCount());
        View ligne = liste.getAdapter().getView(0, null, liste);
        assertTrue(texte(ligne, R.id.heure_alarme).contains("30"));
        assertEquals("Du lundi au vendredi · Travail", texte(ligne, R.id.detail_alarme));

        // Interrupteur : désactive, et plus rien n'est programmé.
        ligne.findViewById(R.id.actif).performClick();
        laisserTourner();
        assertFalse(depot.alarme(1).active);
        assertNull(alarmes.getNextAlarmClock());

        // Appui sur la ligne : dialogue de modification ; « Enregistrer » réactive.
        liste.performItemClick(ligne, 0, 1);
        laisserTourner();
        AlertDialog dialogue = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialogue);
        ((EditText) dialogue.findViewById(R.id.nom_alarme)).setText("  Sport ");
        dialogue.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        laisserTourner();
        assertTrue(depot.alarme(1).active);
        assertEquals("Sport", depot.alarme(1).nom);
        assertEquals(31, depot.alarme(1).jours);
        assertNotNull(alarmes.getNextAlarmClock());

        // « Supprimer ».
        liste.performItemClick(liste.getAdapter().getView(0, null, liste), 0, 1);
        laisserTourner();
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_NEUTRAL).performClick();
        laisserTourner();
        assertTrue(depot.alarmes().isEmpty());
        assertNull(alarmes.getNextAlarmClock());
        assertEquals(0, liste.getAdapter().getCount());
    }

    @Test
    public void ajouterUnReveilOuvreLeChoixDeLHeure() {
        MainActivity a = lancer();
        onglet(a, R.id.onglet_reveil);
        a.findViewById(R.id.ajouter_alarme).performClick();
        laisserTourner();
        Dialog dialogue = ShadowDialog.getLatestDialog();
        assertTrue(dialogue instanceof TimePickerDialog);
        ((TimePickerDialog) dialogue).updateTime(6, 45);
        ((TimePickerDialog) dialogue).getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        laisserTourner();
        assertEquals(1, depot.alarmes().size());
        Alarme creee = depot.alarmes().get(0);
        assertEquals(6, creee.heure);
        assertEquals(45, creee.minute);
        assertTrue(creee.active);
        assertNotNull(alarmes.getNextAlarmClock());
    }

    @Test
    public void laSonnerieProgrammeeEstCelleDeLAlarmeLaPlusProche() {
        ZonedDateTime maintenant = ZonedDateTime.now();
        ZonedDateTime dansDeuxHeures = maintenant.plusHours(2);
        ZonedDateTime dansCinqHeures = maintenant.plusHours(5);
        depot.enregistrer(new Alarme(1, dansCinqHeures.getHour(), dansCinqHeures.getMinute(), 0, "", true, true));
        depot.enregistrer(new Alarme(2, dansDeuxHeures.getHour(), dansDeuxHeures.getMinute(), 0, "", true, true));
        Planificateur.toutProgrammer(appli);
        long prevu = alarmes.getNextAlarmClock().getTriggerTime();
        long attendu = new Alarme(2, dansDeuxHeures.getHour(), dansDeuxHeures.getMinute(), 0, "", true, true)
                .prochaine(maintenant).toInstant().toEpochMilli();
        assertEquals(attendu, prevu);
        assertEquals(attendu, Planificateur.prochaine(appli).toInstant().toEpochMilli());
    }

    @Test
    public void alarmeUneFoisSonnePuisSeDesactive() {
        depot.enregistrer(new Alarme(2, 6, 0, 0, "", true, true));
        new RecepteurAlarme().onReceive(appli,
                new Intent(Planificateur.ACTION_ALARME).putExtra(Planificateur.EXTRA_ID, 2));
        assertFalse(depot.alarme(2).active);
        assertEquals(ServiceSonnerie.ACTION_SONNER, shadowOf(appli).getNextStartedService().getAction());
        assertNull(alarmes.getNextAlarmClock());
    }

    @Test
    public void alarmeRepeteeSonnePuisEstReprogrammee() {
        depot.enregistrer(new Alarme(3, 6, 0, Alarme.TOUS_LES_JOURS, "", true, true));
        new RecepteurAlarme().onReceive(appli,
                new Intent(Planificateur.ACTION_ALARME).putExtra(Planificateur.EXTRA_ID, 3));
        assertTrue(depot.alarme(3).active);
        assertNotNull(shadowOf(appli).getNextStartedService());
        assertNotNull(alarmes.getNextAlarmClock());
    }

    @Test
    public void alarmeDesactiveeOuSupprimeeNeSonnePas() {
        depot.enregistrer(new Alarme(4, 6, 0, 0, "", true, false));
        new RecepteurAlarme().onReceive(appli,
                new Intent(Planificateur.ACTION_ALARME).putExtra(Planificateur.EXTRA_ID, 4));
        new RecepteurAlarme().onReceive(appli,
                new Intent(Planificateur.ACTION_ALARME).putExtra(Planificateur.EXTRA_ID, 99));
        assertNull(shadowOf(appli).getNextStartedService());
    }

    @Test
    public void redemarrageDuTelephoneReprogrammeLesAlarmes() {
        depot.enregistrer(new Alarme(1, 7, 0, Alarme.TOUS_LES_JOURS, "", true, true));
        new RecepteurSysteme().onReceive(appli, new Intent("une.action.inconnue"));
        assertNull(alarmes.getNextAlarmClock());
        new RecepteurSysteme().onReceive(appli, new Intent(Intent.ACTION_LOCKED_BOOT_COMPLETED));
        assertNotNull(alarmes.getNextAlarmClock());
    }

    @Test
    public void sansAlarmesExactesLeReveilResteProgrammeEtLEcranPrevient() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false);
        depot.enregistrer(new Alarme(1, 7, 0, Alarme.TOUS_LES_JOURS, "", true, true));
        MainActivity a = lancer();
        assertEquals("alarme approximative programmée", 1, nombreDAlarmesProgrammees());
        assertNull("mais pas annoncée comme réveil exact", alarmes.getNextAlarmClock());

        onglet(a, R.id.onglet_reveil);
        TextView avertissement = a.findViewById(R.id.avertissement);
        assertEquals(View.VISIBLE, avertissement.getVisibility());
        assertTrue(avertissement.getText().toString().startsWith("Alarmes exactes refusées"));
    }

    @Test
    public void sansNotificationsLEcranReveilPrevient() {
        shadowOf(appli.getSystemService(NotificationManager.class)).setNotificationsEnabled(false);
        depot.enregistrer(new Alarme(1, 7, 0, Alarme.TOUS_LES_JOURS, "", true, true));
        MainActivity a = lancer();
        onglet(a, R.id.onglet_reveil);
        TextView avertissement = a.findViewById(R.id.avertissement);
        assertEquals(View.VISIBLE, avertissement.getVisibility());
        assertTrue(avertissement.getText().toString().startsWith("Notifications désactivées"));
    }

    @Test
    public void sansProblemeAucunAvertissement() {
        depot.enregistrer(new Alarme(1, 7, 0, Alarme.TOUS_LES_JOURS, "", true, true));
        MainActivity a = lancer();
        onglet(a, R.id.onglet_reveil);
        assertEquals(View.GONE, a.findViewById(R.id.avertissement).getVisibility());
    }

    // ---- Sonnerie ----

    private ServiceController<ServiceSonnerie> sonner(String titre, boolean reveil) {
        ServiceSonnerie.sonner(appli, 5, titre, true, reveil);
        Intent intent = shadowOf(appli).getNextStartedService();
        ServiceController<ServiceSonnerie> service =
                Robolectric.buildService(ServiceSonnerie.class, intent).create().startCommand(0, 1);
        laisserTourner();
        return service;
    }

    private List<Notification> notifications() {
        return shadowOf(appli.getSystemService(NotificationManager.class)).getAllNotifications();
    }

    @Test
    public void laSonnerieAfficheUneNotificationAvecEcranEtActions() {
        sonner("Travail", true);
        assertEquals("Travail", ServiceSonnerie.titreEnCours);
        assertEquals(1, notifications().size());
        Notification n = notifications().get(0);
        assertEquals(Notification.CATEGORY_ALARM, n.category);
        assertNotNull(n.fullScreenIntent);
        assertEquals(2, n.actions.length);
        assertEquals("Arrêter", n.actions[0].title.toString());
    }

    @Test
    public void arreterCoupeLaSonnerie() {
        ServiceController<ServiceSonnerie> service = sonner("Travail", true);
        service.get().onStartCommand(ServiceSonnerie.commande(appli, ServiceSonnerie.ACTION_ARRETER), 0, 2);
        assertNull(ServiceSonnerie.titreEnCours);
        assertEquals(0, depot.repetitionA());
    }

    @Test
    public void repeterProgrammeUneNouvelleSonnerieDansDixMinutes() {
        ServiceController<ServiceSonnerie> service = sonner("Travail", true);
        long avant = System.currentTimeMillis();
        service.get().onStartCommand(ServiceSonnerie.commande(appli, ServiceSonnerie.ACTION_REPETER), 0, 2);
        assertNull(ServiceSonnerie.titreEnCours);
        assertEquals(5, depot.repetitionId());
        assertTrue(depot.repetitionA() >= avant + Planificateur.REPETITION_MS);
        assertEquals(depot.repetitionA(), alarmes.getNextAlarmClock().getTriggerTime());

        // À l'heure dite, la répétition sonne et s'efface.
        new RecepteurAlarme().onReceive(appli, new Intent(Planificateur.ACTION_REPETITION));
        assertEquals(0, depot.repetitionA());
        assertEquals(ServiceSonnerie.ACTION_SONNER, shadowOf(appli).getNextStartedService().getAction());
    }

    @Test
    public void leMinuteurNeProposePasDeRepetition() {
        ServiceController<ServiceSonnerie> service = sonner("Minuteur terminé", false);
        assertEquals(1, notifications().get(0).actions.length);
        service.get().onStartCommand(ServiceSonnerie.commande(appli, ServiceSonnerie.ACTION_REPETER), 0, 2);
        assertEquals(0, depot.repetitionA());
    }

    @Test
    public void laSonnerieSArreteSeuleAuBoutDeDixMinutes() {
        sonner("Travail", true);
        attendre(9 * 60_000L);
        assertNotNull(ServiceSonnerie.titreEnCours);
        attendre(61_000L);
        assertNull(ServiceSonnerie.titreEnCours);
    }

    @Test
    public void ecranDeSonnerie() {
        ServiceSonnerie.titreEnCours = "Travail";
        ServiceSonnerie.reveilEnCours = true;
        SonnerieActivity ecran = Robolectric.buildActivity(SonnerieActivity.class).setup().get();
        assertEquals("Travail", texte(ecran.getWindow().getDecorView(), R.id.titre_sonnerie));
        assertEquals(View.VISIBLE, ecran.findViewById(R.id.repeter).getVisibility());
        ecran.findViewById(R.id.arreter).performClick();
        assertTrue(ecran.isFinishing());
        assertEquals(ServiceSonnerie.ACTION_ARRETER, shadowOf(appli).getNextStartedService().getAction());
    }

    @Test
    public void ecranDeSonnerieDuMinuteurEtFermetureAutomatique() {
        ServiceSonnerie.titreEnCours = "Minuteur terminé";
        ServiceSonnerie.reveilEnCours = false;
        SonnerieActivity ecran = Robolectric.buildActivity(SonnerieActivity.class).setup().get();
        assertEquals(View.GONE, ecran.findViewById(R.id.repeter).getVisibility());
        assertFalse(ecran.isFinishing());
        // La sonnerie s'arrête ailleurs (notification, arrêt automatique) : l'écran se ferme.
        appli.sendBroadcast(new Intent(ServiceSonnerie.ACTION_FINIE).setPackage(appli.getPackageName()));
        laisserTourner();
        assertTrue(ecran.isFinishing());
    }

    @Test
    public void ecranDeSonnerieSeFermeSIlNYAPlusDeSonnerie() {
        SonnerieActivity ecran = Robolectric.buildActivity(SonnerieActivity.class).setup().get();
        assertTrue(ecran.isFinishing());
    }

    // ---- Widgets ----

    @Test
    public void lesQuatreWidgetsSeDessinent() {
        int[] misesEnPage = {R.layout.widget_numerique, R.layout.widget_date,
            R.layout.widget_analogique, R.layout.widget_reveil};
        for (int miseEnPage : misesEnPage) {
            View vue = new RemoteViews(appli.getPackageName(), miseEnPage)
                    .apply(appli, new FrameLayout(appli));
            assertNotNull(vue.findViewById(R.id.widget_racine));
        }
        AppWidgetManager gestionnaire = AppWidgetManager.getInstance(appli);
        shadowOf(gestionnaire).createWidget(Widgets.Numerique.class, R.layout.widget_numerique);
        shadowOf(gestionnaire).createWidget(Widgets.AvecDate.class, R.layout.widget_date);
        shadowOf(gestionnaire).createWidget(Widgets.Analogique.class, R.layout.widget_analogique);
    }

    @Test
    public void leWidgetReveilSuitLesAlarmes() {
        AppWidgetManager gestionnaire = AppWidgetManager.getInstance(appli);
        int id = shadowOf(gestionnaire).createWidget(Widgets.AvecReveil.class, R.layout.widget_reveil);
        assertEquals("Aucun réveil", texte(shadowOf(gestionnaire).getViewFor(id), R.id.widget_prochain));

        depot.enregistrer(new Alarme(1, 7, 30, Alarme.TOUS_LES_JOURS, "", true, true));
        Planificateur.toutProgrammer(appli);
        String affiche = texte(shadowOf(gestionnaire).getViewFor(id), R.id.widget_prochain);
        assertTrue(affiche, affiche.startsWith("Réveil "));
        assertTrue(affiche, affiche.contains("30"));
        assertNotEquals("Aucun réveil", affiche);

        depot.enregistrer(depot.alarme(1).avecActive(false));
        Planificateur.toutProgrammer(appli);
        assertEquals("Aucun réveil", texte(shadowOf(gestionnaire).getViewFor(id), R.id.widget_prochain));
    }
}
