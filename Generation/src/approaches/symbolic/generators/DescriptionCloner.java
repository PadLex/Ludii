package approaches.symbolic.generators;

import approaches.symbolic.SymbolMapper;
import approaches.symbolic.nodes.GameNode;
import approaches.symbolic.nodes.GeneratorNode;
import approaches.symbolic.nodes.PrimitiveNode;
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
import java.util.Objects;

public class DescriptionCloner {
    public static GameNode cloneExpandedDescription(String expanded, SymbolMapper symbolMapper) {
        //System.out.println("Expanded:" + expanded);
        List<GeneratorNode> consistentGames = List.of(new GameNode());
        while (true) {
            //System.out.println("\nConsistent games: " + consistentGames.size());
            List<GeneratorNode> newConsistentGames = new ArrayList<>();
            for (GeneratorNode node : consistentGames) {
                //System.out.println("Node: " + node.buildDescription());
                for (GeneratorNode option : node.nextPossibleParameters(symbolMapper)) {
                    //System.out.println("Option: " + option.buildDescription());

                    if (option instanceof PrimitiveNode primitiveOption) {
                        //System.out.println(node.root().buildDescription());
                        String trailingDescription = expanded.substring(node.root().buildDescription().length()).strip();
                        if (option.symbol().label != null) {
                            String prefix = option.symbol().label + ":";
                            if (!trailingDescription.startsWith(prefix))
                                continue;
                            trailingDescription = trailingDescription.substring(prefix.length()).strip();
                        }
                        //System.out.println("Trailing description:" + trailingDescription);

                        switch (primitiveOption.getType()) {
                            case STRING -> {
                                if (trailingDescription.charAt(0) != '"')
                                    continue;

                                int end = trailingDescription.indexOf('"', 1);
                                if (end == -1)
                                    continue;

                                primitiveOption.setValue(trailingDescription.substring(1, end));
                            }
                            case INT, DIM, FLOAT -> {
                                int end = Math.min(Math.min(trailingDescription.indexOf(' '), trailingDescription.indexOf(')')), trailingDescription.indexOf('}'));
                                if (end == -1)
                                    continue;

                                //System.out.println(trailingDescription.substring(0, end));

                                try {
                                    primitiveOption.setUnparsedValue(trailingDescription.substring(0, end));
                                } catch (NumberFormatException e) {
                                    continue;
                                }
                            }
                            case BOOLEAN -> {
                                if (trailingDescription.startsWith("True")) {
                                    primitiveOption.setValue(true);
                                } else if (trailingDescription.startsWith("False")) {
                                    primitiveOption.setValue(false);
                                } else {
                                    continue;
                                }
                            }
                        }

                        //System.out.println("Primitive option: " + primitiveOption);
                        GeneratorNode newNode = node.copyUp();
                        option.setParent(newNode);
                        newNode.addParameter(option);
                        newConsistentGames.add(newNode);

                        //System.out.println("With option:" + newNode.root().buildDescription());
                        //System.out.println("Expanded:" + expanded);
                        //System.out.println("New node:" + newNode.root().buildDescription());
                        assert expanded.startsWith(newNode.root().buildDescription());

                    } else {
                        GeneratorNode newNode = node.copyUp();
                        option.setParent(newNode);
                        newNode.addParameter(option);


                        if (!option.isComplete())
                            newNode = option;
                        else if (newNode.isComplete()) {
                            assert newNode.isRecursivelyComplete();

                            try {
                                newNode.compile();
                            } catch (Exception e) {
                                System.out.println("Could not compile " + newNode);
                                //throw e;
                                continue;
                            }

                            if (newNode instanceof GameNode gameNode) {
                                for (GeneratorNode previous: consistentGames) {
                                    System.out.println("Previous:" + previous.root());
                                }
                                //assert consistentGames.size() == 1;
                                return gameNode;
                            }

                            newNode = newNode.parent();
                        }

                        //System.out.println(expanded.startsWith(newNode.root().buildDescription()) + ":" + newNode.root().buildDescription());

                        if (expanded.startsWith(newNode.root().buildDescription())) {
                            newConsistentGames.add(newNode);
                        }
                    }
                }
            }

            if (newConsistentGames.isEmpty()) {
                System.out.println(expanded);
                System.out.println(consistentGames.get(0).root().buildDescription());
                throw new RuntimeException("No consistent games found ");
            }

            consistentGames = newConsistentGames;
        }
    }

