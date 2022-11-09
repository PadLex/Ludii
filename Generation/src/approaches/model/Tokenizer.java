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
		
		System.out.println(componentNames);
		System.out.println(containerNames);
		
		tokenizeTree(game.description().tokenForest().tokenTrees(), tokens);

		return tokens;
	}

	private void tokenizeTree(Collection<Token> tree, List<Integer> tokens) {

		for (Token token : tree) {
			
			if (token.parameterLabel() != null) {
				tokens.add(parameters.clauseStart + parameters.clauseToId.get(token.parameterLabel()));
			}
			
			if (token.isClass())
				tokens.add(parameters.openClassToken);
			else if (token.isArray())
				tokens.add(parameters.openArrayToken);

			if (token.name() != null) {
				tokens.add(tokenizeLudiiToken(token));
			}

			if (!token.isTerminal()) {
				tokenizeTree(token.arguments(), tokens);
			}

			if (token.isClass())
				tokens.add(parameters.closeClassToken);
			else if (token.isArray())
				tokens.add(parameters.closeArrayToken);

		}
	}

	private int tokenizeLudiiToken(Token ludiiToken) {
		// TODO get symbol from token instead of using strings
		String name = ludiiToken.name();
		
		try {
			return parameters.symbolStart + parameters.symbolToId.get(name);
		} catch (NullPointerException ignored) {}

		if (name.charAt(0) == '"') {
			String label = extractLabel(name);
			int playerIndex = extractPlayerIndex(name);
			
			if (playerIndex >= parameters.maxPlayers)
				throw new RuntimeException("Too many players " + name);
			
			if (componentNames.contains(label))
				return tokenizeComponent(label, playerIndex);
			
			if (containerNames.contains(label))
				return tokenizeContainer(label, playerIndex);

			return tokenizeString(label, playerIndex);
		}

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

		throw new RuntimeException("Failed to tokenize: " + name);
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
