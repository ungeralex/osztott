package hu.elte;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static hu.elte.Main.*;
import static hu.elte.Util.*;


public class Agent extends Thread {
    private int id;
    private AgencyEnum agencyEnum;
    private List<String> names;
    private List<String> toldSecrets = new ArrayList<>();
    private List<String> knownSecrets = new ArrayList<>();
    private ServerSocket server;
    private boolean isArrested;
    private Map<AgencyEnum, String> agencyWithNames = new HashMap<>();

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
        connectToPort();

        System.out.println(this + "started at port: " + this.server.getLocalPort());

        Runnable server = () -> {
            while (!this.isArrested && !isGameOver()) {
                try (
                        Socket socket = this.server.accept();
                        Scanner in = new Scanner(socket.getInputStream());
                        PrintWriter out = new PrintWriter(socket.getOutputStream())
                ) {
                    socket.setSoTimeout(LOWEST_TIMEOUT);

                    String name = getRandomName();
                    System.out.println(this + " sending random name: " + name);
                    write(out, name);

                    String clientGuess = in.nextLine();
                    AgencyEnum guessEnum = AgencyEnum.valueOf(clientGuess);

                    System.out.println(this + " checking client guess");
                    if (guessEnum != this.agencyEnum) {
                        System.out.println("Client guess is incorrect, close socket!");
                        socket.close();
                    } else {
                        System.out.println("Client guess is correct, sending 'OK'");
                        write(out, "OK");

                        if (in.nextLine().equals("OK")) {
                            String receivedSecret = in.nextLine();
                            System.out.println(this + " receiving secret from client: " + receivedSecret);
                            if (!this.knownSecrets.contains(receivedSecret)) {
                                this.knownSecrets.add(receivedSecret);
                            }
                            System.out.println(this + " tell a random secret to client!");
                            write(out, getRandomSecret());
                        } else {
                            if (Integer.parseInt(in.nextLine()) == this.id) {
                                String randomUntoldSecret = getRandomUntoldSecret();
                                System.out.println(this + " sending random untold secret to client: " + randomUntoldSecret);
                                this.toldSecrets.add(randomUntoldSecret);
                                write(out, randomUntoldSecret);
                                if (listEqualsIgnoreOrder(this.toldSecrets, this.knownSecrets)) {
                                    this.isArrested = true;
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
                    socket.setSoTimeout(LOWEST_TIMEOUT);

                    if (in.hasNextLine()) {

                        String agentName = in.nextLine();
                        System.out.println(this + " receiving agent random name: " + agentName);

                        AgencyEnum guess = null;

                        if (this.agencyWithNames.containsValue(agentName)) {
                            System.out.println(this + " known agent's agency");
                            write(out, getKeyByValue(agentName));
                        } else {
                            System.out.println(this + " doesnt know agent's agency");
                            int randomInt = generateRandomInRange(0, 1);
                            guess = randomInt == 0 ? AgencyEnum.FIRST : AgencyEnum.SECOND;
                            System.out.println(this + " sending a guess to server: " + guess);
                            write(out, guess.toString());
                        }

                        if (in.hasNextLine()) {
                            if (in.nextLine().equals("OK")) {
                                if (guess != null) {
                                    System.out.println("Add to known names map (Agency,Agent Name): " + guess + " " + agentName);
                                    this.agencyWithNames.put(guess, agentName);
                                }
                            }

                            if (this.agencyEnum == guess) {
                                write(out, "OK");
                                String randomSecret = getRandomSecret();
                                write(out, randomSecret);
                                System.out.println("Tell a random secret to server: " + randomSecret);

                                String secret = in.nextLine();
                                this.knownSecrets.add(secret);
                                Util.addSecretToList(guess, secret);
                            } else {
                                write(out, "???");
                                int id = generateRandomInRange(1, 5);
                                System.out.println(this + " guess agent id: " + id);
                                write(out, id);

                                if (in.hasNextLine()) {
                                    this.knownSecrets.add(in.nextLine());
                                }
                            }
                        } else if (guess != null) {
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

        Thread serverThread = new Thread(server);
        Thread clientThread = new Thread(client);

        serverThread.start();
        clientThread.start();

        try {
            serverThread.join();
            clientThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (this.isArrested) {
            System.out.println(this + " has been arrested!");
        }

    }

    private int getRandomPort() {
        int port = generateRandomInRange(LOWEST_PORT, HIGHEST_PORT);
        while (port == this.server.getLocalPort()) {
            port = generateRandomInRange(LOWEST_PORT, HIGHEST_PORT);
        }
        return port;
    }

    private String getRandomName() {
        int randomInt = generateRandomInRange(0, this.names.size() - 1);
        return names.get(randomInt);
    }

    private synchronized String getRandomUntoldSecret() {
        List<String> untoldSecrets = new ArrayList<>(this.knownSecrets);
        untoldSecrets.removeAll(this.toldSecrets);

        int randomInt = generateRandomInRange(0, untoldSecrets.size() - 1);
        return untoldSecrets.get(randomInt);
    }

    private synchronized String getRandomSecret() {
        int randomInt = generateRandomInRange(0, this.knownSecrets.size() - 1);
        return this.knownSecrets.get(randomInt);
    }

    private void connectToPort() {
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
        for (Map.Entry<AgencyEnum, String> entry : this.agencyWithNames.entrySet()) {
            if (value.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void write(PrintWriter out, Object value) {
        out.println(value);
        out.flush();
    }
}
