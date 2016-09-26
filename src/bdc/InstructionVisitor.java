package bdc;

import java.util.List;
import java.util.Map;

import bdc.ConstantPool.ClassReference;
import bdc.ConstantPool.FieldReference;
import bdc.ConstantPool.MethodReference;
import bdc.Type.PrimitiveType;

public interface InstructionVisitor<T> {

    public enum BinaryOperation {
	Add("+"), Subtract("/"), Multiply("*"), Divide("/"), Remainder("%");
	private final String symbol;

	private BinaryOperation(final String symbol) {
	    this.symbol = symbol;
	}

	public String getSymbol() {
	    return symbol;
	}

	public static BinaryOperation fromId(final int id) {
	    return values()[id];
	}
    }

    public enum ShiftType {
	Left, Arithmetic, Logical;

	public static ShiftType fromId(final int id) {
	    return values()[id];
	}
    }

    public enum BitwiseOperationType {
	And, Or, Xor;
	public static BitwiseOperationType fromId(final int id) {
	    return values()[id];
	}
    }

    public enum CompareType {
	EQ, NE, LT, GE, GT, LE;

	public static CompareType fromId(final int id) {
	    return values()[id];
	}
    }

    T nullConstant();

    T integerConstant(int value);

    T longConstant(long value);

    T floatConstant(float f);

    T doubleConstant(double f);

    T stringConstant(String value);

    T binaryOperation(PrimitiveType type, BinaryOperation op, T value0, T value1);

    T negate(PrimitiveType type, T value);

    T shift(PrimitiveType type, ShiftType fromId, T left, T right);

    T bitwiseOperation(PrimitiveType type, BitwiseOperationType fromId, T left, T right);

    T convert(PrimitiveType from, PrimitiveType to, T value);

    T compare(PrimitiveType type, T left, T right);

    T checkedCast(ClassReference type, T value);

    T instanceOf(ClassReference type, T value);

    void monitorEnter(T monitor);

    void monitorExit(T monitor);

    T loadElement(PrimitiveType elementType, T arrayref, T index);

    void storeElement(T arrayref, T index);

    T loadStaticField(FieldReference fieldReference);

    void storeStaticField(FieldReference fieldReference, T value);

    T loadField(FieldReference fieldReference, T target);

    void storeField(FieldReference fieldReference, T target, T value);

    List<T> invokeVirtual(MethodReference methodReference, T target, List<T> args);

    List<T> invokeSpecial(MethodReference methodReference, T target, List<T> args);

    List<T> invokeStatic(MethodReference methodReference, List<T> args);

    List<T> invokeInterface(MethodReference methodReference, T target, List<T> args);

    T newInstance(ClassReference classReference);

    T newPrimitiveArray(PrimitiveType type, T size);

    T newArray(ClassReference type, T size);

    T arrayLength(T array);

    void jumpIf(PrimitiveType type, int offset, CompareType compareType, T left, T right);

    void jump(int unsignedShort);

    void jumpTable(T value, int defaultOffset, Map<Integer, Integer> table);

    void returnValue(PrimitiveType type, T value);

    void returnVoid();

    void returnError(T exception);

}