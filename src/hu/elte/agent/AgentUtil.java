package hu.elte.agent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static hu.elte.agent.AgentMain.*;

public class AgentUtil {

    public static List<Agent> arrestedAgents = new ArrayList<>();
    public static Set<String> firstAgentsSecrets = new HashSet<>();
    public static Set<String> secondAgentsSecrets = new HashSet<>();

    private static final String FIRST_AGENT_REGEX = "^agent1-[0-9]+.*$";

    //ügynökök létrehozása a mappából
    public static List<Agent> createAgents(String folder) {
        List<Agent> agents = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(Paths.get(folder))) {
            paths
                    .filter(Files::isRegularFile)
                    .forEach(p -> agents.add(createAgentFromFile(p)));
            return agents;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return agents;
    }

    //eltávolítjuk az ismert titkot az ügynökség titkai közül
    public static void addSecretToList(AgencyEnum agency, String secret) {
        if (agency == AgencyEnum.FIRST) {
            secondAgentsSecrets.remove(secret);
        } else {
            firstAgentsSecrets.remove(secret);
        }
    }

    //random szám generálása
    public static int generateRandomInRange(int low, int high) {
        return ThreadLocalRandom.current().nextInt(low, high + 1);
    }

    //ellenkező ügynökséget adja vissza a metódus
    public static AgencyEnum getOppositeAgency(AgencyEnum agency) {
        return agency == AgencyEnum.FIRST ? AgencyEnum.SECOND : AgencyEnum.FIRST;
    }

    //megnézi, hogy a két lista egyezik-e
    public static boolean listEqualsIgnoreOrder(List<String> list1, List<String> list2) {
        return new HashSet<>(list1).equals(new HashSet<>(list2));
    }

    //ellenőrzi, hogy véget ért-e a játék
    public static synchronized boolean isGameOver() {

        //megnézzük, hogy az egyik ügynökség minden tagja le van-e tartóztatva
        boolean allAgentIsArrestedFromAgency = checkIfAgencyIsLose(AgencyEnum.FIRST) || checkIfAgencyIsLose(AgencyEnum.SECOND);

        //ha ismerünk egy titkot, ezt eltávolítjuk az ügynökség titkai közül az addSecretToList() metódussal.
        //ha már az összeset ismerjük, akkor a lista mérete 0
        boolean agencyHasAllSecret = firstAgentsSecrets.size() == 0 || secondAgentsSecrets.size() == 0;

        //ha ismerjük az összes titkot, vagy az egyik ügynökség összes ügynöke le van tartóztatva akkor a játéknak vége
        return allAgentIsArrestedFromAgency || agencyHasAllSecret;
    }


    //a győztes ügynökség kiírása
    public static synchronized void printOutWinner() {
        //ha az 'Első' ügynükség összes tagja le van tartóztatva, a 'Második' ügynökség nyert
        if (checkIfAgencyIsLose(AgencyEnum.FIRST)) {
            System.out.println("Second agency won the game!");
            //ha az 'Második' ügynükség összes tagja le van tartóztatva, az 'Első' ügynökség nyert
        } else if (checkIfAgencyIsLose(AgencyEnum.SECOND)) {
            System.out.println("First agency won the game!");
            //az 'Első' ügynökség összes titkát megszerezték, a 'Második' ügynökség nyert
        } else if (firstAgentsSecrets.size() == 0) {
            System.out.println("Second agency won the game!");
            //a 'Második' ügynökség összes titkát megszerezték, az 'Első' ügynökség nyert
        } else {
            System.out.println("First agency won the game!");
        }
    }


    //letartóztatott ügynök felvétele
    public static synchronized void addArrestedAgent(Agent agent) {
        arrestedAgents.add(agent);
    }

    //az összes ügynök le van tartóztatva az ügynökségnél?!
    private static synchronized boolean checkIfAgencyIsLose(AgencyEnum agency) {
        int counter = (int) arrestedAgents.stream().filter(a -> a.getAgencyEnum() == agency).count();

        return counter >= AGENT_SUM;
    }

    //ügynök létrehozása fájlból
    private static Agent createAgentFromFile(Path path) {
        try {
            File file = path.toFile();
            Pattern pattern = Pattern.compile("([0-9]+)-([0-9]+)");
            Matcher matcher = pattern.matcher(path.getFileName().toString());
            matcher.find();

            Scanner sc = new Scanner(file);
            String name = sc.nextLine();
            String[] names = name.split(" ");
            String secret = sc.nextLine();
            int id = Integer.parseInt(matcher.group(2));
            Agent agent;
            agent = path.getFileName().toString().matches(FIRST_AGENT_REGEX) ? new Agent(id, AgencyEnum.FIRST, Arrays.asList(names), secret) : new Agent(id, AgencyEnum.SECOND, Arrays.asList(names), secret);
            if (agent.getAgencyEnum() == AgencyEnum.FIRST) {
                firstAgentsSecrets.add(secret);
            } else {
                secondAgentsSecrets.add(secret);
            }
            return agent;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return new Agent();
    }
}
