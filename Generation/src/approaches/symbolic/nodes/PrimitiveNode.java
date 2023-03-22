package approaches.symbolic.nodes;

import approaches.symbolic.SymbolMapper;
import game.functions.dim.DimConstant;
import game.functions.floats.FloatConstant;
import game.functions.ints.IntConstant;
import main.grammar.Symbol;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class PrimitiveNode extends GeneratorNode {
    private Object value;



    PrimitiveNode(Symbol symbol, GeneratorNode parent) {
        super(symbol, parent);
    }

    public void setValue(Object value) {
        this.value = value;
    }

    Object instantiate() {
        return value;
    }

    public List<GeneratorNode> nextPossibleParameters(SymbolMapper symbolMapper) {
        return List.of();
    }

    @Override
    public void addParameter(GeneratorNode param) {
        throw new RuntimeException("Primitive nodes are terminal");
    }

    @Override
    public boolean isComplete() {
        return value != null;
    }

    @Override
    public String toString() {
        if (value == null)
            return "?";

        if (value instanceof String)
            return "\"" + value + "\"";

        return value.toString();
    }

    @Override
    public PrimitiveNode copy() {
        PrimitiveNode clone = (PrimitiveNode) super.copy();
        clone.setValue(value);
        return clone;
    }
}
