package approaches.symbolic;

import game.functions.dim.DimConstant;
import game.functions.graph.generators.basis.hex.HexShapeType;
import main.grammar.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import grammar.Grammar;
import other.BaseLudeme;

public class GeneratorNode {

    private GeneratorNode parent;
    private Symbol symbol;
    private Object primitiveValue;
    private List<GeneratorNode> parameterSet;

    public GeneratorNode(Symbol symbol) {
        this.symbol = symbol;
    }

    public Object compile() {

        if (primitiveValue != null) {
            Class clazz = symbol.cls();
            System.out.println(Arrays.toString(clazz.getConstructors()));
            for (Constructor constructer: symbol.cls().getConstructors()) {

                try {
                    return (BaseLudeme) constructer.newInstance(primitiveValue);
                } catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        if (symbol.cls().isEnum()) {
            try {
                return symbol.cls().getMethod("valueOf", String.class).invoke(null, symbol.name());
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        /*
        if (symbol.isTerminal()) {
            Class clazz = symbol.cls();

            return (BaseLudeme) (Object) Enum.valueOf((Class<Enum>) clazz, clazz.getSimpleName());

            System.out.println("terminal: " + Arrays.toString(clazz.getMethods()));

            try {
                return (BaseLudeme) clazz.getMethod(clazz.getSimpleName(), null).invoke(null, null);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }*/

        //System.out.println(symbol.name() + " parameterSet: " + parameterSet);

        List<Object> arguments = parameterSet.stream().map(param -> param != null? param.compile():null).toList();

        //System.out.println("Args " + arguments.stream().filter(Objects::nonNull).map(Object::getClass).toList());

        // TODO how to know whether to use constructor or static .construct();
        for (Method method: symbol.cls().getMethods()) {
            if (method.getName().equals("construct")) {
                System.out.println("Found " + Arrays.toString(method.getParameterTypes()));

                try {
                    return (BaseLudeme) method.invoke(null, arguments.toArray());
                } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                    System.out.println("Skipped Method " + e.getMessage());
                }
            }
        }

        for (Constructor<?> constructor: symbol.cls().getConstructors()) {
            try {
                return (BaseLudeme) constructor.newInstance(arguments.toArray());
            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException |
                     InstantiationException e) {
                System.out.println("Skipped Constructor " + e.getMessage());
            }
        }

        throw new RuntimeException("Failed to compile " + symbol);
    }

    public GeneratorNode parent() {
        return parent;
    }

    public Symbol symbol() {
        return symbol;
    }

    public static void main(String[] args) {

        SymbolMapper symbolMapper = new SymbolMapper(Grammar.grammar().symbols().stream().filter(Symbol::usedInGrammar).toList());

        // Find Game symbol
        GeneratorNode hex = new GeneratorNode(Grammar.grammar().findSymbolByPath("game.functions.graph.generators.basis.hex.Hex"));

        List<GeneratorNode> arguments = new ArrayList<>();
        List<Symbol> nextPossibleSymbols = symbolMapper.nextPossibilities(hex.symbol, arguments.stream().map(GeneratorNode::symbol).toList());
        System.out.println("nextPossibleSymbols " + nextPossibleSymbols.stream().map(s -> s!=null? s.name():"null").toList());

        GeneratorNode diamond = new GeneratorNode(nextPossibleSymbols.stream().filter(Objects::nonNull).filter(s -> Objects.equals(s.name(), "Diamond")).findFirst().get());
        GeneratorNode dim = new GeneratorNode(Grammar.grammar().findSymbolByPath("game.functions.dim.DimConstant"));
        dim.primitiveValue = 11;

        System.out.println("hex " + hex.symbol);
        System.out.println("diamond " + diamond.symbol);
        System.out.println("dim " + dim.symbol);

        arguments.add(diamond);
        arguments.add(dim);
        arguments.add(null);

        hex.parameterSet = arguments;



        System.out.println(hex.compile());

    }
}
