package bdc;

import java.io.File;
import java.net.URL;

public class Main {

    public static void main(final String[] args) throws Exception {
	final File bin = new File("bin");
	final URLClassParser bytecodeLoader = new URLClassParser(new URL[] { bin.toURI().toURL() });
	final Class c = bytecodeLoader.loadClass(args[0]);
	for (final Method m : c.getMethods()) {
	    if (args[1].equals(m.getName())) {
		System.out.println("digraph \"" + m.getName() + "\" {");
		System.out
			.println("  graph [rankdir = \"LR\", splines = polyline, sep = \"+30,30\", overlap = false];");
		m.parse(c.getConstantPool());
		System.out.println("}");
		break;
	    }
	}
    }

}
