package approaches.symbolic.api;

import compiler.Compiler;
import game.Game;
import main.grammar.Description;
import main.grammar.Report;
import main.options.UserSelections;

import java.util.ArrayList;
import java.util.Scanner;

public class LegacyCompile {
    public static void main(String[] args) throws InterruptedException {
        Scanner sc = new Scanner(System.in);
        System.out.println("Ready");

        while (sc.hasNextLine()) {
            String input = sc.nextLine();
            input = input.replace("\\n", "\n");

            Game game = null;
            try {
                game = (Game) Compiler.compile(new Description(input), new UserSelections(new ArrayList<>()), new Report(), false);
            } catch (Exception ignored) {}

            System.out.println(game != null? 1:0);
            System.out.println(input.length() / 100.0);
            Thread.sleep(10); // Pause to prevent the buffer from filling up.
        }
        sc.close();
    }
}
