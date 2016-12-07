package bdc.test;

public interface Test {

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
