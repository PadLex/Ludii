package approaches.symbolic.api;

import approaches.symbolic.SymbolMapper;
import approaches.symbolic.generators.DescriptionParser;

import java.util.Scanner;

import static approaches.symbolic.generators.DescriptionParser.compilePartialDescription;
import static approaches.symbolic.generators.DescriptionParser.standardize;

public class Compile {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("Ready");

        SymbolMapper symbolMapper = new SymbolMapper();

        while (sc.hasNextLine()) {
            //System.out.println(partialCompilation.consistentGames.peek().description());
            String standardInput = standardize(sc.nextLine());
            DescriptionParser.PartialCompilation partialCompilation = compilePartialDescription(standardInput, symbolMapper);
            System.out.println(partialCompilation.exception == null? 1:0);
            System.out.println(partialCompilation.consistentGames.peek().root().description());
        }
        sc.close();
    }
}
