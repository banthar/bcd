package bdc.test;

public interface Test {
	public static int f() {
		int a = 10;
		a += 1;
		a += a;
		a -= 4;
		a *= 10;
		return a;
	}

	public static void main(final String[] args) {
		f();
	}
}
