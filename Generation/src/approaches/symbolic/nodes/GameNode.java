package approaches.symbolic.nodes;

import approaches.symbolic.SymbolMapper;
import approaches.symbolic.SymbolMapper.MappedSymbol;

import game.Game;
import game.equipment.Equipment;
import game.mode.Mode;
import game.players.Players;
import game.rules.Rules;
import grammar.Grammar;

import java.util.ArrayList;
import java.util.List;

public class GameNode extends GeneratorNode {
    static final MappedSymbol gameSymbol = new MappedSymbol(Grammar.grammar().findSymbolByPath("game.Game"), null);
    static MappedSymbol nameSymbol = new MappedSymbol(Grammar.grammar().findSymbolByPath("java.lang.String"), null);
    static MappedSymbol playersSymbol = new MappedSymbol(Grammar.grammar().findSymbolByPath("game.players.Players"), null);
    static MappedSymbol equipmentSymbol = new MappedSymbol(Grammar.grammar().findSymbolByPath("game.equipment.Equipment"), null);
    static MappedSymbol modeSymbol = new MappedSymbol(Grammar.grammar().findSymbolByPath("game.mode.Mode"), null);
    static MappedSymbol rulesSymbol = new MappedSymbol(Grammar.grammar().findSymbolByPath("game.rules.Rules"), null);

    public GameNode(MappedSymbol symbol) {
        super(symbol, null);
        assert symbol.path().equals("game.Game");
    }

    public GameNode() {
        super(gameSymbol, null);
    }

    @Override
    public Game compile() {
        if (compilerCache != null) return (Game) compilerCache;

        boolean skipEquipment = parameterSet.get(3).compilerCache != null;

        //System.out.println("Skipping equipment: " + skipEquipment);

        Game game = instantiate();

        //TODO skipEquipment
//        if (skipEquipment) game.createGame();
//        else game.create();
        game.create();

        //System.out.println("totalDefaultSites: " + game.equipment().totalDefaultSites());

        compilerCache = game;
        return (Game) compilerCache;
    }

    @Override
    Game instantiate() {
        return new Game((String) nameNode().compile(), (Players) playersNode().compile(), modeNode() != null ? (Mode) modeNode().compile() : null, (Equipment) equipmentNode().compile(), (Rules) rulesNode().compile());
    }

    @Override
    public List<GeneratorNode> nextPossibleParameters(SymbolMapper symbolMapper) {
        if (complete) return List.of();

        switch (parameterSet.size()) {
            case 0 -> {
                return List.of(new PrimitiveNode(nameSymbol, this));
            }
            case 1 -> {
                return List.of(new ClassNode(playersSymbol, this));
            }
            case 2 -> {
                ArrayList<GeneratorNode> options = new ArrayList<>(2);
                options.add(new EmptyNode(this));
                options.add(new ClassNode(modeSymbol, this));
                return options;
            }
            case 3 -> {
                return List.of(new ClassNode(equipmentSymbol, this));
            }
            case 4 -> {
                return List.of(new ClassNode(rulesSymbol, this));
            }
            case 5 -> {
                return List.of(new EndOfClauseNode(this));
            }
            default -> {
                throw new IllegalStateException("Unexpected state: " + parameterSet.size());
            }
        }
    }

    @Override
    public void clearCache() {
        compilerCache = null;
        descriptionCache = null;
    }

    @Override
    public String toString() {
        return "(" + symbol.grammarLabel() + ": " + String.join(", ", parameterSet.stream().map(GeneratorNode::toString).toList()) + ")";
    }

    @Override
    String buildDescription() {
        String parameterString = String.join(" ", parameterSet.stream().filter(s -> !(s instanceof EmptyNode || s instanceof EndOfClauseNode)).map(GeneratorNode::description).toList());
        if (parameterString.length() > 0)
            parameterString = " " + parameterString;

        String close = "";
        if (complete)
            close = ")";

        return "(" + symbol.token() + parameterString + close;
    }

    @Override
    public GameNode copyDown() {
        GameNode clone = new GameNode(symbol);
        clone.parameterSet.addAll(parameterSet.stream().map(GeneratorNode::copyDown).toList());
        clone.complete = complete;
        clone.compilerCache = compilerCache;
        return clone;
    }

//    @Override
//    public GameNode copyUp() {
//        GameNode clone = new GameNode(symbol);
//        clone.parameterSet.addAll(parameterSet);
//        clone.complete = complete;
//        clone.compilerCache = compilerCache;
//        return clone;
//    }

    public GeneratorNode nameNode() {
        return parameterSet.get(0);
    }

    public GeneratorNode playersNode() {
        return parameterSet.get(1);
    }

    public GeneratorNode modeNode() {
        return parameterSet.get(2);
    }

    public GeneratorNode equipmentNode() {
        return parameterSet.get(3);
    }

    public GeneratorNode rulesNode() {
        return parameterSet.get(4);
    }

    @Override
    public void setParent(GeneratorNode parent) {
        throw new RuntimeException("Cannot set parent of GameNode");
    }
}
