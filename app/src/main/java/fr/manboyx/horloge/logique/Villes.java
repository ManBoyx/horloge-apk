package fr.manboyx.horloge.logique;

import java.text.Normalizer;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Villes proposées pour les heures du monde. */
public final class Villes {

    public static final class Ville {
        public final String nom;
        public final String pays;
        /** Identifiant de fuseau, par exemple « Europe/Paris ». */
        public final String zone;

        Ville(String nom, String pays, String zone) {
            this.nom = nom;
            this.pays = pays;
            this.zone = zone;
        }

        /** Identifie la ville dans la mémoire de l'appli. */
        public String cle() {
            return nom + "|" + pays;
        }
    }

    // nom|pays|fuseau. « Europe/Kiev » plutôt que « Europe/Kyiv » : l'ancien
    // nom est connu de toutes les versions d'Android.
    private static final String[] DONNEES = {
        "Abidjan|Côte d'Ivoire|Africa/Abidjan",
        "Accra|Ghana|Africa/Accra",
        "Addis-Abeba|Éthiopie|Africa/Addis_Ababa",
        "Alger|Algérie|Africa/Algiers",
        "Amsterdam|Pays-Bas|Europe/Amsterdam",
        "Anchorage|États-Unis|America/Anchorage",
        "Ankara|Turquie|Europe/Istanbul",
        "Athènes|Grèce|Europe/Athens",
        "Auckland|Nouvelle-Zélande|Pacific/Auckland",
        "Bagdad|Irak|Asia/Baghdad",
        "Bangkok|Thaïlande|Asia/Bangkok",
        "Barcelone|Espagne|Europe/Madrid",
        "Berlin|Allemagne|Europe/Berlin",
        "Beyrouth|Liban|Asia/Beirut",
        "Bogota|Colombie|America/Bogota",
        "Bruxelles|Belgique|Europe/Brussels",
        "Bucarest|Roumanie|Europe/Bucharest",
        "Budapest|Hongrie|Europe/Budapest",
        "Buenos Aires|Argentine|America/Argentina/Buenos_Aires",
        "Calcutta|Inde|Asia/Kolkata",
        "Caracas|Venezuela|America/Caracas",
        "Casablanca|Maroc|Africa/Casablanca",
        "Chicago|États-Unis|America/Chicago",
        "Copenhague|Danemark|Europe/Copenhagen",
        "Dacca|Bangladesh|Asia/Dhaka",
        "Dakar|Sénégal|Africa/Dakar",
        "Delhi|Inde|Asia/Kolkata",
        "Denver|États-Unis|America/Denver",
        "Djakarta|Indonésie|Asia/Jakarta",
        "Doha|Qatar|Asia/Qatar",
        "Dubaï|Émirats arabes unis|Asia/Dubai",
        "Dublin|Irlande|Europe/Dublin",
        "Fort-de-France|Martinique|America/Martinique",
        "Genève|Suisse|Europe/Zurich",
        "Hanoï|Viêt Nam|Asia/Ho_Chi_Minh",
        "Helsinki|Finlande|Europe/Helsinki",
        "Hong Kong|Chine|Asia/Hong_Kong",
        "Honolulu|États-Unis|Pacific/Honolulu",
        "Istanbul|Turquie|Europe/Istanbul",
        "Jérusalem|Israël|Asia/Jerusalem",
        "Johannesbourg|Afrique du Sud|Africa/Johannesburg",
        "Kaboul|Afghanistan|Asia/Kabul",
        "Karachi|Pakistan|Asia/Karachi",
        "Katmandou|Népal|Asia/Kathmandu",
        "Kiev|Ukraine|Europe/Kiev",
        "Kinshasa|RD Congo|Africa/Kinshasa",
        "Kuala Lumpur|Malaisie|Asia/Kuala_Lumpur",
        "Lagos|Nigeria|Africa/Lagos",
        "Le Caire|Égypte|Africa/Cairo",
        "Lima|Pérou|America/Lima",
        "Lisbonne|Portugal|Europe/Lisbon",
        "Londres|Royaume-Uni|Europe/London",
        "Los Angeles|États-Unis|America/Los_Angeles",
        "Luxembourg|Luxembourg|Europe/Luxembourg",
        "Madrid|Espagne|Europe/Madrid",
        "Manille|Philippines|Asia/Manila",
        "Melbourne|Australie|Australia/Melbourne",
        "Mexico|Mexique|America/Mexico_City",
        "Miami|États-Unis|America/New_York",
        "Montréal|Canada|America/Toronto",
        "Moscou|Russie|Europe/Moscow",
        "Mumbai|Inde|Asia/Kolkata",
        "Nairobi|Kenya|Africa/Nairobi",
        "New York|États-Unis|America/New_York",
        "Nouméa|Nouvelle-Calédonie|Pacific/Noumea",
        "Oslo|Norvège|Europe/Oslo",
        "Papeete|Polynésie française|Pacific/Tahiti",
        "Paris|France|Europe/Paris",
        "Pékin|Chine|Asia/Shanghai",
        "Perth|Australie|Australia/Perth",
        "Pointe-à-Pitre|Guadeloupe|America/Guadeloupe",
        "Prague|Tchéquie|Europe/Prague",
        "Québec|Canada|America/Toronto",
        "Reykjavik|Islande|Atlantic/Reykjavik",
        "Rio de Janeiro|Brésil|America/Sao_Paulo",
        "Riyad|Arabie saoudite|Asia/Riyadh",
        "Rome|Italie|Europe/Rome",
        "Saint-Denis|La Réunion|Indian/Reunion",
        "San Francisco|États-Unis|America/Los_Angeles",
        "Santiago|Chili|America/Santiago",
        "São Paulo|Brésil|America/Sao_Paulo",
        "Séoul|Corée du Sud|Asia/Seoul",
        "Shanghai|Chine|Asia/Shanghai",
        "Singapour|Singapour|Asia/Singapore",
        "Stockholm|Suède|Europe/Stockholm",
        "Sydney|Australie|Australia/Sydney",
        "Taipei|Taïwan|Asia/Taipei",
        "Tananarive|Madagascar|Indian/Antananarivo",
        "Téhéran|Iran|Asia/Tehran",
        "Tokyo|Japon|Asia/Tokyo",
        "Toronto|Canada|America/Toronto",
        "Tunis|Tunisie|Africa/Tunis",
        "Vancouver|Canada|America/Vancouver",
        "Varsovie|Pologne|Europe/Warsaw",
        "Vienne|Autriche|Europe/Vienna",
        "Washington|États-Unis|America/New_York",
        "Zurich|Suisse|Europe/Zurich",
    };

