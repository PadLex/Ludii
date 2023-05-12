package approaches.symbolic.generators.string;

import approaches.symbolic.SymbolMapper;
import approaches.symbolic.nodes.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Stream;

public class GenerationPath {

    static final Map<String, Set<String>> equivalenceFilters = Map.of(
            "game.functions.ints.IntConstant", Set.of("java.lang.Integer", "game.functions.dim.DimConstant"),
            "java.lang.Integer", Set.of("game.functions.dim.DimConstant"),
            "game.functions.floats.FloatConstant", Set.of("java.lang.Float"),
            "game.functions.booleans.BooleanConstant", Set.of("java.lang.Boolean")
    );

    final SymbolMapper symbolMapper;
    final PrimitiveMatcher primitiveMatcher;

    // Current leaf node being completed
    GeneratorNode current;
    // As strings are appended, the partial parameter is built up until it matches an option.
    String partialParameter = "";

    // The options and nulls lists are kept in sync. The nulls list keeps track of the number of nulls preceding each option.
    List<GeneratorNode> options;
    List<Integer> nulls;

    // Keeps track of whether the last token ended with a bracket. If so, enforce the required space between parameters.
    boolean closedByBracket = false;
    boolean gameDefined = false;

    public GenerationPath(SymbolMapper symbolMapper) {
        this.symbolMapper = symbolMapper;
        this.primitiveMatcher = new PrimitiveMatcher(symbolMapper);
    }

    /**
     * Generates the next possible non-null options. Keeps track of the number of nulls preceding each option using the
     * nulls list. Note: it assumes the partialParameter string is null.
     */
    private void findOptions() {
        assert partialParameter.isEmpty();
        //System.out.println("Finding options for " + current);

        if (!gameDefined) {
            options = List.of(new GameNode());
            nulls = List.of(0);
            return;
        }

        options = new ArrayList<>();
        options.addAll(filterParameters(current.nextPossibleParameters(symbolMapper)));
        // add a zero to the nulls list for each initial option
        nulls = new ArrayList<>(Collections.nCopies(options.size(), 0));


        List<GeneratorNode> emptyNodes = new ArrayList<>();
        while (true) {
            //System.out.println("Options - in progress: " + options);
            //System.out.println("Nulls - in progress: " + nulls);
            GeneratorNode emptyNode = options.stream().filter(node -> node instanceof EmptyNode).findFirst().orElse(null);
            if (emptyNode == null)
                break;
            int index = options.indexOf(emptyNode);
            nulls.remove(index);
            options.remove(index);
            emptyNodes.add(emptyNode);

            List<GeneratorNode> newOptions = filterParameters(current.nextPossibleParameters(symbolMapper, emptyNodes));
            options.addAll(newOptions);
            nulls.addAll(Collections.nCopies(newOptions.size(), emptyNodes.size()));
        }

        //System.out.println("Options: " + options);
        //System.out.println("Nulls: " + nulls);
    }

    public List<GeneratorNode> filterParameters(List<GeneratorNode> parameters) {
        Stream<GeneratorNode> filteredParameters = parameters.stream();

        for (GeneratorNode parameter : parameters) {
            Set<String> unnecessaryPaths = equivalenceFilters.get(parameter.symbol().path());

            if (unnecessaryPaths != null)
                filteredParameters = filteredParameters.filter(p -> !unnecessaryPaths.contains(p.symbol().path()));
        }

        return filteredParameters.toList();
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


        //assert options != null;
        //assert nulls != null;

        if (options == null)
            findOptions();

        // Enforce obligatory space between parameters. Don't add a space between consecutive closing brackets.
        if (closedByBracket && !token.equals(")") && !token.equals("}")) {
            if (token.equals(" ")) {
                GenerationPath path = this.copy();
                path.closedByBracket = false;
                return List.of(path);
            }
            return List.of();
        }


        return switch (token) {
            case " " -> completeParameter();
            case ")", "}" -> completeCurrent(token);
            case "{" -> newList(token);
            default -> extendPartialParameter(token);
        };
    }

    // Completes a parameter and returns the new GenerationPath list
    private List<GenerationPath> completeParameter() {
        if (partialParameter.isEmpty())
            return List.of();

        List<GenerationPath> newPaths = new ArrayList<>();
        for (int i=0; i < options.size(); i++) {
            if (matches(partialParameter, options.get(i))) {
                GenerationPath path = this.copy();
                path.appendOption(i);
                //System.out.println("New Path Space: " + path);
                newPaths.add(path);
            }
        }
        return newPaths;
    }


