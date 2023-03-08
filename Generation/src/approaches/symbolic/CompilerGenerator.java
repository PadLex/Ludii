package approaches.symbolic;

import compiler.Compiler;
import game.Game;
import game.equipment.Equipment;
import game.equipment.Item;
import game.equipment.component.Piece;
import game.equipment.container.board.Board;
import game.equipment.other.Regions;
import game.functions.booleans.is.Is;
import game.functions.booleans.is.IsConnectType;
import game.functions.dim.DimConstant;
import game.functions.graph.generators.basis.hex.DiamondOnHex;
import game.functions.graph.generators.basis.hex.Hex;
import game.functions.graph.generators.basis.hex.HexShapeType;
import game.functions.region.RegionFunction;
import game.functions.region.sites.Sites;
import game.functions.region.sites.SitesIndexType;
import game.functions.region.sites.SitesSideType;
import game.players.Players;
import game.rules.Rules;
import game.rules.end.End;
import game.rules.end.If;
import game.rules.end.Result;
import game.rules.play.Play;
import game.rules.play.moves.decision.Move;
import game.rules.play.moves.decision.MoveSiteType;
import game.types.play.ResultType;
import game.types.play.RoleType;
import game.util.directions.CompassDirection;
import game.util.moves.To;
import grammar.Grammar;
import main.grammar.*;
import main.grammar.ebnf.EBNF;
import main.options.UserSelections;
import parser.Parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CompilerGenerator {
    public static void main(String[] args) {
        /*
        List<GrammarRule> rules = Grammar.grammar().getRules();

        // Find Game rule (stream)
        GrammarRule gameRule = null;
        for (GrammarRule rule : rules) {
            if (rule.lhs().name().equals("Game")) {
                gameRule = rule;
                break;
            }
        }

        System.out.println(gameRule.rhs());

        System.out.println(Arrays.toString(Game.class.getConstructors()[0].getParameterTypes()));
        System.out.println(Arrays.toString(Game.class.getConstructors()[0].getParameterAnnotations()));
*/
        //playingCustomCallTree();
        playingWithCompiledCallTree();
    }

    static void playingCustomCallTree() {
        Game game = new Game(
                "hex",
                new Players(2),
                null,
                new Equipment(new Item[]{
                        new Board(Hex.construct(HexShapeType.Diamond, new DimConstant(11), null), null, null, null, null,  null, null),
                        new Piece("Marker", RoleType.Each, null, null, null, null, null, null),
                        new Regions(null, RoleType.P1, null, null, new RegionFunction[]{
                                Sites.construct(SitesSideType.Side, null, null, null, CompassDirection.NE),
                                Sites.construct(SitesSideType.Side, null, null, null, CompassDirection.SW)
                        }, null, null, null),
                        new Regions(null, RoleType.P2, null, null, new RegionFunction[]{
                                Sites.construct(SitesSideType.Side, null, null, null, CompassDirection.NW),
                                Sites.construct(SitesSideType.Side, null, null, null, CompassDirection.SE)
                        }
                                , null, null, null)
                }),
                new Rules(
                        null,
                        null,
                        new Play(
                                Move.construct(MoveSiteType.Add, null,
                                        new To(null,
                                                Sites.construct(SitesIndexType.Empty, null, null), null, null, null, null, null),
                                        null, null, null
                                )
                        ),
                        new End(
                                new If(
                                        Is.construct(IsConnectType.Connected, null, null, null, null, null, RoleType.Mover, null),
                                        null, null,
                                        new Result(RoleType.Mover, ResultType.Win)
                                ),
                                null
                        )
                )
        );

        Game game2 = new Game(
                "hex",
                new Players(2),
                null,
                new Equipment(new Item[]{
                        new Board(new DiamondOnHex(new DimConstant(11), null), null, null, null, null,  null, null),
                        new Piece("Marker", RoleType.Each, null, null, null, null, null, null),
                        new Regions(null, RoleType.P1, null, null, new RegionFunction[]{
                                Sites.construct(SitesSideType.Side, null, null, null, CompassDirection.NE),
                                Sites.construct(SitesSideType.Side, null, null, null, CompassDirection.SW)
                        }, null, null, null),
                        new Regions(null, RoleType.P2, null, null, new RegionFunction[]{
                                Sites.construct(SitesSideType.Side, null, null, null, CompassDirection.NW),
                                Sites.construct(SitesSideType.Side, null, null, null, CompassDirection.SE)
                        }
                                , null, null, null)
                }),
                new Rules(
                        null,
                        null,
                        new Play(
                                Move.construct(MoveSiteType.Add, null,
                                        new To(null,
                                                Sites.construct(SitesIndexType.Empty, null, null), null, null, null, null, null),
                                        null, null, null
                                )
                        ),
                        new End(
                                new If(
                                        Is.construct(IsConnectType.Connected, null, null, null, null, null, RoleType.Mover, null),
                                        null, null,
                                        new Result(RoleType.Mover, ResultType.Win)
                                ),
                                null
                        )
                )
        );

        System.out.println(game.willCrash());
    }

    static void playingWithCompiledCallTree() {
        String str =
                "(game \"Hex\" \n" +
                        "    (players 2) \n" +
                        "    (equipment { \n" +
                        "        (board (hex Diamond 11)) \n" +
                        "        (piece \"Marker\" Each)\n" +
                        "        (regions P1 {(sites Side NE) (sites Side SW) })\n" +
                        "        (regions P2 {(sites Side NW) (sites Side SE) })\n" +
                        "    }) \n" +
                        "    (rules \n" +
                        "        (play (move Add (to (sites Empty))))\n" +
                        "        (end (if (is Connected Mover) (result Mover Win))) \n" +
                        "    )\n" +
                        ")";

        final Description description = new Description(str);
        final UserSelections userSelections = new UserSelections(new ArrayList<String>());
        final Report report = new Report();

        //Parser.expandAndParse(description, userSelections, report, false);
        Game game = (Game) Compiler.compileTest(description, false);

        //System.out.println("raw: " + description.raw());
        //System.out.println("tokenForest: " + description.tokenForest().tokenTree());
        //printParseTree(description.parseTree(), 0);

        Call callTree = description.callTree();



        printCallTree(description.callTree(), 0);


    }

    static void printParseTree(ParseItem item, int depth) {
        for (int i = 0; i < depth; i++) {
            System.out.print("    ");
        }
        System.out.print(item.token().name());

        System.out.print("\n");

        for (ParseItem child: item.arguments()) {
            printParseTree(child, depth + 1);
        }
    }

    static void printCallTree(Call call, int depth) {
        for (int i = 0; i < depth; i++) {
            System.out.print("    ");
        }

        if (call.object() != null)
            System.out.print(call.type() + ": " + call.cls() + " = " + call.object());
        else
            System.out.print(call.type() + ": " + call.cls());

        System.out.print("\n");

        for (Call child: call.args()) {
            printCallTree(child, depth + 1);
        }
    }
}