    /** Toutes les villes, triées par nom sans tenir compte des accents. */
    public static final List<Ville> TOUTES = charger();

    private Villes() {
    }

    private static List<Ville> charger() {
        List<Ville> villes = new ArrayList<>(DONNEES.length);
        for (String ligne : DONNEES) {
            String[] champs = ligne.split("\\|");
            villes.add(new Ville(champs[0], champs[1], champs[2]));
        }
        Collections.sort(villes, new Comparator<Ville>() {
            @Override
            public int compare(Ville a, Ville b) {
                return normaliser(a.nom).compareTo(normaliser(b.nom));
            }
        });
        return Collections.unmodifiableList(villes);
    }

    /** Minuscules, sans accents ni espaces autour : sert à comparer et à chercher. */
    public static String normaliser(String texte) {
        if (texte == null) {
            return "";
        }
        return Normalizer.normalize(texte, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    /** Villes dont le nom ou le pays contient le texte. Texte vide : toutes. */
    public static List<Ville> chercher(String texte) {
        String cherche = normaliser(texte);
        if (cherche.isEmpty()) {
            return TOUTES;
        }
        List<Ville> trouvees = new ArrayList<>();
        for (Ville v : TOUTES) {
            if (normaliser(v.nom).contains(cherche) || normaliser(v.pays).contains(cherche)) {
                trouvees.add(v);
            }
        }
        return trouvees;
    }

    public static Ville parCle(String cle) {
        if (cle == null) {
            return null;
        }
        for (Ville v : TOUTES) {
            if (v.cle().equals(cle)) {
                return v;
            }
        }
        return null;
    }

    /** Avance (positif) ou retard (négatif) d'un fuseau sur le fuseau local, en minutes. */
    public static int decalageMinutes(String zone, ZoneId locale, Instant instant) {
        try {
            int la = ZoneId.of(zone).getRules().getOffset(instant).getTotalSeconds();
            int ici = locale.getRules().getOffset(instant).getTotalSeconds();
            return (la - ici) / 60;
        } catch (DateTimeException e) {
            return 0;
        }
    }
}
