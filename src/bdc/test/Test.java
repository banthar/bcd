package bdc.test;

import java.util.Arrays;

public interface Test {

	default int gdc2(final int a, final int b) {
		return Arrays.asList(a, b).size();
	}

	static int gdc(int a, int b) {
		while (a != b) {
			if (a > b) {
				a /= b;
			} else {
				b /= a;
			}
		}
		return a;
	}

	static int f(final int a, final int b, final int c) {
		final int m0 = a * b;
		final int m1 = a * c;
		final int m2 = b * c;
		final int a0 = m0 + m1;
		final int a1 = a0 + m2;
		if (a1 <= 0) {
			return 0;
		} else {
			return a1;
		}
	}
}
