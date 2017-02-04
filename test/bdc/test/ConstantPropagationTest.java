package bdc.test;

import org.junit.runner.RunWith;

@RunWith(BdcRunner.class)
public class ConstantPropagationTest {

	@MethodReturnsConstant
	public static int return_constant_zero() {
		return 0;
	}

	@MethodReturnsConstant
	public static int return_constant_one() {
		return 1;
	}

	@MethodReturnsConstant
	public static int return_max_int() {
		return Integer.MAX_VALUE;
	}

	@MethodReturnsConstant
	public static int return_min_int() {
		return Integer.MIN_VALUE;
	}

	@MethodReturnsConstant
	public static int return_max_plus_min() {
		int a;
		a = Integer.MAX_VALUE;
		a += Integer.MIN_VALUE;
		return a;
	}

	@MethodReturnsConstant
	public static int return_zero_plus_one() {
		int a;
		int b;
		a = 0;
		b = 1;
		return a + b;
	}

	@MethodReturnsConstant
	public static int return_two_minus_one() {
		int a;
		int b;
		a = 2;
		b = 1;
		return a - b;
	}

	@MethodReturnsConstant
	public static int return_two_times_three() {
		int a;
		int b;
		a = 2;
		b = 3;
		return a * b;
	}

	@MethodReturnsConstant
	public static int return_5_divided_by_two() {
		int a;
		int b;
		a = 5;
		b = 2;
		return a / b;
	}

	@MethodReturnsConstant
	public static int return_mixed_operations() {
		int a;
		int b;
		a = 5;
		b = 2;
		a += b;
		b *= a;
		a -= b;
		b /= a;
		b += 1;
		a -= 2;
		a *= b;
		b *= b;
		a += a + b + a + 4;
		return a;
	}

	@MethodReturnsConstant
	public static int return_min_value_divided_by_minus_one() {
		return Integer.MIN_VALUE / -1;
	}

	@MethodReturnsConstant
	public static boolean return_true() {
		return true;
	}

	@MethodReturnsConstant
	public static boolean return_false() {
		return false;
	}

	@MethodReturnsConstant
	public static boolean simplify_comparison() {
		return 1 > 0;
	}
}
