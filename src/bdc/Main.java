package bdc;

import java.io.File;
import java.net.URL;

public class Main {

    public static void main(final String[] args) throws Exception {
	final File bin = new File("bin");
	final URLClassParser bytecodeLoader = new URLClassParser(new URL[] { bin.toURI().toURL() });
	final Class c = bytecodeLoader.loadClass("bdc/Type");
	System.out.println(c.getName() + ":");
	for (final Method m : c.getMethods()) {
	    System.out.println("  " + m.getName() + ":");
	    m.parse(c.getConstantPool());
	}
    }

}
