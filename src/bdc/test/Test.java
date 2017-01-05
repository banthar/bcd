package bdc.test;

public interface Test {
	static int f0(final int a, final int b) {
		return f1(a, b);
	}

	static int f1(final int a, final int b) {
		return f2(a, b);
	}

	static int f2(final int a, final int b) {
		return f0(a, b);
	}

	static void main(final String args[]) {
		f0(0, 0);
	}
}