    // Completes the current node and returns the new GenerationPath list
    private List<GenerationPath> completeCurrent(String token) {
        // Complete the current parameter if there wasn't a space preceding the bracket
        List<GenerationPath> possiblePaths = partialParameter.isEmpty()? List.of(this.copy()) : completeParameter();

        // Identify and complete valid generation paths
        List<GenerationPath> newPaths = new ArrayList<>();
        for (GenerationPath path : possiblePaths) {
            // TODO used to be an if. not sure what could trigger it
            assert path.partialParameter.isEmpty();

            path.findOptions();

            // Verify that the current node can be closed
            GeneratorNode endNode = path.options.stream().filter(node -> node instanceof EndOfClauseNode).findFirst().orElse(null);
            if (endNode == null)
                continue;

            //Verify the bracket corresponds to the correct node type
            if (token.equals(")") && !(path.current instanceof ClassNode || path.current instanceof GameNode))
                continue;

            if (token.equals("}") && !(path.current instanceof ArrayNode))
                continue;

            path.closedByBracket = true;

            // Add the end of clause parameter and it's preceding nulls
            for (int i = 0; i < path.nulls.get(path.options.indexOf(endNode)); i++) {
                path.current.addParameter(new EmptyNode(path.current));
            }
            path.current.addParameter(endNode);

            // Compile the current node and move up to its parent. Filter paths that fail to compile
            path.clearOptions();

            if (path.current.parent() != null) {
                path.current = path.current.parent();
            }

            // Add this path as a possible path
            newPaths.add(path);
        }

        return newPaths;
    }


    private void clearOptions() {
        partialParameter = "";
        options = null;
        nulls = null;
        gameDefined = true;
    }

    // Creates a new list of GenerationPath instances for ArrayNode options
    private List<GenerationPath> newList(String token) {
        // We can't start a new list if we already started building a parameter
        if (!partialParameter.isEmpty())
            return List.of();

        List<GenerationPath> possiblePaths = new ArrayList<>();
        for (int i=0; i < options.size(); i++) {
            if (options.get(i) instanceof ArrayNode) {
                GenerationPath path = this.copy();
                path.appendOption(i);
                possiblePaths.add(path);
            }
        }
        return possiblePaths;
    }

    // Extends the partial parameter and filters out any options that are not consistent with the new partial parameter
    private List<GenerationPath> extendPartialParameter(String token) {
        // check if the new string is consistent with any of the options
        String newPartialParameter = partialParameter + token;
        //System.out.println("New partial parameter: " + newPartialParameter);
        for (GeneratorNode option : options) {
            if (!(option instanceof EndOfClauseNode) && consistent(newPartialParameter, option)) {
                GenerationPath path = this.copy();
                path.partialParameter = newPartialParameter;
                return List.of(path);
            }
        }

        return List.of();
    }

    private void appendOption(int index) {
        GeneratorNode option = options.get(index);
        int nullCount = nulls.get(index);
        if (option instanceof PrimitiveNode)
            ((PrimitiveNode) option).setUnparsedValue(partialParameter.replace("\"", ""));

        //System.out.println("Appending option: " + option);
        if (current == null)
            current = option;
        else {
            for (int i = 0; i < nullCount; i++) {
                current.addParameter(new EmptyNode(current));
            }
            // Necessary in case the parent was cloned after the option was found
            //option.setParent(current);
            current.addParameter(option);
            assert current.parameterSet().contains(option);
            assert current.parent() == null || current.parent().parameterSet().contains(current);
        }

        //System.out.println("Appended: " + current);

        if (!option.isComplete())
            current = option;

        clearOptions();
    }

    public GenerationPath copy() {
        GenerationPath clone = new GenerationPath(symbolMapper);

        if (current != null) {
            //System.out.println("current: "+current);
            clone.current = current.copyUp();
        }

        clone.options = options.stream().map(o -> GeneratorNode.fromSymbol(o.symbol(), clone.current)).toList();
        clone.nulls = new ArrayList<>(nulls);
        clone.partialParameter = partialParameter;
        clone.closedByBracket = closedByBracket;
        clone.gameDefined = gameDefined;
        return clone;
    }

    @Override
    public String toString() {
        return "{partialParameter: \"" + partialParameter + "\", current: " + current + ", options: " + options + "}";
    }

    /**
     * @param string the string to check
     * @param node the node to check against
     * @return true if the string matches the node or could match it if more characters where appended, false otherwise
     */
    private boolean consistent(String string, GeneratorNode node) {
        assert !(node instanceof EmptyNode) && !(node instanceof EndOfClauseNode);
        //System.out.println("Consistent? " + node + ", " + string);

        if (node instanceof PrimitiveNode) {
            Matcher matcher = primitiveMatcher.matcher((PrimitiveNode) node, string);
            return matcher.matches() || matcher.hitEnd();
        }

        String token = node.symbol().token();

        if (node instanceof ClassNode || node instanceof GameNode)
            token = '(' + token;

        if (node instanceof ArrayNode)
            token = '{' + token;

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
            return primitiveMatcher.matcher((PrimitiveNode) node, newString).matches();
        }

        String token = node.symbol().token();

        if (!(node instanceof EnumNode))
            token = "(" + token;

        return token.equals(newString);
    }


}
