package approaches.model;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import game.Game;
import main.grammar.Token;


/* LudiiTokens to NumericTokens */

public class Tokenizer {

	TokenizationParameters parameters;

	private ArrayList<String> previousStrings;
	private ArrayList<String> previousComponents;
	private List<Integer> tokens;
	private HashSet<String> componentNames;
	private HashSet<String> containerNames;


	public Tokenizer(TokenizationParameters parameters) {
		this.parameters = parameters;
	}

	public List<Integer> tokenizeGame(Game game) {
		
		
		previousStrings = new ArrayList<>();
		previousComponents = new ArrayList<>();
		tokens = new ArrayList<>();
		
		componentNames = (HashSet<String>) Arrays.stream(game.equipment().components()).map(p -> extractLabel(p.name())).collect(Collectors.toSet());
		containerNames = (HashSet<String>) Arrays.stream(game.equipment().containers()).map(p -> extractLabel(p.name())).collect(Collectors.toSet());
		
		//System.out.println(componentNames);
		//System.out.println(containerNames);
		
		tokenizeTree(game.description().tokenForest().tokenTrees(), tokens);

		return tokens;
	}

	private void tokenizeTree(Collection<Token> tree, List<Integer> tokens) {

		for (Token token : tree) {
			
			if (token.parameterLabel() != null) 
				tokens.add(tokenizeClause(token.parameterLabel()));
			
			if (token.isClass())
				tokens.add(TokenizationParameters.openClassToken);
			else if (token.isArray())
				tokens.add(TokenizationParameters.openArrayToken);

			String name = token.name();
			if (name != null) {
				tokenizeLudiiToken(name);
			}

			if (!token.isTerminal()) 
				tokenizeTree(token.arguments(), tokens);

			if (token.isClass())
				tokens.add(parameters.closeClassToken);
			else if (token.isArray())
				tokens.add(parameters.closeArrayToken);

		}
	}
	
	private void tokenizeLudiiToken(String name) {
		
		try {
			tokens.add(tokenizeSymbol(name));
			return;
		} catch (NullPointerException ignored) {}
		
		try {
			tokens.add(findPrimitiveToken(name));
			return;
		} catch (RuntimeException ignored) {}
		
		// "1,E,N1,W"
		if (name.matches("\\\"\\S*,\\S*\\\"")) {
			tokens.add(TokenizationParameters.stringedTokensDelimeter);
			
			String[] subTokens = name.substring(1, name.length()-1).split(",");
			for (String subToken: subTokens) {
				tokenizeLudiiToken(subToken);
			}
			
			tokens.add(TokenizationParameters.stringedTokensDelimeter);			
		}
		
		// N4 or "B2"
		else if (name.matches("\\\"?[A-Z]{1,2}[0-9]*\\\"?")) {	
			String letters = name.replaceAll("[^A-Z]", "");
			String numbers = name.replaceAll("[^0-9]", "");
			
			//System.out.println(name + ", " + letters + ", " + numbers);

			
			boolean hasQuotes = name.charAt(0) == '"';

			if (hasQuotes)
				tokens.add(TokenizationParameters.stringedTokensDelimeter);
			
			
			tokens.add(tokenizeSymbol(letters.substring(0, 1)));
						
			if (letters.length() == 2) {
				tokens.add(tokens.size() - 1, TokenizationParameters.tokenJoiner);
				tokens.add(tokenizeSymbol(letters.substring(1)));
			}
			
			if (numbers.length() > 0) {
				tokens.add(tokens.size() - 1, TokenizationParameters.tokenJoiner);
				tokens.add(tokenizeInt(Integer.parseInt(numbers)));
			}
			
			if (hasQuotes)
				tokens.add(TokenizationParameters.stringedTokensDelimeter);
			
		}
		
		else if (name.charAt(0) == '"') {
			String label = extractLabel(name);
			int playerIndex = extractPlayerIndex(name);
			
			if (playerIndex >= parameters.maxPlayers)
				throw new RuntimeException("Too many players " + name);
			
			if (componentNames.contains(label))
				tokens.add(tokenizeComponent(label, playerIndex));
			
			else if (containerNames.contains(label))
				tokens.add(tokenizeContainer(label, playerIndex));
			
			else
				tokens.add(tokenizeString(label, playerIndex));
			
			return;
		}
		else
			throw new RuntimeException("Failed to tokenize " + name);
	}

