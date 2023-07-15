//package approaches.symbolic.nodes;
//
//import approaches.symbolic.SymbolMapper;
//import grammar.Grammar;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class DefinitionNode extends GeneratorNode {
//
//    static SymbolMapper.MappedSymbol nameSymbol = new SymbolMapper.MappedSymbol(Grammar.grammar().findSymbolByPath("java.lang.String"), null);
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
//        switch (parameterSet.size()) {
//            case 0 -> {
//                return List.of(new PrimitiveNode(nameSymbol, this));
//            }
//            case 1 -> {
//                return List.of(symbolMapper.);
//            }
//            default -> {
//                throw new IllegalStateException("Unexpected state: " + parameterSet.size());
//            }
//        }
//    }
//}
