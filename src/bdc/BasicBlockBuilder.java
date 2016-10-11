package bdc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import bdc.ConstantPool.ClassReference;
import bdc.ConstantPool.FieldReference;
import bdc.ConstantPool.MethodReference;
import bdc.Type.FieldType;
import bdc.Type.MethodType;
import bdc.Type.PrimitiveType;

public class BasicBlockBuilder {

    enum BinaryOperation {
	Add("+"), Subtract("-"), Multiply("*"), Divide("/"), Remainder("%");
	private final String symbol;

	private BinaryOperation(final String symbol) {
	    this.symbol = symbol;
	}

	public String getSymbol() {
	    return this.symbol;
	}

	public static BinaryOperation fromId(final int id) {
	    return values()[id];
	}
    }

    enum ShiftType {
	Left, Arithmetic, Logical;

	public static ShiftType fromId(final int id) {
	    return values()[id];
	}
    }

    enum BitwiseOperationType {
	And, Or, Xor;
	public static BitwiseOperationType fromId(final int id) {
	    return values()[id];
	}
    }

    enum CompareType {
	EQ, NE, LT, GE, GT, LE;

	public static CompareType fromId(final int id) {
	    return values()[id];
	}
    }

    public static class Register {
	int id;

	public Register(final int id) {
	    this.id = id;
	}

	@Override
	public String toString() {
	    return "$" + this.id;
	}
    }

    public BasicBlockBuilder() {
    }

    private int index = 0;
    private final List<Object> instructions = new ArrayList<>();
    private Object terminator = null;

    public void putLocal(final int id, final Register value) {
	print("store_local", id, value);
    }

    public Register getLocal(final int id) {
	return instruction(Type.getUnknown(), "load_local", id);
    }

    public Register pop() {
	return instruction(Type.getUnknown(), "pop");

    }

    public void push(final Register value) {
	print("push", value);
    }

    public Register nullConstant() {
	return instruction(PrimitiveType.Reference, "null");
    }

    public Register integerConstant(final int value) {
	return instruction(PrimitiveType.Integer, value);
    }

    public Register longConstant(final long value) {
	return instruction(PrimitiveType.Long, value);
    }

    public Register floatConstant(final float value) {
	return instruction(PrimitiveType.Float, value);

    }

    public Register doubleConstant(final double value) {
	return instruction(PrimitiveType.Double, value);
    }

    public Register stringConstant(final String value) {
	return instruction(PrimitiveType.Reference, "\"" + value + "\"");
    }

    public Register binaryOperation(final PrimitiveType type, final BinaryOperation op, final Register left,
	    final Register right) {
	return instruction(type, left, op.getSymbol(), right);
    }

    public Register negate(final PrimitiveType type, final Register value) {
	return instruction(type, "- $" + value);
    }

    public Register shift(final PrimitiveType type, final ShiftType shiftType, final Register left,
	    final Register right) {
	return instruction(type, left, shiftType, right);
    }

    public Register bitwiseOperation(final PrimitiveType type, final BitwiseOperationType operation,
	    final Register left, final Register right) {
	return instruction(type, left, operation, right);
    }

    public Register convert(final PrimitiveType from, final PrimitiveType to, final Register value) {
	return instruction(to, "from " + from, value);

    }

    public Register compare(final PrimitiveType type, final Register left, final Register right) {
	return instruction(type, "compare $" + left, right);
    }

    public Register loadElement(final PrimitiveType elementType, final Register arrayref, final Register index) {
	return instruction(elementType, "loadElement $" + arrayref, index);
    }

    public void storeElement(final Register arrayref, final Register index) {
	print("storeElement $" + arrayref, index);

    }

    private void print(final Object... args) {
	this.instructions.add(Arrays.asList(args));
    }

    private Register instruction(final FieldType type, final Object... args) {
	final Register targetRegister = new Register(this.index++);
	final ArrayList<Object> list = new ArrayList<>();
	list.addAll(Arrays.asList(type.getJavaName(), targetRegister, "="));
	list.addAll(Arrays.asList(args));
	print(list.toArray());
	return targetRegister;
    }

    public Register checkedCast(final ClassReference type, final Register value) {
	return instruction(PrimitiveType.Reference, "cast", type, value);
    }

    public Register instanceOf(final ClassReference type, final Register value) {
	return instruction(PrimitiveType.Byte, "instanceof", type, value);
    }

    public void monitorEnter(final Register monitor) {
	throw new IllegalStateException();

    }

