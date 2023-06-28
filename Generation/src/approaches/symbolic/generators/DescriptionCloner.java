package approaches.symbolic.generators;

import approaches.symbolic.SymbolMapper;
import approaches.symbolic.SymbolMapper.MappedSymbol;
import approaches.symbolic.nodes.GameNode;
import approaches.symbolic.nodes.GeneratorNode;
import approaches.symbolic.nodes.PrimitiveNode;
import compiler.Compiler;
import grammar.Grammar;
import main.StringRoutines;
import main.grammar.Description;
import main.grammar.Report;
import main.grammar.Symbol;
import main.options.UserSelections;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static approaches.symbolic.generators.Playground.printCallTree;

public class DescriptionCloner {
    static final Pattern endOfParameter = Pattern.compile("[ )}]");

    //static final List<Symbol> aliases = Grammar.grammar().symbols().stream().filter(Symbol::hasAlias).toList();

    public static GameNode cloneExpandedDescription(String expanded, SymbolMapper symbolMapper) {
//        System.out.println("Expanded:" + expanded);
        List<GeneratorNode> consistentGames = List.of(new GameNode());
        while (true) {
            //System.out.println("\nConsistent games: " + consistentGames.size());
            List<GeneratorNode> newConsistentGames = new ArrayList<>();
            for (GeneratorNode node : consistentGames) {
                //System.out.println("Node: " + node.buildDescription());
                List<GeneratorNode> options = new ArrayList<>(node.nextPossibleParameters(symbolMapper));

                // Include aliases, (^ ... should also include (pow ...
                options.addAll(options.stream().filter(n -> n.symbol().hasAlias()).map(n -> {
                    MappedSymbol noAlias = new MappedSymbol(n.symbol());
                    noAlias.setToken(StringRoutines.toDromedaryCase(noAlias.name()));
//                    System.out.println("Alias: " + noAlias);
                    return GeneratorNode.fromSymbol(noAlias, n.parent());
                }).toList());

                for (GeneratorNode option : options) {
                    //System.out.println("Option: " + option.symbol().path());

                    if (option instanceof PrimitiveNode primitiveOption) {
//                        System.out.println("Primitive option: " + primitiveOption);
//                        System.out.println(node.root().buildDescription());
                        String trailingDescription = expanded.substring(node.root().buildDescription().length()).strip();
                        if (option.symbol().label != null) {
                            String prefix = option.symbol().label + ":";
                            if (!trailingDescription.startsWith(prefix))
                                continue;
                            trailingDescription = trailingDescription.substring(prefix.length()).strip();
                        }
//                        System.out.println("Trailing description:" + trailingDescription);

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
                                Matcher match = endOfParameter.matcher(trailingDescription);

                                if (!match.find())
                                    continue;

                                try {
                                    primitiveOption.setUnparsedValue(trailingDescription.substring(0, match.start()));
                                } catch (NumberFormatException e) {
                                    continue;
                                }
                            }
                            case BOOLEAN -> {
                                if (trailingDescription.startsWith("True")) {
                                    primitiveOption.setUnparsedValue("True");
                                } else if (trailingDescription.startsWith("False")) {
                                    primitiveOption.setUnparsedValue("False");
                                } else {
                                    continue;
                                }
                            }
                        }

//                        System.out.println("Primitive option: " + primitiveOption);
                        GeneratorNode newNode = node.copyUp();
                        option.setParent(newNode);
                        newNode.addParameter(option);
                        newConsistentGames.add(newNode);

//                        System.out.println("Expanded:" + expanded);
//                        System.out.println("New node:" + newNode.root().buildDescription());
                        assert expanded.startsWith(newNode.root().buildDescription());

                    } else {
                        // Non-primitive option

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
//                                System.out.println("Could not compile " + newNode);
//                                System.out.println(e.getMessage());
                                //throw e;
                                continue;
                            }

                            if (newNode instanceof GameNode gameNode) {
                                if (consistentGames.size() > 1) {
                                    System.out.println("WARNING multiple possibilities:");
//                                    for (GeneratorNode previous: consistentGames) {
//                                        System.out.println("Previous:" + previous.root());
//                                    }
                                }

                                //assert consistentGames.size() == 1;
                                return gameNode;
                            }

                            newNode = newNode.parent();
                        }

//                        System.out.println(expanded.startsWith(newNode.root().buildDescription()) + ":" + newNode.root().buildDescription());
                        String newDescription = newNode.root().buildDescription();
                        if (newDescription.length() >= expanded.length())
                            continue;
                        char nextChar = expanded.charAt(newDescription.length());
                        char currentChar = expanded.charAt(newDescription.length() - 1);
                        boolean isEnd = nextChar == ' ' || nextChar == ')' || nextChar == '}' || nextChar == '(' || nextChar == '{' || currentChar == '(' || currentChar == '{';

                        if (isEnd && expanded.startsWith(newDescription)) {
//                            System.out.println("path:" + newNode.symbol().path());
                            newConsistentGames.add(newNode);
                        }
                    }
                }
            }

            if (newConsistentGames.isEmpty()) {
                System.out.println("Expanded:" + expanded);
                consistentGames.forEach(node -> System.out.println("Previous:" + node.root().buildDescription()));
                System.out.println("last type: " + consistentGames.get(0).symbol().path());
                consistentGames.get(0).nextPossibleParameters(symbolMapper).forEach(node -> System.out.println("last option:" + node.symbol().path() + " " + node.symbol().label));
                throw new RuntimeException("No consistent games found");
            }

            if (newConsistentGames.size() > 100) {
                throw new RuntimeException("Too many consistent games found");
            }

            consistentGames = newConsistentGames;
        }
    }

    static void testLudiiLibrary() throws IOException {
        SymbolMapper symbolMapper = new SymbolMapper();

        List<String> skip = List.of("Kriegspiel (Chess).lud"); // "To Kinegi tou Lagou.lud"

        String gamesRoot = "./Common/res/lud/board";
        List<Path> paths = Files.walk(Paths.get(gamesRoot)).filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".lud")).sorted().limit(2000).toList();
        int count = 0;
        int preCompilation = 0;
        int compile = 0;
        int recompile = 0;
        int fromString = 0;
        for (Path path : paths) {
            String gameStr = Files.readString(path);

            if (gameStr.contains("match")) {
                System.out.println("Skipping match " + path.getFileName());
                continue;
            }

            if (skip.contains(path.getFileName().toString())) {
                System.out.println("Skipping " + path.getFileName());
                continue;
            }

            System.out.println("\nLoading " + path.getFileName() + " (" + (count + 1) + " of " + paths.size() + " games)");

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
                rootNode = cloneExpandedDescription(standardize(description.expanded()), symbolMapper);
            } catch (Exception e) {
                System.out.println("Could not clone " + path.getFileName());
                System.out.println(e.getMessage());
                System.out.println("Skipping for now...");
                //throw e;
                continue;
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
                System.out.println(standardize(rootNode.buildDescription()));
                System.out.println(standardize(description.expanded()));
                throw e;
                //continue;
            }
            final long endDescription = System.currentTimeMillis();

            count += 1;
            preCompilation += endPreCompilation - startPreCompilation;
            compile += endCompile - endPreCompilation;
            recompile += endRecompile - endCompile;
            fromString += endDescription - endRecompile;

            System.out.println("pre-compile: " + (endPreCompilation - startPreCompilation) + "ms");
            System.out.println("my-compile: " + (endCompile - endPreCompilation) + "ms");
            System.out.println("my-recompile: " + (endRecompile - endCompile) + "ms");
            System.out.println("from-string: " + (endDescription - endRecompile) + "ms");

        }

        System.out.println("Games: " + count);
        System.out.println("Pre-compilation: " + preCompilation + "ms");
        System.out.println("Compile: " + compile + "ms");
        System.out.println("Recompile: " + recompile + "ms");
        System.out.println("From string: " + fromString + "ms");

    }

    static String standardize(String str) {
        str = str.replaceAll("\\s+", " ");
        str = str.replaceAll("\\( ", "(");
        str = str.replaceAll(" \\)", ")");
        str = str.replaceAll("\\{ ", "{");
        str = str.replaceAll(" \\}", "}");
        str = str.replaceAll("(?<![\\d])\\.(\\d)", "0.$1"); // .5 -> 0.5    //TODO this is not correct 1 is not 1.0
        str = str.replaceAll("(\\d+\\.\\d*?)0+\\b", "$1"); // 0.50 -> 0.5
        str = str.replaceAll("(\\d)+\\.([^0-9])", "$1$2"); // 0. -> 0
        str = str.replaceAll("\\s:\\s", ":"); // (forEach of : (... -> (forEach of:(...

        // Constants
        str = str.replaceAll("([ ({])Off([ )}])", "$1-1$2");
        str = str.replaceAll("([ ({])End([ )}])", "$1-2$2");
        str = str.replaceAll("([ ({])Undefined([ )}])", "$1-1$2");


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
//        Description description = new Description(Files.readString(Path.of("./Common/res/lud/board/space/group/Omega.lud"))); //Omega.lud (alias), Bide.lud (probably infinity)
//        Compiler.compile(description, new UserSelections(new ArrayList<>()), new Report(), false);
//        System.out.println(description.expanded());
//        printCallTree(description.callTree(), 0);
//        GameNode gameNode = cloneExpandedDescription(standardize(description.expanded()), new SymbolMapper());

//        System.out.println(standardize("0.0 hjbhjbjhj 9.70 9.09 (9.0) 8888.000  3.36000 3. (5.0} 9.2 or: 9 (game a  :     (g)"));

    }
}