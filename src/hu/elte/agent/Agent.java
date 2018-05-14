package hu.elte.agent;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static hu.elte.agent.AgentMain.*;
import static hu.elte.agent.AgentUtil.*;


public class Agent extends Thread {
    // ügynök azonosítója
    private int id;

    //ügynökség azonosítója
    private AgencyEnum agencyEnum;

    //ügynök álnevei
    private List<String> names;

    //elmondott titkok
    private List<String> toldSecrets = new ArrayList<>();

    //ismert titkok
    private List<String> knownSecrets = new ArrayList<>();

    //szerver
    private ServerSocket server;

    //az ügynök le van-e tartóztatva
    private boolean isArrested;

    //ismert ügynökök az ügynökségükkel
    private Map<AgencyEnum, String> agencyWithNames = new HashMap<>();

    //ügynökökhöz tartozó eddigi tippek
    private Map<String, List<Integer>> agentWithGuesses = new HashMap<>();

    //ismert ügynökök azonosítója
    private Map<String, Integer> agentWithId = new HashMap<>();

    public Agent() {
    }

    public Agent(int id, AgencyEnum agencyEnum, List<String> names, String secret) {
        this.id = id;
        this.agencyEnum = agencyEnum;
        this.names = names;
        this.knownSecrets.add(secret);
        this.isArrested = false;
    }

    public AgencyEnum getAgencyEnum() {
        return agencyEnum;
    }

    public void setAgencyEnum(AgencyEnum agencyEnum) {
        this.agencyEnum = agencyEnum;
    }

    public List<String> getNames() {
        return names;
    }

    public void setNames(List<String> names) {
        this.names = names;
    }

    public ServerSocket getServer() {
        return server;
    }

    public void setServer(ServerSocket server) {
        this.server = server;
    }

    public List<String> getToldSecrets() {
        return toldSecrets;
    }

    public void setToldSecrets(List<String> toldSecrets) {
        this.toldSecrets = toldSecrets;
    }

    public List<String> getKnownSecrets() {
        return knownSecrets;
    }

    public void setKnownSecrets(List<String> knownSecrets) {
        this.knownSecrets = knownSecrets;
    }

