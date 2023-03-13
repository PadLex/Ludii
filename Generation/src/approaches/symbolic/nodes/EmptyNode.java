package approaches.symbolic.nodes;

import approaches.symbolic.SymbolMapper;
import main.grammar.Symbol;

import java.util.List;

public class EmptyNode extends GeneratorNode {
    public static final EmptyNode instance = new EmptyNode(SymbolMapper.emptySymbol, null);

    private EmptyNode(Symbol symbol, GeneratorNode parent) {
        super(symbol, parent);
    }

    @Override
    Object instantiate() {
        return null;
    }

    @Override
    public List<GeneratorNode> nextPossibleParameters(SymbolMapper symbolMapper) {
        return List.of();
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public String toString() {
        return "empty";
    }
}
