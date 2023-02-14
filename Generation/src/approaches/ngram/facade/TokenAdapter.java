package approaches.ngram.facade;

import approaches.model.SymbolCollections;
import approaches.ngram.table.FrequencyTable;
import approaches.ngram.table.SimpleHashTable;
import compiler.Compiler;
import game.Game;
import main.grammar.Description;
import main.grammar.Report;
import main.grammar.Token;
import main.options.UserSelections;
import other.GameLoader;
import parser.Parser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class TokenAdapter {
    final Pattern isInteger = Pattern.compile("-?\\d+");
    final Pattern isFloat = Pattern.compile("-?\\d+\\.\\d+");
    final Pattern isString = Pattern.compile("\".+\"");
    public final FrequencyTable verticalTable = new SimpleHashTable(3);

    List<String> dictionary;

    public TokenAdapter() {
        dictionary = new ArrayList<>();
        dictionary.add("%start%");
        dictionary.add("%end%");
        dictionary.add("%array%");
        dictionary.add("%int%");
        dictionary.add("%float%");
        dictionary.add("%string%");
        dictionary.addAll(SymbolCollections.smallBoardGameSymbolNames.stream().toList());
    }

    GramNode buildGameTree(Token rootToken) {
        GramNode rootNode = new GramNode(toGram(rootToken));

        for (Token childToken: rootToken.arguments()) {
            buildGameTree(childToken, rootNode);
        }

        return rootNode;
    }

    private void buildGameTree(Token token, GramNode parent) {
        GramNode root = new GramNode(toGram(token), parent);
        for (Token childToken: token.arguments()) {
            buildGameTree(childToken, root);
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

        int index = dictionary.indexOf(name);

        if (index < 0)
            return dictionary.indexOf("%unknown%");

        return index;
    }

    void incrementLudiiLibrary() throws IOException {

        String gamesRoot = "../Common/res/lud/board";
        Stream<Path> paths = Files.walk(Paths.get(gamesRoot)).filter(Files::isRegularFile).limit(200);
        paths.forEach(path -> {

            try {

                final Description description = new Description(Files.readString(path));
                final UserSelections userSelections = new UserSelections(new ArrayList<String>());
                final Report report = new Report();

                Parser.expandAndParse(description, userSelections, report, false);
                Game game = (Game) Compiler.compile(description, userSelections, report, false);
                System.out.println(description.tokenForest());
                /*
                Game game = GameLoader.loadGameFromFile(path.toFile());
                Description description = game.description();
                if (description.tokenForest().tokenTree() == null) {
                    System.out.println("Null token tree, skipped " + path);
                    return;
                }
                GramNode root = buildGameTree(description.tokenForest().tokenTree());
                root.recursivelyIncrementNgrams(verticalTable);*/
            } catch (Exception e) {
                System.out.println("Something went wrong, skipped " + path);
            }
        });
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

    public static void main(String[] args) throws IOException {
        /*
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

        TokenAdapter tokenAdapter = new TokenAdapter();
        GramNode hexRoot = tokenAdapter.buildGameTree(description.tokenForest().tokenTree());

         */
        TokenAdapter tokenAdapter = new TokenAdapter();
        tokenAdapter.incrementLudiiLibrary();

        System.out.println("tokenAdapter: " + tokenAdapter.verticalTable.getFrequencies().size());
    }
}
