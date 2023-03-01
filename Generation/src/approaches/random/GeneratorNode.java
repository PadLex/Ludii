package approaches.random;

import main.grammar.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import grammar.Grammar;

public class GeneratorNode {

    private GeneratorNode parent;
    private Symbol symbol;
    private int parameterSetIndex = -1;
    private List<List<Symbol>> parameterSets;

    public GeneratorNode(Symbol symbol) {
        this.symbol = symbol;

    }



    public void nextParameterSet() {
        parameterSetIndex++;
    }

    public void clearParameters() {
        parameterSetIndex = -1;
    }

    public Call buildCallTree() {
        //Call root = new Call();
        return null;
    }

    public GeneratorNode parent() {
        return parent;
    }

    public Symbol symbol() {
        return symbol;
    }

    public static void main(String[] args) {

        // Find Game symbol
        Symbol gameSymbol = Grammar.grammar().symbolsByName("Game").get(1);
        GeneratorNode root = new GeneratorNode(gameSymbol);

    }
}
