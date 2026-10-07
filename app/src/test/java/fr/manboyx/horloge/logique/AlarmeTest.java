package fr.manboyx.horloge.logique;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class AlarmeTest {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final int LUNDI = 1, MERCREDI = 1 << 2, VENDREDI = 1 << 4, DIMANCHE = 1 << 6;

    private static ZonedDateTime paris(int a, int m, int j, int h, int min) {
        return ZonedDateTime.of(a, m, j, h, min, 0, 0, PARIS);
    }

    private static Alarme alarme(int heure, int minute, int jours) {
        return new Alarme(1, heure, minute, jours, "", true, true);
    }

    // Le mercredi 7 octobre 2026 sert de repère.

    @Test
    public void uneFoisPlusTardAujourdhui() {
        assertEquals(paris(2026, 10, 7, 18, 30), alarme(18, 30, 0).prochaine(paris(2026, 10, 7, 9, 0)));
    }

    @Test
    public void uneFoisHeureDejaPasseeSonneDemain() {
        assertEquals(paris(2026, 10, 8, 7, 0), alarme(7, 0, 0).prochaine(paris(2026, 10, 7, 9, 0)));
    }

    @Test
    public void minuteEnCoursSonneALaProchaineOccurrence() {
        assertEquals(paris(2026, 10, 8, 9, 0), alarme(9, 0, 0).prochaine(paris(2026, 10, 7, 9, 0)));
        ZonedDateTime enCoursDeMinute = paris(2026, 10, 7, 9, 0).plusSeconds(20);
        assertEquals(paris(2026, 10, 8, 9, 0), alarme(9, 0, 0).prochaine(enCoursDeMinute));
    }

    @Test
    public void uneSecondeAvantSonneLeJourMeme() {
        ZonedDateTime juste = paris(2026, 10, 7, 8, 59).plusSeconds(59);
        assertEquals(paris(2026, 10, 7, 9, 0), alarme(9, 0, 0).prochaine(juste));
    }

    @Test
    public void repeteLeProchainJourCoche() {
        // Mercredi 9 h : une alarme du lundi et du vendredi à 7 h sonne vendredi.
        assertEquals(paris(2026, 10, 9, 7, 0), alarme(7, 0, LUNDI | VENDREDI).prochaine(paris(2026, 10, 7, 9, 0)));
    }

    @Test
    public void repeteLeJourMemeSiPlusTard() {
        assertEquals(paris(2026, 10, 7, 22, 0), alarme(22, 0, MERCREDI).prochaine(paris(2026, 10, 7, 9, 0)));
    }

    @Test
    public void repeteUnSeulJourDejaPasseSonneDansUneSemaine() {
        assertEquals(paris(2026, 10, 14, 7, 0), alarme(7, 0, MERCREDI).prochaine(paris(2026, 10, 7, 9, 0)));
    }

    @Test
    public void dimancheEstLeDernierBit() {
        assertEquals(paris(2026, 10, 11, 10, 0), alarme(10, 0, DIMANCHE).prochaine(paris(2026, 10, 7, 9, 0)));
    }

    @Test
    public void heureInexistanteAuPassageHeureDEte() {
        // Le 29 mars 2026, 2 h 30 n'existe pas à Paris : l'alarme sonne à 3 h 30.
        ZonedDateTime veille = paris(2026, 3, 28, 12, 0);
        ZonedDateTime sonnerie = alarme(2, 30, 0).prochaine(veille);
        assertEquals(paris(2026, 3, 29, 3, 30), sonnerie);
        // De samedi midi à dimanche 3 h 30 : 14 h 30 réellement écoulées.
        assertEquals(14 * 3600 + 30 * 60, sonnerie.toEpochSecond() - veille.toEpochSecond());
    }

    @Test
    public void heureDoubleAuPassageHeureDHiverSonneUneSeuleFois() {
        // Le 25 octobre 2026, 2 h 30 existe deux fois à Paris.
        Alarme a = alarme(2, 30, 127);
        ZonedDateTime premiere = a.prochaine(paris(2026, 10, 24, 12, 0));
        assertEquals("+02:00", premiere.getOffset().getId());
        ZonedDateTime suivante = a.prochaine(premiere.plusSeconds(1));
        assertEquals(26, suivante.getDayOfMonth());
        // Même en repartant de la seconde 2 h 30 (heure d'hiver), pas de nouvelle sonnerie le 25.
        ZonedDateTime deuxiemePassage = premiere.plusHours(1);
        assertEquals(2, deuxiemePassage.getHour());
        assertEquals(26, a.prochaine(deuxiemePassage).getDayOfMonth());
    }

    @Test
    public void garantieToujoursStrictementApres() {
        for (int masque = 0; masque < 128; masque++) {
            ZonedDateTime maintenant = paris(2026, 10, 7, 9, 0);
            ZonedDateTime p = alarme(9, 0, masque).prochaine(maintenant);
            assertTrue("masque " + masque, p.isAfter(maintenant));
            assertTrue("masque " + masque, p.isBefore(maintenant.plusDays(8)));
        }
    }

    @Test
    public void repete() {
        assertFalse(alarme(7, 0, 0).repete());
        assertTrue(alarme(7, 0, LUNDI).repete());
    }

    @Test
    public void copiesModifiees() {
        Alarme a = new Alarme(4, 7, 15, LUNDI, "Travail", true, true);
        Alarme b = a.avecActive(false).avecHeure(8, 5).avecJours(VENDREDI).avecNom("Sport").avecVibreur(false);
        assertEquals(4, b.id);
        assertEquals(8, b.heure);
        assertEquals(5, b.minute);
        assertEquals(VENDREDI, b.jours);
        assertEquals("Sport", b.nom);
        assertFalse(b.vibreur);
        assertFalse(b.active);
        assertTrue(a.active);
        assertEquals("Travail", a.nom);
    }

    // ---- ListeAlarmes ----

    @Test
    public void allerRetour() {
        List<Alarme> liste = Arrays.asList(
                new Alarme(1, 7, 30, 31, "Travail", true, true),
                new Alarme(2, 23, 59, 0, "", false, false));
        List<Alarme> relue = ListeAlarmes.lire(ListeAlarmes.ecrire(liste));
        assertEquals(2, relue.size());
        assertEquals("Travail", relue.get(0).nom);
        assertEquals(31, relue.get(0).jours);
        assertTrue(relue.get(0).vibreur);
        assertEquals(23, relue.get(1).heure);
        assertEquals(59, relue.get(1).minute);
        assertFalse(relue.get(1).active);
        assertFalse(relue.get(1).vibreur);
    }

    @Test
    public void nomAvecSeparateurSautDeLigneEtBarreOblique() {
        String nom = "a;b\nc\\d;;\r\ne";
        List<Alarme> relue = ListeAlarmes.lire(ListeAlarmes.ecrire(
                Arrays.asList(new Alarme(1, 6, 0, 0, nom, true, true), new Alarme(2, 7, 0, 0, "x", true, true))));
        assertEquals(2, relue.size());
        assertEquals("a;b\nc\\d;;\ne", relue.get(0).nom);
        assertEquals("x", relue.get(1).nom);
    }

    @Test
    public void texteVideOuNul() {
        assertTrue(ListeAlarmes.lire(null).isEmpty());
        assertTrue(ListeAlarmes.lire("").isEmpty());
        assertEquals("", ListeAlarmes.ecrire(new ArrayList<Alarme>()));
    }

    @Test
    public void lignesInvalidesIgnorees() {
        String texte = "1;7;30;31;1;1;Bonne\n"
                + "n'importe quoi\n"
                + "2;25;0;0;1;1;heure impossible\n"
                + "3;7;61;0;1;1;minute impossible\n"
                + "4;7;0;200;1;1;jours impossibles\n"
                + "0;7;0;0;1;1;identifiant nul\n"
                + "1;8;0;0;1;1;identifiant en double\n"
                + "5;x;0;0;1;1;pas un nombre\n"
                + "6;9;0;0\n"
                + "\n"
                + "7;9;45;0;0;0;\n";
        List<Alarme> relue = ListeAlarmes.lire(texte);
        assertEquals(2, relue.size());
        assertEquals("Bonne", relue.get(0).nom);
        assertEquals(7, relue.get(1).id);
        assertEquals("", relue.get(1).nom);
    }

    @Test
    public void prochainId() {
        assertEquals(1, ListeAlarmes.prochainId(new ArrayList<Alarme>()));
        assertEquals(8, ListeAlarmes.prochainId(Arrays.asList(
                new Alarme(3, 7, 0, 0, "", true, true), new Alarme(7, 7, 0, 0, "", true, true))));
    }
}
