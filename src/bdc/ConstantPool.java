package bdc;

import java.io.DataInput;
import java.io.IOException;
import java.util.Arrays;

import bdc.BasicBlockBuilder.Register;
import bdc.Type.FieldType;
import bdc.Type.MethodType;
import bdc.Type.ReferenceType;

public class ConstantPool {
    private final Object[] constants;

    public ConstantPool(final DataInput dataInput) throws IOException, ClassFormatException {
	this.constants = new Object[dataInput.readUnsignedShort()];
	for (int i = 1; i < this.constants.length; i++) {
	    final Object value = readConstant(dataInput);
	    this.constants[i] = value;
	    if (value instanceof LongValueConstant) {
		i++;
	    }
	}
    }

    private Object readConstant(final DataInput dataInput) throws IOException, ClassFormatException {
	final ConstantType type = ConstantType.fromId(dataInput.readUnsignedByte());
	switch (type) {
	case Class: {
	    return new ClassReference(dataInput.readUnsignedShort());
	}
	case Fieldref: {
	    final int classIndex = dataInput.readUnsignedShort();
	    final int nameAndTypeIndex = dataInput.readUnsignedShort();
	    return new FieldReference(classIndex, nameAndTypeIndex);

	}
	case Methodref: {
	    final int classIndex = dataInput.readUnsignedShort();
	    final int nameAndTypeIndex = dataInput.readUnsignedShort();
	    return new MethodReference(classIndex, nameAndTypeIndex);
	}
	case InterfaceMethodref: {
	    final int classIndex = dataInput.readUnsignedShort();
	    final int nameAndTypeIndex = dataInput.readUnsignedShort();
	    return new MethodReference(classIndex, nameAndTypeIndex);
	}
	case String: {
	    final int stringIndex = dataInput.readUnsignedShort();
	    return (ValueConstant) visitor -> visitor.stringConstant(getUTF8(stringIndex));
	}
	case Integer: {
	    final int value = dataInput.readInt();
	    return (ValueConstant) visitor -> visitor.integerConstant(value);
	}
	case Float: {
	    final float value = dataInput.readFloat();
	    return (ValueConstant) visitor -> visitor.floatConstant(value);
	}
	case Long: {
	    final long value = dataInput.readLong();
	    return (LongValueConstant) visitor -> visitor.longConstant(value);
	}
	case Double: {
	    final double value = dataInput.readDouble();
	    return (LongValueConstant) visitor -> visitor.doubleConstant(value);
	}
	case NameAndType: {
	    final int nameIndex = dataInput.readUnsignedShort();
	    final int descriptorIndex = dataInput.readUnsignedShort();
	    return new NameAndType(nameIndex, descriptorIndex);
	}
	case Utf8: {
	    return dataInput.readUTF();
	}
	case InvokeDynamic: {
	    final int bootstrapMethodAttrIndex = dataInput.readUnsignedShort();
	    final int nameAndType = dataInput.readUnsignedShort();
	    return null;
	}
	case MethodHandle: {
	    final int referenceKind = dataInput.readUnsignedByte();
	    final int referenceIndex = dataInput.readUnsignedShort();
	    return null;
	}
	case MethodType: {
	    final int descriptorIndex = dataInput.readUnsignedShort();
	    return null;
	}
	default:
	    throw new IllegalStateException("Unsupported constant type: " + type);
	}
    }

    public Object get(final int index) {
	return this.constants[index];
    }

    public ClassReference getClassReference(final int index) {
	return (ClassReference) this.constants[index];
    }

    public FieldReference getFieldReference(final int index) {
	return (FieldReference) this.constants[index];
    }

    public MethodReference getMethodReference(final int index) {
	return (MethodReference) this.constants[index];
    }

    public String getUTF8(final int index) {
	return (String) this.constants[index];
    }

    public NameAndType getNameAndType(final int index) {
	return (NameAndType) this.constants[index];
    }

    @Override
    public String toString() {
	return "ConstantPool [constants=" + Arrays.toString(this.constants) + "]";
    }

    public class NameAndType {

	private final int nameIndex;
	private final int descriptorIndex;

	public NameAndType(final int nameIndex, final int descriptorIndex) {
	    this.nameIndex = nameIndex;
	    this.descriptorIndex = descriptorIndex;
	}

	@Override
	public String toString() {
	    return getUTF8(this.nameIndex) + " " + getUTF8(this.descriptorIndex);
	}

	public Type getType() {
	    try {
		return Type.fromDescriptor(getUTF8(this.descriptorIndex));
	    } catch (final ClassFormatException e) {
		throw new IllegalStateException(e);
	    }
	}

	public String getName() {
	    return getUTF8(this.nameIndex);
	}
    }

    public class MethodReference {
	private final int classIndex;
	private final int nameAndTypeIndex;

	public MethodReference(final int classIndex, final int nameAndTypeIndex) {
	    this.classIndex = classIndex;
	    this.nameAndTypeIndex = nameAndTypeIndex;
	}

	@Override
	public String toString() {
	    return getClassReference(this.classIndex) + " " + getNameAndType(this.nameAndTypeIndex);
	}

	public MethodType getType() {
	    return (MethodType) getNameAndType(this.nameAndTypeIndex).getType();
	}

	public int getInvokeVirtualCount() {
	    int size = 1;
	    for (final FieldType arg : getType().getArgumentTypes()) {
		size += arg.getByteCodeSize();
	    }
	    return size;
	}
    }

    public class FieldReference {

	private final int classIndex;
	private final int nameAndTypeIndex;

	public FieldReference(final int classIndex, final int nameAndTypeIndex) {
	    this.classIndex = classIndex;
	    this.nameAndTypeIndex = nameAndTypeIndex;
	}

	@Override
	public String toString() {
	    return getClassReference(this.classIndex) + " " + getNameAndType(this.nameAndTypeIndex);
	}

	public FieldType getType() {
	    return (FieldType) getNameAndType(this.nameAndTypeIndex).getType();
	}

	public ClassReference getTarget() {
	    return getClassReference(this.classIndex);
	}

	public String getName() {
	    return getNameAndType(this.nameAndTypeIndex).getName();
	}
    }

    public class ClassReference {
	int nameIndex;

	public ClassReference(final int nameIndex) {
	    this.nameIndex = nameIndex;
	}

	@Override
	public String toString() {
	    return getUTF8(this.nameIndex);
	}

	public String getJavaName() {
	    return getUTF8(this.nameIndex).replace('/', '.');
	}

	public ReferenceType getType() {
	    return ReferenceType.fromClassName(getUTF8(this.nameIndex));
	}
    }

    public interface ValueConstant {
	Register visit(BasicBlockBuilder visitor);
    }

    public interface LongValueConstant {
	Register visit(BasicBlockBuilder visitor);
    }
}
