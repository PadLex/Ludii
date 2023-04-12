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
    final SymbolMapper symbolMapper;

    static final Pattern integerPattern = Pattern.compile("-?\\d+");
    static final Pattern dimPattern = Pattern.compile("\\d+");
    static final Pattern floatPattern = Pattern.compile("-?\\d+\\.\\d+");
    static final Pattern stringPattern = Pattern.compile("\"\\w[\\w\\s]*\\w\"");
    static final Pattern booleanPattern = Pattern.compile("true|false");
    GeneratorNode current;
    String partialParameter = "";
    List<GeneratorNode> options;
    List<Integer> nulls;

    // A bit hacky, it's used to keep track of whether the last token ended with a bracket.
    // If so, enforce the required space between parameters.
    boolean closedByBracket = false;

    public StringGenerator() {
        this(new SymbolMapper());
    }
    public StringGenerator(SymbolMapper symbolMapper) {
        this.symbolMapper = symbolMapper;
    }

    /**
     * Generates the next possible non-null options. Keeps track of the number of nulls preceding each option using the
     * nulls list. Note: it assumes the partialParameter string is null.
     */
    private void findOptions() {
        assert partialParameter.isEmpty();
        System.out.println("Finding options for " + current);
        if (current == null) {
            options = List.of(new GameNode());
            nulls = List.of(0);
            return;
        }

        options = new ArrayList<>();
        options.addAll(current.nextPossibleParameters(symbolMapper));
        // add a zero to the nulls list for each initial option
        nulls = new ArrayList<>(IntStream.range(0, options.size()).mapToObj(j -> 0).toList());

        int n = 0;
        while (options.remove(EmptyNode.instance)) {
            n++;
            current.addParameter(EmptyNode.instance);
            List<GeneratorNode> newOptions = current.nextPossibleParameters(symbolMapper);
            options.addAll(newOptions);
            int finalN = n;
            nulls.addAll(IntStream.range(0, newOptions.size()).mapToObj(j -> finalN).toList());
        }

        for (int i = 0; i < n; i++) {
            current.popParameter();
        }
    }

    /**
     * @param token the token to append
     * @return true if the token was successfully appended, false otherwise
     */
    public boolean append(String token) {
        if (token.length() == 0)
            throw new RuntimeException("Empty token");

        if (token.length() != 1 && (token.indexOf('(') != -1 || token.indexOf(')') != -1 || token.indexOf('{') != -1 || token.indexOf('}') != -1 || token.indexOf(' ') != -1))
            throw new RuntimeException("Multi-character Token contains a special character: " + token);

        if (options == null) {
            findOptions();
        }
        System.out.println("\nAppending: " + token);
        System.out.println("Current: " + current);
        System.out.println("Options: " + options);

        if (closedByBracket && !token.equals(")") && !token.equals("}")) {
            closedByBracket = false;
            return token.equals(" ");
        }

        // check if token completes a parameter
        if (token.equals(" ")) {
            if (partialParameter.isEmpty())
                return false;

            for (GeneratorNode option : options) {
                if (matches(partialParameter, option)) {
                    appendOption(option);
                    return true;
                }
            }
            return false;
        }

        if (token.equals(")") || token.equals("}")) {
            if (!partialParameter.isEmpty()) {
                for (GeneratorNode option : options) {
                    if (matches(partialParameter, option)) {
                        appendOption(option);
                        break;
                    }
                }

                if (!partialParameter.isEmpty())
                    return false;

                assert options == null;
                findOptions();
                closedByBracket = true;
            }

            if (!options.contains(EndOfClauseNode.instance))
                return false;

            for (int i = 0; i < nulls.get(options.indexOf(EndOfClauseNode.instance)); i++) {
                current.addParameter(EmptyNode.instance);
            }
            current.addParameter(EndOfClauseNode.instance);
            completeCurrent();
            return true;
        }

        // deal with arrays
        if (token.equals("{")) {
            for (GeneratorNode option : options) {
                if (option instanceof ArrayNode) {
                    appendOption(option);
                    return true;
                }
            }
            return false;
        }

        // check if the new string is consistent with any of the options
        String newPartialParameter = partialParameter + token.strip();
        for (GeneratorNode option : options) {
            if (consistent(newPartialParameter, option)) {
                partialParameter = newPartialParameter;
                return true;
            }
        }

        return false;
    }


    private void completeCurrent() {
        assert current.isComplete();
        options = null;
        partialParameter = "";
        current = current.parent();
        System.out.println("Moving up to: " + current);
        if (current.isComplete()) {
            completeCurrent();
        }
    }

    private void appendOption(GeneratorNode option) {
        if (option instanceof PrimitiveNode)
            ((PrimitiveNode) option).setUnparsedValue(partialParameter.replace("\"", ""));
        System.out.println("Appending option: " + option);
        if (current == null)
            current = option;
        else {
            for (int i = 0; i < nulls.get(options.indexOf(option)); i++) {
                current.addParameter(EmptyNode.instance);
            }
            current.addParameter(option);
        }

        System.out.println("Appended: " + current);

        if (!option.isComplete())
            current = option;

        options = null;
        partialParameter = "";
    }

    /**
     * @param string the string to check
     * @param node the node to check against
     * @return true if the string matches the node or could match it if more characters where appended, false otherwise
     */
    private boolean consistent(String string, GeneratorNode node) {
        assert !(node instanceof EmptyNode);
        System.out.println("Consistent: " + node + ", " + string);

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
        System.out.println("Matching: " + node + ", " + newString);

        if (node instanceof EndOfClauseNode)
            return newString.strip().equals(")");

        if (node instanceof PrimitiveNode) {
            System.out.println("Matching Primitive: " + node + ", " + newString + ", " + primitiveMatcher((PrimitiveNode) node, newString).matches());
            return primitiveMatcher((PrimitiveNode) node, newString).matches();
        }

        String token = node.symbol().token();

        if (!(node instanceof EnumNode))
            token = "(" + token;

        return token.equals(newString);
    }

    private Matcher primitiveMatcher(PrimitiveNode primitiveNode, String newString) {
        System.out.println("Primitive node: " + primitiveNode.getType() + ", " + newString);
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

    static void stringTest(String gameDescription) {
        List<String> tokens = splitGameDescription(gameDescription);
        System.out.println(tokens);
        StringGenerator generator = new StringGenerator();
        for (String token : tokens) {
            if (!generator.append(token)) {
                System.out.println("Failed to append token: " + token);
                break;
            }
        }
        System.out.println("Terminated on: " + generator.current);
        System.out.println("Final result: " + generator.current.root().buildDescription());
    }

    public static void main(String[] args) {
        SymbolMapper symbolMapper = new SymbolMapper();
        Game hex = GameLoader.loadGameFromName("Hex.lud");
        System.out.println(hex.description().gameOptions());
        GameNode rootNode = cloneCallTree(hex.description().callTree(), symbolMapper);

        stringTest(rootNode.buildDescription());

        System.out.println(rootNode.buildDescription());

    }

}
