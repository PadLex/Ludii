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
                return new EmptyNode(parent);
            }
            case "mapper.endOfClause" -> {
                return new EndOfClauseNode(parent);
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

        if (param instanceof EndOfClauseNode) {
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

    public void setParent(GeneratorNode parent) {
        this.parent = parent;
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

    public GeneratorNode copyDown() {
        GeneratorNode clone = fromSymbol(symbol, parent);
        clone.parameterSet.addAll(parameterSet.stream().map(GeneratorNode::copyDown).toList());
        clone.complete = complete;
        clone.compilerCache = compilerCache;
        return clone;
    }

    public GeneratorNode copyUp() {
        GeneratorNode clone = fromSymbol(symbol, parent);
        clone.parameterSet.addAll(parameterSet);
        clone.complete = complete;
        clone.compilerCache = compilerCache;
        if (parent != null) {
            clone.parent = parent.copyUp();
            clone.parent.parameterSet.set(parent.parameterSet.indexOf(this), clone);
        }

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

    public List<Integer> indexPath() {
        List<Integer> indexes = new ArrayList<>();
        GeneratorNode node = this;
        while (node.parent != null) {
            System.out.println(node.parent.parameterSet + ": " + node + " " + node.parent.parameterSet.indexOf(node));
            indexes.add(node.parent.parameterSet.indexOf(node));
            node = node.parent;
        }
        Collections.reverse(indexes);
        return indexes;
    }

    public GeneratorNode get(List<Integer> indexes) {
        GeneratorNode node = this;
        for (int i : indexes) {
            node = node.parameterSet.get(i);
        }
        return node;
    }
}
