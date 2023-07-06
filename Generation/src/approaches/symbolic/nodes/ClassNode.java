package approaches.symbolic.nodes;

import approaches.symbolic.SymbolMapper;
import approaches.symbolic.SymbolMapper.MappedSymbol;
import main.grammar.Symbol;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import annotations.Opt;
import annotations.Or;
import annotations.Or2;
import annotations.And;
import annotations.And2;

public class ClassNode extends GeneratorNode {
    ClassNode(MappedSymbol symbol, GeneratorNode parent) {
        super(symbol, parent);
        assert !symbol.path().equals("game.Game");
    }

    Object instantiate() {
        List<Object> arguments = parameterSet.stream().map(param -> param != null? param.compile():null).toList();

        // TODO how to know whether to use constructor or static .construct();
        for (Method method: symbol.cls().getMethods()) {
            if (method.getName().equals("construct")) {
                // TODO don't ignore InvocationTargetException
                try {
                    return method.invoke(null, arguments.toArray());
                } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException ignored) {
//                  if (Objects.equals(symbol.toString(), "<operators.foreach.forEach>") && method.getParameterTypes().length == 11) {
//                    System.out.println("Failed to instantiate " + symbol + " with " + arguments);
//                    System.out.println(arguments.stream().map(o -> o==null? null:o.getClass()).toList());
//                    System.out.println(Arrays.toString(method.getParameterTypes()));

//                }
                }
            }
        }

        for (Constructor<?> constructor: symbol.cls().getConstructors()) {
            try {
                return constructor.newInstance(arguments.toArray());
            }catch (IllegalArgumentException | IllegalAccessException | InstantiationException | InvocationTargetException ignored) {
                if (Objects.equals(symbol.path(), "int") && constructor.getParameterTypes().length == 11) {
                    System.out.println("Failed to instantiate " + symbol + " with " + arguments);
                    System.out.println(arguments.stream().map(o -> o==null? null:o.getClass()).toList());
                    System.out.println(Arrays.toString(constructor.getParameterTypes()));

                    throw new RuntimeException(ignored);
                }
            }

//            catch (InvocationTargetException e) {
//                throw new RuntimeException(e);
//            }
        }

        throw new RuntimeException("Failed to compile: " + symbol);
    }

    boolean validateTypes(Class<?>[] parameterTypes) {
        if (parameterTypes.length != parameterSet.size())
            return false;

        for (int i=0; i < parameterTypes.length; i++) {
            Class<?> methodClass = parameterTypes[i];
            Symbol parameterSymbol = parameterSet.get(i).symbol;

            if (parameterSymbol != null
                    && !methodClass.isAssignableFrom(parameterSymbol.cls())
                    && !methodClass.isAssignableFrom(parameterSymbol.returnType().cls())) {
                return false;
            }
        }

        return true;
    }

    boolean validateOptional(Annotation[] parameterAnnotations) {
        for (int i=0; i < parameterAnnotations.length; i++) {
            if (parameterSet.get(i) == null && !(parameterAnnotations[i] instanceof Opt) && !(parameterAnnotations[i] instanceof Or) && !(parameterAnnotations[i] instanceof Or2)) {
                return false;
            }
        }

        return true;
    }

    boolean validateOr(Annotation[] parameterAnnotations) {
        int group = 0;
        boolean empty = true;
        for (int i=0; i < parameterAnnotations.length; i++) {
            boolean newGroup = true;
            if (parameterAnnotations[i] instanceof Or && group != 1) {
                group = 1;
            } else if (parameterAnnotations[i] instanceof Or2 && group != 2) {
                group = 2;
            } else if (!(parameterAnnotations[i] instanceof Or) && !(parameterAnnotations[i] instanceof Or2) && group != 0) {
                group = 0;
            } else {
                newGroup = false;
            }

            // Check if previous group was empty
            if (newGroup && empty && i > 0 && !(parameterAnnotations[i-1] instanceof Opt)) {
                return false;
            }

            // Check if two parameters are active in the same group
            if (!newGroup && group != 0 && parameterSet.get(i) != null && !empty) {
                return false;
            }

            if (newGroup)
                empty = true;

            if (parameterSet.get(i) != null)
                empty = false;
        }

        // Check if last group was empty
        if (group != 0 && empty && !(parameterAnnotations[parameterAnnotations.length-1] instanceof Opt)) {
            return false;
        }

        return true;
    }

    boolean validateAnd(Annotation[] parameterAnnotations) {
        int group = 0;
        boolean empty = true;
        for (int i=0; i < parameterAnnotations.length; i++) {
            boolean newGroup = true;
            if (parameterAnnotations[i] instanceof And && group != 1) {
                group = 1;
            } else if (parameterAnnotations[i] instanceof And2 && group != 2) {
                group = 2;
            } else if (!(parameterAnnotations[i] instanceof And) && !(parameterAnnotations[i] instanceof And) && group != 0) {
                group = 0;
            } else {
                newGroup = false;
            }

            // Check if previous group was empty
            if (newGroup && empty && i > 0 && !(parameterAnnotations[i-1] instanceof Opt)) {
                return false;
            }

            // Check if two parameters are active in the same group
            if (!newGroup && group != 0 && parameterSet.get(i) != null && !empty) {
                return false;
            }

            if (newGroup)
                empty = true;

            if (parameterSet.get(i) != null)
                empty = false;
        }

        // Check if last group was empty
        if (group != 0 && empty && !(parameterAnnotations[parameterAnnotations.length-1] instanceof Opt)) {
            return false;
        }

        return true;
    }


    public List<GeneratorNode> nextPossibleParameters(SymbolMapper symbolMapper) {
        List<MappedSymbol> partialParameters = parameterSet.stream().map(node -> node.symbol).toList();
        List<MappedSymbol> possibleSymbols = symbolMapper.nextPossibilities(symbol, partialParameters);
//        if (symbol.rule() != null)
//            System.out.println("Symbol: " + symbol.path() + " clauses: " + symbol.rule().rhs().stream().map(c -> c.symbol().path()).toList());
//
//        possibleSymbols.stream().forEach(s -> System.out.println("Possible: " + s.path() + " " + s.usedInGrammar()));
        return possibleSymbols.stream().map(s -> GeneratorNode.fromSymbol(s, this)).toList();
    }

    @Override
    public String toString() {
        //.grammarLabel() TODO
        return "(" + symbol.path() + "; " + String.join(" ", parameterSet.stream().map(GeneratorNode::toString).toList()) + ")";
    }

    @Override
    String buildDescription() {
        String label = "";
        if (symbol.label != null)
            label = symbol.label + ":";

        String parameterString = String.join(" ", parameterSet.stream().filter(s -> !(s instanceof EmptyNode || s instanceof EndOfClauseNode)).map(GeneratorNode::description).toList());
        if (parameterString.length() > 0)
            parameterString = " " + parameterString;

        String close = "";
        if (complete)
            close = ")";

        return label + "(" + symbol.token() + parameterString + close;
    }

//    @Override
//    public String buildDescription() {
//        StringBuilder parameterString = new StringBuilder();
//        for (GeneratorNode parameter: parameterSet) {
//            if (parameter instanceof EmptyNode || parameter instanceof EndOfClauseNode) continue;
//            parameterString.append(" ");
//            if (parameter.symbol.label != null) {
//                parameterString.append(parameter.symbol.label);
//                parameterString.append(":");
//            }
//            parameterString.append(parameter.buildDescription());
//        }
//
//        return "(" + symbol.token() + parameterString + ")";
//    }
}