    static void testLudiiLibrary() throws IOException {
        SymbolMapper symbolMapper = new SymbolMapper();

        String gamesRoot = "./Common/res/lud/board";
        List<Path> paths = Files.walk(Paths.get(gamesRoot)).filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".lud")).sorted().limit(2000).toList();
        int count = 0;
        int preCompilation = 0;
        int clone = 0;
        int compile = 0;
        int recompile = 0;
        int fromString = 0;
        for (Path path : paths) {
            String gameStr = Files.readString(path);

            if (gameStr.contains("match"))
                continue;

            System.out.println("Loading " + path.getFileName() + " (" + (count + 1) + " of " + paths.size() + " games)");

            Description description = new Description(gameStr);

            final UserSelections userSelections = new UserSelections(new ArrayList<>());
            final Report report = new Report();

            final long startPreCompilation = System.currentTimeMillis();
            try {
                Compiler.compile(description, userSelections, report, false);
            } catch (Exception e) {
                System.out.println("Could not pre-compile " + path.getFileName());
                continue;
            }
            final long endPreCompilation = System.currentTimeMillis();
            //System.out.println("Old compile: " + (endPreCompilation - startPreCompilation) + "ms");

            //Playground.printCallTree(originalGame.description().callTree(), 0);

            GameNode rootNode;
            try {
                rootNode = cloneExpandedDescription(squish(description.expanded()), symbolMapper);
            } catch (Exception e) {
                System.out.println("Could not clone " + path.getFileName());
                throw e;
            }
            final long endClone = System.currentTimeMillis();
            //System.out.println("Clone: " + (endClone - endPreCompilation) + "ms");

            try {
                rootNode.compile();
            } catch (Exception e) {
                System.out.println("Could not compile " + path.getFileName());
                throw e;
            }
            final long endCompile = System.currentTimeMillis();
            //System.out.println("My Compile: " + (endCompile - endClone) + "ms");

            try {
                rootNode.rulesNode().clearCompilerCache();
                rootNode.compile();
            } catch (Exception e) {
                System.out.println("Could not recompile " + path.getFileName());
                throw e;
            }
            final long endRecompile = System.currentTimeMillis();
            //System.out.println("My Recompile: " + (endRecompile - endCompile) + "ms");

            try {
                Compiler.compile(new Description(rootNode.buildDescription()), new UserSelections(new ArrayList<>()), new Report(), false);
            } catch (Exception e) {
                System.out.println("Could not compile from description " + path.getFileName());
                System.out.println(squish(rootNode.buildDescription()));
                System.out.println(squish(description.expanded()));
                throw e;
                //continue;
            }
            final long endDescription = System.currentTimeMillis();

            count += 1;
            preCompilation += endPreCompilation - startPreCompilation;
            clone += endClone - endPreCompilation;
            compile += endCompile - endClone;
            recompile += endRecompile - endCompile;
            fromString += endDescription - endRecompile;
        }

        System.out.println("Games: " + count);
        System.out.println("Pre-compilation: " + preCompilation + "ms");
        System.out.println("Clone: " + clone + "ms");
        System.out.println("Compile: " + compile + "ms");
        System.out.println("Recompile: " + recompile + "ms");
        System.out.println("From string: " + fromString + "ms");

    }

    static String squish(String str) {
        str = str.replaceAll("\\s+", " ");
        str = str.replaceAll("\\( ", "(");
        str = str.replaceAll(" \\)", ")");
        str = str.replaceAll("\\{ ", "{");
        str = str.replaceAll(" \\}", "}");
        return str;
    }

    public static void main(String[] args) throws IOException {
//        String str =
//                "(game \"Hex\" \n" +
//                        "    (players 2) \n" +
//                        "    (equipment { \n" +
//                        "        (board (hex Diamond 11)) \n" +
//                        "        (piece \"Marker\" Each)\n" +
//                        "        (regions P1 {(sites Side NE) (sites Side SW) })\n" +
//                        "        (regions P2 {(sites Side NW) (sites Side SE) })\n" +
//                        "    }) \n" +
//                        "    (rules \n" +
//                        "        (play (move Add (to (sites Empty))))\n" +
//                        "        (end (if (is Connected Mover) (result Mover Win))) \n" +
//                        "    )\n" +
//                        ")";
//
//        DescriptionCloner.cloneExpandedDescription(squish(str), new SymbolMapper());

        testLudiiLibrary();

    }
}
