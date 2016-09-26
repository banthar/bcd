package bdc;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import bdc.ConstantPool.ClassReference;
import bdc.ConstantPool.LongValueConstant;
import bdc.ConstantPool.MethodReference;
import bdc.ConstantPool.ValueConstant;
import bdc.InstructionVisitor.BinaryOperation;
import bdc.InstructionVisitor.BitwiseOperationType;
import bdc.InstructionVisitor.CompareType;
import bdc.InstructionVisitor.ShiftType;
import bdc.Type.FieldType;
import bdc.Type.MethodType;
import bdc.Type.PrimitiveType;
import bdc.Type.ReferenceType;

public class InstructionParser {

    public static void parseCode(final DataInputStream dataInput, final ConstantPool constantPool,
	    final ReferenceType selfType, final MethodType methodType) throws IOException, ClassFormatException {
	final int maxStack = dataInput.readUnsignedShort();
	final int maxLocals = dataInput.readUnsignedShort();
	final int codeLength = dataInput.readInt();
	if (codeLength >= 65536 || codeLength < 0) {
	    throw new ClassFormatException("Invalid method code length: " + Integer.toUnsignedString(codeLength));
	}
	final byte[] instructionBytes = new byte[codeLength];
	dataInput.readFully(instructionBytes);
	parseInstructions(new InstructionPrinter(System.out), ByteBuffer.wrap(instructionBytes).asReadOnlyBuffer(),
		constantPool, selfType, methodType);
	final int exceptionLength = dataInput.readUnsignedShort();
	for (int i = 0; i < exceptionLength; i++) {
	    final int startPc = dataInput.readUnsignedShort();
	    final int endPc = dataInput.readUnsignedShort();
	    final int handlerPc = dataInput.readUnsignedShort();
	    final ClassReference catchType = constantPool.getClassReference(dataInput.readUnsignedShort());
	}
	final int attributes = dataInput.readUnsignedShort();
	final String signature = null;
	for (int i = 0; i < attributes; i++) {
	    final String name = constantPool.getUTF8(dataInput.readUnsignedShort());
	    final int length = dataInput.readInt();
	    switch (name) {
	    case "LineNumberTable":
		dataInput.readFully(new byte[length]);
		break;
	    case "LocalVariableTypeTable":
		dataInput.readFully(new byte[length]);
		break;
	    case "LocalVariableTable":
		dataInput.readFully(new byte[length]);
		break;
	    case "StackMapTable":
		dataInput.readFully(new byte[length]);
		break;
	    default:
		throw new ClassFormatException("Unknown method code attribute: " + name);

	    }
	}
    }

