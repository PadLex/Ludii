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

        if (PrimitiveNode.typeOf(symbol.path()) != null)
            return new PrimitiveNode(symbol, parent);

        if (symbol.cls().isEnum())
            return new EnumNode(symbol, parent);

        switch (symbol.path()) {
            case "mapper.unused" -> {
                return EmptyNode.instance;
            }
            case "mapper.endOfClause" -> {
                return EndOfClauseNode.instance;
            }
        }

        return new ClassNode(symbol, parent);
    }

    public Object compile() {
        if (compilerCache == null)
            compilerCache = instantiate();

        return compilerCache;
    }

    abstract Object instantiate();

    public abstract List<GeneratorNode> nextPossibleParameters(SymbolMapper symbolMapper);

    public List<GeneratorNode> nextPossibleParameters(SymbolMapper symbolMapper, List<GeneratorNode> partialArguments) {
        int i = parameterSet.size();
        parameterSet.addAll(partialArguments);
        List<GeneratorNode> next = nextPossibleParameters(symbolMapper);
        parameterSet.subList(i, parameterSet.size()).clear();
        return next;
    };

    public void addParameter(GeneratorNode param) {
        assert param != null;

        if (param == EndOfClauseNode.instance) {
            complete = true;
            return;
        }

        parameterSet.add(param);
    }

    public void popParameter() {
        parameterSet.remove(parameterSet.size() - 1);
        clearCompilerCache();
        complete = false;
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
        return isComplete() && parameterSet.stream().allMatch(GeneratorNode::isRecursivelyComplete);
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
    public GeneratorNode copy() {
        GeneratorNode clone = fromSymbol(symbol, parent);
        clone.parameterSet.addAll(parameterSet.stream().map(GeneratorNode::copy).toList());
        clone.complete = complete;
        clone.compilerCache = compilerCache;
        return clone;
    }

    public String buildDescription() {
        return toString();
    }

    public GameNode root() {
        GeneratorNode node = this;
        while (node.parent != null)
            node = node.parent;
        return (GameNode) node;
    }
}
