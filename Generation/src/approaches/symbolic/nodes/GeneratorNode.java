package approaches.symbolic.nodes;

import approaches.symbolic.SymbolMapper;
import compiler.Compiler;
import game.Game;
import game.functions.dim.DimConstant;
import main.grammar.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    public static GeneratorNode cloneCallTree(Call root, SymbolMapper symbolMapper) {
        assert root.type() == Call.CallType.Class;
        return cloneCallTree(root, List.of(new ClassNode(root.symbol())), symbolMapper);
    }

    public static GeneratorNode cloneCallTree(Call call, List<GeneratorNode> options, SymbolMapper symbolMapper) {
        System.out.println("options: " + options.stream().map(node -> node==null ? null : node.symbol).toList());
        System.out.println("call: " + call.type() + ", " + call.cls());

        GeneratorNode node = switch (call.type()) {
            case Array -> {
                int nesting = getNesting(call, 0);
                List<Symbol> nestedSymbols = new ArrayList<>();
                getNestedSymbols(call, nestedSymbols);

                System.out.println("nested symbols " + nestedSymbols);

                optionLoop: for (GeneratorNode option: options) {
                    if (option == null)
                        continue;

                    if (option.symbol.nesting() != nesting) {
                        System.out.println(option.symbol.nesting() + " incompatible with " + nesting);
                        continue;
                    }

                    for (Symbol nestedSymbol: nestedSymbols) {
                        assert nestedSymbol != null;
                        System.out.println(option.symbol + " compatible with " + nestedSymbol + "? " + option.symbol.compatibleWith(nestedSymbol));

                        if (!option.symbol.compatibleWith(nestedSymbol))
                            continue optionLoop;
                    }

                    yield option;
                }

                throw new RuntimeException("Could not find a compatible array type");
            }

            case Class, Terminal -> {
                for (GeneratorNode option: options) {
                    if (option == null)
                        continue;

                    if (option instanceof PrimitiveNode) {
                        System.out.println(option.symbol + " compatible with " + call.symbol() + "? " + option.symbol.compatibleWith(call.symbol()));
                        if (option.symbol.compatibleWith(call.symbol())) {
                            ((PrimitiveNode) option).setValue(call.object());
                            yield option;
                        }
                        continue;
                    }

                    if (option.symbol.matches(call.symbol())) {
                        yield option;
                    }
                }

                throw new RuntimeException("Could not find a compatible class");
            }

            case Null -> {
                for (GeneratorNode option : options) {
                    if (option == null)
                        yield null;
                }

                throw new RuntimeException("null is not an option");
            }
        };
        System.out.println("selected: " + (node == null? "null":node.symbol));

        for (Call childCall: call.args()) {
            assert node != null;
            System.out.println("\nparent: " + node.symbol + ", " + node.getClass());
            GeneratorNode child = cloneCallTree(childCall, node.nextPossibleParameters(symbolMapper), symbolMapper);
            node.addParameter(child);
        }

        return node;
    }

    private static int getNesting(Call call, int nesting) {
        if (call.type() != Call.CallType.Array)
            return nesting;

        return getNesting(call.args().get(0), nesting+1);
    }

    private static void getNestedSymbols(Call call, List<Symbol> nestedSymbols) {
        if (call.type() != Call.CallType.Array)
            nestedSymbols.add(call.symbol());
        else
            call.args().forEach(child -> getNestedSymbols(child, nestedSymbols));
    }

    public static void main(String[] args) {
        //System.out.println(Grammar.grammar().findSymbolByPath("game.equipment.Item"));

//        SymbolMapper symbolMapper = new SymbolMapper(Grammar.grammar().symbols());
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

        GeneratorNode rootNode = cloneCallTree(description.callTree(), symbolMapper);
        Game newGame = (Game) rootNode.compile();

        System.out.println("Will it crash? " + newGame.willCrash());
    }
}
