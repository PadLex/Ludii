package approaches.symbolic.generators.string;

import approaches.symbolic.SymbolMapper;
import approaches.symbolic.nodes.GameNode;
import compiler.Compiler;
import main.grammar.Description;
import main.grammar.Report;
import main.options.UserSelections;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static approaches.symbolic.generators.CallTreeCloner.cloneCallTree;

public class DatasetBuilder {

    public static void saveDataset(List<Map<String, String>> dataset, String outputFile) throws IOException {
        Path outputPath = Paths.get(outputFile);
        Files.createDirectories(outputPath.getParent()); // Create parent directories if they don't exist
        Files.deleteIfExists(outputPath); // Delete existing file if it exists

        Files.write(outputPath, List.of("["), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        for (int i = 0; i < dataset.size(); i++) {
            Map<String, String> game = dataset.get(i);

            List<String> gameStr = game.entrySet().stream().map(y -> "\"" + y.getKey() + "\": \"" + y.getValue() + "\",").collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

            String last = gameStr.get(gameStr.size() - 1);
            gameStr.set(gameStr.size() - 1, last.substring(0, last.length() - 1)); // Remove last comma
            gameStr.add(0, "{");

            if (i == dataset.size() - 1)
                gameStr.add("}");
            else
                gameStr.add("},");

            Files.write(outputPath, gameStr, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
        Files.write(outputPath, List.of("]"), StandardOpenOption.CREATE, StandardOpenOption.APPEND);


//        Files.write(outputPath, List.of("{",
//                "\"allowMatch\": " + allowMatch + ",",
//                "\"maxGameLength\": " + maxGameLength + ",",
//                "\"maxGames\": " + maxGames + ",",
//                "\"data\": ["
//        ), StandardOpenOption.CREATE, StandardOpenOption.APPEND);


    }

    public static List<Map<String, String>> buildDataset(String gamesRoot, boolean allowMatch, int maxGameLength, int maxGames) throws IOException {
        SymbolMapper symbolMapper = new SymbolMapper();

        System.out.println("Working Directory = " + System.getProperty("user.dir"));

        List<Path> gamePaths = Files.walk(Paths.get(gamesRoot)).filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".lud")).toList();

        List<Map<String, String>> dataset = new ArrayList<>();

        int appendedGames = 0;
        for (Path gamePath : gamePaths) {

            if (appendedGames >= maxGames)
                break;

            String gameStr = Files.readString(gamePath);
            System.out.println("Processing " + gamePath.getFileName() + " - " + appendedGames + "/" + gamePaths.size());

            if (gameStr.contains("match"))
                continue;

            Description description = new Description(gameStr);

            final UserSelections userSelections = new UserSelections(new ArrayList<>());
            final Report report = new Report();

            try {
                Compiler.compile(description, userSelections, report, false);
            } catch (Exception e) {
                System.out.println("Could not pre-compile " + gamePath.getFileName());
                continue;
            }

            //printCallTree(description.callTree(), 0);
            //System.out.println("Game description:\n" + description.raw());

            GameNode rootNode;
            try {
                rootNode = cloneCallTree(description.callTree(), symbolMapper);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Could not clone " + gamePath.getFileName());
                continue;
            }

            String rules = description.metadata();
            rules = rules.substring(rules.indexOf("(rules \"") + 8);
            rules = rules.substring(0, rules.indexOf("\")"));

            // TODO - remove this when we can handle games with multiple rulesets
            if (rules.contains("\""))
                continue;

            String game = rootNode.buildDescription();
            if (game.length() > maxGameLength)
                continue;

            HashMap<String, String> map = new HashMap<>();
            map.put("instruction", "Construct a Ludii game following the provided rules");
            map.put("input", rules.replace('\n', ' '));
            map.put("output", game.replaceAll("\"", "\\\\\""));

            dataset.add(map);

            appendedGames++;
//            String comma = appendedGames < maxGames ? "," : "";
//            Files.write(outputPath, List.of("{\"description\": \"" + meta + "\",", "\"LudiiCode\": \"" + game + "\"}" + comma), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }

        System.out.println("Appended " + appendedGames + " games to dataset");

//        Files.write(outputPath, List.of("]", "}"), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        return dataset;
    }

    public static void main(String[] args) throws IOException {
        saveDataset(buildDataset("./Common/res/lud/board/", false, 10000, 3000), "./Output/dataset.json");
    }
}
