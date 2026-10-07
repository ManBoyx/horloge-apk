package fr.manboyx.horloge.logique;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Passage d'une liste d'alarmes à du texte et retour, pour la mémoire de
 * l'appli. Une alarme par ligne :
 * id;heure;minute;jours;vibreur;active;nom
 * Le nom est le dernier champ : il peut contenir des points-virgules.
 */
public final class ListeAlarmes {

    private ListeAlarmes() {
    }

    public static String ecrire(List<Alarme> alarmes) {
        StringBuilder sortie = new StringBuilder();
        for (Alarme a : alarmes) {
            if (sortie.length() > 0) {
                sortie.append('\n');
            }
            sortie.append(a.id).append(';')
                    .append(a.heure).append(';')
                    .append(a.minute).append(';')
                    .append(a.jours).append(';')
                    .append(a.vibreur ? 1 : 0).append(';')
                    .append(a.active ? 1 : 0).append(';')
                    .append(proteger(a.nom));
        }
        return sortie.toString();
    }

    /** Les lignes illisibles ou incohérentes sont ignorées. */
    public static List<Alarme> lire(String texte) {
        List<Alarme> alarmes = new ArrayList<>();
        if (texte == null || texte.isEmpty()) {
            return alarmes;
        }
        Set<Integer> vus = new HashSet<>();
        for (String ligne : texte.split("\n")) {
            Alarme a = lireLigne(ligne);
            if (a != null && vus.add(a.id)) {
                alarmes.add(a);
            }
        }
        return alarmes;
    }

    public static int prochainId(List<Alarme> alarmes) {
        int max = 0;
        for (Alarme a : alarmes) {
            max = Math.max(max, a.id);
        }
        return max + 1;
    }

    private static Alarme lireLigne(String ligne) {
        String[] champs = ligne.split(";", 7);
        if (champs.length != 7) {
            return null;
        }
        try {
            int id = Integer.parseInt(champs[0]);
            int heure = Integer.parseInt(champs[1]);
            int minute = Integer.parseInt(champs[2]);
            int jours = Integer.parseInt(champs[3]);
            if (id < 1 || heure < 0 || heure > 23 || minute < 0 || minute > 59
                    || jours < 0 || jours > Alarme.TOUS_LES_JOURS) {
                return null;
            }
            return new Alarme(id, heure, minute, jours, retablir(champs[6]),
                    "1".equals(champs[4]), "1".equals(champs[5]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Le nom tient sur une ligne : les sauts de ligne sont codés « \n ». */
    private static String proteger(String nom) {
        return nom.replace("\r", "").replace("\\", "\\\\").replace("\n", "\\n");
    }

    private static String retablir(String nom) {
        StringBuilder sortie = new StringBuilder(nom.length());
        for (int i = 0; i < nom.length(); i++) {
            char c = nom.charAt(i);
            if (c == '\\' && i + 1 < nom.length()) {
                char suivant = nom.charAt(++i);
                sortie.append(suivant == 'n' ? '\n' : suivant);
            } else {
                sortie.append(c);
            }
        }
        return sortie.toString();
    }
}
