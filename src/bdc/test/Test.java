package bdc.test;

public interface Test {

    static int pow(final int a, final int b) {
	int c = 1;
	for (int i = 1; i <= b; i++) {
	    c *= a > c ? a : c;
	}
	return c;
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
}
