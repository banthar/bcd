package bdc;

import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public interface Type {

	public interface FieldType extends Type {
		String getJavaName();

		int getByteCodeSize();
	}

	public enum UnknownType implements FieldType {
		INSTANCE;

		@Override
		public String getJavaName() {
			return "?";
		}

		@Override
		public int getByteCodeSize() {
			return 1;
		}

		@Override
		public String toDescriptor() {
			throw new IllegalStateException();
		}

	}

	public enum PrimitiveType implements FieldType {
		Integer("int", "I"), Long("long", "L"), Float("float", "F"), Double("double", "D"), Reference("Object",
				"LObject;"), Byte("byte", "B"), Char("char", "C"), Short("short", "S"), Boolean("boolean", "Z");

		private final String javaName;

		private final String descriptor;

		private PrimitiveType(final String shortName, final String descriptor) {
			this.javaName = shortName;
			this.descriptor = descriptor;
		}

		@Override
		public String getJavaName() {
			return javaName;
		}

		public static PrimitiveType fromId(final int id) {
			return values()[id];
		}

		public static PrimitiveType fromArrayTypeId(final int id) throws ClassFormatException {
			switch (id) {
			case 4:
				return Boolean;
			case 5:
				return Char;
			case 6:
				return Float;
			case 7:
				return Double;
			case 8:
				return Byte;
			case 9:
				return Short;
			case 10:
				return Integer;
			case 11:
				return Long;
			default:
				throw new ClassFormatException("Invalid array type id: " + id);
			}
		}

		@Override
		public int getByteCodeSize() {
			return this == Long || this == Double ? 2 : 1;
		}

		@Override
		public String toDescriptor() {
			return descriptor;
		}

		static FieldType fromJavaType(final java.lang.Class<?> type) {
			if (type == int.class) {
				return PrimitiveType.Integer;
			} else if (type == boolean.class) {
				return PrimitiveType.Boolean;
			} else {
				throw new IllegalStateException();
			}
		}

	}

	class ArrayType implements FieldType {

		private final FieldType elementType;

		public ArrayType(final FieldType elementType) {
			this.elementType = elementType;
		}

		@Override
		public String getJavaName() {
			return elementType + "[]";
		}

		@Override
		public int getByteCodeSize() {
			return 1;
		}

		@Override
		public String toString() {
			return "Array<" + elementType + ">";
		}

		@Override
		public String toDescriptor() {
			return "[" + elementType.toDescriptor() + ";";
		}

	}

	class ReferenceType implements FieldType {
		private final String name;

		public ReferenceType(final String name) {
			this.name = name;
		}

		@Override
		public String getJavaName() {
			return name.replace('/', '.');
		}

		@Override
		public int getByteCodeSize() {
			return 1;
		}

		public static ReferenceType fromClassName(final String name) {
			return new ReferenceType(name);
		}

		@Override
		public String toString() {
			return "Reference<" + name + ">";
		}

		@Override
		public String toDescriptor() {
			return "L" + name + ";";
		}
	}

	class MethodType implements Type {

		private final List<FieldType> argumentTypes;
		private final Type returnType;

		public MethodType(final List<FieldType> argumentTypes, final Type returnType) {
			this.argumentTypes = argumentTypes;
			this.returnType = returnType;
		}

		public List<? extends FieldType> getArgumentTypes() {
			return argumentTypes;
		}

		public Type getReturnType() {
			return returnType;
		}

		public boolean isVoid() {
			return returnType instanceof VoidType;
		}

		@Override
		public String toString() {
			return argumentTypes + " -> " + returnType;
		}

		public static MethodType fromDescriptor(final String descriptor) throws ClassFormatException {
			return (MethodType) Type.fromDescriptor(descriptor);
		}

		@Override
		public String toDescriptor() {
			String descriptor = "(";
			for (final FieldType arg : argumentTypes) {
				descriptor += arg.toDescriptor();
			}
			descriptor += ")";
			descriptor += returnType.toDescriptor();
			return descriptor;
		}
	}

	class VoidType implements Type {

		@Override
		public String toString() {
			return "Void";
		}

		@Override
		public String toDescriptor() {
			return "V";
		}

	}

	static Type fromSignature(final String signature) throws ClassFormatException {
		final CharBuffer buffer = CharBuffer.wrap(signature);
		final Type type = parseFieldTypeSignature(buffer);
		if (buffer.position() != buffer.capacity()) {
			throw signatureParseError(buffer, "extra characters");
		}
		return type;
	}

	String toDescriptor();

	static Type parseFieldTypeSignature(final CharBuffer buffer) throws ClassFormatException {
		final char c = buffer.get();
		switch (c) {
		case 'L':
			return readClassTypeSignature(buffer);
		case '[':
			throw new IllegalStateException();
		case 'T':
			final String name = readUntil(buffer, Arrays.asList('.', ';', '[', '/', '<', '>'));
			expect(buffer, ';');
			return typeVariable(name);
		default:
			throw signatureParseError(buffer, "unknown signature type: \"" + c + "\"");
		}
	}

	static Type typeVariable(final String name) {
		return getUnknown();
	}

	static Type readClassTypeSignature(final CharBuffer buffer) throws ClassFormatException {
		final String name = readUntil(buffer, Arrays.asList('.', ';', '[', '<', '>'));
		final char c = buffer.get();
		switch (c) {
		case ';':
			return new ReferenceType(name);
		case '<':
			while (buffer.get(buffer.position()) != '>') {
				readTypeArgument(buffer);
				if (buffer.position() == buffer.capacity()) {
					throw signatureParseError(buffer, "expected: \">\"");
				}
			}
			buffer.get();
			expect(buffer, ';');
			return new ReferenceType(name);
		default:
			throw signatureParseError(buffer, "unexpected character: \"" + c + "\"");
		}
	}

	static void expect(final CharBuffer buffer, final char expected) throws ClassFormatException {
		if (buffer.get(buffer.position()) != expected) {
			throw signatureParseError(buffer, "expected: \"" + expected + "\"");
		}
		buffer.get();
	}

	static Type readTypeArgument(final CharBuffer buffer) throws ClassFormatException {
		final char c = buffer.get(buffer.position());
		switch (c) {
		case '+':
			buffer.get();
			return parseFieldTypeSignature(buffer);
		case '-':
			buffer.get();
			return parseFieldTypeSignature(buffer);
		case '*':
			buffer.get();
			return wildcard();
		default:
			return parseFieldTypeSignature(buffer);
		}
	}

	static Type fromDescriptor(final String descriptor) throws ClassFormatException {
		final CharBuffer buffer = CharBuffer.wrap(descriptor);
		final Type type = parseType(buffer);
		if (buffer.position() != buffer.capacity()) {
			throw descriptorParseError(buffer, "extra characters");
		}
		return type;
	}

	static Type parseType(final CharBuffer buffer) throws ClassFormatException {
		final char c = buffer.get();
		switch (c) {
		case 'B':
			return PrimitiveType.Byte;
		case 'C':
			return PrimitiveType.Char;
		case 'D':
			return PrimitiveType.Double;
		case 'F':
			return PrimitiveType.Float;
		case 'I':
			return PrimitiveType.Integer;
		case 'J':
			return PrimitiveType.Long;
		case 'L':
			final String name = readUntil(buffer, Arrays.asList(';'));
			buffer.get();
			return new ReferenceType(name);
		case 'S':
			return PrimitiveType.Short;
		case 'Z':
			return PrimitiveType.Boolean;
		case 'V':
			return new VoidType();
		case '[': {
			final FieldType elementType = (FieldType) parseType(buffer);
			return new ArrayType(elementType);
		}
		case '(': {
			final List<FieldType> argumentTypes = new ArrayList<>();
			while (buffer.get(buffer.position()) != ')') {
				argumentTypes.add((FieldType) parseType(buffer));
			}
			if (buffer.get() != ')') {
				throw descriptorParseError(buffer, "expected \")\"");
			}
			final Type returnType = parseType(buffer);
			return new MethodType(argumentTypes, returnType);
		}
		default:
			throw descriptorParseError(buffer, "unknown type: \"" + c + "\"");
		}
	}

	static String readUntil(final CharBuffer buffer, final List<Character> terminators) throws ClassFormatException {
		final int start = buffer.position();
		int end = start;
		while (!terminators.contains(buffer.get(end))) {
			end++;
			if (end == buffer.capacity()) {
				throw descriptorParseError(buffer, "expected " + String.join(" or " + terminators));
			}
		}
		final char[] nameBuffer = new char[end - start];
		buffer.get(nameBuffer);
		return new String(nameBuffer);
	}

	static ClassFormatException signatureParseError(final CharBuffer buffer, final String message) {
		final StringBuilder builder = new StringBuilder();
		builder.append("Invalid type signature \"");
		return parseError(builder, buffer, message);
	}

	static ClassFormatException descriptorParseError(final CharBuffer buffer, final String message) {
		final StringBuilder builder = new StringBuilder();
		builder.append("Invalid type descriptor \"");
		return parseError(builder, buffer, message);
	}

	static ClassFormatException parseError(final StringBuilder builder, final CharBuffer buffer, final String message) {
		final CharBuffer readOnly = buffer.asReadOnlyBuffer();
		readOnly.position(0);
		builder.append(readOnly);
		builder.append("\" at position ");
		builder.append(buffer.position());
		builder.append(": ");
		builder.append(message);
		builder.append(": \"");
		readOnly.position(buffer.position());
		builder.append(readOnly);
		builder.append("\"");
		return new ClassFormatException(builder.toString());
	}

	static FieldType getUnknown() {
		return UnknownType.INSTANCE;
	}

	static Type wildcard() {
		return getUnknown();
	}

	static FieldType string() {
		return fromJavaClass(String.class);
	}

	static FieldType integer() {
		return fromJavaClass(int.class);
	}

	static ArrayType array(final FieldType elementType) {
		return new ArrayType(elementType);
	}

	static FieldType fromJavaClass(final java.lang.Class<?> type) {
		if (type.isPrimitive()) {
			return PrimitiveType.fromJavaType(type);
		} else {
			return ReferenceType.fromClassName(getRuntimeClassName(type));
		}
	}

	static String getRuntimeClassName(final java.lang.Class<?> type) {
		return type.getName().replace('.', '/');
	}

}
