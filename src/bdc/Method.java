package bdc;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import bdc.ConstantPool.ClassConstant;

public class Method {

    private final int accessFlags;
    private final String name;
    private final String descriptor;
    private final byte[] code;
    private final List<ClassConstant> exceptions;
    private final String signature;

    public Method(final int accessFlags, final String name, final String descriptor, final byte[] code,
	    final List<ClassConstant> exceptions, final String signature) {
	this.accessFlags = accessFlags;
	this.name = name;
	this.descriptor = descriptor;
	this.code = code;
	this.exceptions = exceptions;
	this.signature = signature;
    }

    public String getName() {
	return this.name;
    }

    public void parse(final ConstantPool constantPool) throws IOException, ClassFormatError {
	final DataInputStream dataInput = new DataInputStream(new ByteArrayInputStream(this.code));
	parseCode(dataInput, constantPool);
	if (dataInput.read() != -1) {
	    throw new ClassFormatError("Extra bytes at end of method code");
	}

    }

    private void parseCode(final DataInputStream dataInput, final ConstantPool constantPool)
	    throws IOException, ClassFormatError {
	final int maxStack = dataInput.readUnsignedShort();
	final int maxLocals = dataInput.readUnsignedShort();
	final int codeLength = dataInput.readInt();
	if (codeLength >= 65536 || codeLength < 0) {
	    throw new ClassFormatError("Invalid method code length: " + Integer.toUnsignedString(codeLength));
	}
	dataInput.readFully(new byte[codeLength]);
	final int exceptionLength = dataInput.readUnsignedShort();
	for (int i = 0; i < exceptionLength; i++) {
	    final int startPc = dataInput.readUnsignedShort();
	    final int endPc = dataInput.readUnsignedShort();
	    final int handlerPc = dataInput.readUnsignedShort();
	    final ClassConstant catchType = constantPool.getClass(dataInput.readUnsignedShort());
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
		throw new ClassFormatError("Unknown method code attribute: " + name);

	    }
	}
    }

    @Override
    public String toString() {
	return "Method [accessFlags=" + this.accessFlags + ", name=" + this.name + ", descriptor=" + this.descriptor
		+ ", code=" + Arrays.toString(this.code) + ", exceptions=" + this.exceptions + ", signature="
		+ this.signature + "]";
    }

}
