package bdc.test;

public interface Test {

	static int deep_branch_tree(final int a, final int b) {
		return a * 1 > b * 1 ? a * 1 > b * 2 ? a * 1 > b * 4 ? 0 : 1 : a * 3 > b * 4 ? 2 : 3
				: a * 2 > b * 1 ? a * 4 > b * 3 ? 4 : 5 : a * 4 > b * 1 ? 6 : 7;
	}

	static int pass_stack_values_across_multiple_blocks(final int a, final int b) {
		return (a + 1) * (b - 1) * (a >= b ? a == b ? 0 : 1 : -1);
	}

	static int pass_multiple_stack_values_across_block(final int a, final int b) {
		return a * (1 - a * (1 - a * (1 - a * (1 - a * (1 - a * (1 - a * (1 - a * (b > a ? a * 2 : b))))))));
	}

	static int g(final Number x) {
		if (x instanceof Integer) {
			return (Integer) x;
		} else {
			throw new IllegalStateException("Invalid number " + x);
		}
	}

	static void main(final String args[]) {
		for (int a = 0; a < 20; a++) {
			for (int b = 0; b < 20; b++) {
				System.out.print(pass_multiple_stack_values_across_block(a, b) + (b == 19 ? "\n" : " "));
			}
		}
	}
}