    private static <T> void parseInstructions(final InstructionVisitor<T> visitor, final ByteBuffer in,
	    final ConstantPool constantPool, final ReferenceType selfType, final MethodType methodType)
	    throws ClassFormatException {
	final Queue<T> stack = new ArrayDeque<>();
	final HashMap<Integer, T> locals = new HashMap<>();
	{
	    int n = 0;
	    locals.put(n++, visitor.nullConstant());
	    for (final FieldType arg : methodType.getArgumentTypes()) {
		locals.put(n++, visitor.nullConstant());
	    }
	}
	while (in.position() < in.limit()) {
	    final int opcodeOffset = in.position();
	    try {
		final int opcode = getUnsignedByte(in);
		switch (opcode) {
		case 0x0:
		    break;
		case 0x01:
		    stack.add(visitor.nullConstant());
		    break;
		case 0x02:
		case 0x03:
		case 0x04:
		case 0x05:
		case 0x06:
		case 0x07:
		case 0x08:
		    stack.add(visitor.integerConstant(opcode - 0x03));
		    break;
		case 0x09:
		case 0x0a:
		    stack.add(visitor.longConstant(opcode - 0x09));
		    break;
		case 0x0b:
		case 0x0c:
		case 0x0d:
		    stack.add(visitor.floatConstant(opcode - 0x0b));
		    break;
		case 0x0e:
		case 0x0f:
		    stack.add(visitor.doubleConstant(opcode - 0x0e));
		    break;
		case 0x10:
		    stack.add(visitor.integerConstant(in.get()));
		    break;
		case 0x11:
		    stack.add(visitor.integerConstant(in.getShort()));
		    break;
		case 0x12:
		    stack.add(loadConstant(visitor, constantPool, getUnsignedByte(in)));
		    break;
		case 0x13:
		    stack.add(loadConstant(visitor, constantPool, getUnsignedShort(in)));
		    break;
		case 0x14:
		    stack.add(loadLongConstant(visitor, constantPool, getUnsignedShort(in)));
		    break;
		case 0x15:
		case 0x16:
		case 0x17:
		case 0x18:
		case 0x19:
		    stack.add(locals.get(getUnsignedByte(in)));
		    break;
		case 0x1a:
		case 0x1b:
		case 0x1c:
		case 0x1d:
		case 0x1e:
		case 0x1f:
		case 0x20:
		case 0x21:
		case 0x22:
		case 0x23:
		case 0x24:
		case 0x25:
		case 0x26:
		case 0x27:
		case 0x28:
		case 0x29:
		case 0x2a:
		case 0x2b:
		case 0x2c:
		case 0x2d: {
		    final int n = opcode - 0x1a;
		    final PrimitiveType type = PrimitiveType.fromId(n / 4);
		    stack.add(locals.get(n % 4));
		    break;
		}
		case 0x2e:
		case 0x2f:
		case 0x30:
		case 0x31:
		case 0x32:
		case 0x33:
		case 0x34:
		case 0x35: {
		    final int n = opcode - 0x2e;
		    stack.add(visitor.loadElement(PrimitiveType.fromId(n), stack.remove(), stack.remove()));
		    break;
		}
		case 0x36:
		case 0x37:
		case 0x38:
		case 0x39:
		case 0x3a: {
		    final PrimitiveType type = PrimitiveType.fromId(opcode - 0x36);
		    locals.put(getUnsignedByte(in), stack.remove());
		    break;
		}
		case 0x3b:
		case 0x3c:
		case 0x3d:
		case 0x3e:
		case 0x3f:
		case 0x40:
		case 0x41:
		case 0x42:
		case 0x43:
		case 0x44:
		case 0x45:
		case 0x46:
		case 0x47:
		case 0x48:
		case 0x49:
		case 0x4a:
		case 0x4b:
		case 0x4c:
		case 0x4d:
		case 0x4e: {
		    final int n = opcode - 0x3b;
		    final PrimitiveType type = PrimitiveType.fromId(n / 4);
		    locals.put(n % 4, stack.remove());
		    break;
		}
		case 0x4f:
		case 0x50:
		case 0x51:
		case 0x52:
		case 0x53:
		case 0x54:
		case 0x55:
		case 0x56:
		    visitor.storeElement(stack.remove(), stack.remove());
		    break;
		case 0x57:
		    stack.remove();
		    break;
		case 0x58:
		    for (int i = 0; i < 2; i++) {
			stack.remove();
		    }
		    break;
		case 0x59: {
		    final T value = stack.remove();
		    stack.add(value);
		    stack.add(value);
		    break;
		}
		case 0x5a: {
		    final T value0 = stack.remove();
		    final T value1 = stack.remove();
		    stack.add(value0);
		    stack.add(value1);
		    stack.add(value0);
		    break;
		}
		case 0x5b: {
		    final T value0 = stack.remove();
		    final T value1 = stack.remove();
		    final T value2 = stack.remove();
		    stack.add(value0);
		    stack.add(value2);
		    stack.add(value1);
		    stack.add(value0);
		    break;
		}
		case 0x5c: {
		    final T value0 = stack.remove();
		    final T value1 = stack.remove();
		    stack.add(value1);
		    stack.add(value0);
		    stack.add(value1);
		    stack.add(value0);
		    break;
		}
		case 0x5d: {
		    final T value0 = stack.remove();
		    final T value1 = stack.remove();
		    final T value2 = stack.remove();
		    stack.add(value1);
		    stack.add(value0);
		    stack.add(value2);
		    stack.add(value1);
		    stack.add(value0);
		    break;
		}
		case 0x5e: {
		    final T value0 = stack.remove();
		    final T value1 = stack.remove();
		    final T value2 = stack.remove();
		    final T value3 = stack.remove();
		    stack.add(value1);
		    stack.add(value0);
		    stack.add(value3);
		    stack.add(value2);
		    stack.add(value1);
		    stack.add(value0);
		    break;
		}
		case 0x5f: {
		    final T value0 = stack.remove();
		    final T value1 = stack.remove();
		    stack.add(value0);
		    stack.add(value1);
		    break;
		}
		case 0x60:
		case 0x61:
		case 0x62:
		case 0x63:
		case 0x64:
		case 0x65:
		case 0x66:
		case 0x67:
		case 0x68:
		case 0x69:
		case 0x6a:
		case 0x6b:
		case 0x6c:
		case 0x6d:
		case 0x6e:
		case 0x6f:
		case 0x70:
		case 0x71:
		case 0x72:
		case 0x73: {
		    final int n = opcode - 0x60;
		    final PrimitiveType type = PrimitiveType.fromId(n % 4);
		    final BinaryOperation op = BinaryOperation.fromId(n / 4);
		    stack.add(visitor.binaryOperation(type, op, stack.remove(), stack.remove()));
		    break;
		}
		case 0x74:
		case 0x75:
		case 0x76:
		case 0x77: {
		    final int n = opcode - 0x74;
		    final PrimitiveType type = PrimitiveType.fromId(n);
		    stack.add(visitor.negate(type, stack.remove()));
		    break;
		}
		case 0x78:
		case 0x79:
		case 0x7a:
		case 0x7b:
		case 0x7c:
		case 0x7d: {
		    final int n = opcode - 0x78;
		    final PrimitiveType type = PrimitiveType.fromId(n % 2);
		    stack.add(visitor.shift(type, ShiftType.fromId(n / 2), stack.remove(), stack.remove()));
		    break;
		}
		case 0x7e:
		case 0x7f:
		case 0x80:
		case 0x81:
		case 0x82:
		case 0x83: {
		    final int n = opcode - 0x7e;
		    final PrimitiveType type = PrimitiveType.fromId(n % 2);
		    stack.add(visitor.bitwiseOperation(type, BitwiseOperationType.fromId(n / 2), stack.remove(),
			    stack.remove()));
		    break;
		}
		case 0x84: {
		    final int index = getUnsignedByte(in);
		    locals.put(index, visitor.binaryOperation(PrimitiveType.Integer, BinaryOperation.Add,
			    locals.get(index), visitor.integerConstant(in.get())));
		    break;
		}
		case 0x85:
		case 0x86:
		case 0x87:
		case 0x88:
		case 0x89:
		case 0x90:
		case 0x91:
		case 0x92:
		case 0x93: {
		    final int n = opcode - 0x85;
		    final int fromType = n / 3;
		    final int k = n % 3;
		    final int toType = k <= fromType ? k + 1 : k;
		    stack.add(visitor.convert(PrimitiveType.fromId(fromType), PrimitiveType.fromId(toType),
			    stack.remove()));
		    break;
		}
		case 0x94:
		    stack.add(visitor.compare(PrimitiveType.Long, stack.remove(), stack.remove()));
		    break;
		case 0x95:
		case 0x96:
		    stack.add(visitor.compare(PrimitiveType.Float, stack.remove(), stack.remove()));
		    break;
		case 0x97:
		case 0x98:
		    stack.add(visitor.compare(PrimitiveType.Double, stack.remove(), stack.remove()));
		    break;
		case 0x99:
		case 0x9a:
		case 0x9b:
		case 0x9c:
		case 0x9d:
		case 0x9e: {
		    final CompareType compareType = InstructionVisitor.CompareType.fromId(opcode - 0x99);
		    visitor.jumpIf(PrimitiveType.Integer, in.getShort() + opcodeOffset, compareType, stack.remove(),
			    visitor.integerConstant(0));
		    break;
		}
		case 0x9f:
		case 0xa0:
		case 0xa1:
		case 0xa2:
		case 0xa3:
		case 0xa4: {
		    final CompareType compareType = InstructionVisitor.CompareType.fromId(opcode - 0x9f);
		    visitor.jumpIf(PrimitiveType.Integer, in.getShort() + opcodeOffset, compareType, stack.remove(),
			    stack.remove());
		    break;
		}
		case 0xa5:
		case 0xa6: {
		    final CompareType compareType = InstructionVisitor.CompareType.fromId(opcode - 0xa5);
		    visitor.jumpIf(PrimitiveType.Reference, in.getShort() + opcodeOffset, compareType, stack.remove(),
			    stack.remove());
		    break;
		}
		case 0xa7: {
		    visitor.jump(in.getShort() + opcodeOffset);
		    break;
		}
		case 0xa8:
		    throw new ClassFormatException("jsr instruction not supported");
		case 0xa9:
		    throw new ClassFormatException("ret instruction not supported");
		case 0xaa: {
		    while (in.position() % 4 != 0) {
			if (in.get() != 0) {
			    throw new ClassFormatException("non-zero padding byte");
			}
		    }
		    final int defaultOffset = in.getInt() + opcodeOffset;
		    final int low = in.getInt();
		    final int high = in.getInt();
		    final Map<Integer, Integer> lookupTable = new HashMap<>();
		    for (int i = low; i <= high; i++) {
			final int offset = in.getInt() + opcodeOffset;
			lookupTable.put(i, offset);
		    }
		    visitor.jumpTable(stack.remove(), defaultOffset, lookupTable);
		    break;
		}
		case 0xab: {
		    while (in.position() % 4 != 0) {
			if (in.get() != 0) {
			    throw new ClassFormatException("non-zero padding byte");
			}
		    }
		    final int defaultOffset = in.getInt() + opcodeOffset;
		    final int n = in.getInt();
		    final Map<Integer, Integer> lookupTable = new HashMap<>();
		    for (int i = 0; i < n; i++) {
			final int match = in.getInt();
			final int offset = in.getInt() + opcodeOffset;
			lookupTable.put(match, offset);
		    }
		    visitor.jumpTable(stack.remove(), defaultOffset, lookupTable);
		    break;
		}
		case 0xac:
		case 0xad:
		case 0xae:
		case 0xaf:
		case 0xb0: {
		    final int n = opcode - 0xac;
		    visitor.returnValue(PrimitiveType.fromId(n), stack.remove());
		    break;
		}
		case 0xb1:
		    visitor.returnVoid();
		    break;

		case 0xb2:
		    stack.add(visitor.loadStaticField(constantPool.getFieldReference(getUnsignedShort(in))));
		    break;
		case 0xb3:
		    visitor.storeStaticField(constantPool.getFieldReference(getUnsignedShort(in)), stack.remove());
		    break;
		case 0xb4:
		    stack.add(visitor.loadField(constantPool.getFieldReference(getUnsignedShort(in)), stack.remove()));
		    break;
		case 0xb5:
		    visitor.storeField(constantPool.getFieldReference(getUnsignedShort(in)), stack.remove(),
			    stack.remove());
		    break;
		case 0xb6: {
		    final MethodReference methodReference = constantPool.getMethodReference(getUnsignedShort(in));
		    stack.addAll(visitor.invokeVirtual(methodReference, stack.remove(),
			    readMethodArguments(methodReference.getType(), stack)));
		    break;
		}
		case 0xb7: {
		    final MethodReference methodReference = constantPool.getMethodReference(getUnsignedShort(in));
		    stack.addAll(visitor.invokeSpecial(methodReference, stack.remove(),
			    readMethodArguments(methodReference.getType(), stack)));
		    break;
		}
		case 0xb8: {
		    final MethodReference methodReference = constantPool.getMethodReference(getUnsignedShort(in));
		    stack.addAll(visitor.invokeStatic(methodReference,
			    readMethodArguments(methodReference.getType(), stack)));
		    break;
		}
		case 0xb9: {
		    final MethodReference methodReference = constantPool.getMethodReference(getUnsignedShort(in));
		    final int count = getUnsignedByte(in);
		    final int expectedCount = methodReference.getInvokeVirtualCount();
		    if (count != expectedCount) {
			throw new ClassFormatException(
				"invalid invokeinterface count. expected: " + expectedCount + " actual: " + count);
		    }
		    if (getUnsignedByte(in) != 0) {
			throw new ClassFormatException("expected 0 byte after invokeinterface");
		    }
		    stack.addAll(visitor.invokeInterface(methodReference, stack.remove(),
			    readMethodArguments(methodReference.getType(), stack)));
		    break;
		}
		case 0xba:
		    throw new ClassFormatException("invokedynamic instruction not supported");
		case 0xbb:
		    stack.add(visitor.newInstance(constantPool.getClassReference(getUnsignedShort(in))));
		    break;
		case 0xbc: {
		    final PrimitiveType type = PrimitiveType.fromArrayTypeId(getUnsignedByte(in));
		    stack.add(visitor.newPrimitiveArray(type, stack.remove()));
		    break;
		}
		case 0xbd: {
		    final ClassReference type = constantPool.getClassReference(getUnsignedShort(in));
		    stack.add(visitor.newArray(type, stack.remove()));
		    break;
		}
		case 0xbe: {
		    stack.add(visitor.arrayLength(stack.remove()));
		    break;
		}
		case 0xbf: {
		    visitor.returnError(stack.remove());
		    break;
		}
		case 0xc0: {
		    final ClassReference type = constantPool.getClassReference(getUnsignedShort(in));
		    stack.add(visitor.checkedCast(type, stack.remove()));
		    break;
		}
		case 0xc1: {
		    final ClassReference type = constantPool.getClassReference(getUnsignedShort(in));
		    stack.add(visitor.instanceOf(type, stack.remove()));
		    break;
		}
		case 0xc2: {
		    visitor.monitorEnter(stack.remove());
		    break;
		}
		case 0xc3: {
		    visitor.monitorExit(stack.remove());
		    break;
		}
		case 0xc4:
		    throw new ClassFormatException("wide instruction not supported");
		case 0xc5:
		    throw new ClassFormatException("multianewarray instruction not supported");
		case 0xc6:
		case 0xc7: {
		    final CompareType compareType = InstructionVisitor.CompareType.fromId(opcode - 0xc6);
		    visitor.jumpIf(PrimitiveType.Reference, in.getShort() + opcodeOffset, compareType, stack.remove(),
			    visitor.nullConstant());
		    break;
		}
		case 0xc8:
		    visitor.jump(in.getInt() + opcodeOffset);
		    break;
		case 0xc9:
		    throw new ClassFormatException("jsr_w instruction not supported");
		case 0xca:
		    throw new ClassFormatException("breakpoint instruction not supported");
		case 0xfe:
		    throw new ClassFormatException("impdep1 instruction not supported");
		case 0xff:
		    throw new ClassFormatException("impdep2 instruction not supported");
		default:
		    throw new ClassFormatException(String.format("Unknown opcode: 0x%02x", opcode));
		}
	    } catch (final ClassFormatException e) {
		String buf = "";
		for (int i = opcodeOffset; i < in.position(); i++) {
		    buf += String.format("%02x ", in.get(i));
		}
		throw new ClassFormatException("Invalid bytecode: " + buf + ": " + e.getMessage(), e);
	    }
	}
    }

    private static <T> List<T> readMethodArguments(final MethodType methodType, final Queue<T> stack) {
	final List<T> arguments = new ArrayList<>();
	for (final Type arg : methodType.getArgumentTypes()) {
	    arguments.add(stack.remove());
	}
	return arguments;
    }

    private static <T> T loadConstant(final InstructionVisitor<T> visitor, final ConstantPool constantPool,
	    final int index) throws ClassFormatException {
	final Object value = constantPool.get(index);
	if (value instanceof ValueConstant) {
	    return ((ValueConstant) value).visit(visitor);
	} else {
	    throw new ClassFormatException("Expected constant at index " + index);
	}
    }

    private static <T> T loadLongConstant(final InstructionVisitor<T> visitor, final ConstantPool constantPool,
	    final int index) throws ClassFormatException {
	final Object value = constantPool.get(index);
	if (value instanceof LongValueConstant) {
	    return ((LongValueConstant) value).visit(visitor);
	} else {
	    throw new ClassFormatException("Expected long constant at index " + index);
	}
    }

    private static int getUnsignedByte(final ByteBuffer in) {
	return in.get() & 0xff;
    }

    private static int getUnsignedShort(final ByteBuffer in) {
	return in.getShort() & 0xffff;
    }
}
