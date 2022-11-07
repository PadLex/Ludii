package approaches.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import compiler.Compiler;
import game.Game;
import grammar.Grammar;
import main.grammar.Description;
import other.GameLoader;
import java.nio.file.Path;

public class CorrectnessTest {
	
	static boolean verbose = true;

	public static void main(String[] args) {
		TokenizationParameters parameters = TokenizationParameters.completeParameters();
		Tokenizer tokenizer = new Tokenizer(parameters);
		Restorer restorer = new Restorer(parameters); // Could also use the old one. I'm just making sure that it also works with a new instance

		System.out.println("\ntokenizer. Vocabulary size: " + parameters.tokenCount + " tokens");
		
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
			
			int i = invalidTests + failedToTokenize + failedToRestore + incorrectRestore + successes;
			if (i % (paths.size()/100) == 0) {
				System.out.println(i + " of " + paths.size() + " tested");
			}
			
			try {
				Game originalGame = GameLoader.loadGameFromFile(path.toFile());
			
				try {
					List<Integer> tokens = tokenizer.tokenizeGame(originalGame);
					
					String restoredString = restorer.restoreAsString(tokens);
	
					try {
						Game restoredGame = (Game) Compiler.compileTest(new Description(restoredString), false);
						
						if (!compareGames(originalGame, restoredGame)) {
							if (verbose) {
								System.out.println("The restored game is not identical to the original " + path.toString());
								System.out.println("Original vs Restored strings");
								System.out.println(squishSpaces(originalGame.description().tokenForest().tokenTrees().get(0).toString()));
								System.out.println(squishSpaces(restoredString));
								System.out.println("---------------------------------------- # ----------------------------------------");
							}
							
							incorrectRestore++;
							continue;
						}
						
						successes++;
						
	
					} catch (RuntimeException e) {
						if (verbose) {
							System.out.println("An exception occured while attempting to restore " + path.toString());
							System.out.println("Original vs Restored strings");
							System.out.println(squishSpaces(originalGame.description().tokenForest().tokenTrees().get(0).toString()));
							System.out.println(squishSpaces(restoredString));
							System.out.println("---------------------------------------- # ----------------------------------------");
		
							e.printStackTrace();
						}
						
						failedToRestore++;
						continue;
					}	
					
				} catch (RuntimeException e) {
					if (verbose) {
						System.out.println("An exception occured while attempting to tokenize " + path.toString());
						System.out.println("Original string");
						System.out.println(squishSpaces(originalGame.description().tokenForest().tokenTrees().get(0).toString()));
						System.out.println("---------------------------------------- # ----------------------------------------");
		
						e.printStackTrace();
					}
					
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
	
	
	public static String squishSpaces(String str) {
		str = str.replaceAll("\\s+", " ");
		str = str.replace(" )", ")");
		str = str.replace(" )", ")");
		str = str.replace(" }", "}");
		str = str.replace("( ", "(");
		str = str.replace("{ ", "{");
		return str;
	}
	
	public static boolean compareGames(Game game1, Game game2) {
		String str1 = game1.description().tokenForest().tokenTrees().toString();
		String str2 = game1.description().tokenForest().tokenTrees().toString();
		return anonymizeSubstrings(str1).equals(anonymizeSubstrings(str2));
	}
	
	public static String anonymizeSubstrings(String str) {
		HashMap<String, String> dict = new HashMap();

		Pattern pattern = Pattern.compile("\"(.*?)\"");
		Matcher matcher = pattern.matcher(str);

		for (String sub: matcher.results().map(MatchResult::group).toArray(String[]::new)) {
			if (!dict.containsKey(sub)) {
				dict.put(sub, "\"string " + dict.size() + '"');
			}
		}
		
		for (String original: dict.keySet()) {
			str = str.replace(original, dict.get(original));
		}
		
		return str;
	}
}
