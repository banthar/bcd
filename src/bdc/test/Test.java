package bdc.test;

import java.io.Closeable;
import java.io.IOException;

public interface Test {

    static int f() throws IOException {
	final Closeable out = System.out;
	out.close();
	return new int[10].length;
    }
}
