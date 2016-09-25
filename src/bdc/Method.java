package bdc;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import bdc.ConstantPool.ClassReference;

public class Method {

    private final int accessFlags;
    private final String name;
    private final String descriptor;
    private final byte[] code;
    private final List<ClassReference> exceptions;
    private final String signature;

    public Method(final int accessFlags, final String name, final String descriptor, final byte[] code,
	    final List<ClassReference> exceptions, final String signature) {
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

    public void parse(final ConstantPool constantPool) throws IOException, ClassFormatException {
	if (this.code == null) {
	    return;
	}
	final DataInputStream dataInput = new DataInputStream(new ByteArrayInputStream(this.code));
	InstructionParser.parseCode(dataInput, constantPool);
	if (dataInput.read() != -1) {
	    throw new ClassFormatException("Extra bytes at end of method code");
	}
    }

    @Override
    public String toString() {
	return "Method [accessFlags=" + this.accessFlags + ", name=" + this.name + ", descriptor=" + this.descriptor
		+ ", code=" + Arrays.toString(this.code) + ", exceptions=" + this.exceptions + ", signature="
		+ this.signature + "]";
    }

}
