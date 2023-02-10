package approaches.ngram.facade;

import compiler.Compiler;
import game.Game;
import main.grammar.Description;
import main.grammar.Report;
import main.grammar.Token;
import main.options.UserSelections;
import parser.Parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

public class TokenAdapter {
    final Pattern isInteger = Pattern.compile("-?\\d+");
    final Pattern isFloat = Pattern.compile("-?\\d+\\.\\d+");
    final Pattern isString = Pattern.compile("\".+\"");

    HashMap<String, Integer> dictionary;

    public TokenAdapter(Token rootToken) {
        setNodeTree(rootToken);
    }
    void setNodeTree(Token rootToken) {
        GramNode root = new GramNode(toGram(rootToken));
        for (Token childToken: rootToken.arguments()) {
            setNodeTree(childToken, root);
        }
    }

    private void setNodeTree(Token rootToken, GramNode parent) {
        GramNode root = new GramNode(toGram(rootToken), parent);
        for (Token childToken: rootToken.arguments()) {
            setNodeTree(childToken, root);
        }
    }

    int toGram(Token token) {
        if (token.isArray())
            return 2;

        String name = token.name();

        if (token.isTerminal()) {
            if (isInteger.matcher(name).matches())
                return 3;

            if (isFloat.matcher(name).matches())
                return 4;

            if (isString.matcher(name).matches()) {
                return 5;
            }
        }

        return dictionary.get(name);
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
    }
}
