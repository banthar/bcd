package wasm;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import bdc.Method;
import bdc.MethodType;
import bdc.Type;
import bdc.Type.FieldType;
import bdc.Type.PrimitiveType;
import bdc.URLClassParser;

public class WASMModule {

	List<MethodType> types = new ArrayList<>();

	List<Integer> signatures = new ArrayList<>();

	List<Method> methods = new ArrayList<>();

	int addType(final MethodType type) {
		final int index = this.types.size();
		this.types.add(type);
		return index;
	}

	int addMethod(final Method method) {
		final int index = this.signatures.size();
		this.methods.add(method);
		this.signatures.add(addType(method.getType()));
		return index;
	}

	private static void writeVarUInt(final DataOutputStream output, int n) throws IOException {
		do {
			final int m = n & 0b01111111;
			n >>>= 7;
			output.writeByte(m | (n == 0 ? 0b00000000 : 0b10000000));
		} while (n != 0);
	}

	private void writeVarInt(final DataOutputStream output, long n) throws IOException {
		do {
			final int m = (int) n & 0b01111111;
			n >>= 7;
			final boolean end = n == -1 || n == 0 && (m & 0b1000000) == 0;
			final int b = m | (end ? 0b00000000 : 0b10000000);
			output.writeByte(b);
			if (end) {
				break;
			}
		} while (true);
	}

	private void writeTo(final OutputStream out) throws IOException {
		final DataOutputStream output = new DataOutputStream(out);
		output.writeInt(0x0061736d);
		output.writeInt(0x01000000);
		writeTypes(output);
		writeSignatures(output);
		writeCode(output);
	}

	private void writeCode(final DataOutputStream output) throws IOException {
		writeVarUInt(output, WASMSectionType.CODE.id);
		final ByteArrayOutputStream sectionStream = new ByteArrayOutputStream();
		final DataOutputStream sectionOutput = new DataOutputStream(sectionStream);
		writeVarUInt(sectionOutput, this.methods.size());
		for (final Method method : this.methods) {
			writeMethod(sectionOutput, method);
		}
		writeVarUInt(output, sectionStream.size());
		output.write(sectionStream.toByteArray());
	}

	private void writeMethod(final DataOutputStream output, final Method method) throws IOException {
		final ByteArrayOutputStream codeBytes = new ByteArrayOutputStream();
		final DataOutputStream code = new DataOutputStream(codeBytes);
		writeVarUInt(code, 0);
		code.writeByte(0x20);
		writeVarInt(code, 0);
		code.writeByte(0x20);
		writeVarInt(code, 1);
		code.writeByte(0x6a);
		code.writeByte(0x0f);
		code.writeByte(0x0b);

		writeVarInt(output, codeBytes.size());
		output.write(codeBytes.toByteArray());
	}

	private void writeSignatures(final DataOutputStream output) throws IOException {
		writeVarUInt(output, WASMSectionType.FUNCTION.id);
		final ByteArrayOutputStream sectionStream = new ByteArrayOutputStream();
		final DataOutputStream sectionOutput = new DataOutputStream(sectionStream);
		writeVarUInt(sectionOutput, this.signatures.size());
		for (final Integer typeIndex : this.signatures) {
			writeVarUInt(sectionOutput, typeIndex);
		}
		writeVarUInt(output, sectionStream.size());
		output.write(sectionStream.toByteArray());
	}

	private void writeTypes(final DataOutputStream output) throws IOException {
		writeVarUInt(output, WASMSectionType.TYPE.id);
		final ByteArrayOutputStream sectionBytes = new ByteArrayOutputStream();
		final DataOutputStream sectionOutput = new DataOutputStream(sectionBytes);
		writeVarUInt(sectionOutput, this.types.size());
		for (final MethodType type : this.types) {
			writeType(sectionOutput, type);
		}
		writeVarUInt(output, sectionBytes.size());
		output.write(sectionBytes.toByteArray());
	}

	private void writeType(final DataOutputStream sectionOutput, final MethodType type) throws IOException {
		writeVarUInt(sectionOutput, 0x60);
		final List<? extends FieldType> argumentTypes = type.getArgumentTypes();
		writeVarUInt(sectionOutput, argumentTypes.size());
		for (final FieldType argumentType : argumentTypes) {
			writeVarUInt(sectionOutput, toValueTypeIndex(argumentType));
		}
		if (type.getReturnType() instanceof Type.VoidType) {
			writeVarUInt(sectionOutput, 0);
		} else {
			writeVarUInt(sectionOutput, 1);
			writeVarUInt(sectionOutput, toValueTypeIndex(type.getReturnType()));
		}
	}

	private int toValueTypeIndex(final Type type) {
		if (type == PrimitiveType.Integer) {
			return 0x7f;
		} else if (type == PrimitiveType.Long) {
			return 0x7e;
		} else if (type == PrimitiveType.Float) {
			return 0x7d;
		} else if (type == PrimitiveType.Double) {
			return 0x7c;
		} else {
			throw new IllegalArgumentException("Not a value type: " + type);
		}
	}

	public static void main(final String[] args) throws Exception {
		final File bin = new File("bin");
		final URLClassParser bytecodeLoader = new URLClassParser(new URL[] { bin.toURI().toURL() });
		final Method method = bytecodeLoader.loadClass("bdc.test.WASMExample").getMethod("f", "(II)I");
		method.parse();
		final WASMModule wasmModule = new WASMModule();
		wasmModule.addMethod(method);

		final File file = File.createTempFile("out", ".wasm");
		file.deleteOnExit();
		try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
			wasmModule.writeTo(out);
		}
		final String wasm2wast = "/home/piotr/programy/wabt/build/wasm-objdump";
		final ProcessBuilder processBuilder = new ProcessBuilder(wasm2wast, "-x", "-d", file.getAbsolutePath());
		processBuilder.redirectOutput(Redirect.INHERIT);
		processBuilder.redirectError(Redirect.INHERIT);
		processBuilder.start().waitFor();
	}
}
