package hu.elte.agent;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class AgentMain {

    private static final String FIRST_AGENT_PATH = "./first";
    private static final String SECOND_AGENT_PATH = "./second";

    public static final String HOST = "localhost";
    public static final int LOWEST_PORT = 20000;
    public static final int HIGHEST_PORT = 20100;

    public static int LOWEST_TIMEOUT;
    public static int HIGHEST_TIMEOUT;

    public static int AGENT_SUM;

    public static void main(String[] args) {
        List<Agent> firstAgents = AgentUtil.createAgents(FIRST_AGENT_PATH);
        List<Agent> secondAgents = AgentUtil.createAgents(SECOND_AGENT_PATH);

        AGENT_SUM = 5;
        LOWEST_TIMEOUT = Integer.parseInt(args[2]);
        HIGHEST_TIMEOUT = Integer.parseInt(args[3]);

        firstAgents.forEach(Thread::start);

        secondAgents.forEach(Thread::start);

        firstAgents.forEach(a -> {
            try {
                a.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        });

        secondAgents.forEach(a -> {
            try {
                a.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        AgentUtil.printOutWinner();
    }
}
