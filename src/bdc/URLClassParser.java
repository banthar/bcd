package bdc;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bdc.ConstantPool.ClassReference;

public class URLClassParser {

	private final List<URL> urls;

	private final Map<String, Class> cache = new HashMap<>();

	public URLClassParser(final List<URL> urls) {
		this.urls = urls;
	}

	public URLClassParser(final URL[] urls) {
		this(Arrays.asList(urls));
	}

	public Class loadClass(final String name) throws IOException, ClassFormatException, ClassNotFoundException {
		final Class cached = this.cache.get(name);
		if (cached != null) {
			return cached;
		}
		try {
			for (final URL url : this.urls) {
				final URI absoluteURI = new URI(url + name.replace('.', '/') + ".class");
				try (InputStream input = absoluteURI.toURL().openStream()) {
					final DataInputStream dataInput = new DataInputStream(input);
					final Class parsedClass = parseClass(dataInput);
					if (dataInput.read() != -1) {
						throw new ClassFormatException("Extra bytes at end of class");
					}
					this.cache.put(name, parsedClass);
					return parsedClass;
				} catch (final FileNotFoundException e) {
					continue;
				}
			}
			throw new ClassNotFoundException("Class not found: " + name);
		} catch (final URISyntaxException | MalformedURLException e) {
			throw new IllegalStateException(e);
		}
	}

	private Class parseClass(final DataInput dataInput) throws IOException, ClassFormatException {
		final int magic = dataInput.readInt();
		if (magic != 0xcafebabe) {
			throw new ClassFormatException(String.format("Invalid magic number: 0x%08x", magic));
		}
		final int minorVersion = dataInput.readUnsignedShort();
		final int majorVersion = dataInput.readUnsignedShort();
		if (majorVersion != 52 && minorVersion != 0) {
			throw new ClassFormatException("Unsupported version: " + majorVersion + "." + minorVersion);
		}
		final ConstantPool constantPool = new ConstantPool(dataInput);
		final int accessFlags = dataInput.readUnsignedShort();
		final ClassReference thisClass = constantPool.getClassReference(dataInput.readUnsignedShort());
		final ClassReference superClass = constantPool.getClassReference(dataInput.readUnsignedShort());
		final ClassReference[] interfaces = readInterfaces(dataInput, constantPool);
		final Field[] fields = readFields(dataInput, constantPool);
		final Method[] methods = readMethods(dataInput, thisClass, constantPool);
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
			case "BootstrapMethods":
				dataInput.readFully(new byte[length]);
				break;
			case "EnclosingMethod":
				dataInput.readFully(new byte[length]);
				break;
			case "SourceFile":
				if (length != 2) {
					throw new ClassFormatException("Invalid SourceFile attribute length");
				}
				sourceFile = constantPool.getUTF8(dataInput.readUnsignedShort());
				break;
			case "Signature":
				if (length != 2) {
					throw new ClassFormatException("Invalid Signature attribute length");
				}
				signature = constantPool.getUTF8(dataInput.readUnsignedShort());
				break;
			case "RuntimeVisibleAnnotations":
				dataInput.readFully(new byte[length]);
				break;
			default:
				throw new ClassFormatException("Unknown class attribute: " + name);
			}
		}
		return new Class(constantPool, accessFlags, thisClass, superClass, interfaces, fields, methods, sourceFile,
				signature);
	}

	private Method[] readMethods(final DataInput dataInput, final ClassReference thisClass,
			final ConstantPool constantPool) throws IOException, ClassFormatException {
		final Method[] methods = new Method[dataInput.readUnsignedShort()];
		for (int i = 0; i < methods.length; i++) {
			methods[i] = readMethod(dataInput, thisClass, constantPool);
		}
		return methods;
	}

	private Method readMethod(final DataInput dataInput, final ClassReference thisClass,
			final ConstantPool constantPool) throws IOException, ClassFormatException {
		final int accessFlags = dataInput.readUnsignedShort();
		final int nameIndex = dataInput.readUnsignedShort();
		final int descriptorIndex = dataInput.readUnsignedShort();
		final int attributes = dataInput.readUnsignedShort();
		byte[] code = null;
		final List<ClassReference> exceptions = new ArrayList<>();
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
					exceptions.add(constantPool.getClassReference(dataInput.readUnsignedShort()));
				}
				break;
			case "Signature":
				if (length != 2) {
					throw new ClassFormatException("Invalid Signature attribute length");
				}
				signature = constantPool.getUTF8(dataInput.readUnsignedShort());
				break;
			case "RuntimeInvisibleAnnotations":
				dataInput.readFully(new byte[length]);
				break;
			case "RuntimeVisibleAnnotations":
				dataInput.readFully(new byte[length]);
				break;
			default:
				throw new ClassFormatException("Unknown method attribute: " + name);

			}
		}
		return new Method(this, constantPool, thisClass, accessFlags, constantPool.getUTF8(nameIndex),
				constantPool.getUTF8(descriptorIndex), code, exceptions, signature);
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
			case "ConstantValue": {
				if (length != 2) {
					throw new IllegalStateException("Invalid Signature attribute length");
				}
				final int valueIndex = dataInput.readUnsignedShort();
				break;
			}
			default:
				throw new IllegalStateException("Unknown field attribute: " + name);
			}
		}
		return new Field(accessFlags, fieldName, descriptor, signature);
	}

	private ClassReference[] readInterfaces(final DataInput dataInput, final ConstantPool constantPool)
			throws IOException {
		final ClassReference[] interfaces = new ClassReference[dataInput.readUnsignedShort()];
		for (int i = 0; i < interfaces.length; i++) {
			interfaces[i] = constantPool.getClassReference(dataInput.readUnsignedShort());
		}
		return interfaces;
	}

	public void dump(final PrintStream out) {
		out.println("digraph G {");
		for (final Class c : this.cache.values()) {
			out.println("  subgraph cluster_" + c.getName().toString().replace('/', '_').replace('$', '_') + " {");
			for (final Method m : c.getMethods()) {
				m.dump(out);
			}
			out.println("}");
		}
		out.println("}");
	}
}
