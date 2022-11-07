package approaches.model;

import java.io.File;
import java.util.HashSet;
import java.util.List;

import compiler.Compiler;
import game.Game;
import grammar.Grammar;
import main.grammar.Description;
import other.GameLoader;

public class DebugTest {
	public static void main(String[] args) {
		// System.out.println(Grammar.grammar().symbols());
		// System.out.println(Grammar.grammar().aliases());
		// System.out.println(Grammar.grammar().getRules());
		// System.out.println(Grammar.grammar().ludemesUsed());

		// EBNF ebnf = Grammar.grammar().ebnf();
		System.out.println("grammar");
		
		TokenizationParameters parameters = TokenizationParameters.completeParameters();
		System.out.println("\nparameters. Vocabulary size: " + parameters.tokenCount + " tokens");

		Tokenizer tokenizer = new Tokenizer(parameters);
		Restorer restorer = new Restorer(parameters);

		System.out.println("Dictionary: " + restorer.dictionary());
		System.out.println("caluses: " + parameters.clauseToId);
		
		//String file = "../Common/res/lud/board/war/leaping/diagonal/American Pool Checkers.lud"
		String file = "../Common/res/lud/subgame/race/escape/MughrabiehSubgame.lud";
		

		Game originalGame = GameLoader.loadGameFromFile(new File(file));
		System.out.println("\ngame loaded");
		System.out.println(CorrectnessTest.squishSpaces(originalGame.description().tokenForest().tokenTrees().get(0).toString()));

		List<Integer> tokens = tokenizer.tokenizeGame(originalGame);
		HashSet<Integer> uniqueTokens = new HashSet<>(tokens);
		System.out.println("\ngame tokenized with " + tokens.size() + " tokens and a vocabulary of "
				+ uniqueTokens.size() + " unique tokens");
		System.out.println(tokens + "\n");

		String restoredString = restorer.restoreAsString(tokens);
		
		
		restoredString = restoredString.replace("string 1", "aaac1");
		restoredString = restoredString.replace("string 2", "aaac2");

		restoredString = restoredString.replace("string 4", "aaac");


		
		System.out.println(CorrectnessTest.squishSpaces(restoredString));

		Game restoredGame = (Game) Compiler.compileTest(new Description(restoredString), false);

		System.out.println("\nrestored game: " + CorrectnessTest.compareGames(originalGame, restoredGame));
		System.out.println(CorrectnessTest.squishSpaces(restoredGame.description().tokenForest().tokenTrees().toString()));


		// File out = new File("/Users/alex/Downloads/tokens.json");

	}
}

//../Common/res/lud/subgame/race/escape/GrandTrictracSubgame.lud
//restoredString = restoredString.replace("string 0", "GrandTrictracSubgame");
		//restoredString = restoredString.replace("string 1", "Track1");
		//restoredString = restoredString.replace("string 2", "Track2");
		//restoredString = restoredString.replace("string 3", "ReverseHuckeTrack1");
		//restoredString = restoredString.replace("string 4", "ReverseHuckeTrack2");
		//restoredString = restoredString.replace("string 5", "BeforeHucke");
		//restoredString = restoredString.replace("string 6", "Track");
		//restoredString = restoredString.replace("string 7", "FirstHalfOpponent");
		//restoredString = restoredString.replace("string 8", "Hucke");
		//restoredString = restoredString.replace("string 9", "MovingToHucke");
		//restoredString = restoredString.replace("Pawn", "Disc");
