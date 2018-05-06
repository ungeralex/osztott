package hu.elte;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Util {

    private static final String FIRST_AGENT_REGEX = "^agent1-[0-9]+.*$";

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

    public static int generateRandomInRange(int low, int high) {
        return ThreadLocalRandom.current().nextInt(low, high + 1);
    }

    public static boolean listEqualsIgnoreOrder(List<String> list1, List<String> list2) {
        return new HashSet<>(list1).equals(new HashSet<>(list2));
    }
    
    public static void shutDownExecutor(ExecutorService executorService) {
            try {
                executorService.shutdown();
                executorService.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                executorService.shutdownNow();
                System.out.println("executor shutDown finishes");
            }
    }

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
            agent = path.getFileName().toString().matches(FIRST_AGENT_REGEX) ?  new Agent(id, AgencyEnum.FIRST, Arrays.asList(names), secret) : new Agent(id, AgencyEnum.SECOND, Arrays.asList(names), secret);
            return agent;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return new Agent();
    }
}
