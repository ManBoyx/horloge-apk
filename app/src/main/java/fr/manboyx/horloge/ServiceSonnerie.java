package fr.manboyx.horloge;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;

/**
 * Fait sonner le téléphone : son d'alarme en boucle, vibreur, notification
 * avec écran de sonnerie. C'est le seul service de l'appli, et il n'existe
 * que le temps d'une sonnerie (10 minutes au plus).
 */
public class ServiceSonnerie extends Service {

    static final String ACTION_SONNER = "fr.manboyx.horloge.SONNER";
    static final String ACTION_ARRETER = "fr.manboyx.horloge.ARRETER";
    static final String ACTION_REPETER = "fr.manboyx.horloge.REPETER";
    /** Diffusé à la fin de la sonnerie, pour fermer l'écran de sonnerie. */
    static final String ACTION_FINIE = "fr.manboyx.horloge.SONNERIE_FINIE";

    private static final String EXTRA_ID = "id";
    private static final String EXTRA_TITRE = "titre";
    private static final String EXTRA_VIBREUR = "vibreur";
    private static final String EXTRA_REVEIL = "reveil";

    private static final String CANAL = "sonnerie";
    private static final String CANAL_SECOURS = "secours";
    private static final int NOTIFICATION = 1;
    private static final int NOTIFICATION_SECOURS = 2;
    private static final long DUREE_MAX_MS = 10 * 60_000L;

    /** Titre de la sonnerie en cours, ou null. Lu par SonnerieActivity. */
    static volatile String titreEnCours;
    /** Vrai pour un réveil (répétition possible), faux pour le minuteur. */
    static volatile boolean reveilEnCours;

    private final Handler principal = new Handler(Looper.getMainLooper());
    private final Runnable arretAutomatique = this::terminer;
    private MediaPlayer lecteur;
    private Vibrator vibreur;
    private int idAlarme;

    /** Lance la sonnerie. Appelé depuis RecepteurAlarme. */
    static void sonner(Context contexte, int idAlarme, String titre, boolean vibrer, boolean reveil) {
        Intent intent = new Intent(contexte, ServiceSonnerie.class)
                .setAction(ACTION_SONNER)
                .putExtra(EXTRA_ID, idAlarme)
                .putExtra(EXTRA_TITRE, titre)
                .putExtra(EXTRA_VIBREUR, vibrer)
                .putExtra(EXTRA_REVEIL, reveil);
        try {
            contexte.startForegroundService(intent);
        } catch (RuntimeException e) {
            // Android refuse parfois de lancer un service depuis l'arrière-plan
            // (alarme non exacte) : on prévient au moins par une notification sonore.
            notifierSansService(contexte, titre);
        }
    }

    static Intent commande(Context contexte, String action) {
        return new Intent(contexte, ServiceSonnerie.class).setAction(action);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int drapeaux, int idDemarrage) {
        String action = intent == null ? null : intent.getAction();
        if (ACTION_SONNER.equals(action)) {
            commencer(intent);
        } else {
            if (ACTION_REPETER.equals(action) && titreEnCours != null && reveilEnCours) {
                new Depot(this).ecrireRepetition(idAlarme,
                        System.currentTimeMillis() + Planificateur.REPETITION_MS);
                Planificateur.toutProgrammer(this);
            }
            terminer();
        }
        return START_NOT_STICKY;
    }

