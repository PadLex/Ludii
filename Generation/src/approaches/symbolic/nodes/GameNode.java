package approaches.symbolic.nodes;

import game.Game;
import main.grammar.Symbol;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class GameNode extends ClassNode {
    GameNode(Symbol symbol) {
        super(symbol);
        assert symbol.path().equals("game.Game") || symbol.path().equals("game.match.Match");
    }

    @Override
    public Game compile() {
        Game game = (Game) super.compile();

        game.create();

        return game;
    }
}
