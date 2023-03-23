package approaches.symbolic.generators;

import approaches.symbolic.SymbolMapper;
import approaches.symbolic.nodes.*;
import grammar.Grammar;
import main.grammar.Symbol;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.regex.Pattern;

public class StringGenerator {
    final SymbolMapper symbolMapper;

    static final Pattern integerPattern = Pattern.compile("-?((\\d*)|(\\d+\\s))");
    static final Pattern dimPattern = Pattern.compile("\\d+\\s?");
    static final Pattern floatPattern = Pattern.compile("-?((\\d*)|(\\d+\\s)|(\\d+\\.\\d*)|(\\d+\\.\\d+\\s))");
    static final Pattern stringPattern = Pattern.compile("\"(\\w[\\w\\s]*\\w\"\\s?)|(\\w[\\w\\s]*)");
    static final Pattern booleanPattern = Pattern.compile("(true )|(true)|(tru)|(tr)|t|(false )|(false)|(fals)|(fal)|(fa)|f");
    GeneratorNode current;
    String currentString;
    List<List<GeneratorNode>> options;

    public StringGenerator(SymbolMapper symbolMapper) {
        this.symbolMapper = symbolMapper;
    }

    private void nextOptions() {
        if (current == null) {
            options = List.of(List.of(new GameNode()));
            return;
        }

        options = new ArrayList<>();
        options.add(current.nextPossibleParameters(symbolMapper));
        while (options.get(options.size()-1).remove(EmptyNode.instance)) {
            current.addParameter(EmptyNode.instance);
            options.add(current.nextPossibleParameters(symbolMapper));
        }
    }

    public boolean canAppend(String token) {
        assert token.equals(" ") || !token.contains(" ");

        if (currentString == null) {
            currentString = "";
            nextOptions();
        }

        String newString = currentString + token;
        for (List<GeneratorNode> optionGroup : options) {
            for (GeneratorNode node : optionGroup) {
                if (consistent(newString, node))
                    return true;
            }
        }

        return false;
    }

    private boolean consistent(String newString, GeneratorNode node) {
        assert !(node instanceof EmptyNode);

        if (node instanceof EndOfClauseNode)
            return newString.endsWith(")");

        if (node instanceof PrimitiveNode) {
            switch (node.symbol().path()) {
                case "java.lang.Integer", "game.functions.ints.IntConstant" -> {
                    return integerPattern.matcher(newString).matches();
                }

                case "game.functions.dim.DimConstant" -> {
                    return dimPattern.matcher(newString).matches();
                }

                case "java.lang.Float", "game.functions.floats.FloatConstant" -> {
                    return floatPattern.matcher(newString).matches();
                }

                case "java.lang.Boolean", "game.functions.booleans.BooleanConstant" -> {
                    return booleanPattern.matcher(newString).matches();
                }

                case "java.lang.String" -> {
                    return stringPattern.matcher(newString).matches();
                }

                default -> {
                    throw new RuntimeException("Unexpected primitive type: " + node.symbol().path());
                }
            }
        }

        String token = node.symbol().token();

        if (!(node instanceof EnumNode))
            token = '(' + token;

        System.out.println(newString + " vs " + token);

        return token.startsWith(newString);
    }

    public static void main(String[] args) {
        List<Symbol> symbols = Grammar.grammar().symbols().stream().filter(s -> s.usedInGrammar() || s.usedInDescription() || !s.usedInMetadata()).toList();
        SymbolMapper symbolMapper = new SymbolMapper(symbols);
        StringGenerator generator = new StringGenerator(symbolMapper);
        System.out.println(generator.canAppend("(ga"));
    }

}
