package bdc;

import java.io.File;
import java.io.PrintStream;
import java.net.URL;

public class Main {

	public static void main(final String[] args) throws Exception {
		final File bin = new File("bin");
		final URLClassParser bytecodeLoader = new URLClassParser(new URL[] { bin.toURI().toURL() });
		bytecodeLoader.loadClass(args[0]).getMethod("main", "([Ljava/lang/String;)V").parse();
		try (final PrintStream out = new PrintStream(new File("graph.gv"))) {
			bytecodeLoader.dump(out);
		}
	}
}
