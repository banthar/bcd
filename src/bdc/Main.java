package bdc;

import java.io.File;
import java.io.PrintStream;
import java.net.URL;

public class Main {

    public static void main(final String[] args) throws Exception {
	final File bin = new File("bin");
	final URLClassParser bytecodeLoader = new URLClassParser(new URL[] { bin.toURI().toURL() });
	final PrintStream out = new PrintStream(new File("graph.gv"));
	out.println("digraph G {");
	out.println("  graph [rankdir = \"LR\", splines = polyline];");
	for (final String arg : args) {
	    final Class c = bytecodeLoader.loadClass(arg);
	    out.println("  subgraph cluster_" + c.getName().toString().replace('/', '_').replace('$', '_') + " {");
	    for (final Method m : c.getMethods()) {
		out.println("  subgraph cluster_" + m.getName().replace('<', '_').replace('>', '_').replace('$', '_')
			+ " {");
		out.println("    label=\"" + m.getName() + "\";");
		m.parse(c.getConstantPool());
		m.dump(out);
		out.println("  }");
	    }
	    out.println("}");
	}
	out.println("}");

    }

}
