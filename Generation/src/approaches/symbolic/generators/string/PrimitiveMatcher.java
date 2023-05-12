package approaches.symbolic.generators.string;

import approaches.symbolic.SymbolMapper;
import approaches.symbolic.nodes.PrimitiveNode;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PrimitiveMatcher {

    static final String naturalNumber = "(0|([1-9]\\d{0,4}))";
    static final Pattern integerPattern = Pattern.compile("-?" + naturalNumber);
    static final Pattern dimPattern = Pattern.compile(naturalNumber);
    static final Pattern floatPattern = Pattern.compile("-?" + naturalNumber + "\\." + naturalNumber);
    static final Pattern stringPattern = Pattern.compile("\"\\w[\\w\\s]{0,30}\\w\"");
    static final Pattern booleanPattern = Pattern.compile("true|false");

    static final Pattern playerPattern = Pattern.compile("1|2|3|4|5|6|7|8|9|10|11|12|13|14|15|16");

    final SymbolMapper symbolMapper;

    PrimitiveMatcher(SymbolMapper symbolMapper) {
        this.symbolMapper = symbolMapper;
    }

    public Matcher matcher(PrimitiveNode primitiveNode, String newString) {
        //System.out.println("Primitive node: " + primitiveNode.getType() + ", " + newString);
        switch (primitiveNode.getType()) {
            case INT -> {
                if (primitiveNode.parent().symbol().path().equals("game.players.Players"))
                    return playerPattern.matcher(newString);

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
}
