package bdc;

import java.io.DataInput;
import java.io.IOException;
import java.util.Arrays;

public class ConstantPool {
    private final Object[] constants;

    public ConstantPool(final DataInput dataInput) throws IOException {
	this.constants = new Object[dataInput.readUnsignedShort()];
	for (int i = 1; i < this.constants.length; i++) {
	    this.constants[i] = readConstant(dataInput);
	}
    }

    private Object readConstant(final DataInput dataInput) throws IOException {
	final ConstantType type = ConstantType.fromId(dataInput.readUnsignedByte());
	switch (type) {
	case Class: {
	    return new ClassConstant(dataInput.readUnsignedShort());
	}
	case Fieldref: {
	    final int classIndex = dataInput.readUnsignedShort();
	    final int nameAndTypeIndex = dataInput.readUnsignedShort();
	    return null;

	}
	case Methodref: {
	    final int classIndex = dataInput.readUnsignedShort();
	    final int nameAndTypeIndex = dataInput.readUnsignedShort();
	    return null;
	}
	case InterfaceMethodref: {
	    final int classIndex = dataInput.readUnsignedShort();
	    final int nameAndTypeIndex = dataInput.readUnsignedShort();
	    return null;
	}
	case String: {
	    final int stringIndex = dataInput.readUnsignedShort();
	    return null;
	}
	case Integer: {
	    final int value = dataInput.readInt();
	    return value;
	}
	case Float: {
	    final float value = dataInput.readFloat();
	    return value;
	}
	case Long: {
	    final long value = dataInput.readLong();
	    return value;
	}
	case Double: {
	    final double value = dataInput.readDouble();
	    return value;
	}
	case NameAndType: {
	    final int nameIndex = dataInput.readUnsignedShort();
	    final int descriptorIndex = dataInput.readUnsignedShort();
	    return null;
	}
	case Utf8: {
	    return dataInput.readUTF();
	}
	default:
	    throw new IllegalStateException("Unsupported constant type: " + type);
	}
    }

    public ClassConstant getClass(final int index) {
	return (ClassConstant) this.constants[index];
    }

    public String getUTF8(final int index) {
	return (String) this.constants[index];
    }

    @Override
    public String toString() {
	return "ConstantPool [constants=" + Arrays.toString(this.constants) + "]";
    }

    public class ClassConstant {
	int nameIndex;

	public ClassConstant(final int nameIndex) {
	    this.nameIndex = nameIndex;
	}

	@Override
	public String toString() {
	    return getUTF8(this.nameIndex);
	}
    }
}