    public void monitorExit(final Register monitor) {
	throw new IllegalStateException();

    }

    public Register loadStaticField(final FieldReference fieldReference) {
	return instruction(fieldReference.getType(), fieldReference.getTarget().getJavaName(), ".",
		fieldReference.getName());
    }

    public void storeStaticField(final FieldReference fieldReference, final Register value) {
	print("storeStaticField", fieldReference.getType(), fieldReference, value);
    }

    public Register loadField(final FieldReference fieldReference, final Register target) {
	return instruction(fieldReference.getType(), target, ".", fieldReference.getName());
    }

    public void storeField(final FieldReference fieldReference, final Register target, final Register value) {
	print("storeField", target, fieldReference.getName(), value);

    }

    public List<Register> invokeVirtual(final MethodReference methodReference, final Register target,
	    final List<Register> args) {
	final MethodType methodType = methodReference.getType();
	if (methodType.isVoid()) {
	    print("invokeVirtual", methodReference, target, args);
	    return Collections.emptyList();
	} else {
	    return Arrays.asList(instruction((FieldType) methodType.getReturnType(), "invokeVirtual", methodReference,
		    target, args));
	}

    }

    public List<Register> invokeSpecial(final MethodReference methodReference, final Register target,
	    final List<Register> args) {
	final MethodType methodType = methodReference.getType();
	if (methodType.isVoid()) {
	    print("invokeSpecial", methodReference, target, args);
	    return Collections.emptyList();
	} else {
	    return Arrays.asList(instruction((FieldType) methodType.getReturnType(), "invokeSpecial", methodReference,
		    target, args));
	}
    }

    public List<Register> invokeStatic(final MethodReference methodReference, final List<Register> args) {
	final MethodType methodType = methodReference.getType();
	if (methodType.isVoid()) {
	    print("invokeStatic", methodReference, args);
	    return Collections.emptyList();
	} else {
	    return Arrays
		    .asList(instruction((FieldType) methodType.getReturnType(), "invokeStatic", methodReference, args));
	}
    }

    public List<Register> invokeInterface(final MethodReference methodReference, final Register target,
	    final List<Register> args) {
	final MethodType methodType = methodReference.getType();
	if (methodType.isVoid()) {
	    print("invokeInterface", methodReference, target, args);
	    return Collections.emptyList();
	} else {
	    return Arrays.asList(instruction((FieldType) methodType.getReturnType(), "invokeInterface", methodReference,
		    target, args));
	}
    }

    public Register newInstance(final ClassReference classReference) {
	return instruction(PrimitiveType.Reference, "new", classReference.getJavaName());
    }

    public Register newPrimitiveArray(final PrimitiveType type, final Register size) {
	return instruction(PrimitiveType.Reference, "new", type.getJavaName(), "[", size, "]");
    }

    public Register newArray(final ClassReference type, final Register size) {
	return instruction(PrimitiveType.Reference, "newArray", type, size);

    }

    public Register arrayLength(final Register array) {
	return instruction(PrimitiveType.Integer, array, ".", "length");
    }

    public void returnValue(final PrimitiveType type, final Register ref) {
	terminate("return", "(", type.getJavaName(), ")", ref);
    }

    public void returnVoid() {
	terminate("return");
    }

    public void jumpIf(final PrimitiveType type, final BasicBlockBuilder then, final BasicBlockBuilder otherwise,
	    final CompareType compareType, final Register left, final Register right) {
	terminate("jumpIf", type.getJavaName(), then, otherwise, left, compareType, right);
    }

    public void jumpTable(final Register value, final int defaultOffset, final Map<Integer, Integer> table) {
	terminate("lookupSwitch", value, defaultOffset, table);
    }

    public void jump(final BasicBlockBuilder target) {
	terminate("jump", target);
    }

    public void returnError(final Register exception) {
	terminate("throw", exception);
    }

    private void terminate(final Object... args) {
	if (this.terminator != null) {
	    throw new IllegalStateException();
	}
	this.terminator = Arrays.asList(args);
    }

    public boolean isTerminated() {
	return this.terminator != null;
    }

    private boolean printing = false;

    @Override
    public String toString() {
	if (this.printing) {
	    return "...";
	}
	this.printing = true;
	try {
	    String out = "\n";
	    for (final Object s : this.instructions) {
		out += "    " + s + "\n";
	    }
	    out += "    " + this.terminator + "\n";
	    return out;
	} finally {
	    this.printing = false;
	}
    }
}
