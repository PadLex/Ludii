package approaches.symbolic.api;

import approaches.symbolic.SymbolMapper;
import approaches.symbolic.generators.DescriptionParser;
import approaches.symbolic.nodes.ArrayNode;
import approaches.symbolic.nodes.EmptyNode;
import approaches.symbolic.nodes.EndOfClauseNode;
import approaches.symbolic.nodes.GeneratorNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static approaches.symbolic.generators.DescriptionParser.compilePartialDescription;
import static approaches.symbolic.generators.DescriptionParser.standardize;

public class Autocomplete {

    static class Completion {
        final String completion;
        final String description;

        public Completion(String completion, String description) {
            this.completion = completion;
            this.description = description;
        }
    }

    // TODO make it consider possibilities bellow the top of the stack
    public static List<Completion> autocomplete(String rawInput, SymbolMapper symbolMapper) {
        String standardInput = standardize(rawInput);
        if (standardInput.isEmpty())
            return List.of(new Completion("Game", "game.Game"));
        if (standardInput.length() < 5)
            return new ArrayList<>();
        DescriptionParser.PartialCompilation partialCompilation = compilePartialDescription(standardInput, symbolMapper);
        GeneratorNode node = partialCompilation.consistentGames.peek();
        List<Completion> completions = new ArrayList<>();

        System.out.println(node.root().description());

        if (standardInput.chars().filter(c -> c == ' ').count() != node.root().description().chars().filter(c -> c == ' ').count())
            return completions;

//        System.out.println("Autocompleting: " + node.root().description());

        for (GeneratorNode option: node.nextPossibleParameters(symbolMapper, null, false, true)) {
            assert !(option instanceof EmptyNode);

            GeneratorNode newNode = node.copyUp();
            newNode.addParameter(option);

            String description = option.symbol().path();
            String completion = option.description();

            if (option instanceof EndOfClauseNode) {
                description = "End of clause";
                completion = option.parent() instanceof ArrayNode ? "}" : ")";
            }

            completions.add(new Completion(completion, description));
        }

        return completions;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("Ready");

        SymbolMapper symbolMapper = new SymbolMapper();

        while (sc.hasNextLine()) {
            //System.out.println(partialCompilation.consistentGames.peek().description());
            for (Completion completion : autocomplete(sc.nextLine(), symbolMapper)) {
                System.out.print(completion.completion + "|" + completion.description + "||");
            }
            System.out.println();
        }
        sc.close();


//        String full    = "(game \"Hex\" (players 2) (equipment {(board (hex Diamond 11)) (piece \"Marker\" Each) (regions P1 {(sites Side NE) (sites Side SW)}) (regions P2 {(sites Side NW) (sites Side SE)})}) (rules (meta (swap)) (play (move Add (to (sites Empty)))) (end (if (is Connected Mover) (result Mover Win)))))";
//        String partial = "(game \"Hex\" (players 2) (equipment {(board (hex Diamond 11)) (piece \"Marker\" Each";
//        SymbolMapper symbolMapper = new SymbolMapper();
//        compilePartialDescription(standardize(partial), symbolMapper).consistentGames.forEach(n -> System.out.println(n.root().description()));
//
//        for (Completion completion : autocomplete(partial, symbolMapper)) {
//            System.out.print(completion.completion + "|" + completion.description + "||");
//        }
//        System.out.println();
    }
}
