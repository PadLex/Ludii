package approaches.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import compiler.Compiler;
import game.Game;
import grammar.Grammar;
import main.grammar.Description;
import other.GameLoader;
import java.nio.file.Path;

public class CorrectnessTest {

	public static void main(String[] args) {
		Tokenizer tokenizer = new Tokenizer(Grammar.grammar().symbols());
		Tokenizer secondTokenizer = new Tokenizer(Grammar.grammar().symbols()); // Could also use the old one. I'm just making sure that it also works with a new instance

		System.out.println("\ntokenizer. Vocabulary size: " + tokenizer.tokenCount() + " tokens");
		
		File directory = new File("../Common/res/lud");
		System.out.println(directory.getAbsolutePath());
		
		List<Path> paths = new ArrayList<>();
		
		try {
			Files.find(Paths.get("../Common/res/lud"),
			           Integer.MAX_VALUE,
			           (filePath, fileAttr) -> fileAttr.isRegularFile() && filePath.toString().toLowerCase().endsWith(".lud"))
			        .forEach(paths::add);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		int invalidTests = 0;
		int failedToTokenize = 0;
		int failedToRestore = 0;
		int incorrectRestore = 0;
		int successes = 0;
		
		for (Path path: paths) {
			try {
				Game originalGame = GameLoader.loadGameFromFile(path.toFile());
			
				try {
					List<Integer> tokens = tokenizer.tokenizeGame(originalGame);
					
					String restoredString = secondTokenizer.restoreAsString(tokens);
	
					try {
						Game restoredGame = (Game) Compiler.compileTest(new Description(restoredString), false);
						
						if (!originalGame.description().tokenForest().tokenTrees().toString().equals(restoredGame.description().tokenForest().tokenTrees().toString())) {
							System.out.println("The restored game is not identical to the original " + path.toString());
							System.out.println("Original vs Restored strings");
							System.out.println(Tokenizer.squishSpaces(originalGame.description().tokenForest().tokenTrees().get(0).toString()));
							System.out.println(Tokenizer.squishSpaces(restoredString));
							System.out.println("---------------------------------------- # ----------------------------------------");
							
							incorrectRestore++;
							continue;
						}
						
						successes++;
						
	
					} catch (RuntimeException e) {
						System.out.println("An exception occured while attempting to restore " + path.toString());
						System.out.println("Original vs Restored strings");
						System.out.println(Tokenizer.squishSpaces(originalGame.description().tokenForest().tokenTrees().get(0).toString()));
						System.out.println(Tokenizer.squishSpaces(restoredString));
						System.out.println("---------------------------------------- # ----------------------------------------");
	
						e.printStackTrace();
						
						failedToRestore++;
						continue;
					}	
					
				} catch (RuntimeException e) {
					System.out.println("An exception occured while attempting to tokenize " + path.toString());
					System.out.println("Original string");
					System.out.println(Tokenizer.squishSpaces(originalGame.description().tokenForest().tokenTrees().get(0).toString()));
					System.out.println("---------------------------------------- # ----------------------------------------");
	
					e.printStackTrace();
					
					failedToTokenize++;
					continue;
				}
			} catch (Exception e) {
				invalidTests++;
				continue;
			}
		}
				
		System.out.println("invalidTests: " + invalidTests);
		System.out.println("failedToTokenize: " + failedToTokenize);
		System.out.println("failedToRestore: " + failedToRestore);
		System.out.println("incorrectRestore: " + incorrectRestore);
		System.out.println("successes: " + successes);

	}
		
}
