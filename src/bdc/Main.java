package bdc;

import java.io.File;
import java.net.URL;

public class Main {

    public static void main(final String[] args) throws Exception {
	final File bin = new File("bin");
	final URLClassParser bytecodeLoader = new URLClassParser(new URL[] { bin.toURI().toURL() });
	System.out.println("digraph G {");
	System.out.println("  graph [rankdir = \"LR\", splines = polyline];");
	for (final String arg : args) {
	    final Class c = bytecodeLoader.loadClass(arg);
	    System.out
		    .println("  subgraph cluster_" + c.getName().toString().replace('/', '_').replace('$', '_') + " {");
	    for (final Method m : c.getMethods()) {
		System.out.println("  subgraph cluster_"
			+ m.getName().replace('<', '_').replace('>', '_').replace('$', '_') + " {");
		System.out.println("    label=\"" + m.getName() + "\";");
		m.parse(c.getConstantPool());
		System.out.println("  }");
	    }
	    System.out.println("}");
	}
	System.out.println("}");
    }

}
