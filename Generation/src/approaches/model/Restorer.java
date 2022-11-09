package approaches.model;

import java.util.List;

import compiler.Compiler;
import game.Game;
import main.grammar.Description;


/* NumericTokens to LudiiTokens */

public class Restorer {

	TokenizationParameters parameters;

	public Restorer(TokenizationParameters parameters) {
		this.parameters = parameters;
	}

	public String restoreAsString(List<Integer> tokens) {
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < tokens.size(); i++) {
			str.append(restoreNumericToken(tokens.get(i)));
		}

		return str.toString();
	}

	public Game restoreGame(List<Integer> tokens) {
		try {
			return (Game) Compiler.compileTest(new Description(restoreAsString(tokens)), false);
		} catch (final Exception e) {
		}

		return null;
	}

	private String restoreNumericToken(int token) {
		switch(parameters.classifyToken(token)) {
			case BASE:
				return restoreBase(token);
			case INT:
				return Integer.toString(restoreInt(token)) + ' ';
			case FLOAT:
				return Float.toString(restoreFloat(token)) + ' ';
			case BOOLEAN:
				return restoreBoolean(token)? "True ":"False ";
			case COMPONENT:
				return restoreComponent(token) + ' ';
			case CONTAINER:
				return restoreContainer(token) + ' ';
			case STRING:
				return restoreString(token) + ' ';
			case SYMBOL:
				return parameters.idToSymbol.get(token - parameters.symbolStart) + ' ';
			case CLAUSE:
				return parameters.idToClause.get(token - parameters.clauseStart) + ':';
		}

		throw new RuntimeException(parameters.classifyToken(token) + " is not implemented!");
	}
	
	private String restoreBase(int numericToken) {
		if (numericToken == parameters.openClassToken)
			return "(";

		if (numericToken == parameters.closeClassToken)
			return ") ";

		if (numericToken == parameters.openArrayToken)
			return "{";

		if (numericToken == parameters.closeArrayToken)
			return "} ";
		
		throw new RuntimeException("Not a base token");
	}

	private int restoreInt(int numericToken) {
		return numericToken - parameters.intStart - parameters.intTokens / 2;
	}

	private float restoreFloat(int numericToken) {
		return parameters.floats[numericToken - parameters.floatStart];
	}

	private boolean restoreBoolean(int numericToken) {
		return numericToken - parameters.booleanStart == 1;
	}

	private String restoreComponent(int numericToken) {
		int finalIndex = numericToken - parameters.componentStart;
		int nameIndex = finalIndex / parameters.maxPlayers;
		int playerIndex = finalIndex % parameters.maxPlayers;
						
		if (playerIndex > 0)
			return '"' + parameters.replacementComponents[nameIndex] + playerIndex + '"';
		
		return '"' + parameters.replacementComponents[nameIndex] + '"';
	}
	
	private String restoreContainer(int numericToken) {
		int finalIndex = numericToken - parameters.containerStart;
		int nameIndex = finalIndex / parameters.maxPlayers;
		int playerIndex = finalIndex % parameters.maxPlayers;
						
		if (playerIndex > 0)
			return '"' + parameters.containers[nameIndex] + playerIndex + '"';
		
		return '"' + parameters.containers[nameIndex] + '"';
	}

	private String restoreString(int numericToken) {
		int finalIndex = numericToken - parameters.stringStart;
		int nameIndex = finalIndex / parameters.maxPlayers;
		int playerIndex = finalIndex % parameters.maxPlayers;
						
		if (playerIndex > 0)
			return '"' + parameters.replacementStrings[nameIndex] + playerIndex + '"';
		
		return '"' + parameters.replacementStrings[nameIndex] + '"';
	}
	
	
	/* Debugging functions */

	public String dictionary() {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < parameters.tokenCount; i++) {
			String str = "null";

			try {
				str = restoreNumericToken(i);
			} catch (RuntimeException ignored) {
			}

			sb.append(str).append(", ");
		}

		return sb.toString();
	}
}