	private int findPrimitiveToken(String name) {
		
		try {
			return tokenizeInt(Integer.parseInt(name));
		} catch (NumberFormatException ignored) {}

		try {
			return tokenizeFloat(Float.parseFloat(name));
		} catch (NumberFormatException ignored) {}

		if (name.toLowerCase().equals("true"))
			return tokenizeBoolean(true);
		else if (name.toLowerCase().equals("false"))
			return tokenizeBoolean(false);

		throw new RuntimeException("Not a primitive token: " + name);
	}

	// TODO prevent token overflow by returning -1 or raising exception
	private int tokenizeInt(int n) {
		int range = (parameters.intTokens - 1) / 2;
		if (n < -range || n > range)
			throw new RuntimeException(n + " int is out of range " + range);

		return parameters.intStart + n + parameters.intTokens / 2;
	}

	private int tokenizeFloat(float f) {
		int i = Arrays.binarySearch(parameters.floats, f);
		
		if (i < 0)
			throw new RuntimeException(f + " float is not supported");

		return parameters.floatStart + i;
	}

	private int tokenizeBoolean(boolean bool) {
		return parameters.booleanStart + (bool ? 1 : 0);
	}

	private int tokenizeComponent(String label, int playerIndex) {
		int nameIndex = previousComponents.indexOf(label);

		if (nameIndex < 0) {
			nameIndex = previousComponents.size();
			previousComponents.add(label);
		}

		int finaIndex = nameIndex * parameters.maxPlayers + playerIndex;

		if (finaIndex >= parameters.componentTokens)
			throw new RuntimeException("Too many unique components " + previousComponents.size());

		return parameters.componentStart + finaIndex;
	}
	
	private int tokenizeContainer(String label, int playerIndex) {
		int nameIndex = Arrays.binarySearch(parameters.containers, label);
		
		if (nameIndex < 0) {
			System.out.println(Arrays.toString(parameters.containers) + "|" + label + "|" + nameIndex);

			throw new RuntimeException("Not a known container " + label);
		}

		int finaIndex = nameIndex * parameters.maxPlayers + playerIndex;

		return parameters.containerStart + finaIndex;
	}

	// TODO understand this better. Can I really replace Track1, Track2, Track with A, A, A or A1, A2, A?
	private int tokenizeString(String label, int playerIndex) {

		int nameIndex = previousStrings.indexOf(label);

		if (nameIndex < 0) {
			nameIndex = previousStrings.size();
			previousStrings.add(label);
		}

		int finaIndex = nameIndex * parameters.maxPlayers + playerIndex;

		if (finaIndex >= parameters.stringTokens)
			throw new RuntimeException("Too many unique strings " + previousStrings.size());

		return parameters.stringStart + finaIndex;
	}
	
	private int tokenizeSymbol(String name) {
		return parameters.symbolStart + parameters.symbolToId.get(name);
	}
	
	private int tokenizeClause(String parameterLabel) {
		return parameters.clauseStart + parameters.clauseToId.get(parameterLabel);
	}
	
	private int extractPlayerIndex(String string) {
		if (string.indexOf(' ') >= 0 || string.matches(".*\\d.*\\d.*"))
			return 0;
		
		try {
			return Integer.parseInt(string.replaceAll("[^0-9]", ""));
		} catch (NumberFormatException ignored) {}
		return 0;
	}
	
	private String extractLabel(String string) {
		return string.replaceAll("\\d","").replace("\"", "");
	}

}
