package approaches.symbolic;

import approaches.symbolic.CachedMapper;
import approaches.symbolic.SymbolMapper;
import approaches.symbolic.nodes.*;

import compiler.Compiler;
import main.grammar.Description;
import main.options.UserSelections;
import main.grammar.Report;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DescriptionParser {
    static final Pattern endOfParameter = Pattern.compile("[ )}]");

    public static class CompilationException extends Exception {
        public CompilationException(String errorMessage) {
            super(errorMessage);
        }
    }

    public static class CompilationState {
        public final GeneratorNode consistentGame;
        public final List<GeneratorNode> remainingOptions;

        public CompilationState(GeneratorNode consistentGame, List<GeneratorNode> remainingOptions) {
            this.consistentGame = consistentGame;
            this.remainingOptions = remainingOptions;
        }
    }

    public static class PartialCompilation {
        public final Stack<CompilationState> consistentGames;
        public final CompilationException exception;

        public PartialCompilation(Stack<CompilationState> consistentGames, CompilationException exception) {
            this.consistentGames = consistentGames;
            this.exception = exception;
        }
    }

    public static GameNode compileDescription(String expanded, SymbolMapper symbolMapper) {
        PartialCompilation partialCompilation = compilePartialDescription(expanded, symbolMapper);

//        TODO
//        if (partialCompilation.consistentGames.size() > 1)
//            System.out.println("WARNING multiple possibilities:");

        if (partialCompilation.exception != null)
            throw new RuntimeException(partialCompilation.exception);

        return partialCompilation.consistentGames.peek().consistentGame.root();
    }

    public static PartialCompilation compilePartialDescription(String expanded, SymbolMapper symbolMapper) {
        Stack<CompilationState> consistentGames = new Stack<>();
        GeneratorNode gameNode = new GameNode();
        List<GeneratorNode> nextOptions = gameNode.nextPossibleParameters(symbolMapper, null, true, false);
        consistentGames.add(new CompilationState(gameNode, nextOptions));
        return compilePartialDescription(expanded, consistentGames, symbolMapper);
    }

    //TODO optimize by sorting options by frequency and performing dfs (remove option for loop)
    public static PartialCompilation compilePartialDescription(String expanded, Stack<CompilationState> consistentGames, SymbolMapper symbolMapper) {
        // If a complete game isn't found, the state of the stack is returned
        Stack<CompilationState> lastValidStack = consistentGames;
        Stack<CompilationState> currentStack = (Stack<CompilationState>) consistentGames.clone();

        CompilationException compilationException = null;

        // Loop until a consistent game's description matches the expanded description
        while (true) {
            // Since we are performing a depth-first search, we can just pop the most recent partial game
            CompilationState state = currentStack.pop();

//            System.out.println(state.consistentGame + " -> " + state.remainingOptions);

//            if (state.remainingOptions.isEmpty()) {
//                System.out.println(state.consistentGame.root().description());
//                System.out.println(state.consistentGame.nextPossibleParameters(symbolMapper, null, true, false));
//            }

            // If there are no more options, we have reached a dead end
            if (state.remainingOptions.size() > 1)
                currentStack.add(new CompilationState(state.consistentGame, state.remainingOptions.subList(1, state.remainingOptions.size())));

            // Loops through all options and adds them to the stack if they are consistent with the expanded description

            try {
                GeneratorNode newNode = appendOption(state.consistentGame, state.remainingOptions.get(0), expanded);
//                    System.out.println("tried option:" + option + " -> " + (newNode != null) + "\n");
                if (newNode != null) {
                    assert !newNode.isComplete() || newNode instanceof GameNode;
                    List<GeneratorNode> nextOptions = newNode.nextPossibleParameters(symbolMapper, null, true, false);
                    currentStack.add(new CompilationState(newNode, nextOptions));
                }

                // Successful termination condition
                if (newNode instanceof GameNode && newNode.isComplete())
                    return new PartialCompilation(currentStack, null);

            } catch (CompilationException e) {
                System.out.println("Compilation exception: " + e.getMessage());
                compilationException = e;
            }

            if (currentStack.isEmpty()) {
                if (compilationException == null)
                    compilationException = new CompilationException("Syntax error");

                return new PartialCompilation(lastValidStack, compilationException);
            }

            // TODO is it right
            if (currentStack.peek().consistentGame.root().description().length() > lastValidStack.peek().consistentGame.root().description().length()) {
                lastValidStack = (Stack<CompilationState>) currentStack.clone();
//                System.out.println("New valid stack: " + lastValidStack.size());
            }
        }
    }

    static GeneratorNode appendOption(GeneratorNode node, GeneratorNode option, String expanded) throws CompilationException {
        String currentDescription = node.root().description();

        if (currentDescription.length() >= expanded.length())
            return null;

        String trailingDescription = expanded.substring(currentDescription.length()).strip();

//        System.out.println("Trying:" + option);
//        System.out.println("Trailing:" + trailingDescription);

        // Parse primitive options
        if (option instanceof PrimitiveNode primitiveOption) {

            // Manually deal with possible labels
            if (option.symbol().label != null) {
                String prefix = option.symbol().label + ":";
                if (!trailingDescription.startsWith(prefix))
                    return null;
                trailingDescription = trailingDescription.substring(prefix.length()).strip();
            }

            switch (primitiveOption.getType()) {
                case STRING -> {
                    if (trailingDescription.isEmpty() || trailingDescription.charAt(0) != '"')
                        return null;

                    int end = trailingDescription.indexOf('"', 1);
                    if (end == -1)
                        return null;

                    primitiveOption.setValue(trailingDescription.substring(1, end));
                }
                case INT, DIM, FLOAT -> {
                    Matcher match = endOfParameter.matcher(trailingDescription);

                    if (!match.find())
                        return null;

                    try {
                        primitiveOption.setUnparsedValue(trailingDescription.substring(0, match.start()));
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
                case BOOLEAN -> { // TODO maybe check if after the True/False there is a space or bracket
                    if (trailingDescription.startsWith("True")) {
                        primitiveOption.setUnparsedValue("True");
                    } else if (trailingDescription.startsWith("False")) {
                        primitiveOption.setUnparsedValue("False");
                    } else {
                        return null;
                    }
                }
            }

            GeneratorNode newNode = node.copyUp();
            option.setParent(newNode);
            newNode.addParameter(option);

//            if (!expanded.startsWith(newNode.root().description())) {
//                System.out.println(expanded);
//                System.out.println(newNode.root().description());
//            }

            assert expanded.startsWith(newNode.root().description());
            return newNode;
        }

        // Parse non-primitive options
        else {
            // option.description accounts for the label already
            if (!(option instanceof EmptyNode) && !(option instanceof EndOfClauseNode)) {
//                System.out.println("Starts:" + trailingDescription.startsWith(option.description()));

                if (!trailingDescription.startsWith(option.description()))
                    return null;

                if (trailingDescription.length() > option.description().length()) {
                    char nextChar = trailingDescription.charAt(option.description().length());
                    char currentChar = trailingDescription.charAt(option.description().length() - 1);
                    boolean isEnd = nextChar == ' ' || nextChar == ')' || nextChar == '}' || nextChar == '(' || nextChar == '{' || currentChar == '(' || currentChar == '{';
//                    System.out.println(nextChar + ", " + currentChar + ", " + isEnd);
                    if (!isEnd)
                        return null;
                }

            }

            if (option instanceof EndOfClauseNode) {
                char currentChar = trailingDescription.charAt(0);
                if (currentChar != ')' && currentChar != '}')
                    return null;
            }

            GeneratorNode nodeCopy = node.copyUp();
            option.setParent(nodeCopy);
            nodeCopy.addParameter(option);

            if (!expanded.startsWith(nodeCopy.root().description())) // If slow, remove for complete games
                return null;

            if (!option.isComplete())
                return option;

            if (nodeCopy.isComplete()) {
                assert nodeCopy.isRecursivelyComplete();

                try {
//                    System.out.println("nodeCopy:" + nodeCopy);
//                    System.out.println(nodeCopy.parameterSet().stream().map(o -> o==null? null:o.symbol().path()).toList());

                    nodeCopy.compile();
                } catch (Exception e) {
//                    throw e;
                    throw new CompilationException(e.getMessage());
                }

                if (nodeCopy instanceof GameNode)
                    return nodeCopy;

                return nodeCopy.parent();
            }

            return nodeCopy;

//            String newDescription = newNode.root().description();
//            System.out.println("newDescription: " + newDescription);

        }
    }

    static void testLudiiLibrary(SymbolMapper symbolMapper, int limit) throws IOException {
        List<String> skip = List.of("Kriegspiel (Chess).lud", "Throngs.lud", "Tai Shogi.lud", "Taikyoku Shogi.lud", "Yonin Seireigi.lud", "Yonin Shogi.lud"); // "To Kinegi tou Lagou.lud"

        String gamesRoot = "./Common/res/lud/board";
        List<Path> paths = Files.walk(Paths.get(gamesRoot)).filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".lud")).sorted().limit(limit).toList();
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
                rootNode = compileDescription(standardize(description.expanded()), symbolMapper);
            } catch (Exception e) {
                System.out.println("Could not compile description " + path.getFileName());
                System.out.println(e.getMessage());
                System.out.println("Skipping for now...");
                //throw e;
                continue;
            }
            final long endCompile = System.currentTimeMillis();
            //System.out.println("My Compile: " + (endCompile - endClone) + "ms");

            try {
                rootNode.rulesNode().clearCache();
                rootNode.compile();
            } catch (Exception e) {
                System.out.println("Could not recompile " + path.getFileName());
                throw e;
            }
            final long endRecompile = System.currentTimeMillis();
            //System.out.println("My Recompile: " + (endRecompile - endCompile) + "ms");

            try {
                Compiler.compile(new Description(rootNode.description()), new UserSelections(new ArrayList<>()), new Report(), false);
            } catch (Exception e) {
                System.out.println("Could not compile from description " + path.getFileName());
                System.out.println(standardize(rootNode.description()));
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

            System.out.println("pre-compile:  " + (endPreCompilation - startPreCompilation) + "ms");
            System.out.println("my-compile:   " + (endCompile - endPreCompilation) + "ms");
            System.out.println("my-recompile: " + (endRecompile - endCompile) + "ms");
            System.out.println("from-string:  " + (endDescription - endRecompile) + "ms");

        }

        System.out.println("Games:           " + count);
        System.out.println("Pre-compilation: " + preCompilation + "ms");
        System.out.println("Compile:         " + compile + "ms");
        System.out.println("Recompile:       " + recompile + "ms");
        System.out.println("From string:     " + fromString + "ms");

    }

    public static String standardize(String str) {
        str = str.strip();
        str = str.replaceAll("\\s+", " ");
        str = str.replace("( ", "(");
        str = str.replace(" )", ")");
        str = str.replace("{ ", "{");
        str = str.replace(" }", "}");
        str = str.replaceAll("\\s:\\s", ":"); // (forEach of : (... -> (forEach of:(...
        str = str.replaceAll("(?<![\\d])\\.(\\d)", "0.$1"); // .5 -> 0.5    //TODO this is not correct 1 is not 1.0
        str = str.replaceAll("(\\d+\\.\\d*?)0+\\b", "$1"); // 0.50 -> 0.5
        str = str.replaceAll("(\\d)+\\.([^0-9])", "$1$2"); // 0. -> 0

        // Constants
        str = str.replaceAll("([ ({])Off([ )}])", "$1-1$2");
        str = str.replaceAll("([ ({])End([ )}])", "$1-2$2");
        str = str.replaceAll("([ ({])Undefined([ )}])", "$1-1$2");

        return str;
    }

    public static String destandardize(String original, String standard) {
        String regex = standard;
        regex = regex.replace(" ", "\\s+");
        regex = regex.replace("(", "\\(\\s*");
        regex = regex.replace(")", "\\s*\\)");
        regex = regex.replace("{", "\\{\\s*");
        regex = regex.replace("}", "\\s*\\}");
        regex = regex.replace(":", "\\s*:\\s*");
        regex = regex.replaceAll("\\d+\\.?\\d*", "\\\\d+\\\\.?\\\\d*");
        regex = regex.replaceAll("[ ({]-1[ )}]", "[ ({](Off)|(-1)[ )}]");
        regex = regex.replaceAll("[ ({]-2[ )}]", "[ ({](End)|(-2)[ )}]");
        regex = regex.replaceAll("[ ({]-1[ )}]", "[ ({](Undefined)|(-1)[ )}]");
//        System.out.println(regex);

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(original);

        if (matcher.lookingAt())
            return original.substring(0, matcher.end());
//        System.out.println("Could not unstandardize");
        return "";
    }

    public static void main(String[] args) throws IOException {
        CachedMapper symbolMapper = new CachedMapper();
        testLudiiLibrary(symbolMapper, 50);
        System.out.println("cache:" + symbolMapper.cachedQueries.size());

//        testLudiiLibrary(symbolMapper, 100);
//        String gameName = "Pagade Kayi Ata (Sixteen-handed)"; // TODO Throngs (memory error), There and Back, Pyrga, There and Back, Kriegspiel (Chess), Tai Shogi
//        Description description = new Description(Files.readString(Path.of("./Common/res/" + GameLoader.getFilePath(gameName))));
//        Compiler.compile(description, new UserSelections(new ArrayList<>()), new Report(), false);
//        System.out.println(description.expanded());
//        System.out.println(standardize(description.expanded()));
////        printCallTree(description.callTree(), 0);
//        GameNode gameNode = compileDescription(standardize(description.expanded()), new SymbolMapper());
//        System.out.println(gameNode.isRecursivelyComplete());

//        System.out.println(standardize("0.0 hjbhjbjhj 9.70 9.09 (9.0) 8888.000  3.36000 3. (5.0} 9.2 or: 9 (game a  :     (g)"));

//        String gameString = "(game \"Hex\" (players 2)\n\n\n (equipment         { (board (hex Diamond 10)) (piece \"Marker\" Each) (regions P1 {(    sites Side NE) (sites Side SW)\n}) (regions P2 {(sites Side NW) (sites Side SE)    }\n\n)\n}    ) (rules (meta (swap\n\n\n)) (play (move \n\nAdd (to (sites \n\nEmpty)))) (end\n\n (if (is Connected      Mover   ) (   result Mover Win))   )))";
//        String standard = standardize(gameString).substring(0, 289);
//        System.out.println(standard);
//        System.out.println(destandardize(gameString, standard));

//        String gameDescription = Files.readString(Path.of("./Common/res/" + GameLoader.getFilePath("Adugo"))).substring(0, 110); //
//        System.out.println(gameDescription);
//        Description description = new Description(gameDescription);
//        Parser.expandAndParse(description, new UserSelections(List.of()), new Report(), true, true);
//        System.out.println("Expanded: " + description.expanded());

    }
}
