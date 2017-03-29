package bdc.test;

import org.junit.runner.RunWith;

@RunWith(BdcRunner.class)
public class ArraysTest {

	@MethodReturnsConstant
	public static int return_length() {
		return new int[3].length;
	}

	@MethodReturnsConstant
	public static int single_read() {
		return (new int[1])[0];
	}

	@MethodReturnsConstant
	public static int single_write() {
		(new int[1])[0] = 1;
		return 0;
	}

	@MethodReturnsConstant
	public static int write_and_read() {
		final int[] a = new int[3];
		a[0] = 1;
		return a[2];
	}

	@MethodReturnsConstant
	public static int multiple_reads() {
		final int[] a = new int[2];
		return a[0] + a[1];
	}

	@MethodReturnsConstant
	public static int multiple_writes() {
		int[] a;
		a = new int[1];
		a[0] = 1;
		a[0] = 2;
		return a[0];
	}

	@MethodReturnsConstant
	public static int two_references() {
		final int[] a = new int[1];
		final int[] b = a;
		a[0] = 1;
		return b[0];
	}

	@MethodReturnsConstant
	public static boolean self_reference() {
		final Object[][] a = new Object[1][];
		a[0] = a;
		return a[0][0] == a[0];
	}

	@MethodReturnsConstant
	public static int long_initializer_list() {
		return new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }[2];
	}

}