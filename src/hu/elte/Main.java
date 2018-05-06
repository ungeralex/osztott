package hu.elte;

import java.util.List;

public class Main {

    private static final String FIRST_AGENT_PATH = "./first";
    private static final String SECOND_AGENT_PATH = "./second";

    public static final String HOST = "localhost";
    public static final int LOWEST_PORT = 20000;
    public static final int HIGHEST_PORT = 20010;

    public static int LOWEST_TIMEOUT;
    public static int HIGHEST_TIMEOUT;

    public static int AGENT_SUM;

    public static void main(String[] args) {
        List<Agent> firstAgents = Util.createAgents(FIRST_AGENT_PATH);
        List<Agent> secondAgents = Util.createAgents(SECOND_AGENT_PATH);

        AGENT_SUM = Integer.parseInt(args[1]);
        LOWEST_TIMEOUT = Integer.parseInt(args[2]);
        HIGHEST_TIMEOUT = Integer.parseInt(args[3]);

//        new Thread(firstAgents.get(0)).start();
//        new Thread(secondAgents.get(0)).start();

        firstAgents.forEach(a -> {
            Thread thread = new Thread(a);
            thread.start();
        });

        secondAgents.forEach(a -> {
            Thread thread = new Thread(a);
            thread.start();
        });
    }
}
