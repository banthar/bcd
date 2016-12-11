package bdc.test;

public interface Test {

    static int pow(final int a, final int b) {
	int c = 1;
	for (int i = 1; i <= b; i++) {
	    c *= a;
	}
	return c;
    }
}
