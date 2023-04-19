package approaches.symbolic.generators;

import approaches.symbolic.SymbolMapper;
import approaches.symbolic.nodes.*;
import game.Game;
import other.GameLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static approaches.symbolic.generators.CallTreeCloner.cloneCallTree;

public class StringGenerator {
    static final Pattern integerPattern = Pattern.compile("-?\\d+");
    static final Pattern dimPattern = Pattern.compile("\\d+");
    static final Pattern floatPattern = Pattern.compile("-?\\d+\\.\\d+");
    static final Pattern stringPattern = Pattern.compile("\"\\w[\\w\\s]*\\w\"");
    static final Pattern booleanPattern = Pattern.compile("true|false");

    final SymbolMapper symbolMapper;

    // Sometimes multiple options match the same string. In such a case, the generation path is cloned and all options
    // are considered. Eventually, a string that is only compatible with one of the generation paths will be appended.
    List<GenerationPath> generationPaths = new ArrayList<>();

    public StringGenerator() {
        this(new SymbolMapper());
    }
    public StringGenerator(SymbolMapper symbolMapper) {
        this.symbolMapper = symbolMapper;
        generationPaths.add(new GenerationPath());
    }

    class GenerationPath {
        // Current leaf node being completed
        GeneratorNode current;
        // As strings are appended, the partial parameter is built up until it matches an option.
        String partialParameter = "";

        // The options and nulls lists are kept in sync. The nulls list keeps track of the number of nulls preceding each option.
        List<GeneratorNode> options;
        List<Integer> nulls;

        // Keeps track of whether the last token ended with a bracket. If so, enforce the required space between parameters.
        boolean closedByBracket = false;

        public GenerationPath() {
            findOptions();
        }

        /**
         * Generates the next possible non-null options. Keeps track of the number of nulls preceding each option using the
         * nulls list. Note: it assumes the partialParameter string is null.
         */
        private void findOptions() {
            assert partialParameter.isEmpty();
            //System.out.println("Finding options for " + current);
            if (current == null) {
                options = List.of(new GameNode());
                nulls = List.of(0);
                return;
            }

            options = new ArrayList<>();
            options.addAll(current.nextPossibleParameters(symbolMapper));
            // add a zero to the nulls list for each initial option
            nulls = new ArrayList<>(IntStream.range(0, options.size()).mapToObj(j -> 0).toList());


            List<GeneratorNode> emptyNodes = new ArrayList<>();
            while (true) {
                //System.out.println("Options - in progress: " + options);
                //System.out.println("Nulls - in progress: " + nulls);
                GeneratorNode emptyNode = options.stream().filter(node -> node instanceof EmptyNode).findFirst().orElse(null);
                if (emptyNode == null)
                    break;
                nulls.remove(options.indexOf(emptyNode));
                options.remove(emptyNode);
                emptyNodes.add(emptyNode);

                List<GeneratorNode> newOptions = current.nextPossibleParameters(symbolMapper, emptyNodes);
                options.addAll(newOptions);
                nulls.addAll(IntStream.range(0, newOptions.size()).mapToObj(j -> emptyNodes.size()).toList());
            }

            //System.out.println("Options: " + options);
            //System.out.println("Nulls: " + nulls);
        }

