package approaches.symbolic.nodes;

import approaches.symbolic.SymbolMapper;
import approaches.symbolic.SymbolMapper.MappedSymbol;
import main.StringRoutines;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public abstract class GeneratorNode {
    final MappedSymbol symbol;
    final List<GeneratorNode> parameterSet = new ArrayList<>();
    GeneratorNode parent;
    Object compilerCache = null;
    String descriptionCache = null;
    boolean complete;

    GeneratorNode(MappedSymbol symbol, GeneratorNode parent) {
        assert symbol != null;
        this.symbol = symbol;
        this.parent = parent;
    }

    public static GeneratorNode fromSymbol(MappedSymbol symbol, GeneratorNode parent) {
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
            case "game.Game" -> {
                return new GameNode();
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

    public List<GeneratorNode> nextPossibleParameters(SymbolMapper symbolMapper, List<GeneratorNode> partialArguments, boolean includeAliases, boolean expandEmpty) {
        List<GeneratorNode> options;

        if (partialArguments == null || partialArguments.isEmpty()) {
            options = nextPossibleParameters(symbolMapper);
        } else {
            int i = parameterSet.size();
            parameterSet.addAll(partialArguments);
            options = nextPossibleParameters(symbolMapper);
            parameterSet.subList(i, parameterSet.size()).clear();
        }

        if (includeAliases || expandEmpty)
            options = new ArrayList<>(options);

        // Expand empty nodes
        if (expandEmpty) {
            EmptyNode empty = options.stream().filter(n -> n instanceof EmptyNode).map(n -> (EmptyNode) n).findFirst().orElse(null);
            if (empty != null) {
                options.remove(empty);
                List<GeneratorNode> nextPartialArguments = partialArguments==null? new ArrayList<>() : new ArrayList<>(partialArguments);
                nextPartialArguments.add(empty);
                options.addAll(nextPossibleParameters(symbolMapper, nextPartialArguments, includeAliases, expandEmpty));
            }
        }

        // Add aliases to the options (^ ... should also include (pow ...
        if (includeAliases) {
            options.addAll(options.stream().filter(n -> n.symbol().hasAlias()).map(n -> {
                MappedSymbol noAlias = new MappedSymbol(n.symbol());
                noAlias.setToken(StringRoutines.toDromedaryCase(noAlias.name()));
                return GeneratorNode.fromSymbol(noAlias, n.parent());
            }).toList());
        }

        return options;
    }

    public void addParameter(GeneratorNode param) {
        assert param != null;
        param.parent = this;

        if (param instanceof EndOfClauseNode) {
            complete = true;
            return;
        }

        parameterSet.add(param);
    }

    public void popParameter() {
        parameterSet.remove(parameterSet.size() - 1);
        clearCache();
        complete = false;
    }

    public void clearParameters() {
        parameterSet.clear();
        clearCache();
        complete = false;
    }

    public void clearCache() {
        if (parent.compilerCache != null)
            parent.clearCache();

        compilerCache = null;
        descriptionCache = null;
    }

    public boolean isComplete() {
        return complete;
    }

    public boolean isRecursivelyComplete() {
        return isComplete() && parameterSet.stream().allMatch(GeneratorNode::isRecursivelyComplete);
    }

    public void assertRecursivelyComplete() {
        if(!isComplete()) {
            System.out.println("Params " + this.parameterSet.stream().map(GeneratorNode::symbol).map(MappedSymbol::grammarLabel).toList());
            throw new RuntimeException("Node is not complete: " + this);
        }

        parameterSet.forEach(GeneratorNode::assertRecursivelyComplete);
    }

    public MappedSymbol symbol() {
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

    public String description() {
        if (descriptionCache == null)
            descriptionCache = buildDescription();

        return descriptionCache;
    }

    String buildDescription() {
        if (symbol.label != null)
            return symbol.label + ":" + this.toString();

        return this.toString();
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

    public boolean equivalent(GeneratorNode other) {
        if (!Objects.equals(symbol.path(), other.symbol.path()) && symbol.nesting() != other.symbol.nesting())
            return false;

        if (parameterSet.size() != other.parameterSet.size())
            return false;

        for (int i = 0; i < parameterSet.size(); i++) {
            if (!parameterSet.get(i).equivalent(other.parameterSet.get(i)))
                return false;
        }

        return true;
    }
}
