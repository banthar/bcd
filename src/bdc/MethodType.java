package bdc;

import java.util.List;

import bdc.Type.FieldType;
import bdc.Type.VoidType;

public class MethodType implements Type {

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