package bdc.test;

public interface Test {

	public static int f(final int a, final int c, final int b) {
		return a + b + c;
	}

	public static void main() {
		f(0, 0, 0);
	}

	public static void _start() {
		main();
	}

}
