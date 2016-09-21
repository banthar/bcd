package bdc;

import java.util.Arrays;

import bdc.ConstantPool.ClassConstant;

public class Class {

    private final ConstantPool constants;
    private final int accessFlags;
    private final ClassConstant thisClass;
    private final ClassConstant superClass;
    private final ClassConstant[] interfaces;
    private final Field[] fields;
    private final Method[] methods;
    private final String sourceFile;
    private final String signature;

    public Class(final ConstantPool constantPool, final int accessFlags, final ClassConstant thisClass,
	    final ClassConstant superClass, final ClassConstant[] interfaces, final Field[] fields,
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

    public ClassConstant getName() {
	return this.thisClass;
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

}
