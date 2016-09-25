package bdc;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import bdc.ConstantPool.ClassReference;
import bdc.ConstantPool.FieldReference;
import bdc.ConstantPool.InterfaceMethodReference;
import bdc.ConstantPool.MethodReference;
import bdc.InstructionPrinter.Register;
import bdc.Type.FieldType;
import bdc.Type.MethodType;
import bdc.Type.PrimitiveType;

public class InstructionPrinter implements InstructionVisitor<Register> {

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

    public InstructionPrinter(final PrintStream out) {
	super();
	this.out = out;
    }

    int index = 0;
    private final PrintStream out;

    @Override
    public Register nullConstant() {
	return instruction(PrimitiveType.Reference, "null");
    }

    @Override
    public Register integerConstant(final int value) {
	return instruction(PrimitiveType.Integer, value);
    }

    @Override
    public Register longConstant(final long value) {
	return instruction(PrimitiveType.Long, value);
    }

    @Override
    public Register floatConstant(final float value) {
	return instruction(PrimitiveType.Float, value);

    }

    @Override
    public Register doubleConstant(final double value) {
	return instruction(PrimitiveType.Double, value);
    }

    @Override
    public Register stringConstant(final String value) {
	return instruction(PrimitiveType.Reference, "\"" + value + "\"");
    }

    @Override
    public Register binaryOperation(final PrimitiveType type, final bdc.InstructionVisitor.BinaryOperation op,
	    final Register left, final Register right) {
	return instruction(type, left, op.getSymbol(), right);
    }

    @Override
    public Register negate(final PrimitiveType type, final Register value) {
	return instruction(type, "- $" + value);
    }

    @Override
    public Register shift(final PrimitiveType type, final ShiftType shiftType, final Register left,
	    final Register right) {
	return instruction(type, left, shiftType, right);
    }

    @Override
    public Register bitwiseOperation(final PrimitiveType type, final BitwiseOperationType operation,
	    final Register left, final Register right) {
	return instruction(type, left, operation, right);
    }

    @Override
    public Register convert(final PrimitiveType from, final PrimitiveType to, final Register value) {
	return instruction(to, "from " + from, value);

    }

    @Override
    public Register compare(final PrimitiveType type, final Register left, final Register right) {
	return instruction(type, "compare $" + left, right);
    }

    @Override
    public Register loadElement(final PrimitiveType elementType, final Register arrayref, final Register index) {
	return instruction(elementType, "loadElement $" + arrayref, index);
    }

    @Override
    public void storeElement(final Register arrayref, final Register index) {
	print("storeElement $" + arrayref, index);

    }

    @Override
    public void returnValue(final PrimitiveType type, final Register ref) {
	print("return", "(", type.getJavaName(), ")", ref);
    }

    @Override
    public void returnVoid() {
	print("return");
    }

    @Override
    public void jumpIf(final PrimitiveType type, final int offset, final bdc.InstructionVisitor.CompareType compareType,
	    final Register left, final Register right) {
	print("jumpIf", type.getJavaName(), offset, left, compareType, right);
    }

    @Override
    public void jumpTable(final Register value, final int defaultOffset, final Map<Integer, Integer> table) {
	print("lookupSwitch", value, defaultOffset, table);
    }

    @Override
    public void jump(final int unsignedShort) {
	print("jump", unsignedShort);
    }

    private void print(final Object... args) {
	String s = "   ";
	for (final Object arg : args) {
	    s += " ";
	    s += arg;
	}
	s += ";";
	this.out.println(s);
    }

    private Register instruction(final FieldType type, final Object... args) {
	final Register targetRegister = new Register(this.index++);
	final ArrayList<Object> list = new ArrayList<>();
	list.addAll(Arrays.asList(type.getJavaName(), targetRegister, "="));
	list.addAll(Arrays.asList(args));
	print(list.toArray());
	return targetRegister;
    }

    @Override
    public Register checkedCast(final ClassReference type, final Register value) {
	return instruction(PrimitiveType.Reference, "cast", type, value);
    }

    @Override
    public Register instanceOf(final ClassReference type, final Register value) {
	return instruction(PrimitiveType.Byte, "instanceof", type, value);
    }

    @Override
    public void monitorEnter(final Register monitor) {
	throw new IllegalStateException();

    }

    @Override
    public void monitorExit(final Register monitor) {
	throw new IllegalStateException();

    }

    @Override
    public Register loadStaticField(final FieldReference fieldReference) {
	return instruction(fieldReference.getType(), fieldReference.getTarget().getJavaName(), ".",
		fieldReference.getName());
    }

    @Override
    public void storeStaticField(final FieldReference fieldReference, final Register value) {
	instruction(fieldReference.getType(), "storeStaticField", fieldReference, value);
    }

    @Override
    public Register loadField(final FieldReference fieldReference, final Register target) {
	throw new IllegalStateException();

    }

    @Override
    public void storeField(final FieldReference fieldReference, final Register target, final Register value) {
	throw new IllegalStateException();

    }

    @Override
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

    @Override
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

    @Override
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

    @Override
    public List<Register> invokeInterface(final InterfaceMethodReference methodReference, final Register target,
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

    @Override
    public Register newInstance(final ClassReference classReference) {
	return instruction(PrimitiveType.Reference, "new", classReference.getJavaName());
    }

    @Override
    public Register newPrimitiveArray(final PrimitiveType type, final Register size) {
	return instruction(PrimitiveType.Reference, "new", type.getJavaName(), "[", size, "]");
    }

    @Override
    public Register newArray(final ClassReference type, final Register size) {
	return instruction(PrimitiveType.Reference, "newArray", type, size);

    }

    @Override
    public Register arrayLength(final Register array) {
	return instruction(PrimitiveType.Integer, array, ".", "length");
    }

    @Override
    public void returnError(final Register exception) {
	print("throw", exception);
    }
}
