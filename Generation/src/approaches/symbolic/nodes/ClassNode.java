package approaches.symbolic.nodes;

import approaches.symbolic.SymbolMapper;
import main.grammar.Symbol;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class ClassNode extends GeneratorNode {
    ClassNode(Symbol symbol, GeneratorNode parent) {
        super(symbol, parent);
    }

    Object instantiate() {
        List<Object> arguments = parameterSet.stream().map(param -> param != null? param.compile():null).toList();
//        System.out.println("\nCompiling: " + this);
//        System.out.println("Args value " + arguments);
//        System.out.println("Args type  " + arguments.stream().filter(Objects::nonNull).map(Object::getClass).toList());

        // TODO how to know whether to use constructor or static .construct();
        for (Method method: symbol.cls().getMethods()) {
            if (method.getName().equals("construct")) {
//                System.out.println("Found " + Arrays.toString(method.getParameterTypes()));

                try {
                    return method.invoke(null, arguments.toArray());
                } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
//                    System.out.println("Skipped Method " + e.getMessage());
                }
            }
        }

        for (Constructor<?> constructor: symbol.cls().getConstructors()) {
//            System.out.println("Found " + Arrays.toString(constructor.getParameterTypes()));

            try {
                return constructor.newInstance(arguments.toArray());
            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException |
                     InstantiationException e) {
//                System.out.println("Skipped Constructor " + e.getMessage());
            }
        }

        throw new RuntimeException("Failed to compile: " + symbol);
    }

    public List<GeneratorNode> nextPossibleParameters(SymbolMapper symbolMapper) {
        List<Symbol> partialParameters = parameterSet.stream().map(node -> node.symbol).toList();
        List<Symbol> possibleSymbols = symbolMapper.nextPossibilities(symbol, partialParameters);
        return possibleSymbols.stream().map(s -> GeneratorNode.fromSymbol(s, this)).toList();
    }

    @Override
    public String toString() {
        return "(" + symbol.token() + ": " + String.join(", ", parameterSet.stream().map(GeneratorNode::toString).toList()) + ")";
    }
}
