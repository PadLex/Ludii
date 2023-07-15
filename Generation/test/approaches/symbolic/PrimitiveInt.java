package approaches.symbolic;

import grammar.Grammar;

import java.util.Comparator;
import java.util.List;

public class PrimitiveInt {
    public static void main(String[] args) {
        SymbolMapper symbolMapper = new SymbolMapper();

        // TODO, is int handled correctly? I don't think so.
        Grammar.grammar().symbols().stream().max(Comparator.comparingInt(s -> s.rule() == null? 0:s.rule().rhs().size())).ifPresent(s -> System.out.println(s.path() + " " + s.rule().rhs()));
        System.out.println(symbolMapper.parameterMap.get("int"));
        System.out.println(symbolMapper.nextPossibilities(Grammar.grammar().findSymbolByPath("java.lang.Integer"), List.of()));

    }
}
