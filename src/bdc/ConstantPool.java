package bdc;

import java.io.DataInput;
import java.io.IOException;
import java.util.Arrays;

import bdc.Type.FieldType;
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
			return new ValueConstant() {
				@Override
				public OutputPort visit(final BasicBlockBuilder visitor) {
					return visitor.stringConstant(getUTF8(stringIndex));
				}
			};
		}
		case Integer: {
			final int value = dataInput.readInt();
			return new ValueConstant() {
				@Override
				public OutputPort visit(final BasicBlockBuilder visitor) {
					return visitor.integerConstant(value);
				}
			};
		}
		case Float: {
			final float value = dataInput.readFloat();
			return new ValueConstant() {
				@Override
				public OutputPort visit(final BasicBlockBuilder visitor) {
					return visitor.floatConstant(value);
				}
			};
		}
		case Long: {
			final long value = dataInput.readLong();
			return new LongValueConstant() {
				@Override
				public OutputPort visit(final BasicBlockBuilder visitor) {
					return visitor.longConstant(value);
				}
			};
		}
		case Double: {
			final double value = dataInput.readDouble();
			return new LongValueConstant() {
				@Override
				public OutputPort visit(final BasicBlockBuilder visitor) {
					return visitor.doubleConstant(value);
				}
			};
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
			throw new IllegalStateException(
					"InvokeDynamic(" + bootstrapMethodAttrIndex + "," + nameAndType + ") not supported");
		}
		case MethodHandle: {
			final int referenceKind = dataInput.readUnsignedByte();
			final int referenceIndex = dataInput.readUnsignedShort();
			throw new IllegalStateException("MethodHandle(" + referenceKind + "," + referenceIndex + ") not supported");
		}
		case MethodType: {
			final int descriptorIndex = dataInput.readUnsignedShort();
			throw new IllegalStateException("MethodType(" + descriptorIndex + ") not supported");
		}
		default:
			throw new ClassFormatException("Unsupported constant type: " + type);
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

	public Type getDescriptor(final int index) {
		try {
			return Type.fromDescriptor(getUTF8(index));
		} catch (final ClassFormatException e) {
			throw new IllegalStateException(e);
		}
	}

	public Type getSignature(final int index) {
		try {
			return Type.fromSignature(getUTF8(index));
		} catch (final ClassFormatException e) {
			throw new IllegalStateException(e);
		}
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
			return getUTF8(this.nameIndex) + " " + getDescriptor();
		}

		public Type getType() {
			return ConstantPool.this.getDescriptor(this.descriptorIndex);
		}

		private String getDescriptor() {
			return getUTF8(this.descriptorIndex);
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
			return getOwnerClass() + " " + getNameAndType(this.nameAndTypeIndex);
		}

		public ClassReference getOwnerClass() {
			return getClassReference(this.classIndex);
		}

		public String getName() {
			return getNameAndType(this.nameAndTypeIndex).getName();
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

		public String getDescriptor() {
			return getNameAndType(this.nameAndTypeIndex).getDescriptor();
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

	public class ClassReference implements ValueConstant {
		int nameIndex;

		public ClassReference(final int nameIndex) {
			this.nameIndex = nameIndex;
		}

		@Override
		public String toString() {
			return getName();
		}

		public String getJavaName() {
			return getName().replace('/', '.');
		}

		public String getName() {
			return getUTF8(this.nameIndex);
		}

		public ReferenceType getType() {
			return ReferenceType.fromClassName(getName());
		}

		@Override
		public OutputPort visit(final BasicBlockBuilder visitor) {
			return visitor.stringConstant("TODO class " + getType());
		}

	}

	public interface ValueConstant {
		OutputPort visit(BasicBlockBuilder visitor);
	}

	public interface LongValueConstant {
		OutputPort visit(BasicBlockBuilder visitor);
	}

}
