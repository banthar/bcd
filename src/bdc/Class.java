package bdc;

import java.util.Arrays;

import bdc.ConstantPool.ClassReference;
import bdc.Type.ReferenceType;

public class Class {

	private final ConstantPool constants;
	private final int accessFlags;
	private final ClassReference thisClass;
	private final ClassReference superClass;
	private final ClassReference[] interfaces;
	private final Field[] fields;
	private final Method[] methods;
	private final String sourceFile;
	private final String signature;

	public Class(final ConstantPool constantPool, final int accessFlags, final ClassReference thisClass,
			final ClassReference superClass, final ClassReference[] interfaces, final Field[] fields,
			final Method[] methods, final String sourceFile, final String signature) {
		this.constants = constantPool;
		this.accessFlags = accessFlags;
		this.thisClass = thisClass;
		this.superClass = superClass;
		this.interfaces = interfaces;
		this.fields = fields;
		this.methods = methods;
		this.sourceFile = sourceFile;
		this.signature = signature;

	}

	public ClassReference getName() {
		return this.thisClass;
	}

	public Method getMethod(final String name, final String descriptor) throws NoSuchMethodException {
		for (final Method m : this.methods) {
			if (m.getName().equals(name) && m.getDescriptor().equals(descriptor)) {
				return m;
			}
		}
		throw new NoSuchMethodException("No such method: " + this.thisClass + " " + name + descriptor);
	}

	public Method[] getMethods() {
		return this.methods;
	}

	public ConstantPool getConstantPool() {
		return this.constants;
	}

	@Override
	public String toString() {
		return "Class [constants=" + this.constants + ", accessFlags=" + this.accessFlags + ", thisClass="
				+ this.thisClass + ", superClass=" + this.superClass + ", interfaces="
				+ Arrays.toString(this.interfaces) + ", fields=" + Arrays.toString(this.fields) + ", methods="
				+ Arrays.toString(this.methods) + ", sourceFile=" + this.sourceFile + ", signature=" + this.signature
				+ "]";
	}

	public ReferenceType getType() {
		return getName().getType();
	}

}
