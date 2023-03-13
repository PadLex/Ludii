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

        // TODO how to know whether to use constructor or static .construct();
        for (Method method: symbol.cls().getMethods()) {
            if (method.getName().equals("construct")) {
                try {
                    return method.invoke(null, arguments.toArray());
                } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException ignored) {}
            }
        }

        for (Constructor<?> constructor: symbol.cls().getConstructors()) {
            try {
                return constructor.newInstance(arguments.toArray());
            } catch (IllegalAccessException | InvocationTargetException | InstantiationException ignored) {}
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
