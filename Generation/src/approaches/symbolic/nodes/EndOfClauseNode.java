package approaches.symbolic.nodes;

import approaches.symbolic.SymbolMapper;
import main.grammar.Symbol;

import java.util.List;

public class EndOfClauseNode extends GeneratorNode {
    public static final EndOfClauseNode instance = new EndOfClauseNode(SymbolMapper.endOfClauseSymbol, null);
    private EndOfClauseNode(Symbol symbol, GeneratorNode parent) {
        super(symbol, parent);
    }
    @Override
    Object instantiate() {
        throw new RuntimeException("EndOfClauseNode should never be instantiated");
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
        return "END";
    }
}
