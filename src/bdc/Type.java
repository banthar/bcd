package bdc;

import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;

public interface Type {

    public interface FieldType extends Type {
	String getJavaName();

	int getByteCodeSize();
    }

    public enum PrimitiveType implements FieldType {
	Integer("int"), Long("long"), Float("float"), Double("double"), Reference("Object"), Byte("byte"), Char(
		"char"), Short("short"), Boolean("boolean");

	private final String javaName;

	private PrimitiveType(final String shortName) {
	    this.javaName = shortName;
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
    }

    class VoidType implements Type {

	@Override
	public String toString() {
	    return "Void";
	}

    }

    static Type fromDescriptor(final String descriptor) throws ClassFormatException {
	final CharBuffer buffer = CharBuffer.wrap(descriptor);
	final Type type = parseType(buffer);
	if (buffer.position() != buffer.capacity()) {
	    throw parseError(buffer, "extra characters");
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
	case 'L': {
	    final int start = buffer.position();
	    int end = start;
	    while (buffer.get(end) != ';') {
		end++;
		if (end == buffer.capacity()) {
		    throw parseError(buffer, "missing ;");
		}
	    }
	    final char[] nameBuffer = new char[end - start];
	    buffer.get(nameBuffer);
	    final String name = new String(nameBuffer);
	    buffer.get();
	    return new ReferenceType(name);
	}
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
		throw parseError(buffer, "expected \")\"");
	    }
	    final Type returnType = parseType(buffer);
	    return new MethodType(argumentTypes, returnType);
	}
	default:
	    throw parseError(buffer, "unknown type: \"" + c + "\"");
	}
    }

    static ClassFormatException parseError(final CharBuffer buffer, final String message) {
	final StringBuilder builder = new StringBuilder();
	builder.append("Invalid type descriptor \"");
	final CharBuffer readOnly = buffer.asReadOnlyBuffer();
	readOnly.position(0);
	builder.append(readOnly);
	builder.append("\" at position ");
	builder.append(buffer.position());
	builder.append(": ");
	builder.append(message);
	return new ClassFormatException(builder.toString());
    }
}
