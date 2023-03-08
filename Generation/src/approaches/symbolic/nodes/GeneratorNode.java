package approaches.symbolic.nodes;

import approaches.symbolic.SymbolMapper;
import compiler.Compiler;
import game.Game;
import game.functions.dim.DimConstant;
import main.grammar.*;

import java.util.ArrayList;
import java.util.List;

import grammar.Grammar;
import main.options.UserSelections;

public abstract class GeneratorNode {
    final Symbol symbol;
    final List<GeneratorNode> parameterSet = new ArrayList<>();

    GeneratorNode(Symbol symbol) {
        assert symbol != null;
        this.symbol = symbol;
    }

    public abstract Object compile();

    public abstract List<GeneratorNode> nextPossibleParameters(SymbolMapper symbolMapper);

    public void addParameter(GeneratorNode param) {
        parameterSet.add(param);
    }

    public static GeneratorNode fromSymbol(Symbol symbol) {
        if (symbol.nesting() > 0) {
            return new ArrayNode(symbol);
        }

        switch (symbol.path()) {
            case "java.lang.Integer", "java.lang.Float", "java.lang.String", "game.functions.dim.DimConstant" -> {
                return new PrimitiveNode(symbol);
            }
        }

        if (symbol.cls().isEnum())
            return new EnumNode(symbol);

        return new ClassNode(symbol);
    }

    public static GeneratorNode cloneCallTree(Call call) {
        switch (call.type()) {
            case Class -> {
                GeneratorNode node = new ClassNode(call.symbol());

                for (Call callArg: call.args()) {
                    node.addParameter(cloneCallTree(callArg));
                }

                return node;
            }

            case Terminal -> {
                if (call.symbol().cls().isEnum()) {
                    return new EnumNode(call.symbol());
                }

                PrimitiveNode node = new PrimitiveNode(call.symbol());
                if (call.object() instanceof DimConstant) {
                    node.setValue(((DimConstant) call.object()).eval());
                } else {
                    node.setValue(call.object());
                }
                return node;
            }

            case Array -> {
                // TODO how to get symbol correctly? This would fail if array was empty or 2D.
                System.out.println(call.args().stream().map(Call::symbol).map(Symbol::cls).toList());
                Symbol newSymbol = new Symbol(call.args().get(0).symbol());
                newSymbol.setNesting(newSymbol.nesting() + 1);
                System.out.println("nesting: " + newSymbol.cls() + " " + newSymbol.nesting());

                GeneratorNode node = new ArrayNode(newSymbol);

                for (Call callArg: call.args()) {
                    node.addParameter(cloneCallTree(callArg));
                }

                return node;
            }

            default -> {return null;}
        }
    }
    public static void main(String[] args) {

        SymbolMapper symbolMapper = new SymbolMapper(Grammar.grammar().symbols().stream().filter(Symbol::usedInGrammar).toList());

//        GeneratorNode hex = new GeneratorNode(Grammar.grammar().findSymbolByPath("game.functions.graph.generators.basis.hex.Hex"));
//
//        List<GeneratorNode> arguments = new ArrayList<>();
//        List<Symbol> nextPossibleSymbols = symbolMapper.nextPossibilities(hex.symbol, arguments.stream().map(GeneratorNode::symbol).toList());
//        System.out.println("nextPossibleSymbols " + nextPossibleSymbols.stream().map(s -> s!=null? s.name():"null").toList());
//
//        GeneratorNode diamond = new GeneratorNode(nextPossibleSymbols.stream().filter(Objects::nonNull).filter(s -> Objects.equals(s.name(), "Diamond")).findFirst().get());
//        GeneratorNode dim = new GeneratorNode(Grammar.grammar().findSymbolByPath("game.functions.dim.DimConstant"));
//        dim.primitiveValue = 11;
//
//        System.out.println("hex " + hex.symbol);
//        System.out.println("diamond " + diamond.symbol);
//        System.out.println("dim " + dim.symbol);
//
//        arguments.add(diamond);
//        arguments.add(dim);
//        arguments.add(null);
//
//        hex.parameterSet = arguments;
//
//        System.out.println(hex.compile());


        String str =
                "(game \"Hex\" \n" +
                        "    (players 2) \n" +
                        "    (equipment { \n" +
                        "        (board (hex Diamond 11)) \n" +
                        "        (piece \"Marker\" Each)\n" +
                        "        (regions P1 {(sites Side NE) (sites Side SW) })\n" +
                        "        (regions P2 {(sites Side NW) (sites Side SE) })\n" +
                        "    }) \n" +
                        "    (rules \n" +
                        "        (play (move Add (to (sites Empty))))\n" +
                        "        (end (if (is Connected Mover) (result Mover Win))) \n" +
                        "    )\n" +
                        ")";

        final Description description = new Description(str);
        final UserSelections userSelections = new UserSelections(new ArrayList<String>());
        final Report report = new Report();

        //Parser.expandAndParse(description, userSelections, report, false);
        Game originalGame = (Game) Compiler.compileTest(description, false);

        GeneratorNode rootNode = cloneCallTree(description.callTree());
        Game newGame = (Game) rootNode.compile();

        System.out.println("Will it crash? " + newGame.willCrash());

    }
}
