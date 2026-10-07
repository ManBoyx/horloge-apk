package fr.manboyx.horloge.logique;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class VillesTest {

    private static boolean contient(List<Villes.Ville> villes, String nom) {
        for (Villes.Ville v : villes) {
            if (v.nom.equals(nom)) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void toutesLesZonesExistent() {
        assertTrue(Villes.TOUTES.size() >= 80);
        for (Villes.Ville v : Villes.TOUTES) {
            assertNotNull(v.nom, ZoneId.of(v.zone));
            assertFalse(v.nom.isEmpty());
            assertFalse(v.pays.isEmpty());
        }
    }

    @Test
    public void aucunDoublon() {
        Set<String> vus = new HashSet<>();
        for (Villes.Ville v : Villes.TOUTES) {
            assertTrue("doublon " + v.cle(), vus.add(v.cle()));
        }
    }

    @Test
    public void listeTrieeParNom() {
        for (int i = 1; i < Villes.TOUTES.size(); i++) {
            String avant = Villes.normaliser(Villes.TOUTES.get(i - 1).nom);
            String apres = Villes.normaliser(Villes.TOUTES.get(i).nom);
            assertTrue(avant + " avant " + apres, avant.compareTo(apres) <= 0);
        }
    }

    @Test
    public void rechercheSansAccentsNiMajuscules() {
        assertTrue(contient(Villes.chercher("sao"), "São Paulo"));
        assertTrue(contient(Villes.chercher("  MONTRÉAL "), "Montréal"));
        assertTrue(contient(Villes.chercher("montreal"), "Montréal"));
        assertTrue(contient(Villes.chercher("tok"), "Tokyo"));
    }

    @Test
    public void rechercheParPays() {
        List<Villes.Ville> usa = Villes.chercher("etats");
        assertTrue(contient(usa, "New York"));
        assertTrue(contient(usa, "Los Angeles"));
        assertFalse(contient(usa, "Tokyo"));
    }

    @Test
    public void rechercheVideDonneTout() {
        assertEquals(Villes.TOUTES.size(), Villes.chercher("").size());
        assertEquals(Villes.TOUTES.size(), Villes.chercher(null).size());
        assertTrue(Villes.chercher("zzzzqq").isEmpty());
    }

    @Test
    public void parCle() {
        Villes.Ville tokyo = Villes.chercher("tokyo").get(0);
        assertEquals("Tokyo", Villes.parCle(tokyo.cle()).nom);
        assertNull(Villes.parCle("Atlantide|Nulle part"));
        assertNull(Villes.parCle(null));
    }

    @Test
    public void decalageEnMinutes() {
        ZoneId paris = ZoneId.of("Europe/Paris");
        Instant janvier = Instant.parse("2026-01-15T12:00:00Z");
        assertEquals(480, Villes.decalageMinutes("Asia/Tokyo", paris, janvier));
        assertEquals(-360, Villes.decalageMinutes("America/New_York", paris, janvier));
        assertEquals(270, Villes.decalageMinutes("Asia/Kolkata", paris, janvier));
        assertEquals(0, Villes.decalageMinutes("Europe/Madrid", paris, janvier));
        // Mi-mars : les États-Unis sont déjà à l'heure d'été, pas l'Europe.
        assertEquals(-300, Villes.decalageMinutes("America/New_York", paris, Instant.parse("2026-03-15T12:00:00Z")));
        // Zone inconnue : pas de plantage.
        assertEquals(0, Villes.decalageMinutes("Nulle/Part", paris, janvier));
    }
}
