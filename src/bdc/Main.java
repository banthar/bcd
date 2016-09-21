package bdc;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class Main {

    public static void main(final String[] args) throws Exception {
	final File bin = new File("bin");
	final URLClassParser bytecodeLoader = new URLClassParser(new URL[] { bin.toURI().toURL() });
	dump(bin, bin, bytecodeLoader);
    }

    private static void dump(final File root, final File parent, final URLClassParser bytecodeLoader)
	    throws IOException, ClassFormatError {
	for (final File f : parent.listFiles()) {
	    if (f.isDirectory()) {
		dump(root, f, bytecodeLoader);
	    } else {
		if (f.getName().endsWith(".class")) {
		    final String name = root.toURI().relativize(f.toURI()).getPath();
		    final String className = name.substring(0, name.length() - 6);
		    final Class c = bytecodeLoader.loadClass(className);
		    System.out.println(c.getName());
		    for (final Method m : c.getMethods()) {
			System.out.println("\t" + m.getName());
			m.parse(c.getConstantPool());
		    }
		}
	    }
	}
    }
}
