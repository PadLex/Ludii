package approaches.symbolic.nodes;

import approaches.symbolic.SymbolMapper;
import main.grammar.Symbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class GeneratorNode {
    final Symbol symbol;
    final List<GeneratorNode> parameterSet = new ArrayList<>();
    GeneratorNode parent;
    Object compilerCache = null;
    boolean complete;



    GeneratorNode(Symbol symbol, GeneratorNode parent) {
        assert symbol != null;
        this.symbol = symbol;
        this.parent = parent;
    }

    public static GeneratorNode fromSymbol(Symbol symbol, GeneratorNode parent) {
        if (symbol.nesting() > 0) {
            return new ArrayNode(symbol, parent);
        }

        switch (symbol.path()) {
            case    "java.lang.Integer", "game.functions.ints.IntConstant", "game.functions.dim.DimConstant",
                    "java.lang.Float", "game.functions.floats.FloatConstant",
                    "java.lang.String", "game.functions.strings.StringConstant",
                    "java.lang.Boolean", "game.functions.booleans.BooleanConstant", "game.functions.booleans.TrueConstant", "game.functions.booleans.FalseConstant"
                    -> {
                return new PrimitiveNode(symbol, parent);
            }
            case "mapper.unused" -> {
                return EmptyNode.instance;
            }
            case "mapper.endOfClause" -> {
                return EndOfClauseNode.instance;
            }
        }

        if (symbol.cls().isEnum())
            return new EnumNode(symbol, parent);

        return new ClassNode(symbol, parent);
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
        assert param != null;

        if (param == EndOfClauseNode.instance) {
            complete = true;
            return;
        }

        parameterSet.add(param);
    }

    public void clearParameters() {
        parameterSet.clear();
        clearCompilerCache();
        complete = false;
    }

    public void clearCompilerCache() {
        if (parent.compilerCache != null)
            parent.clearCompilerCache();

        compilerCache = null;
    }

    public boolean isComplete() {
        return complete;
    }

    public boolean isRecursivelyComplete() {
        return complete && parameterSet.stream().allMatch(GeneratorNode::isRecursivelyComplete);
    }

    public Symbol symbol() {
        return symbol;
    }

    public GeneratorNode parent() {
        return parent;
    }

    public List<GeneratorNode> parameterSet() {
        return Collections.unmodifiableList(parameterSet);
    }

    public GeneratorNode find(String token) {
        for (GeneratorNode node : parameterSet) {
            if (node.symbol.token().equals(token))
                return node;
        }

        return null;
    }

    // TODO fix this
    public GeneratorNode clone() {
        GeneratorNode clone = fromSymbol(symbol, parent);
        clone.parameterSet.addAll(parameterSet.stream().map(GeneratorNode::clone).toList());
        clone.complete = complete;
        clone.compilerCache = compilerCache;
        return clone;
    }
}
