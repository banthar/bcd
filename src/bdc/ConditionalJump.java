package bdc;

import bdc.BasicBlockBuilder.CompareType;
import bdc.Type.PrimitiveType;

public class ConditionalJump extends Jump {

	private final PrimitiveType type;
	private final CompareType compareType;

	public ConditionalJump(final PrimitiveType type, final CompareType compareType) {
		this.type = type;
		this.compareType = compareType;
	}

	public int compute(final Object left, final Object right) {
		if (this.type == PrimitiveType.Integer) {
			return compareIntegers((Integer) left, (Integer) right) ? 0 : 1;
		} else {
			throw new IllegalStateException("Unsupported primitive type: " + this.type);
		}
	}

	private boolean compareIntegers(final int left, final int right) {
		switch (this.compareType) {
		case EQ:
			return left == right;
		case GE:
			return left >= right;
		case GT:
			return left > right;
		case LE:
			return left <= right;
		case LT:
			return left < right;
		case NE:
			return left != right;
		}
		throw new IllegalStateException("Unknown compare type: " + this.compareType);
	}

}
