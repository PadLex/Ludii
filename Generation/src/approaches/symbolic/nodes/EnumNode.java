package approaches.symbolic.nodes;

import approaches.symbolic.SymbolMapper;
import approaches.symbolic.SymbolMapper.MappedSymbol;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class EnumNode extends GeneratorNode {
    EnumNode(MappedSymbol symbol, GeneratorNode parent) {
        super(symbol, parent);
    }

    Object instantiate() {
        try {
            return symbol.cls().getMethod("valueOf", String.class).invoke(null, symbol.name());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to compile enum node: " + e);
        }
    }

    public List<GeneratorNode> nextPossibleParameters(SymbolMapper symbolMapper) {
        return List.of();
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public void addParameter(GeneratorNode param) {
        throw new RuntimeException("Enum nodes are terminal");
    }

    @Override
    public String toString() {
        return symbol.grammarLabel();
    }

//    @Override
//    public String buildDescription() {
//        if (symbol.label != null)
//            return symbol.label + ":" + symbol.grammarLabel();
//
//        return symbol.grammarLabel();
//    }
}
