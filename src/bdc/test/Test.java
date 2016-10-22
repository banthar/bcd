package bdc.test;

import java.util.Random;

public interface Test {

    static int parallel(final int a, final int b) {
	final int c = a * a * a * a;
	final int d = b + b + b + b;
	return c + d;
    }

    static int gcd(int a, int b) {
	while (a != b) {
	    int max;
	    int min;
	    if (a > b) {
		max = a;
		min = b;
	    } else {
		min = a;
		max = b;
	    }
	    final int d = max - min;
	    if (a > b) {
		a = d;
	    } else {
		b = d;
	    }
	}
	return a;
    }

    public static void main(final String[] args) {
	final Random r = new Random(0);
	int n = 0;
	for (int i = 0; i < 1024 * 1024 * 1024; i++) {
	    n += gcd(r.nextInt(Integer.MAX_VALUE), r.nextInt(Integer.MAX_VALUE));
	}
	System.out.println(n);
    }

    static int infinite(final int a) {
	int b = a;
	while (true) {
	    b = a + b;
	}
    }
}
