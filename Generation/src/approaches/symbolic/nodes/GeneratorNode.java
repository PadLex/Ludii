package approaches.symbolic.nodes;

import approaches.random.Generator;
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
    GeneratorNode parent;
    final List<GeneratorNode> parameterSet = new ArrayList<>();

    Object compilerCache = null;

    GeneratorNode(Symbol symbol, GeneratorNode parent) {
        assert symbol != null;
        this.symbol = symbol;
        this.parent = parent;
    }

    public Object compile() {
        if (compilerCache == null)
            compilerCache = instantiate();

        return compilerCache;
    }

    abstract Object instantiate();

    public abstract List<GeneratorNode> nextPossibleParameters(SymbolMapper symbolMapper);

    // TODO do I need termination Nodes? if I select (game <string>) instead of
    //  (game <string> <players> [<mode>] <equipment> <rules.rules>) should I pad parameterSet with null values?
    //  Rn the add parameter wil force you to define players
    public void addParameter(GeneratorNode param) {
        parameterSet.add(param);
    }

    public void clearParameters() {
        parameterSet.clear();
        clearCompilerCache();
    }

    public void clearCompilerCache() {
        if (parent.compilerCache != null)
            parent.clearCompilerCache();

        compilerCache = null;
    }

    public static GeneratorNode fromSymbol(Symbol symbol, GeneratorNode parent) {
        if (symbol.nesting() > 0) {
            return new ArrayNode(symbol, parent);
        }

        switch (symbol.path()) {
            case "java.lang.Integer", "java.lang.Float", "java.lang.String", "game.functions.dim.DimConstant" -> {
                return new PrimitiveNode(symbol, parent);
            }
        }

        if (symbol.cls().isEnum())
            return new EnumNode(symbol, parent);

        return new ClassNode(symbol, parent);
    }

    public static GameNode cloneCallTree(Call root, SymbolMapper symbolMapper) {
        assert root.type() == Call.CallType.Class;
        return (GameNode) cloneCallTree(root, List.of(new GameNode(root.symbol())), symbolMapper);
    }

    public static GeneratorNode cloneCallTree(Call call, List<GeneratorNode> options, SymbolMapper symbolMapper) {
//        System.out.println("options: " + options.stream().map(node -> node==null ? null : node.symbol).toList());
//        System.out.println("call: " + call.type() + ", " + call.cls());

        GeneratorNode node = switch (call.type()) {
            case Array -> {
                int nesting = getNesting(call, 0);
                List<Symbol> nestedSymbols = new ArrayList<>();
                getNestedSymbols(call, nestedSymbols);

//                System.out.println("nested symbols " + nestedSymbols);

                optionLoop: for (GeneratorNode option: options) {
                    if (option == null)
                        continue;

                    if (option.symbol.nesting() != nesting) {
                        System.out.println(option.symbol.nesting() + " incompatible with " + nesting);
                        continue;
                    }

                    for (Symbol nestedSymbol: nestedSymbols) {
                        assert nestedSymbol != null;
//                        System.out.println(option.symbol + " compatible with " + nestedSymbol + "? " + option.symbol.compatibleWith(nestedSymbol));

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
//                        System.out.println(option.symbol + " compatible with " + call.symbol() + "? " + option.symbol.compatibleWith(call.symbol()));
                        if (option.symbol.compatibleWith(call.symbol())) {
                            if (call.object() instanceof DimConstant)
                                ((PrimitiveNode) option).setValue(((DimConstant) call.object()).eval());
                            else
                                ((PrimitiveNode) option).setValue(call.object());

                            yield option;
                        }
                        continue;
                    }

                    if (option.symbol.matches(call.symbol())) {
                        yield option;
                    }
                }
//                System.out.println("options: " + options.stream().map(n -> n==null ? null : n.symbol).toList());
                throw new RuntimeException("Could not find a compatible class " + call.symbol());
            }

            case Null -> {
                for (GeneratorNode option : options) {
                    if (option == null)
                        yield null;
                }

                throw new RuntimeException("null is not an option");
            }
        };
//        System.out.println("selected: " + (node == null? "null":node.symbol));

        for (Call childCall: call.args()) {
            assert node != null;
//            System.out.println("\nparent: " + node.symbol + ", " + node.getClass());
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

        List<Symbol> symbols = Grammar.grammar().symbols().stream().filter(s -> s.usedInGrammar() || s.usedInDescription() || !s.usedInMetadata()).toList();

        SymbolMapper symbolMapper = new SymbolMapper(symbols);

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

        final long startPreCompilation = System.currentTimeMillis();
        final Description description = new Description(str);
        final UserSelections userSelections = new UserSelections(new ArrayList<String>());
        final Report report = new Report();

        //Parser.expandAndParse(description, userSelections, report, false);
        Game originalGame = (Game) Compiler.compileTest(description, false);
        final long endPreCompilation = System.currentTimeMillis();

        GameNode rootNode = cloneCallTree(description.callTree(), symbolMapper);

        final long endClone = System.currentTimeMillis();

        Game game1 = rootNode.compile();

        final long endCompile = System.currentTimeMillis();

        //rootNode.equipmentNode().clearCompilerCache();

        Game game = rootNode.compile();

        final long endRecompile = System.currentTimeMillis();

        System.out.println("PreCompilation time: " + (endPreCompilation - startPreCompilation));
        System.out.println("Clone time: " + (endClone - endPreCompilation));
        System.out.println("Compile time: " + (endCompile - endClone));
        System.out.println("Recompile time: " + (endRecompile - endCompile));

        System.out.println("\n\nGAME: " + rootNode + "\n\n");

        System.out.println("hasMissingRequirement? " + game.hasMissingRequirement());
        System.out.println("Will it crash? " + game.willCrash());

        System.out.println("Functional? " + Generator.isFunctional(game));
        System.out.println("isPlayable? " + Generator.isPlayable(game));
        System.out.println("isFunctionalAndWithOnlyDecision? " + Generator.isFunctionalAndWithOnlyDecision(game));

    }
}
