package approaches.symbolic.generators;

import approaches.symbolic.SymbolMapper;
import approaches.symbolic.nodes.GameNode;
import approaches.symbolic.nodes.GeneratorNode;
import approaches.symbolic.nodes.PrimitiveNode;

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

    static class PartialCompilation {
        final Stack<GeneratorNode> consistentGames;
        final CompilationException exception;

        public PartialCompilation(Stack<GeneratorNode> consistentGames, CompilationException exception) {
            this.consistentGames = consistentGames;
            this.exception = exception;
        }
    }

    static class Completion {
        final String completion;
        final String description;

        public Completion(String completion, String description) {
            this.completion = completion;
            this.description = description;
        }
    }

    // TODO make it consider possibilities bellow the top of the stack
    public static List<Completion> autocomplete(String rawInput, SymbolMapper symbolMapper) {
        String standardInput = standardize(rawInput);
        PartialCompilation partialCompilation = compilePartialDescription(standardInput, symbolMapper);
        GeneratorNode node = partialCompilation.consistentGames.peek();
        List<Completion> completions = new ArrayList<>();

        if (standardInput.chars().filter(c -> c == ' ').count() != node.root().description().chars().filter(c -> c == ' ').count())
            return completions;

//        System.out.println("Autocompleting: " + node.root().description());

        for (GeneratorNode option: node.nextPossibleParameters(symbolMapper, null, false, true)) {

            GeneratorNode newNode = node.copyUp();
            newNode.addParameter(option);

            String description = option.symbol().path();
            String completion = option.description();

            completions.add(new Completion(completion, description));
        }

        return completions;
    }

    public static GameNode compileDescription(String expanded, SymbolMapper symbolMapper) {
        PartialCompilation partialCompilation = compilePartialDescription(expanded, symbolMapper);

//        TODO
//        if (partialCompilation.consistentGames.size() > 1)
//            System.out.println("WARNING multiple possibilities:");

        if (partialCompilation.exception != null)
            throw new RuntimeException(partialCompilation.exception);

        return partialCompilation.consistentGames.peek().root();
    }

    public static PartialCompilation compilePartialDescription(String expanded, SymbolMapper symbolMapper) {
        Stack<GeneratorNode> consistentGames = new Stack<>();
        consistentGames.add(new GameNode());
        return compilePartialDescription(expanded, consistentGames, symbolMapper);
    }

    //TODO optimize by sorting options by frequency and performing dfs (remove option for loop)
    public static PartialCompilation compilePartialDescription(String expanded, Stack<GeneratorNode> consistentGames, SymbolMapper symbolMapper) {
        // If a complete game isn't found, the state of the stack is returned
        Stack<GeneratorNode> lastValidStack = consistentGames;
        Stack<GeneratorNode> currentStack = (Stack<GeneratorNode>) consistentGames.clone();

        // Loop until a consistent game's description matches the expanded description
        while (true) {
            // Since we are performing a depth-first search, we can just pop the most recent partial game
            GeneratorNode node = currentStack.pop();
//            System.out.println("Current node: " + node.root().description());

            // Most intensive operation, it finds all possible options for the next parameter
            List<GeneratorNode> options = new ArrayList<>(node.nextPossibleParameters(symbolMapper, null, true, false));

            CompilationException compilationException = null;

            // Loops through all options and adds them to the stack if they are consistent with the expanded description
            for (GeneratorNode option : options) {
                try {
                    GeneratorNode newNode = appendOption(node, option, expanded);
//                    System.out.println("option: " + option.description() + " " + (newNode != null));
                    if (newNode != null)
                        currentStack.add(newNode);

                    // Successful termination condition
                    if (newNode instanceof GameNode && newNode.isComplete())
                        return new PartialCompilation(currentStack, null);

                } catch (CompilationException e) {
                    System.out.println("Compilation exception: " + e.getMessage());
                    compilationException = e;
                }
            }

            if (currentStack.isEmpty()) {
                if (compilationException == null)
                    compilationException = new CompilationException("Syntax error");

                return new PartialCompilation(lastValidStack, compilationException);
            }

            // TODO is it right
            if (currentStack.peek().root().description().length() > lastValidStack.peek().root().description().length()) {
                lastValidStack = (Stack<GeneratorNode>) currentStack.clone();
//                System.out.println("New valid stack: " + lastValidStack.size());
            }
        }
    }

    static GeneratorNode appendOption(GeneratorNode node, GeneratorNode option, String expanded) throws CompilationException {
        // Parse primitive options
        if (option instanceof PrimitiveNode primitiveOption) {
            String trailingDescription = expanded.substring(node.root().description().length()).strip();
            if (option.symbol().label != null) {
                String prefix = option.symbol().label + ":";
                if (!trailingDescription.startsWith(prefix))
                    return null;
                trailingDescription = trailingDescription.substring(prefix.length()).strip();
            }

            switch (primitiveOption.getType()) {
                case STRING -> {
                    if (trailingDescription.charAt(0) != '"')
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

            assert expanded.startsWith(newNode.root().description());
            return newNode;
        }

        // Parse non-primitive options
        else {
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
                    throw new CompilationException(e.getMessage());
                }

                if (newNode instanceof GameNode)
                    return newNode;

                newNode = newNode.parent();
            }

            String newDescription = newNode.root().description();
//            System.out.println("newDescription: " + newDescription);

            if (newDescription.length() == expanded.length() && newDescription.equals(expanded))
                return newNode;

            if (newDescription.length() < expanded.length()) {
                char nextChar = expanded.charAt(newDescription.length());
                char currentChar = expanded.charAt(newDescription.length() - 1);
                boolean isEnd = nextChar == ' ' || nextChar == ')' || nextChar == '}' || nextChar == '(' || nextChar == '{' || currentChar == '(' || currentChar == '{';

                if (isEnd && expanded.startsWith(newDescription))
                    return newNode;
            }
        }

        return null;
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
                rootNode = compileDescription(standardize(description.expanded()), symbolMapper);
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

    static String standardize(String str) {
        str = str.strip();
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

    public static void communicate() {
        Scanner sc = new Scanner(System.in);
        System.out.println("Ready");

        SymbolMapper symbolMapper = new SymbolMapper();

        while (sc.hasNextLine()) {
            //System.out.println(partialCompilation.consistentGames.peek().description());
            for (Completion completion : autocomplete(sc.nextLine(), symbolMapper)) {
                System.out.print(completion.completion + "|" + completion.description + "||");
            }
            System.out.println();
        }
        sc.close();
    }


    public static void main(String[] args) throws IOException {
//        testLudiiLibrary();
//        Description description = new Description(Files.readString(Path.of("./Common/res/lud/board/space/connection/Hex.lud"))); // TODO Throngs.lud (memory error)
//        Compiler.compile(description, new UserSelections(new ArrayList<>()), new Report(), false);
//        System.out.println(description.expanded());
//        System.out.println(standardize(description.expanded()));
//        //printCallTree(description.callTree(), 0);
//        GameNode gameNode = compileDescription(standardize(description.expanded()), new SymbolMapper());

//        System.out.println(standardize("0.0 hjbhjbjhj 9.70 9.09 (9.0) 8888.000  3.36000 3. (5.0} 9.2 or: 9 (game a  :     (g)"));

//        String full    = "(game \"Hex\" (players 2) (equipment {(board (hex Diamond 11)) (piece \"Marker\" Each) (regions P1 {(sites Side NE) (sites Side SW)}) (regions P2 {(sites Side NW) (sites Side SE)})}) (rules (meta (swap)) (play (move Add (to (sites Empty)))) (end (if (is Connected Mover) (result Mover Win)))))";
//        String partial = "(game \"Hex\" (players 2) (equipment {(board (hex Diamond 11)) (piece \"Marker\" Each";
//        SymbolMapper symbolMapper = new SymbolMapper();
//        compilePartialDescription(standardize(partial), symbolMapper).consistentGames.forEach(n -> System.out.println(n.root().description()));
//
//        for (Completion completion : autocomplete(partial, symbolMapper)) {
//            System.out.print(completion.completion + "|" + completion.description + "||");
//        }
//        System.out.println();

        communicate();
    }
}
