package approaches.ngram;

import compiler.Compiler;
import game.Game;
import main.grammar.Description;
import main.grammar.Report;
import main.grammar.Token;
import main.options.UserSelections;
import parser.Parser;

import java.util.ArrayList;

public class TokenAdapter {
    final NGramCollection verticalNGrams;
    final NGramCollection horizontalNGrams;
    public TokenAdapter(NGramCollection verticalNGrams, NGramCollection horizontalNGrams) {
        this.verticalNGrams = verticalNGrams;
        this.horizontalNGrams = horizontalNGrams;
    }

    void addTokenTree(Token rootToken, int maxN) {
        buildNodeTree(rootToken).propagateVerticalNGrams(maxN);
    }
    AdapterNode buildNodeTree(Token rootToken) {
        AdapterNode root = new AdapterNode(rootToken.name(), verticalNGrams, horizontalNGrams);
        for (Token childToken: rootToken.arguments()) {
            buildNodeTree(childToken, root);
        }
        return root;
    }

    private void buildNodeTree(Token rootToken, AdapterNode parent) {
        AdapterNode root = new AdapterNode(rootToken.name(), parent);
        for (Token childToken: rootToken.arguments()) {
            buildNodeTree(childToken, root);
        }
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

        TokenAdapter tokenAdapter = new TokenAdapter(new SimpleCollection(), new SimpleCollection());
        tokenAdapter.addTokenTree(description.tokenForest().tokenTree(), 5);
        System.out.println(tokenAdapter.verticalNGrams);
    }
}
