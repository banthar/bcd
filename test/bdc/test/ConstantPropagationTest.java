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
	public static int remove_local_used_in_dead_right_branch() {
		int a;
		int b;
		int c;
		a = 0;
		b = 1;
		c = 1;
		return a == 0 ? a : b;
	}

	@MethodReturnsConstant
	public static int remove_multiple_branches() {
		int a;
		int b;
		int c;
		a = 0;
		b = 1;
		c = 2;
		return a == 0 ? b == 1 ? c == 1 ? a : b == 2 ? b : c : a : b;
	}

	@MethodReturnsConstant
	public static int remove_local_used_in_dead_left_branch() {
		int a;
		int b;
		a = 0;
		b = 1;
		return a == 1 ? a : b;
	}

	@MethodReturnsConstant
	public static boolean remove_eq_else_block() {
		int a;
		a = 0;
		if (a == 0) {
			return true;
		} else {
			return false;
		}
	}

	@MethodReturnsConstant
	public static boolean remove_eq_then_block() {
		int a;
		a = 1;
		if (a == 0) {
			return true;
		} else {
			return false;
		}
	}

	@MethodReturnsConstant
	public static boolean remove_ne_else_block() {
		int a;
		a = 1;
		if (a != 0) {
			return true;
		} else {
			return false;
		}
	}

	@MethodReturnsConstant
	public static boolean remove_ne_then_block() {
		int a;
		a = 0;
		if (a != 0) {
			return true;
		} else {
			return false;
		}
	}

	@MethodReturnsConstant
	public static int remove_table_switch_take_first_branch() {
		int a;
		a = 0;
		switch (a) {
		case 0:
			return 1000;
		case 1:
			return 2000;
		}
		return 3000;
	}

	@MethodReturnsConstant
	public static int remove_table_switch_take_second_branch() {
		int a;
		a = 1;
		switch (a) {
		case 0:
			return 1000;
		case 1:
			return 2000;
		}
		return 3000;
	}

	@MethodReturnsConstant
	public static int remove_table_switch_take_default_branch() {
		int a;
		a = 2;
		switch (a) {
		case 0:
			return 1000;
		case 1:
			return 2000;
		}
		return 3000;
	}

	@MethodReturnsConstant
	public static int remove_table_switch_take_common_branch() {
		int a;
		a = 0;
		switch (a) {
		case 0:
		case 1:
			return 1;
		}
		return 3;
	}

	@MethodReturnsConstant
	public static int remove_lookup_switch_take_first_branch() {
		int a;
		a = 1000;
		switch (a) {
		case 1000:
			return 1;
		case 2000:
			return 2;
		}
		return 3;
	}

	@MethodReturnsConstant
	public static int remove_lookup_switch_take_second_branch() {
		int a;
		a = 2000;
		switch (a) {
		case 1000:
			return 1;
		case 2000:
			return 2;
		}
		return 3;
	}

	@MethodReturnsConstant
	public static int remove_lookup_switch_take_default_branch() {
		int a;
		a = 3000;
		switch (a) {
		case 1000:
			return 1;
		case 2000:
			return 2;
		}
		return 3;
	}

	@MethodReturnsConstant
	public static int remove_lookup_switch_take_common_branch() {
		int a;
		a = 1000;
		switch (a) {
		case 2000:
		case 1000:
			return 1;
		}
		return 3;
	}
}
