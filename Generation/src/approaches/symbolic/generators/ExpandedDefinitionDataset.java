package approaches.symbolic.generators;

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
import java.util.ArrayList;
import java.util.List;

import static parser.Expander.expandDefines;

public class ExpandedDefinitionDataset {
    public static void main(String[] args) throws IOException {

        String gamesRoot = "/Users/alex/Documents/Marble/Ludii/Common/res/lud/board";
        String outputRoot = "/Users/alex/PycharmProjects/ludii-lms/games/realised_definitions/";
        List<Path> paths = Files.walk(Paths.get(gamesRoot)).filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".lud")).toList();

        for (Path path : paths) {
            String gameStr = Files.readString(path);

            Description description = new Description(gameStr);

            try {
                Compiler.compile(description, new UserSelections(new ArrayList<>()), new Report(), false);
            } catch (Exception e) {
                System.out.println("Could not compile game " + path.getFileName());
                continue;
            }

            String out = expandDefines(gameStr, new Report(), description.defineInstances());

            Description expandedDescription = new Description(out);

            try {
                final UserSelections userSelections = new UserSelections(new ArrayList<>());
                Compiler.compile(expandedDescription, userSelections, new Report(), false);
            } catch (Exception e) {
                System.out.println("WARNING!! Could not compile expansion " + path.getFileName());
                System.out.println(out);
                continue;
            }


        }

    }

}
