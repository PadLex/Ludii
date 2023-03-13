package approaches.symbolic.generators;

import approaches.random.Generator;
import approaches.symbolic.SymbolMapper;
import approaches.symbolic.nodes.*;
import compiler.Compiler;
import game.Game;
import game.functions.dim.DimConstant;
import grammar.Grammar;
import main.grammar.Call;
import main.grammar.Description;
import main.grammar.Symbol;

import java.util.ArrayList;
import java.util.List;

public class CallTreeCloner {
    public static GameNode cloneCallTree(Call root, SymbolMapper symbolMapper) {
        assert root.type() == Call.CallType.Class;
        return (GameNode) cloneCallTree(root, List.of(new GameNode(root.symbol())), symbolMapper);
    }

    public static GeneratorNode cloneCallTree(Call call, List<GeneratorNode> options, SymbolMapper symbolMapper) {
        GeneratorNode node = switch (call.type()) {
            case Array -> {
                int nesting = getNesting(call, 0);
                List<Symbol> nestedSymbols = new ArrayList<>();
                getNestedSymbols(call, nestedSymbols);

                optionLoop:
                for (GeneratorNode option : options) {
                    if (option == null)
                        continue;

                    if (option.symbol().nesting() != nesting) {
                        System.out.println(option.symbol() + ": " + option.symbol().nesting() + " incompatible with " + nesting);
                        continue;
                    }

                    for (Symbol nestedSymbol : nestedSymbols) {
                        assert nestedSymbol != null;

                        if (!option.symbol().compatibleWith(nestedSymbol))
                            continue optionLoop;
                    }

                    yield option;
                }

                throw new RuntimeException("Could not find a compatible array type");
            }

            case Class, Terminal -> {
                for (GeneratorNode option : options) {
                    if (option == EmptyNode.instance)
                        continue;

                    if (option instanceof PrimitiveNode) {
                        if (option.symbol().compatibleWith(call.symbol())) {
                            if (call.object() instanceof DimConstant)
                                ((PrimitiveNode) option).setValue(((DimConstant) call.object()).eval());
                            else
                                ((PrimitiveNode) option).setValue(call.object());

                            yield option;
                        }
                        continue;
                    }

                    if (option.symbol().matches(call.symbol())) {
                        yield option;
                    }
                }

                throw new RuntimeException("Could not find a compatible class " + call.symbol());
            }

            case Null -> {
                for (GeneratorNode option : options) {
                    if (option == EmptyNode.instance)
                        yield EmptyNode.instance;
                }

                throw new RuntimeException("null is not an option");
            }
        };

        for (Call childCall : call.args()) {
            assert node != EmptyNode.instance;
            GeneratorNode child = cloneCallTree(childCall, node.nextPossibleParameters(symbolMapper), symbolMapper);
            node.addParameter(child);
        }

        if (call.args().size() > 0) {
            assert node.nextPossibleParameters(symbolMapper).contains(EndOfClauseNode.instance);
            node.addParameter(EndOfClauseNode.instance);
        }
        assert node.isComplete();

        return node;
    }

    private static int getNesting(Call call, int nesting) {
        if (call.type() != Call.CallType.Array)
            return nesting;

        return getNesting(call.args().get(0), nesting + 1);
    }

    private static void getNestedSymbols(Call call, List<Symbol> nestedSymbols) {
        if (call.type() != Call.CallType.Array)
            nestedSymbols.add(call.symbol());
        else
            call.args().forEach(child -> getNestedSymbols(child, nestedSymbols));
    }

    public static void main(String[] args) {
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
        Game originalGame = (Game) Compiler.compileTest(description, false);
        final long endPreCompilation = System.currentTimeMillis();

        GameNode rootNode = cloneCallTree(description.callTree(), symbolMapper);

        final long endClone = System.currentTimeMillis();

        Game game1 = rootNode.compile();

        final long endCompile = System.currentTimeMillis();

        //rootNode.equipmentNode().clearCompilerCache();
        rootNode.rulesNode().clearCompilerCache();

        Game game = rootNode.compile();

        final long endRecompile = System.currentTimeMillis();

        System.out.println("\nPreCompilation time: " + (endPreCompilation - startPreCompilation) + "ms");
        System.out.println("Clone time: " + (endClone - endPreCompilation) + "ms");
        System.out.println("Compile time: " + (endCompile - endClone) + "ms");
        System.out.println("Recompile time: " + (endRecompile - endCompile) + "ms");

        System.out.println("\nGAME: " + rootNode);

        System.out.println("\nhasMissingRequirement? " + game.hasMissingRequirement());
        System.out.println("Will it crash? " + game.willCrash());

        System.out.println("Functional? " + Generator.isFunctional(game));
        System.out.println("isPlayable? " + Generator.isPlayable(game));
        System.out.println("isFunctionalAndWithOnlyDecision? " + Generator.isFunctionalAndWithOnlyDecision(game));

    }
}
