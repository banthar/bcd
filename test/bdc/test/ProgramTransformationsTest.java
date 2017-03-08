package bdc.test;

import org.junit.runner.RunWith;

@RunWith(BdcRunner.class)
public class ProgramTransformationsTest {

	private static int zero() {
		return 0;
	}

	@MethodReturnsConstant
	public static int inline_constant_function() {
		return zero();
	}

	private static int zero(final int a) {
		return 0;
	}

	@MethodReturnsConstant
	public static int inline_constant_function_with_argument() {
		return zero(0);
	}

	@MethodReturnsConstant
	public static int inline_multiple_constant_functions() {
		return zero() + zero();
	}

	private static int identity(final int a) {
		return a;
	}

	private static int identity2(final int a) {
		return identity(a);
	}

	@MethodReturnsConstant
	public static int inline_identity() {
		return identity(0);
	}

	@MethodReturnsConstant
	public static int inline_nested_identity() {
		return identity2(0);
	}

	private static int max(final int a, final int b) {
		return a > b ? a : b;
	}

	@MethodReturnsConstant
	public static int inline_pure_function_with_branch() {
		return max(0, 1);
	}

	private static int switch_over_arg(final int a) {
		switch (a) {
		case 0:
			return 1;
		case 1:
			return 2;
		default:
			return 3;
		}
	}

	@MethodReturnsConstant
	public static int inline_pure_function_with_switch() {
		return switch_over_arg(0);
	}

	private static int constant_switch(final int a) {
		switch (a) {
		case 0:
			return 0;
		case 1:
			return 0;
		default:
			return 0;
		}
	}

	@MethodReturnsConstant
	public static int inline_constant_function_with_switch() {
		return constant_switch(0);
	}

	private static int constant_branch(final int a) {
		if (a > 0) {
			return 0;
		} else {
			return 0;
		}
	}

	@MethodReturnsConstant
	public static int inline_constant_function_with_branch() {
		return constant_branch(0);
	}

}
