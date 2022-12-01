package approaches.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import compiler.Compiler;
import game.Game;
import grammar.Grammar;
import main.grammar.Description;
import main.grammar.Symbol;
import other.GameLoader;
import java.nio.file.Path;

public class TokenUsage {
	
	public static void main(String[] args) {
		TokenizationParameters parameters = TokenizationParameters.completeParameters();
		Tokenizer tokenizer = new Tokenizer(parameters);
		Restorer restorer = new Restorer(parameters);

		List<File> files = new ArrayList<>();
		
		try {
			Files.find(Paths.get("../Common/res/lud/board"),
			           Integer.MAX_VALUE,
			           (filePath, fileAttr) -> fileAttr.isRegularFile() && filePath.toString().toLowerCase().endsWith(".lud"))
			        .forEach(path -> files.add(path.toFile()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		HashSet<String> usedBaseTokens = new HashSet<>();
		HashSet<String> usedSymbols = new HashSet<>();
		HashSet<String> usedIntegers = new HashSet<>();
		HashSet<String> usedFloats = new HashSet<>();
		HashSet<String> usedBooleans = new HashSet<>();
		HashSet<String> usedComponents = new HashSet<>();
		HashSet<String> usedContainers = new HashSet<>();
		HashSet<String> usedStrings = new HashSet<>();

		
		for (File file: files) {
			System.out.println(file.getPath());
			
			try {

				Game originalGame = GameLoader.loadGameFromFile(file);
			
				try {
					List<Integer> tokens = tokenizer.tokenizeGame(originalGame);
					
					for (int token: tokens) {
						
						if (token == 4) {
							usedBaseTokens.add("&");
							continue;
						}
						
						String restored = restorer.restoreNumericToken(token);
												
						switch (parameters.classifyToken(token)) {
						case BASE:
							usedBaseTokens.add(restored);
							break;
						case BOOLEAN:
							usedBooleans.add(restored);
							break;
						case CLAUSE:
							break;
						case COMPONENT:
							usedComponents.add(restored);
							break;
						case CONTAINER:
							usedContainers.add(restored);
							break;
						case FLOAT:
							usedFloats.add(restored);
							break;
						case INT:
							usedIntegers.add(restored);
							break;
						case STRING:
							usedStrings.add(restored);
							break;
						case SYMBOL:
							usedSymbols.add(restored);
							break;
						}
							
					}	
					
				} catch (RuntimeException e) {
					System.out.println("An exception occured while attempting to tokenize or restore" + file.getPath());
					System.out.println("---------------------------------------- # ----------------------------------------");
	
					e.printStackTrace();
					continue;
				}
			} catch (Exception e) {
				System.out.println("Bad file. Failed to compile " + file.getPath());
				System.out.println("---------------------------------------- # ----------------------------------------");
				continue;
			}			
		}
		
		System.out.println(usedBaseTokens);
		System.out.println(usedSymbols);
		System.out.println(usedIntegers);
		System.out.println(usedFloats);
		System.out.println(usedBooleans);
		System.out.println(usedComponents);
		System.out.println(usedContainers);
		System.out.println(usedStrings);
				
	}
}