    @Override
    public void run() {
        //véletlenszerűen választunk egy portot 20000 és 20100 között
        connectToPort();

        System.out.println(this + "started at port: " + this.server.getLocalPort());

        Runnable server = () -> {
            while (!this.isArrested && !isGameOver()) {
                try (
                        Socket socket = this.server.accept();
                        Scanner in = new Scanner(socket.getInputStream());
                        PrintWriter out = new PrintWriter(socket.getOutputStream())
                ) {
                    //várunk egy kapcsolatra
                    socket.setSoTimeout(HIGHEST_TIMEOUT);

                    //a szerver elküldi az álnevei közül az egyiket
                    String name = getRandomName();
                    System.out.println(this + " sending random name: " + name);
                    write(out, name);

                    //a tipp megérkezése
                    String clientGuess = in.nextLine();
                    AgencyEnum guessEnum = AgencyEnum.valueOf(clientGuess);

                    //a tipp ellenőrzése
                    System.out.println(this + " checking client guess");
                    if (guessEnum != this.agencyEnum) {

                        //hibás tipp, ezért bontjuk a kapcsolatot
                        System.out.println("Client guess is incorrect, close socket!");
                        socket.close();

                    } else {
                        //a tipp helyes, ezért egy 'OK' választ küldök vissza
                        System.out.println("Client guess is correct, sending 'OK'");
                        write(out, "OK");

                        //azonos ügynökséghez tartoznak
                        if (in.nextLine().equals("OK")) {
                            //fogadom a titkot
                            String receivedSecret = in.nextLine();
                            System.out.println(this + " receiving secret from client: " + receivedSecret);

                            //ha még nem ismertem ezt a titkot, hozzáadom az ismert titkok közé
                            if (!this.knownSecrets.contains(receivedSecret)) {
                                this.knownSecrets.add(receivedSecret);
                            }

                            //elárulunk mi is egy ismert titkunkat
                            System.out.println(this + " tell a random secret to client!");
                            write(out, getRandomSecret());

                        } else {
                            //fogadom a tippet és ellenőrzöm, hogy helyes-e
                            if (Integer.parseInt(in.nextLine()) == this.id) {

                                //ha helyes, akkor elárulunk egy még el nem árult titkot
                                String randomUntoldSecret = getRandomUntoldSecret();
                                System.out.println(this + " sending random untold secret to client: " + randomUntoldSecret);

                                //felvesszük ezt a titkot az elárult titkok közé
                                this.toldSecrets.add(randomUntoldSecret);

                                //elküldjük a titkot
                                write(out, randomUntoldSecret);

                                //ellenőrizzük, hogy az összes titkunkat elárultuk-e már
                                if (listEqualsIgnoreOrder(this.toldSecrets, this.knownSecrets)) {
                                    this.isArrested = true;

                                    //felvesszük a letartózatott ügynökök közé
                                    addArrestedAgent(this);
                                }
                            }
                        }
                    }


                } catch (IOException e) {
                    //e.printStackTrace();
                }
            }

        };

        Runnable client = () -> {
            while (!this.isArrested && !isGameOver()) {

                //megpróbálunk egy szerverhez kapcsolódni
                try {
                    TimeUnit.MILLISECONDS.sleep(generateRandomInRange(LOWEST_TIMEOUT, HIGHEST_TIMEOUT));
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }

                System.out.println(this + " try to connect server");
                try (
                        Socket socket = new Socket(HOST, getRandomPort());
                        Scanner in = new Scanner(socket.getInputStream());
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
                ) {
                    System.out.println(this + " started at port: " + this.server.getLocalPort());
                    socket.setSoTimeout(generateRandomInRange(LOWEST_TIMEOUT, HIGHEST_TIMEOUT));

                    if (in.hasNextLine()) {

                        //az ügynök nevét fogadom
                        String agentName = in.nextLine();
                        System.out.println(this + " receiving agent random name: " + agentName);

                        AgencyEnum guess = null;

                        //ha már ismerjük az ügynököt akkor jó tippet adunk
                        if (this.agencyWithNames.containsValue(agentName)) {
                            System.out.println(this + " known agent's agency");
                            write(out, getKeyByValue(agentName));
                        } else {
                            //nem találkoztunk még az ügynökkel, ezért tippelünk
                            System.out.println(this + " doesnt know agent's agency");
                            int randomInt = generateRandomInRange(0, 1);
                            guess = randomInt == 0 ? AgencyEnum.FIRST : AgencyEnum.SECOND;
                            System.out.println(this + " sending a guess to server: " + guess);
                            write(out, guess.toString());
                        }

                        //ha van válasz, akkor jó volt a tippünk
                        if (in.hasNextLine()) {
                            if (in.nextLine().equals("OK")) {
                                if (guess != null) {
                                    //felvesszük az ismert ügynökök közé
                                    System.out.println("Add to known names map (Agency,Agent Name): " + guess + " " + agentName);
                                    this.agencyWithNames.put(guess, agentName);
                                }
                            }

                            //ha azonos ügynökséghez tartozik a két ügynök
                            if (this.agencyEnum == guess) {

                                //elküldök egy ismert titkot
                                write(out, "OK");
                                String randomSecret = getRandomSecret();
                                write(out, randomSecret);
                                System.out.println("Tell a random secret to server: " + randomSecret);

                                //fogadom a titkot az ügynöktől
                                String secret = in.nextLine();

                                //hozzáadom az ismert titkaim közé
                                this.knownSecrets.add(secret);

                                //eltávolítom az ügynökség titkai közül
                                addSecretToList(this.agencyEnum, secret);
                            } else {

                                //az ügynök a másik ügynökséghez tartozik
                                write(out, "???");
                                int id;

                                //ha már egyszer tippeltünk egy jót, és ismerjük az ügynök azonosítóját, akkor azt küldjük el tippként
                                //így a tippünk biztosan helyes lesz
                                if (agentWithId.containsKey(agentName)) {
                                    id = agentWithId.get(agentName);
                                } else {
                                    //ha nem találkoztunk még vele akkor tippelünk, de úgy, hogy rossz tippet már nem adunk
                                    id = guessAgentId(agentName);
                                }

                                //elküldjük a tippünket
                                System.out.println(this + " guess agent id: " + id);
                                write(out, id);

                                if (in.hasNextLine()) {

                                    //jól tippeltünk, ezért felvesszük az ügynököt a sorszámával együtt
                                    addAgentWithIdToMap(agentName, id);

                                    //fogadjuk a titkot
                                    String secret = in.nextLine();

                                    //felvesszük az ismert titkok közé
                                    this.knownSecrets.add(secret);

                                    //eltávolítom az ügynökség titkai közül
                                    addSecretToList(this.agencyEnum, secret);
                                } else {
                                    //rosszul tippeltünk ezért felvesszük az ügynökhöz a tippet, hogy ezzel már többet ne tippeljünk
                                    addNewAgentWithGuessesToMap(agentName, id);
                                }
                            }
                        } else if (guess != null) {
                            //rosszul tippeltünk az ügynökséggel kapcsolatban, ezért felvesszük az ellenkező ügynökséggel az ügynököt
                            AgencyEnum agency = getOppositeAgency(guess);
                            System.out.println("Add to known names map (Agency,Agent Name): " + agency + " " + agentName);
                            this.agencyWithNames.put(agency, agentName);
                        }
                    }


                } catch (IOException e) {
                    //e.printStackTrace();
                }
            }

        };

        //létrehozzuk a szálakat
        Thread serverThread = new Thread(server);
        Thread clientThread = new Thread(client);

        //elindítjuk a szálakat
        serverThread.start();
        clientThread.start();

        try {
            serverThread.join();
            clientThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //ha letartóztatták kiírjuk a kimenetre ezt
        if (this.isArrested) {
            System.out.println(this + " has been arrested!");
        }

    }

    private void addNewAgentWithGuessesToMap(String agentName, int id) {
        //ha már tippeltünk ennek az ügynöknek az azonosítójára, hozzáadjuk az új tippet
        if (agentWithGuesses.containsKey(agentName)) {
            List<Integer> guesses = agentWithGuesses.get(agentName);
            guesses.add(id);
        } else {
            //ha még nem tippeltünk, akkor hozzáadjuk ezt az új ügynököt a tippel együtt
            List<Integer> guesses = new ArrayList<>();
            guesses.add(id);
            agentWithGuesses.put(agentName, guesses);
        }
    }

    private void addAgentWithIdToMap(String agentName, int id) {
        //ha még nem találtuk ki az ügynök sorszámát, akkor hozzáadjuk az ügynököt a helyes tippel
        if (!agentWithId.containsKey(agentName)) {
            agentWithId.put(agentName, id);
        }
    }

    private int getRandomPort() {
        int port = generateRandomInRange(LOWEST_PORT, HIGHEST_PORT);
        //addig keresek egy portot, ami nem az enyémmel azonos
        while (port == this.server.getLocalPort()) {
            port = generateRandomInRange(LOWEST_PORT, HIGHEST_PORT);
        }
        return port;
    }

    private String getRandomName() {
        //kiválasztunk a neveink közül egyet véletlenszerűen
        int randomInt = generateRandomInRange(0, this.names.size() - 1);
        return names.get(randomInt);
    }

    private synchronized String getRandomUntoldSecret() {
        //az ismert titkok közül kivonjuk a már elmondott titkainkat
        List<String> untoldSecrets = new ArrayList<>(this.knownSecrets);
        untoldSecrets.removeAll(this.toldSecrets);

        //random választunk egyet ebből a listából
        int randomInt = generateRandomInRange(0, untoldSecrets.size() - 1);
        return untoldSecrets.get(randomInt);
    }

    private synchronized String getRandomSecret() {
        //kiválasztok egyet véletlenszerűen az ismert titkok közül
        int randomInt = generateRandomInRange(0, this.knownSecrets.size() - 1);
        return this.knownSecrets.get(randomInt);
    }

    private synchronized int guessAgentId(String agentName) {
        int id = generateRandomInRange(1, 5);
        //ha már tippeltem az ügynök sorszámára
        if (agentWithGuesses.containsKey(agentName)) {
            List<Integer> guesses = agentWithGuesses.get(agentName);
            //addig generálok véletlenszerű számot, amig olyat számot nem kapok amivel még nem tippeltem
            while (guesses.contains(id)) {
                id = generateRandomInRange(LOWEST_PORT, HIGHEST_PORT);
            }
        }
        return id;
    }

    private void connectToPort() {
        //keresek egy portot 20000 és 20100 között
        while (true) {
            try {
                this.server = new ServerSocket(generateRandomInRange(LOWEST_PORT, HIGHEST_PORT));
                server.setSoTimeout(HIGHEST_TIMEOUT);
                return;
            } catch (IOException e) {
                //e.printStackTrace();
            }
        }
    }

    private AgencyEnum getKeyByValue(String value) {
        //megkapjuk az ügynökséget az ügynök neve alapján
        for (Map.Entry<AgencyEnum, String> entry : this.agencyWithNames.entrySet()) {
            if (value.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void write(PrintWriter out, Object value) {
        //válasz küldése
        out.println(value);
        out.flush();
    }
}
