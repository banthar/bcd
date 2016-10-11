package bdc.test;

import java.util.Random;

public interface Test {

    static int gcd(int a, int b) {
	while (a != b) {
	    if (a > b) {
		a -= b;
	    } else {
		b -= a;
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

}
