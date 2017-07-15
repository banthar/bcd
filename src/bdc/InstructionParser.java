package bdc;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import bdc.BasicBlockBuilder.BinaryOperationType;
import bdc.BasicBlockBuilder.BitwiseOperationType;
import bdc.BasicBlockBuilder.CompareType;
import bdc.BasicBlockBuilder.ShiftType;
import bdc.ConstantPool.ClassReference;
import bdc.ConstantPool.LongValueConstant;
import bdc.ConstantPool.MethodReference;
import bdc.ConstantPool.ValueConstant;
import bdc.Type.PrimitiveType;
import bdc.Type.ReferenceType;

public class InstructionParser {

	public static BasicBlockBuilder parseCode(final DataInputStream dataInput, final ConstantPool constantPool,
			final ReferenceType selfType, final MethodType methodType) throws IOException, ClassFormatException {
		final int maxStack = dataInput.readUnsignedShort();
		final int maxLocals = dataInput.readUnsignedShort();
		final int codeLength = dataInput.readInt();
		if (codeLength >= 65536 || codeLength < 0) {
			throw new ClassFormatException("Invalid method code length: " + Integer.toUnsignedString(codeLength));
		}
		final byte[] instructionBytes = new byte[codeLength];
		dataInput.readFully(instructionBytes);
		final BasicBlockBuilder block = parseInstructions(ByteBuffer.wrap(instructionBytes).asReadOnlyBuffer(),
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
				readLocalVariableTypeTable(dataInput, constantPool);
				break;
			case "LocalVariableTable":
				readLocalVariableTable(dataInput, constantPool);
				break;
			case "StackMapTable":
				dataInput.readFully(new byte[length]);
				break;
			default:
				throw new ClassFormatException("Unknown method code attribute: " + name);

			}
		}
		return block;
	}

	private static void readLocalVariableTable(final DataInputStream dataInput, final ConstantPool constantPool)
			throws IOException {
		final int n = dataInput.readUnsignedShort();
		for (int i = 0; i < n; i++) {
			final int startPc = dataInput.readUnsignedShort();
			final int length = dataInput.readUnsignedShort();
			final int name_index = dataInput.readUnsignedShort();
			final int descriptor_index = dataInput.readUnsignedShort();
			final int index = dataInput.readUnsignedShort();
			Arrays.asList(startPc, length, constantPool.getUTF8(name_index),
					constantPool.getDescriptor(descriptor_index), index);
		}
	}

	private static void readLocalVariableTypeTable(final DataInputStream dataInput, final ConstantPool constantPool)
			throws IOException {
		final int n = dataInput.readUnsignedShort();
		for (int i = 0; i < n; i++) {
			final int startPc = dataInput.readUnsignedShort();
			final int length = dataInput.readUnsignedShort();
			final int name_index = dataInput.readUnsignedShort();
			final int signature_index = dataInput.readUnsignedShort();
			final int index = dataInput.readUnsignedShort();
			Arrays.asList(startPc, length, constantPool.getUTF8(name_index), constantPool.getSignature(signature_index),
					index);
		}
	}

	private static BasicBlockBuilder parseInstructions(final ByteBuffer in, final ConstantPool constantPool,
			final ReferenceType selfType, final MethodType methodType) throws ClassFormatException {
		final Map<Integer, BasicBlockBuilder> blocks = new HashMap<>();
		final Function<Integer, BasicBlockBuilder> getBlock = new Function<Integer, BasicBlockBuilder>() {
			@Override
			public BasicBlockBuilder apply(final Integer id) {
				BasicBlockBuilder block = blocks.get(id);
				if (block == null) {
					block = BasicBlockBuilder.createBlock();
					blocks.put(id, block);
				}
				return block;
			}
		};
		while (in.position() < in.limit()) {
			final int opcodeOffset = in.position();
			final BasicBlockBuilder block = getBlock.apply(opcodeOffset);
			try {
				final int opcode = getUnsignedByte(in);
				switch (opcode) {
				case 0x0:
					break;
				case 0x01:
					block.push(block.nullConstant());
					break;
				case 0x02:
				case 0x03:
				case 0x04:
				case 0x05:
				case 0x06:
				case 0x07:
				case 0x08:
					block.push(block.integerConstant(opcode - 0x03));
					break;
				case 0x09:
				case 0x0a:
					block.push(block.longConstant(opcode - 0x09));
					break;
				case 0x0b:
				case 0x0c:
				case 0x0d:
					block.push(block.floatConstant(opcode - 0x0b));
					break;
				case 0x0e:
				case 0x0f:
					block.push(block.doubleConstant(opcode - 0x0e));
					break;
				case 0x10:
					block.push(block.integerConstant(in.get()));
					break;
				case 0x11:
					block.push(block.integerConstant(in.getShort()));
					break;
				case 0x12:
					block.push(loadConstant(block, constantPool, getUnsignedByte(in)));
					break;
				case 0x13:
					block.push(loadConstant(block, constantPool, getUnsignedShort(in)));
					break;
				case 0x14:
					block.push(loadLongConstant(block, constantPool, getUnsignedShort(in)));
					break;
				case 0x15:
				case 0x16:
				case 0x17:
				case 0x18:
				case 0x19:
					block.push(block.getLocal(getUnsignedByte(in)));
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
					block.push(block.getLocal(n % 4));
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
					final OutputPort index = block.pop();
					final OutputPort arrayHandle = block.pop();
					block.push(block.extractElement(PrimitiveType.fromId(n), block.loadHeap(arrayHandle), index));
					break;
				}
				case 0x36:
				case 0x37:
				case 0x38:
				case 0x39:
				case 0x3a: {
					final PrimitiveType type = PrimitiveType.fromId(opcode - 0x36);
					block.putLocal(getUnsignedByte(in), block.pop());
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
					block.putLocal(n % 4, block.pop());
					break;
				}
				case 0x4f:
				case 0x50:
				case 0x51:
				case 0x52:
				case 0x53:
				case 0x54:
				case 0x55:
				case 0x56: {
					final int n = opcode - 0x4f;
					final OutputPort value = block.pop();
					final OutputPort index = block.pop();
					final OutputPort arrayRef = block.pop();
					block.storeHeap(arrayRef,
							block.insertElement(PrimitiveType.fromId(n), block.loadHeap(arrayRef), index, value));
					break;
				}
				case 0x57:
					block.pop();
					break;
				case 0x58:
					for (int i = 0; i < 2; i++) {
						block.pop();
					}
					break;
				case 0x59: {
					final OutputPort value = block.pop();
					block.push(value);
					block.push(value);
					break;
				}
				case 0x5a: {
					final OutputPort value0 = block.pop();
					final OutputPort value1 = block.pop();
					block.push(value0);
					block.push(value1);
					block.push(value0);
					break;
				}
				case 0x5b: {
					final OutputPort value0 = block.pop();
					final OutputPort value1 = block.pop();
					final OutputPort value2 = block.pop();
					block.push(value0);
					block.push(value2);
					block.push(value1);
					block.push(value0);
					break;
				}
				case 0x5c: {
					final OutputPort value0 = block.pop();
					final OutputPort value1 = block.pop();
					block.push(value1);
					block.push(value0);
					block.push(value1);
					block.push(value0);
					break;
				}
				case 0x5d: {
					final OutputPort value0 = block.pop();
					final OutputPort value1 = block.pop();
					final OutputPort value2 = block.pop();
					block.push(value1);
					block.push(value0);
					block.push(value2);
					block.push(value1);
					block.push(value0);
					break;
				}
				case 0x5e: {
					final OutputPort value0 = block.pop();
					final OutputPort value1 = block.pop();
					final OutputPort value2 = block.pop();
					final OutputPort value3 = block.pop();
					block.push(value1);
					block.push(value0);
					block.push(value3);
					block.push(value2);
					block.push(value1);
					block.push(value0);
					break;
				}
				case 0x5f: {
					final OutputPort value0 = block.pop();
					final OutputPort value1 = block.pop();
					block.push(value0);
					block.push(value1);
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
					final BinaryOperationType op = BinaryOperationType.fromId(n / 4);
					final OutputPort right = block.pop();
					final OutputPort left = block.pop();
					block.push(block.binaryOperation(type, op, left, right));
					break;
				}
				case 0x74:
				case 0x75:
				case 0x76:
				case 0x77: {
					final int n = opcode - 0x74;
					final PrimitiveType type = PrimitiveType.fromId(n);
					block.push(block.negate(type, block.pop()));
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
					block.push(block.shift(type, ShiftType.fromId(n / 2), block.pop(), block.pop()));
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
					block.push(
							block.bitwiseOperation(type, BitwiseOperationType.fromId(n / 2), block.pop(), block.pop()));
					break;
				}
				case 0x84: {
					final int index = getUnsignedByte(in);
					block.putLocal(index, block.binaryOperation(PrimitiveType.Integer, BinaryOperationType.Add,
							block.getLocal(index), block.integerConstant(in.get())));
					break;
				}
				case 0x85:
				case 0x86:
				case 0x87:
				case 0x88:
				case 0x89:
				case 0x8a:
				case 0x8b:
				case 0x8c:
				case 0x8d:
				case 0x8e:
				case 0x8f:
				case 0x90:
				case 0x91:
				case 0x92:
				case 0x93: {
					final int n = opcode - 0x85;
					final int fromType = n / 3;
					final int k = n % 3;
					final int toType = k <= fromType ? k + 1 : k;
					block.push(
							block.convert(PrimitiveType.fromId(fromType), PrimitiveType.fromId(toType), block.pop()));
					break;
				}
				case 0x94:
					block.push(block.compare(PrimitiveType.Long, block.pop(), block.pop()));
					break;
				case 0x95:
				case 0x96:
					block.push(block.compare(PrimitiveType.Float, block.pop(), block.pop()));
					break;
				case 0x97:
				case 0x98:
					block.push(block.compare(PrimitiveType.Double, block.pop(), block.pop()));
					break;
				case 0x99:
				case 0x9a:
				case 0x9b:
				case 0x9c:
				case 0x9d:
				case 0x9e: {
					final CompareType compareType = BasicBlockBuilder.CompareType.fromId(opcode - 0x99);
					final BasicBlockBuilder then = getBlock.apply(in.getShort() + opcodeOffset);
					final BasicBlockBuilder otherwise = getBlock.apply(in.position());
					block.jumpIf(PrimitiveType.Integer, then, otherwise, compareType, block.pop(),
							block.integerConstant(0));
					break;
				}
				case 0x9f:
				case 0xa0:
				case 0xa1:
				case 0xa2:
				case 0xa3:
				case 0xa4: {
					final CompareType compareType = BasicBlockBuilder.CompareType.fromId(opcode - 0x9f);
					final BasicBlockBuilder then = getBlock.apply(in.getShort() + opcodeOffset);
					final BasicBlockBuilder otherwise = getBlock.apply(in.position());
					final OutputPort right = block.pop();
					final OutputPort left = block.pop();
					block.jumpIf(PrimitiveType.Integer, then, otherwise, compareType, left, right);
					break;
				}
				case 0xa5:
				case 0xa6: {
					final CompareType compareType = BasicBlockBuilder.CompareType.fromId(opcode - 0xa5);
					final BasicBlockBuilder then = getBlock.apply(in.getShort() + opcodeOffset);
					final BasicBlockBuilder otherwise = getBlock.apply(in.position());
					block.jumpIf(PrimitiveType.Reference, then, otherwise, compareType, block.pop(), block.pop());
					break;
				}
				case 0xa7: {
					block.jump(getBlock.apply(in.getShort() + opcodeOffset));
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
					final BasicBlockBuilder defaultTarget = getBlock.apply(in.getInt() + opcodeOffset);
					final int low = in.getInt();
					final int high = in.getInt();
					final Map<Integer, Integer> lookupTable = new HashMap<>();
					final List<BasicBlockBuilder> targets = new ArrayList<>();
					for (int i = low; i <= high; i++) {
						final int offset = in.getInt() + opcodeOffset;
						lookupTable.put(i, addJumpTarget(targets, getBlock.apply(offset)));
					}
					block.jumpTable(block.pop(), addJumpTarget(targets, defaultTarget), lookupTable, targets);
					break;
				}
				case 0xab: {
					while (in.position() % 4 != 0) {
						if (in.get() != 0) {
							throw new ClassFormatException("non-zero padding byte");
						}
					}
					final BasicBlockBuilder defaultTarget = getBlock.apply(in.getInt() + opcodeOffset);
					final int n = in.getInt();
					final Map<Integer, Integer> lookupTable = new HashMap<>();
					final List<BasicBlockBuilder> targets = new ArrayList<>();
					for (int i = 0; i < n; i++) {
						final int match = in.getInt();
						final int offset = in.getInt() + opcodeOffset;
						lookupTable.put(match, targets.size());
						lookupTable.put(i, addJumpTarget(targets, getBlock.apply(offset)));
					}
					block.jumpTable(block.pop(), addJumpTarget(targets, defaultTarget), lookupTable, targets);
					break;
				}
				case 0xac:
				case 0xad:
				case 0xae:
				case 0xaf:
				case 0xb0: {
					final int n = opcode - 0xac;
					block.returnValue(PrimitiveType.fromId(n), block.pop());
					break;
				}
				case 0xb1:
					block.returnVoid();
					break;

				case 0xb2:
					block.push(block.loadStaticField(constantPool.getFieldReference(getUnsignedShort(in))));
					break;
				case 0xb3:
					block.storeStaticField(constantPool.getFieldReference(getUnsignedShort(in)), block.pop());
					break;
				case 0xb4:
					final OutputPort reference = block.pop();
					block.push(block.loadField(constantPool.getFieldReference(getUnsignedShort(in)),
							block.loadHeap(reference)));
					break;
				case 0xb5: {
					final OutputPort value = block.pop();
					final OutputPort target = block.pop();
					block.storeHeap(target, block.storeField(constantPool.getFieldReference(getUnsignedShort(in)),
							block.loadHeap(target), value));
					break;
				}
				case 0xb6: {
					final MethodReference methodReference = constantPool.getMethodReference(getUnsignedShort(in));
					final List<OutputPort> args = readMethodArguments(methodReference.getType(), block);
					final List<OutputPort> returned = block.invokeVirtual(methodReference, block.pop(), args);
					for (final OutputPort value : returned) {
						block.push(value);
					}
					break;
				}
				case 0xb7: {
					final MethodReference methodReference = constantPool.getMethodReference(getUnsignedShort(in));
					final List<OutputPort> args = readMethodArguments(methodReference.getType(), block);
					final List<OutputPort> returned = block.invokeSpecial(methodReference, block.pop(), args);
					for (final OutputPort value : returned) {
						block.push(value);
					}
					break;
				}
				case 0xb8: {
					final MethodReference methodReference = constantPool.getMethodReference(getUnsignedShort(in));
					final List<OutputPort> returned = block.invokeStatic(methodReference,
							readMethodArguments(methodReference.getType(), block));
					for (final OutputPort value : returned) {
						block.push(value);
					}
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
					final List<OutputPort> args = readMethodArguments(methodReference.getType(), block);
					final List<OutputPort> returned = block.invokeInterface(methodReference, block.pop(), args);
					for (final OutputPort value : returned) {
						block.push(value);
					}
					break;
				}
				case 0xba:
					throw new ClassFormatException("invokedynamic instruction not supported");
				case 0xbb:
					block.push(
							block.allocHeap(block.newInstance(constantPool.getClassReference(getUnsignedShort(in)))));
					break;
				case 0xbc: {
					final PrimitiveType type = PrimitiveType.fromArrayTypeId(getUnsignedByte(in));
					block.push(block.allocHeap(block.newPrimitiveArray(type, block.pop())));
					break;
				}
				case 0xbd: {
					final ClassReference type = constantPool.getClassReference(getUnsignedShort(in));
					block.push(block.allocHeap(block.newArray(type, block.pop())));
					break;
				}
				case 0xbe: {
					block.push(block.arrayLength(block.loadHeap(block.pop())));
					break;
				}
				case 0xbf: {
					block.returnError(block.pop());
					break;
				}
				case 0xc0: {
					final ClassReference type = constantPool.getClassReference(getUnsignedShort(in));
					block.push(block.checkedCast(type, block.pop()));
					break;
				}
				case 0xc1: {
					final ClassReference type = constantPool.getClassReference(getUnsignedShort(in));
					block.push(block.instanceOf(type, block.pop()));
					break;
				}
				case 0xc2: {
					block.monitorEnter(block.pop());
					break;
				}
				case 0xc3: {
					block.monitorExit(block.pop());
					break;
				}
				case 0xc4:
					throw new ClassFormatException("wide instruction not supported");
				case 0xc5:
					throw new ClassFormatException("multianewarray instruction not supported");
				case 0xc6:
				case 0xc7: {
					final CompareType compareType = BasicBlockBuilder.CompareType.fromId(opcode - 0xc6);
					final BasicBlockBuilder then = getBlock.apply(in.getShort() + opcodeOffset);
					final BasicBlockBuilder otherwise = getBlock.apply(in.position());
					block.jumpIf(PrimitiveType.Reference, then, otherwise, compareType, block.pop(),
							block.nullConstant());
					break;
				}
				case 0xc8:
					block.jump(getBlock.apply(in.getInt() + opcodeOffset));
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
				throw new ClassFormatException(
						"Invalid bytecode at offset: " + opcodeOffset + ": " + buf + ": " + e.getMessage(), e);
			}
			if (!block.isTerminated()) {
				block.jump(getBlock.apply(in.position()));
			}
		}
		return getBlock.apply(0);
	}

	private static int addJumpTarget(final List<BasicBlockBuilder> targets, final BasicBlockBuilder target) {
		final int index = targets.indexOf(target);
		if (index != -1) {
			return index;
		} else {
			targets.add(target);
			return targets.size() - 1;
		}
	}

	private static List<OutputPort> readMethodArguments(final MethodType methodType, final BasicBlockBuilder stack) {
		final List<OutputPort> arguments = new ArrayList<>();
		for (final Type arg : methodType.getArgumentTypes()) {
			arguments.add(0, stack.pop());
		}
		return arguments;
	}

	private static OutputPort loadConstant(final BasicBlockBuilder visitor, final ConstantPool constantPool,
			final int index) throws ClassFormatException {
		final Object value = constantPool.get(index);
		if (value instanceof ValueConstant) {
			return ((ValueConstant) value).visit(visitor);
		} else {
			throw new ClassFormatException("Expected constant at index " + index);
		}
	}

	private static OutputPort loadLongConstant(final BasicBlockBuilder visitor, final ConstantPool constantPool,
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