    private void commencer(Intent intent) {
        String titre = intent.getStringExtra(EXTRA_TITRE);
        if (titre == null) {
            titre = "Réveil";
        }
        boolean reveil = intent.getBooleanExtra(EXTRA_REVEIL, true);
        idAlarme = intent.getIntExtra(EXTRA_ID, 0);
        titreEnCours = titre;
        reveilEnCours = reveil;

        creerCanaux(this);
        Notification notification = notification(titre, reveil);
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIFICATION, notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED);
            } else {
                startForeground(NOTIFICATION, notification);
            }
        } catch (RuntimeException e) {
            // Autorisation d'alarme exacte retirée : pas de service possible.
            notifierSansService(this, titre);
            terminer();
            return;
        }

        jouer();
        if (intent.getBooleanExtra(EXTRA_VIBREUR, true)) {
            vibrer();
        }
        principal.removeCallbacks(arretAutomatique);
        principal.postDelayed(arretAutomatique, DUREE_MAX_MS);
    }

    private Notification notification(String titre, boolean reveil) {
        PendingIntent ecran = PendingIntent.getActivity(this, 0,
                new Intent(this, SonnerieActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder constructeur = new Notification.Builder(this, CANAL)
                .setSmallIcon(R.drawable.ic_notif)
                .setContentTitle(titre)
                .setContentText("Touchez pour ouvrir l'écran de sonnerie")
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setShowWhen(true)
                .setContentIntent(ecran)
                .setFullScreenIntent(ecran, true)
                .addAction(action("Arrêter", ACTION_ARRETER, 1));
        if (reveil) {
            constructeur.addAction(action("Répéter dans 10 min", ACTION_REPETER, 2));
        }
        return constructeur.build();
    }

    private Notification.Action action(String libelle, String action, int code) {
        PendingIntent intention = PendingIntent.getService(this, code, commande(this, action),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Action.Builder(
                Icon.createWithResource(this, R.drawable.ic_notif), libelle, intention).build();
    }

    private static void creerCanaux(Context contexte) {
        NotificationManager gestionnaire = contexte.getSystemService(NotificationManager.class);

        // Canal muet : le son est joué par le service, en boucle.
        NotificationChannel canal = new NotificationChannel(CANAL, "Sonnerie",
                NotificationManager.IMPORTANCE_HIGH);
        canal.setDescription("Réveil et fin du minuteur");
        canal.setSound(null, null);
        canal.enableVibration(false);
        canal.setBypassDnd(true);
        canal.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        gestionnaire.createNotificationChannel(canal);

        NotificationChannel secours = new NotificationChannel(CANAL_SECOURS, "Sonnerie de secours",
                NotificationManager.IMPORTANCE_HIGH);
        secours.setDescription("Utilisée si la sonnerie normale est bloquée par le téléphone");
        secours.setSound(sonAlarme(), attributsAlarme());
        secours.enableVibration(true);
        secours.setBypassDnd(true);
        gestionnaire.createNotificationChannel(secours);
    }

    private static void notifierSansService(Context contexte, String titre) {
        creerCanaux(contexte);
        Notification notification = new Notification.Builder(contexte, CANAL_SECOURS)
                .setSmallIcon(R.drawable.ic_notif)
                .setContentTitle(titre)
                .setContentText("C'est l'heure.")
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(Planificateur.ouvrirAppli(contexte))
                .build();
        try {
            contexte.getSystemService(NotificationManager.class)
                    .notify(NOTIFICATION_SECOURS, notification);
        } catch (SecurityException e) {
            // Notifications interdites : il ne reste aucun moyen de prévenir.
        }
    }

    private static AudioAttributes attributsAlarme() {
        return new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
    }

    /** Son d'alarme du téléphone ; à défaut, sa sonnerie ou son de notification. */
    private static Uri sonAlarme() {
        for (int type : new int[] {RingtoneManager.TYPE_ALARM, RingtoneManager.TYPE_RINGTONE,
                RingtoneManager.TYPE_NOTIFICATION}) {
            Uri son = RingtoneManager.getDefaultUri(type);
            if (son != null) {
                return son;
            }
        }
        return null;
    }

    private void jouer() {
        if (lecteur != null) {
            return;
        }
        Uri son = sonAlarme();
        if (son == null) {
            return;
        }
        MediaPlayer nouveau = new MediaPlayer();
        try {
            nouveau.setAudioAttributes(attributsAlarme());
            nouveau.setDataSource(this, son);
            nouveau.setLooping(true);
            // Garde le processeur éveillé tant que le son joue, écran éteint compris.
            nouveau.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
            nouveau.setOnPreparedListener(MediaPlayer::start);
            nouveau.setOnErrorListener((l, quoi, detail) -> {
                libererLecteur();
                return true;
            });
            nouveau.prepareAsync();
            lecteur = nouveau;
        } catch (Exception e) {
            // Son illisible : il reste le vibreur et la notification.
            nouveau.release();
        }
    }

    @SuppressWarnings("deprecation")
    private void vibrer() {
        Vibrator v = getSystemService(Vibrator.class);
        if (v == null || !v.hasVibrator()) {
            return;
        }
        vibreur = v;
        v.vibrate(VibrationEffect.createWaveform(new long[] {0, 700, 700}, 0), attributsAlarme());
    }

    private void libererLecteur() {
        if (lecteur != null) {
            try {
                lecteur.release();
            } catch (RuntimeException e) {
                // Déjà libéré.
            }
            lecteur = null;
        }
    }

    private void toutCouper() {
        principal.removeCallbacks(arretAutomatique);
        libererLecteur();
        if (vibreur != null) {
            vibreur.cancel();
            vibreur = null;
        }
        if (titreEnCours != null) {
            titreEnCours = null;
            sendBroadcast(new Intent(ACTION_FINIE).setPackage(getPackageName()));
        }
    }

    private void terminer() {
        toutCouper();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        toutCouper();
        super.onDestroy();
    }
}
