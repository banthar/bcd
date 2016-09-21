package bdc;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import bdc.ConstantPool.ClassConstant;

public class URLClassParser {

    private final URL[] urls;

    public URLClassParser(final URL[] urls) {
	this.urls = urls;
    }

    public Class loadClass(final String name) throws IOException, ClassFormatError {
	try {
	    for (final URL url : this.urls) {
		final URI absoluteURI = url.toURI().resolve(new URI(null, null, name + ".class", null));
		try (InputStream input = absoluteURI.toURL().openStream()) {
		    final DataInputStream dataInput = new DataInputStream(input);
		    final Class parsedClass = parseClass(dataInput);
		    if (dataInput.read() != -1) {
			throw new ClassFormatError("Extra bytes at end of class");
		    }
		    return parsedClass;
		} catch (final FileNotFoundException e) {
		    continue;
		}
	    }
	    throw new IllegalStateException("Class not found: " + name);
	} catch (final URISyntaxException | MalformedURLException e) {
	    throw new IllegalStateException(e);
	}
    }

    private Class parseClass(final DataInput dataInput) throws IOException, ClassFormatError {
	final int magic = dataInput.readInt();
	if (magic != 0xcafebabe) {
	    throw new ClassFormatError(String.format("Invalid magic number: 0x%08x", magic));
	}
	final int minorVersion = dataInput.readUnsignedShort();
	final int majorVersion = dataInput.readUnsignedShort();
	if (majorVersion != 52 && minorVersion != 0) {
	    throw new ClassFormatError("Unsupported version: " + majorVersion + "." + minorVersion);
	}
	final ConstantPool constantPool = new ConstantPool(dataInput);
	final int accessFlags = dataInput.readUnsignedShort();
	final ClassConstant thisClass = constantPool.getClass(dataInput.readUnsignedShort());
	final ClassConstant superClass = constantPool.getClass(dataInput.readUnsignedShort());
	final ClassConstant[] interfaces = readInterfaces(dataInput, constantPool);
	final Field[] fields = readFields(dataInput, constantPool);
	final Method[] methods = readMethods(dataInput, constantPool);
	String sourceFile = null;
	final int attributes = dataInput.readUnsignedShort();
	String signature = null;
	for (int i = 0; i < attributes; i++) {
	    final String name = constantPool.getUTF8(dataInput.readUnsignedShort());
	    final int length = dataInput.readInt();
	    switch (name) {
	    case "InnerClasses":
		dataInput.readFully(new byte[length]);
		break;
	    case "SourceFile":
		if (length != 2) {
		    throw new ClassFormatError("Invalid SourceFile attribute length");
		}
		sourceFile = constantPool.getUTF8(dataInput.readUnsignedShort());
		break;
	    case "Signature":
		if (length != 2) {
		    throw new ClassFormatError("Invalid Signature attribute length");
		}
		signature = constantPool.getUTF8(dataInput.readUnsignedShort());
		break;
	    default:
		throw new ClassFormatError("Unknown class attribute: " + name);
	    }
	}
	return new Class(constantPool, accessFlags, thisClass, superClass, interfaces, fields, methods, sourceFile,
		signature);
    }

    private Method[] readMethods(final DataInput dataInput, final ConstantPool constantPool)
	    throws IOException, ClassFormatError {
	final Method[] methods = new Method[dataInput.readUnsignedShort()];
	for (int i = 0; i < methods.length; i++) {
	    methods[i] = readMethod(dataInput, constantPool);
	}
	return methods;
    }

    private Method readMethod(final DataInput dataInput, final ConstantPool constantPool)
	    throws IOException, ClassFormatError {
	final int accessFlags = dataInput.readUnsignedShort();
	final int nameIndex = dataInput.readUnsignedShort();
	final int descriptorIndex = dataInput.readUnsignedShort();
	final int attributes = dataInput.readUnsignedShort();
	byte[] code = null;
	final List<ClassConstant> exceptions = new ArrayList<>();
	String signature = null;
	for (int i = 0; i < attributes; i++) {
	    final String name = constantPool.getUTF8(dataInput.readUnsignedShort());
	    final int length = dataInput.readInt();
	    switch (name) {
	    case "Code":
		code = new byte[length];
		dataInput.readFully(code);
		break;
	    case "Exceptions":
		final int exceptionLength = dataInput.readUnsignedShort();
		for (int j = 0; j < exceptionLength; j++) {
		    exceptions.add(constantPool.getClass(dataInput.readUnsignedShort()));
		}
		break;
	    case "Signature":
		if (length != 2) {
		    throw new ClassFormatError("Invalid Signature attribute length");
		}
		signature = constantPool.getUTF8(dataInput.readUnsignedShort());
		break;
	    default:
		throw new ClassFormatError("Unknown method attribute: " + name);

	    }
	}
	return new Method(accessFlags, constantPool.getUTF8(nameIndex), constantPool.getUTF8(descriptorIndex), code,
		exceptions, signature);
    }

    private Field[] readFields(final DataInput dataInput, final ConstantPool constantPool) throws IOException {
	final Field[] fields = new Field[dataInput.readUnsignedShort()];
	for (int i = 0; i < fields.length; i++) {
	    fields[i] = readField(dataInput, constantPool);
	}
	return fields;
    }

    private Field readField(final DataInput dataInput, final ConstantPool constantPool) throws IOException {
	final int accessFlags = dataInput.readUnsignedShort();
	final String fieldName = constantPool.getUTF8(dataInput.readUnsignedShort());
	final String descriptor = constantPool.getUTF8(dataInput.readUnsignedShort());
	final int attributes = dataInput.readUnsignedShort();
	String signature = null;
	for (int i = 0; i < attributes; i++) {
	    final String name = constantPool.getUTF8(dataInput.readUnsignedShort());
	    final int length = dataInput.readInt();
	    switch (name) {
	    case "Signature":
		if (length != 2) {
		    throw new IllegalStateException("Invalid Signature attribute length");
		}
		signature = constantPool.getUTF8(dataInput.readUnsignedShort());
		break;
	    default:
		throw new IllegalStateException("Unknown field attribute: " + name);
	    }
	}
	return new Field(accessFlags, fieldName, descriptor, signature);
    }

    private ClassConstant[] readInterfaces(final DataInput dataInput, final ConstantPool constantPool)
	    throws IOException {
	final ClassConstant[] interfaces = new ClassConstant[dataInput.readUnsignedShort()];
	for (int i = 0; i < interfaces.length; i++) {
	    interfaces[i] = constantPool.getClass(dataInput.readUnsignedShort());
	}
	return interfaces;
    }
}
