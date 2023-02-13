package approaches.ngram.facade;

import approaches.model.SymbolCollections;
import approaches.model.TokenizationParameters;
import approaches.ngram.table.FrequencyTable;
import approaches.ngram.table.HashMapTrie;
import approaches.ngram.table.SimpleHashTable;
import approaches.ngram.table.TreeMapTrie;
import compiler.Compiler;
import game.Game;
import grammar.Grammar;
import main.grammar.Description;
import main.grammar.Report;
import main.grammar.Symbol;
import main.grammar.Token;
import main.options.UserSelections;
import parser.Parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class TokenAdapter {
    final Pattern isInteger = Pattern.compile("-?\\d+");
    final Pattern isFloat = Pattern.compile("-?\\d+\\.\\d+");
    final Pattern isString = Pattern.compile("\".+\"");

    public final Token rootToken;
    public final GramNode rootNode;
    public final FrequencyTable verticalTable = new SimpleHashTable(5);

    List<String> dictionary;

    public TokenAdapter(Token rootToken) {
        dictionary = new ArrayList<>();
        dictionary.add("%start%");
        dictionary.add("%end%");
        dictionary.add("%array%");
        dictionary.add("%int%");
        dictionary.add("%float%");
        dictionary.add("%string%");
        dictionary.addAll(SymbolCollections.smallBoardGameSymbolNames.stream().toList());

        this.rootToken = rootToken;
        rootNode = new GramNode(toGram(rootToken));
        for (Token childToken: rootToken.arguments()) {
            setNodeTree(childToken, rootNode);
        }

        rootNode.recursivelyIncrementNgrams(verticalTable);
    }

    private void setNodeTree(Token rootToken, GramNode parent) {
        GramNode root = new GramNode(toGram(rootToken), parent);
        for (Token childToken: rootToken.arguments()) {
            setNodeTree(childToken, root);
        }
    }

    int toGram(Token token) {
        String name = token.name();

        if (token.isArray())
            name = "%array%";
        else if (token.isTerminal()) {
            if (isInteger.matcher(name).matches())
                name = "%int%";
            else if (isFloat.matcher(name).matches())
                name = "%float%";
            else if (isString.matcher(name).matches()) {
                name = "%string%";
            }
        }

        if (!dictionary.contains(name))
            System.out.println("Unknown symbol: " + name);

        return dictionary.indexOf(name);
    }

    @Override
    public String toString() {
        HashMap<List<Integer>, Integer> counts = verticalTable.getFrequencies();
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<List<Integer>, Integer> e : counts.entrySet()) {
            sb.append(e.getKey().stream().map(s -> dictionary.get(s)).toList());
            sb.append(": ").append(e.getValue());
            sb.append(", ");
        }

        return sb.toString();
    }

    public static void main(String[] args) {
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

        Parser.expandAndParse(description, userSelections, report, false);
        Game game = (Game) Compiler.compileTest(description, false);

        //System.out.println("raw: " + description.raw());
        System.out.println("tokenForest: " + description.tokenForest().tokenTree());
        //System.out.println("parseTree: " + description.parseTree());
        //System.out.println("callTree: " + description.callTree());

        TokenAdapter tokenAdapter = new TokenAdapter(description.tokenForest().tokenTree());
        System.out.println("tokenAdapter: " + tokenAdapter);
    }
}
