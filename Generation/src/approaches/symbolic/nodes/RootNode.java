//package approaches.symbolic.nodes;
//
//import approaches.symbolic.SymbolMapper;
//
//import java.util.List;
//
//public class RootNode extends GeneratorNode {
//    RootNode(SymbolMapper.MappedSymbol symbol) {
//        super(symbol, null);
//    }
//
//    @Override
//    Object instantiate() {
//        return null;
//    }
//
//    @Override
//    public List<GeneratorNode> nextPossibleParameters(SymbolMapper symbolMapper) {
//        if (complete) return List.of();
//
//        return List.of(new GameNode(), new DefinitionNode());
//    }
//}
