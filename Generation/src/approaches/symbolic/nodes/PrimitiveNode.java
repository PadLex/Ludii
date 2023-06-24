package approaches.symbolic.nodes;

import approaches.symbolic.SymbolMapper;
import approaches.symbolic.SymbolMapper.MappedSymbol;
import game.functions.booleans.BooleanConstant;
import game.functions.dim.DimConstant;
import game.functions.floats.FloatConstant;
import game.functions.floats.FloatFunction;
import game.functions.ints.IntConstant;
import game.functions.ints.IntFunction;

import java.util.List;
import java.util.Objects;

public class PrimitiveNode extends GeneratorNode {

    public enum PrimitiveType {INT, FLOAT, DIM, STRING, BOOLEAN}
    private Object value;

    PrimitiveNode(MappedSymbol symbol, GeneratorNode parent) {
        super(symbol, parent);
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public void setUnparsedValue(String strValue) {
        switch (getType()) {
            case INT -> value = Integer.parseInt(strValue);
            case DIM -> value = new DimConstant(Integer.parseInt(strValue));
            case FLOAT -> value = Float.parseFloat(strValue);
            case STRING -> value = strValue;
            case BOOLEAN -> value = Boolean.parseBoolean(strValue);
        }

        if (IntFunction.class.isAssignableFrom(symbol.cls()))
            value = new IntConstant((Integer) value);

        if (FloatFunction.class.isAssignableFrom(symbol.cls()))
            value = new FloatConstant((Float) value);

        if (BooleanConstant.class.isAssignableFrom(symbol.cls()))
            value = new BooleanConstant((Boolean) value);

    }

    Object instantiate() {
        return value;
    }

    public List<GeneratorNode> nextPossibleParameters(SymbolMapper symbolMapper) {
        return List.of();
    }

    @Override
    public void addParameter(GeneratorNode param) {
        throw new RuntimeException("Primitive nodes are terminal");
    }

    @Override
    public boolean isComplete() {
        return value != null;
    }

    @Override
    public String toString() {
        if (value == null)
            return getType() + "?";

        if (value instanceof String)
            return "\"" + value + "\"";

        String strValue = value.toString();
        if (Objects.equals(strValue, "true"))
            return "True";
        if (Objects.equals(strValue, "false"))
            return "False";

        if (strValue.endsWith(".0"))
            return strValue.substring(0, strValue.length() - 2);

        return strValue;
    }

    @Override
    public PrimitiveNode copyDown() {
        PrimitiveNode clone = (PrimitiveNode) super.copyDown();
        clone.setValue(value);
        return clone;
    }

    public PrimitiveType getType() {
        return typeOf(symbol.path());
    }

    public static PrimitiveType typeOf(String path) {
        switch (path) {
            case "java.lang.Integer", "game.functions.ints.IntConstant" -> {
                return PrimitiveType.INT;
            }

            case "game.functions.dim.DimConstant" -> {
                return PrimitiveType.DIM;
            }

            case "java.lang.Float", "game.functions.floats.FloatConstant" -> {
                return PrimitiveType.FLOAT;
            }

            case "java.lang.Boolean", "game.functions.booleans.BooleanConstant" -> {
                return PrimitiveType.BOOLEAN;
            }

            case "java.lang.String" -> {
                return PrimitiveType.STRING;
            }

            default -> {
                return null;
            }
        }
    }
}
