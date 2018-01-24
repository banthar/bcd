package bdc;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.URL;

import x86_64.Codegen;
import x86_64.Elf;

public class Main {

	public static void main(final String[] args) throws Exception {
		final File bin = new File("bin");
		final URLClassParser bytecodeLoader = new URLClassParser(new URL[] { bin.toURI().toURL() });
		final Method mainMethod = bytecodeLoader.loadClass(args[0]).getMethod("f", "(II)I");
		mainMethod.parse();
		try (final PrintStream out = new PrintStream(new File("graph.gv"))) {
			bytecodeLoader.dump(out);
		}
		final byte[] bytes = Codegen.codegen(mainMethod);
		for (final byte b : bytes) {
			System.out.format("%02x", b);
		}
		System.out.println();

		final File file = new File("a.out");
		try (DataOutputStream out = new DataOutputStream(new FileOutputStream(file))) {
			file.setExecutable(true);
			new Elf().write(out, true, true, bytes);
		}
		Elf.read(new File("a.out"));

	}
}
