package fr.manboyx.horloge.logique;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public class ChronoTest {

    @Test
    public void videAuDepart() {
        assertFalse(Chrono.VIDE.enCours);
        assertEquals(0, Chrono.VIDE.ecoule(12_345));
        assertTrue(Chrono.VIDE.tours.isEmpty());
    }

    @Test
    public void demarrerPausePuisReprendreCumule() {
        Chrono c = Chrono.VIDE.demarrer(1_000);
        assertTrue(c.enCours);
        assertEquals(2_500, c.ecoule(3_500));
        c = c.pause(3_500);
        assertFalse(c.enCours);
        assertEquals(2_500, c.ecoule(999_999));
        c = c.demarrer(10_000);
        assertEquals(3_500, c.ecoule(11_000));
    }

    @Test
    public void demarrerDeuxFoisNeRepartPasDeZero() {
        Chrono c = Chrono.VIDE.demarrer(1_000).demarrer(5_000);
        assertEquals(6_000, c.ecoule(7_000));
    }

    @Test
    public void toursEnTempsCumule() {
        Chrono c = Chrono.VIDE.demarrer(0).tour(1_200).tour(3_000);
        assertEquals(Arrays.asList(1_200L, 3_000L), c.tours);
        // À l'arrêt, « Tour » ne fait rien.
        assertEquals(2, c.pause(4_000).tour(9_000).tours.size());
    }

    @Test
    public void nombreDeToursPlafonne() {
        Chrono c = Chrono.VIDE.demarrer(0);
        for (int i = 1; i <= 150; i++) {
            c = c.tour(i * 10);
        }
        assertEquals(Chrono.TOURS_MAX, c.tours.size());
    }

    @Test
    public void horlogeQuiReculeNeDonneJamaisDeTempsNegatif() {
        assertEquals(0, Chrono.VIDE.demarrer(5_000).ecoule(1_000));
    }

    @Test
    public void allerRetourTexte() {
        Chrono c = Chrono.VIDE.demarrer(1_000).tour(1_500).pause(4_000).demarrer(9_000).tour(10_000);
        Chrono relu = Chrono.depuisTexte(c.versTexte(), 10_000);
        assertTrue(relu.enCours);
        assertEquals(c.ecoule(12_000), relu.ecoule(12_000));
        assertEquals(c.tours, relu.tours);

        Chrono enPause = Chrono.depuisTexte(Chrono.VIDE.demarrer(0).pause(700).versTexte(), 5);
        assertFalse(enPause.enCours);
        assertEquals(700, enPause.ecoule(123_456));
    }

    @Test
    public void apresRedemarrageDuTelephoneLeChronoEnCoursEstRemisAZero() {
        // L'horloge de référence repart de zéro au redémarrage : « maintenant »
        // se retrouve avant le départ mémorisé.
        String memoire = Chrono.VIDE.demarrer(500_000).versTexte();
        Chrono relu = Chrono.depuisTexte(memoire, 3_000);
        assertFalse(relu.enCours);
        assertEquals(0, relu.ecoule(3_000));
    }

    @Test
    public void texteIllisibleDonneUnChronoVide() {
        for (String s : new String[] {null, "", "x", "1;a;b;", "1;5", "2;0;0;", "0;0;-4;", "0;0;10;5,abc"}) {
            Chrono c = Chrono.depuisTexte(s, 1_000);
            assertFalse(String.valueOf(s), c.enCours);
            assertEquals(String.valueOf(s), 0, c.ecoule(1_000));
            assertTrue(String.valueOf(s), c.tours.isEmpty());
        }
    }
}