        /**
         * @param token the token to append
         * @return true if the token was successfully appended, false otherwise
         */
        public List<GenerationPath> append(String token) {
            if (token.length() == 0)
                throw new RuntimeException("Empty token");

            if (token.length() != 1 && (token.indexOf('(') != -1 || token.indexOf(')') != -1 || token.indexOf('{') != -1 || token.indexOf('}') != -1 || token.indexOf(' ') != -1))
                throw new RuntimeException("Multi-character Token contains a special character: " + token);

            assert options != null;
            assert nulls != null;

            System.out.println("\nAppending: " + token);
            //System.out.println("Current: " + current);
            //System.out.println("Options: " + options);

            if (closedByBracket && !token.equals(")") && !token.equals("}")) {
                closedByBracket = false;
                if (token.equals(" "))
                    return List.of(this);
                return List.of();
            }

            // check if token completes a parameter
            if (token.equals(" ")) {
                if (partialParameter.isEmpty())
                    return List.of();

                List<GenerationPath> newPaths = new ArrayList<>();
                for (GeneratorNode option : options) {
                    if (matches(partialParameter, option)) {
                        GenerationPath path = this.copy();
                        path.appendOption(option);
                        System.out.println("New Path Space: " + path);
                        newPaths.add(path);
                    }
                }
                return newPaths;
            }

            if (token.equals(")") || token.equals("}")) {
                List<GenerationPath> possiblePaths = new ArrayList<>();

                // Complete the current parameter if there wasn't a space preceding the bracket
                if (!partialParameter.isEmpty()) {
                    for (GeneratorNode option : options) {
                        if (matches(partialParameter, option)) {
                            GenerationPath path = this.copy();
                            path.appendOption(option);
                            System.out.println("New Path Bracket: " + path);
                            possiblePaths.add(path);
                        }
                    }
                } else {
                    possiblePaths.add(this);
                }

                // Identify and complete valid paths
                List<GenerationPath> newPaths = new ArrayList<>();
                for (GenerationPath path : possiblePaths) {
                    // TODO used to be an if. not sure what could trigger it
                    assert path.partialParameter.isEmpty();

                    // Verify that the current node can be closed
                    GeneratorNode endNode = path.options.stream().filter(node -> node instanceof EndOfClauseNode).findFirst().orElse(null);
                    if (endNode == null)
                        continue;

                    path.closedByBracket = true;

                    // Add the end of clause parameter and it's preceding nulls
                    for (int i = 0; i < path.nulls.get(path.options.indexOf(endNode)); i++) {
                        path.current.addParameter(new EmptyNode(path.current));
                    }
                    path.current.addParameter(endNode);

                    // Complete current node, moving up the tree until a node is incomplete
                    path.completeCurrent();

                    System.out.println("New Path Complete: " + path);

                    // Add this path as a possible path
                    newPaths.add(path);
                }

                return newPaths;
            }

            // deal with arrays
            if (token.equals("{")) {
                List<GenerationPath> possiblePaths = new ArrayList<>();
                for (GeneratorNode option : options) {
                    if (option instanceof ArrayNode) {
                        GenerationPath path = this.copy();
                        path.appendOption(option);
                        possiblePaths.add(path);
                    }
                }
                return possiblePaths;
            }

            // check if the new string is consistent with any of the options
            String newPartialParameter = partialParameter + token;
            System.out.println("New partial parameter: " + newPartialParameter);
            for (GeneratorNode option : options) {
                if (consistent(newPartialParameter, option)) {
                    GenerationPath path = this.copy();
                    path.partialParameter = newPartialParameter;
                    return List.of(path);
                }
            }

            return List.of();
        }


        private void completeCurrent() {
            assert current.isComplete();
            partialParameter = "";
            if (current.parent() == null)
                return;
            current = current.parent();
            System.out.println("Moving up to: " + current);
            if (current.isComplete()) {
                completeCurrent();
            } else {
                findOptions();
            }
        }

        private void appendOption(GeneratorNode option) {
            if (option instanceof PrimitiveNode)
                ((PrimitiveNode) option).setUnparsedValue(partialParameter.replace("\"", ""));

            //System.out.println("Appending option: " + option);
            if (current == null)
                current = option;
            else {
                for (int i = 0; i < nulls.get(options.indexOf(option)); i++) {
                    current.addParameter(new EmptyNode(current));
                }
                // Necessary in case the parent was cloned after the option was found
                option.setParent(current);
                current.addParameter(option);
                assert current.parameterSet().contains(option);
                assert current.parent() == null || current.parent().parameterSet().contains(current);
            }

            System.out.println("Appended: " + current);

            if (!option.isComplete())
                current = option;

            partialParameter = "";
            findOptions();
        }

        public GenerationPath copy() {
            GenerationPath clone = new GenerationPath();

            if (current != null) {
                System.out.println("current: "+current);
                clone.current = current.copyUp();
            }

            clone.options = new ArrayList<>(options);
            clone.nulls = nulls != null ? new ArrayList<>(nulls) : null;
            clone.partialParameter = partialParameter;
            clone.closedByBracket = closedByBracket;
            return clone;
        }

        @Override
        public String toString() {
            return "{partialParameter: '" + partialParameter + "', current: " + current + ", options: " + options + "}";
        }
    }

    /**
     * @param string the string to check
     * @param node the node to check against
     * @return true if the string matches the node or could match it if more characters where appended, false otherwise
     */
    private boolean consistent(String string, GeneratorNode node) {
        assert !(node instanceof EmptyNode);
        //System.out.println("Consistent? " + node + ", " + string);

        if (node instanceof PrimitiveNode) {
            Matcher matcher = primitiveMatcher((PrimitiveNode) node, string);
            return matcher.matches() || matcher.hitEnd();
        }

        String token = node.symbol().token();

        if (!(node instanceof EnumNode))
            token = '(' + token;

        return token.startsWith(string);
    }

