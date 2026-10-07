package fr.manboyx.horloge.logique;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MinuteurTest {

    @Test
    public void auDepartIlEstArrete() {
        Minuteur m = Minuteur.ARRET_INITIAL;
        assertEquals(Minuteur.Etat.ARRET, m.etat);
        assertEquals(5 * 60_000L, m.dureeMs);
        assertEquals(5 * 60_000L, m.reste(123));
        assertFalse(m.termine(123));
    }

    @Test
    public void demarrerPuisDecompter() {
        Minuteur m = Minuteur.ARRET_INITIAL.demarrer(90_000, 1_000);
        assertEquals(Minuteur.Etat.EN_COURS, m.etat);
        assertEquals(91_000, m.finMs);
        assertEquals(90_000, m.reste(1_000));
        assertEquals(30_000, m.reste(61_000));
        assertFalse(m.termine(90_999));
        assertTrue(m.termine(91_000));
    }

    @Test
    public void finDejaPasseeResteAZeroEtTermine() {
        Minuteur m = Minuteur.ARRET_INITIAL.demarrer(10_000, 0);
        assertEquals(0, m.reste(99_999_999L));
        assertTrue(m.termine(99_999_999L));
    }

    @Test
    public void pauseGeleLeReste() {
        Minuteur m = Minuteur.ARRET_INITIAL.demarrer(60_000, 0).pause(20_000);
        assertEquals(Minuteur.Etat.PAUSE, m.etat);
        assertEquals(40_000, m.reste(20_000));
        assertEquals(40_000, m.reste(500_000));
        assertFalse(m.termine(500_000));
    }

    @Test
    public void reprendreRepartDuReste() {
        Minuteur m = Minuteur.ARRET_INITIAL.demarrer(60_000, 0).pause(20_000).reprendre(100_000);
        assertEquals(Minuteur.Etat.EN_COURS, m.etat);
        assertEquals(140_000, m.finMs);
        assertEquals(40_000, m.reste(100_000));
    }

    @Test
    public void arreterGardeLaDureeChoisie() {
        Minuteur m = Minuteur.ARRET_INITIAL.demarrer(60_000, 0).pause(20_000).arreter();
        assertEquals(Minuteur.Etat.ARRET, m.etat);
        assertEquals(60_000, m.dureeMs);
        assertEquals(60_000, m.reste(999));
    }

    @Test
    public void transitionsSansEffet() {
        Minuteur arrete = Minuteur.ARRET_INITIAL;
        assertEquals(Minuteur.Etat.ARRET, arrete.pause(5).etat);
        assertEquals(Minuteur.Etat.ARRET, arrete.reprendre(5).etat);
        Minuteur enCours = arrete.demarrer(60_000, 0);
        assertEquals(60_000, enCours.reprendre(30_000).finMs);
        // Une durée nulle ou négative ne démarre rien.
        assertEquals(Minuteur.Etat.ARRET, arrete.demarrer(0, 0).etat);
        assertEquals(Minuteur.Etat.ARRET, arrete.demarrer(-5, 0).etat);
        // Mettre en pause un minuteur déjà terminé l'arrête.
        assertEquals(Minuteur.Etat.ARRET, enCours.pause(70_000).etat);
    }

    @Test
    public void allerRetourTexte() {
        Minuteur m = Minuteur.ARRET_INITIAL.demarrer(60_000, 1_000).pause(21_000);
        Minuteur relu = Minuteur.depuisTexte(m.versTexte());
        assertEquals(Minuteur.Etat.PAUSE, relu.etat);
        assertEquals(60_000, relu.dureeMs);
        assertEquals(40_000, relu.reste(0));

        Minuteur enCours = Minuteur.depuisTexte(Minuteur.ARRET_INITIAL.demarrer(60_000, 1_000).versTexte());
        assertEquals(Minuteur.Etat.EN_COURS, enCours.etat);
        assertEquals(61_000, enCours.finMs);
    }

    @Test
    public void texteIllisibleDonneLEtatInitial() {
        for (String s : new String[] {null, "", "x", "EN_COURS;a;b;c", "INCONNU;1;2;3", "PAUSE;1;2", "EN_COURS;-5;10;0"}) {
            Minuteur m = Minuteur.depuisTexte(s);
            assertEquals(String.valueOf(s), Minuteur.Etat.ARRET, m.etat);
            assertEquals(5 * 60_000L, m.dureeMs);
        }
    }
}
