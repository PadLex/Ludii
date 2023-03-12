package approaches.symbolic.nodes;

import approaches.symbolic.SymbolMapper;
import game.functions.dim.DimConstant;
import main.grammar.Symbol;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

public class PrimitiveNode extends GeneratorNode {
    private Object value;
    PrimitiveNode(Symbol symbol, GeneratorNode parent) {
        super(symbol, parent);
        assert symbol.cls().equals(Integer.class) || symbol.cls().equals(String.class) || symbol.cls().equals(DimConstant.class);
    }
    public void setValue(Object value) {
        this.value = value;
    }
    Object instantiate() {
//        System.out.println(value.getClass());
        for (Constructor<?> constructor: symbol.cls().getConstructors()) {
            try {
                return constructor.newInstance(value);
            } catch (InvocationTargetException | IllegalAccessException | InstantiationException | IllegalArgumentException ignored) {}
        }

        throw new RuntimeException("Failed to compile primitive node: " + symbol + ", " + value + ": " + value.getClass());
    }

    public List<GeneratorNode> nextPossibleParameters(SymbolMapper symbolMapper) {
        return List.of();
    }

    @Override
    public void addParameter(GeneratorNode param) {
        throw new RuntimeException("Primitive nodes are terminal");
    }

    @Override
    public String toString() {
        if (value == null)
            return "?";

        if (value instanceof String)
            return "\"" + value + "\"";

        return value.toString();
    }
}