    /**
     * @param newString the string to check
     * @param node the node to check against
     * @return true if the string entirely matches the node, false otherwise
     */
    private boolean matches(String newString, GeneratorNode node) {
        assert !(node instanceof EmptyNode);
        //System.out.println("Matching: " + node + ", " + newString);

        if (node instanceof EndOfClauseNode)
            return newString.strip().equals(")");

        if (node instanceof PrimitiveNode) {
            //System.out.println("Matching Primitive: " + node + ", " + newString + ", " + primitiveMatcher((PrimitiveNode) node, newString).matches());
            return primitiveMatcher((PrimitiveNode) node, newString).matches();
        }

        String token = node.symbol().token();

        if (!(node instanceof EnumNode))
            token = "(" + token;

        return token.equals(newString);
    }

    private Matcher primitiveMatcher(PrimitiveNode primitiveNode, String newString) {
        //System.out.println("Primitive node: " + primitiveNode.getType() + ", " + newString);
        switch (primitiveNode.getType()) {
            case INT -> {
                return integerPattern.matcher(newString);
            }
            case DIM -> {
                return dimPattern.matcher(newString);
            }
            case FLOAT -> {
                return floatPattern.matcher(newString);
            }
            case BOOLEAN -> {
                return booleanPattern.matcher(newString);
            }
            case STRING -> {
                return stringPattern.matcher(newString);
            }
            default -> {
                throw new RuntimeException("Unexpected primitive type: " + primitiveNode.symbol().path());
            }
        }
    }
    public static List<String> splitGameDescription(String gameDescription) {
        gameDescription = gameDescription.replaceAll("\\s+", " ");
        gameDescription = gameDescription.replaceAll("\\( ", "(");
        gameDescription = gameDescription.replaceAll("\\{ ", "{");
        gameDescription = gameDescription.replaceAll("\\(\\{ ", "( {");
        gameDescription = gameDescription.replaceAll("\\{\\( ", "{ (");


        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < gameDescription.length(); i++) {
            char c = gameDescription.charAt(i);
            if (c == ' ' || c == '(' || c == ')' || c == '[' || c == ']' || c == '{' || c == '}') {
                if (current.length() > 0) {
                    result.add(current.toString());
                    current.setLength(0);
                }
                if (c == ' ' && (result.isEmpty() || result.get(result.size() - 1).equals(" "))) {
                    continue;
                }
                result.add(String.valueOf(c));
            } else {
                current.append(c);
                if (i < gameDescription.length() - 1 && random.nextBoolean()) {
                    result.add(current.toString());
                    current.setLength(0);
                }
            }
        }

        if (current.length() > 0) {
            result.add(current.toString());
        }

        return result;
    }

    public boolean append(String token) {
        List<GenerationPath> newGenerationPaths = new ArrayList<>();

        for (GenerationPath generationPath: generationPaths) {
            List<GenerationPath> generationPathsForPath = generationPath.append(token);
            System.out.println("Generation paths for path: " + generationPathsForPath);
            newGenerationPaths.addAll(generationPathsForPath);
        }

        if (newGenerationPaths.isEmpty())
            return false;

        generationPaths = newGenerationPaths;
        return true;
    }

    static void stringTest(String gameDescription) {
        List<String> tokens = splitGameDescription(gameDescription);
        System.out.println(tokens);
        StringGenerator generator = new StringGenerator();
        for (String token : tokens) {
            if (!generator.append(token)) {
                System.out.println("\nFailed to append token: " + token);
                break;
            }

            System.out.println("\nAppended token: " + token);
            System.out.println("Path count: " + generator.generationPaths.size());
            System.out.println("Current paths: " + generator.generationPaths.stream().limit(100).toList());

        }
        System.out.println("Terminated on: " + generator.generationPaths.stream().map(p -> p.current).toList());
        System.out.println("Final result: " + generator.generationPaths.stream().map(p -> p.current.root().buildDescription()).toList());
        System.out.println("Expected: " + gameDescription);
        System.out.println("Identical? " + generator.generationPaths.get(0).current.buildDescription().equals(gameDescription));
    }

    public static void main(String[] args) {
        SymbolMapper symbolMapper = new SymbolMapper();
        Game hex = GameLoader.loadGameFromName("Hex.lud");
        System.out.println(hex.description().gameOptions());
        GameNode rootNode = cloneCallTree(hex.description().callTree(), symbolMapper);

        stringTest(rootNode.buildDescription());


    }
}
