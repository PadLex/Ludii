package approaches.symbolic.nodes;

import approaches.symbolic.SymbolMapper;
import approaches.symbolic.SymbolMapper.MappedSymbol;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ArrayNode extends GeneratorNode {
    ArrayNode(MappedSymbol symbol, GeneratorNode parent) {
        super(symbol, parent);

        assert symbol.nesting() > 0;
    }

    Object instantiate() {
        List<Object> arguments = parameterSet.stream().filter(Objects::nonNull).map(GeneratorNode::compile).toList();

        //System.out.println("Compiling: " + symbol.cls() + "-" + symbol.nesting() + " with " + arguments.stream().map(o -> o==null? null:o.getClass()).toList());

        Object array;
        if (symbol.nesting() == 1)
            array = Array.newInstance(symbol.cls(), arguments.size());
        else {
            array = Array.newInstance(arguments.get(0).getClass(), arguments.size());  // TODO Is this correct?

//            if (int.class.isAssignableFrom(symbol.cls()) || Integer.class.isAssignableFrom(symbol.cls())) {
//                arguments = arguments.stream().map(o -> {
//                    if (o instanceof IntFunction intFunction)
//                        return intFunction;
//                }).toList();
//            }
        }

        for (int i = 0; i < arguments.size(); i++) {
            //System.out.println("Compiling: " + symbol + "-" + symbol.nesting() + " adding: " + arguments.get(i).getClass() + " vs " + array.getClass());
            Array.set(array, i, arguments.get(i));
        }

        return array;
    }

    // TODO do I really need to find all the permutations of IntFunctions and int? Probably not.
    public List<GeneratorNode> nextPossibleParameters(SymbolMapper symbolMapper) {
        if (!parameterSet.isEmpty() && parameterSet.get(parameterSet.size() - 1) instanceof EndOfClauseNode)
            return List.of();

        List<GeneratorNode> options = new ArrayList<>();
        if (symbol.nesting() == 1) {
            switch (symbol.path()) {
                case "int", "float", "boolean" -> {  // TODO not sure this is correct
                    options.add(new PrimitiveNode(new MappedSymbol(symbol, 0, null), this));
                }
                default -> {
                    options.addAll(symbolMapper.getCompatibleSymbols(symbol).stream().map(s -> fromSymbol(new MappedSymbol(s, null), this)).toList());
                }
            }
        } else {
            MappedSymbol childSymbol = new MappedSymbol(symbol, null);
            childSymbol.setNesting(symbol.nesting() - 1);
            options.add(new ArrayNode(childSymbol, this));
        }

        //if (!parameterSet.isEmpty())
        options.add(new EndOfClauseNode(this));

        return options;
    }

    @Override
    public String toString() {
        return "{" + symbol.grammarLabel() + "; " + String.join(" ", parameterSet.stream().map(GeneratorNode::toString).toList()) + "}";
    }

    @Override
    String buildDescription() {
        String label = "";
        if (symbol.label != null)
            label = symbol.label + ":";

        String close = "";
        if (complete)
            close = "}";

        return label + "{" + String.join(" ", parameterSet.stream().filter(s -> !(s instanceof EmptyNode || s instanceof EndOfClauseNode)).map(GeneratorNode::description).toList()) + close;
    }

}
