package approaches.model;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import game.Game;
import main.grammar.Token;


/* LudiiTokens to NumericTokens */

public class Tokenizer {

	TokenizationParameters parameters;

	private ArrayList<String> previousStrings;
	private ArrayList<String> previousSvgs;

	public Tokenizer(TokenizationParameters parameters) {
		this.parameters = parameters;
	}

	public List<Integer> tokenizeGame(Game game) {
		List<Integer> tokens = new ArrayList<>();
		
		previousStrings = new ArrayList<>();
		previousSvgs = new ArrayList<>();

		tokenizeTree(game.description().tokenForest().tokenTrees(), tokens);

		return tokens;
	}

	private void tokenizeTree(Collection<Token> tree, List<Integer> tokens) {

		for (Token token : tree) {

			if (token.parameterLabel() != null) {
				//System.out.println(token.parameterLabel());
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
		} catch (NullPointerException ignored) {
		}

		if (name.charAt(0) == '"') {
			try {
				return tokenizeSvg(name);
			} catch (RuntimeException ignored) {}

			return tokenizeString(name);
		}

		try {
			return tokenizeInt(Integer.parseInt(name));
		} catch (NumberFormatException ignored) {}

		try {
			return tokenizeFloat(Float.parseFloat(name));
		} catch (NumberFormatException ignored) {
		}

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

	private int tokenizeSvg(String string) {

		String svgName = string.toLowerCase().replaceAll("\\d","");

		if (!parameters.svgNames.contains(svgName))
			throw new RuntimeException(svgName + " is not a vald svg name");

		int playerIndex = 0;
		try {
			playerIndex = Integer.parseInt(string.replaceAll("[^0-9]", ""));
		} catch (NumberFormatException ignored) {}

		if (playerIndex >= parameters.maxPlayers)
			throw new RuntimeException("Too many players " + string);


		int nameIndex = previousSvgs.indexOf(svgName);

		if (nameIndex < 0) {
			nameIndex = previousSvgs.size();
			previousSvgs.add(svgName);
		}

		int finaIndex = nameIndex * parameters.maxPlayers + playerIndex;

		if (finaIndex >= parameters.svgTokens)
			throw new RuntimeException("Too many unique svgs " + previousSvgs.size());

		return parameters.svgStart + finaIndex;
	}

	private int tokenizeString(String string) {
		int id = previousStrings.indexOf(string);

		if (id < 0) {
			id = previousStrings.size();
			previousStrings.add(string);
		}

		if (id >= parameters.stringStart + parameters.stringTokens)
			throw new RuntimeException("Too many unique strings " + previousStrings.size());

		return parameters.stringStart + id;
	}
}
