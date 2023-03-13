package approaches.symbolic.nodes;

import approaches.symbolic.SymbolMapper;
import main.grammar.Symbol;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

public class ArrayNode extends GeneratorNode {
    //private final List<GeneratorNode> nextSymbols = new ArrayList<>();
    ArrayNode(Symbol symbol, GeneratorNode parent) {
        super(symbol, parent);

        assert symbol.nesting() > 0;
    }

    Object instantiate() {
        List<Object> arguments = parameterSet.stream().filter(Objects::nonNull).map(GeneratorNode::compile).toList();

//        System.out.println("\nCompiling: " + this);
//        System.out.println("Args value " + arguments);
//        System.out.println("Args type  " + arguments.stream().filter(Objects::nonNull).map(Object::getClass).toList());

        Object array = Array.newInstance(symbol.cls(), arguments.size());

        for (int i = 0; i < arguments.size(); i++) {
            Array.set(array, i, arguments.get(i));
        }

        return array;
    }

    public List<GeneratorNode> nextPossibleParameters(SymbolMapper symbolMapper) {
        if (!parameterSet.isEmpty() && parameterSet.get(parameterSet.size() - 1) == EndOfClauseNode.instance)
            return List.of();

//        System.out.println("nesting: " + symbol.nesting() + ", " + symbol.path() + " <- " + symbolMapper.getCompatibleSymbols(symbol));

        List<GeneratorNode> options = new ArrayList<>();
        if (symbol.nesting() == 1) {
            options.addAll(symbolMapper.getCompatibleSymbols(symbol).stream().filter(s -> s.ludemeType() != Symbol.LudemeType.Structural).map(s -> fromSymbol(s, this)).toList());
        } else {
            Symbol childSymbol = new Symbol(symbol);
            childSymbol.setNesting(symbol.nesting() - 1);
            options.add(GeneratorNode.fromSymbol(childSymbol, this));
        }

        options.add(EndOfClauseNode.instance);
        return options;
    }

    @Override
    public String toString() {
        return "{" + String.join(", ", parameterSet.stream().map(s -> s!=null? s.toString() : "null").toList()) + "}";
    }

    static Class<?> getSharedType(List<Object> objects) {
        Class<?> type = objects.get(0).getClass();
        out: while (true) {
            for (Object o: objects) {
                if (!type.isInstance(o)) {
                    type = type.getSuperclass();
                    continue out;
                }
            }

            return type;
        }
    }
}
