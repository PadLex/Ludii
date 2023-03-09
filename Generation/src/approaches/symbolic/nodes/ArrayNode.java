package approaches.symbolic.nodes;

import approaches.symbolic.SymbolMapper;
import main.grammar.Symbol;

import java.lang.reflect.Array;
import java.util.*;

public class ArrayNode extends GeneratorNode {
    private final List<GeneratorNode> nextSymbols = new ArrayList<>();
    ArrayNode(Symbol symbol) {
        super(symbol);

        assert symbol.nesting() > 0;

        nextSymbols.add(null);
        Symbol childSymbol = new Symbol(symbol);
        childSymbol.setNesting(symbol.nesting() - 1);
        nextSymbols.add(GeneratorNode.fromSymbol(childSymbol));
    }

    public Object compile() {
        List<Object> arguments = parameterSet.stream().filter(Objects::nonNull).map(GeneratorNode::compile).toList();

        System.out.println("\nCompiling: " + this);
        System.out.println("Args value " + arguments);
        System.out.println("Args type  " + arguments.stream().filter(Objects::nonNull).map(Object::getClass).toList());

        Class<?> type;
        if (arguments.size() == 0)
            type = symbol.cls();
        else
            type = getSharedType(arguments);

        System.out.println("array type " + type);

        Object array = Array.newInstance(type, arguments.size());

        System.out.println("array thing " + array);

        for (int i = 0; i < arguments.size(); i++) {
            Array.set(array, i, arguments.get(i));
        }

        return array;
    }

    public List<GeneratorNode> nextPossibleParameters(SymbolMapper symbolMapper) {
        if (parameterSet.get(parameterSet.size() - 1) == null)
            return List.of();

        return Collections.unmodifiableList(nextSymbols);
    }

    @Override
    public String toString() {
        return "{" + String.join(", ", parameterSet.stream().map(s -> s!=null? s.toString() : "").toList()) + "}";
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
