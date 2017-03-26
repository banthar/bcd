package bdc;

import java.util.Map;

import bdc.BasicBlockBuilder.BinaryOperationType;
import bdc.BasicBlockBuilder.BitwiseOperationType;
import bdc.BasicBlockBuilder.ShiftType;
import bdc.PureTransformationType.Compare;
import bdc.PureTransformationType.Negate;
import bdc.Type.FieldType;
import bdc.Type.PrimitiveType;

public class PureTransformation extends PureOperation {

	private final int inputPorts;
	private final Object operation;

	public PureTransformation(final FieldType type, final int inputPorts, final PureTransformationType operation) {
		super(type);

		assert inputPorts >= 0;
		assert operation != null;

		this.inputPorts = inputPorts;
		this.operation = operation;
	}

	public int getInputPorts() {
		return this.inputPorts;
	}

	public static PureTransformation binaryOperation(final PrimitiveType type, final BinaryOperationType op) {
		return new PureTransformation(type, 2, op);
	}

	public static PureTransformation negate(final PrimitiveType type) {
		return new PureTransformation(type, 1, PureTransformationType.Negate.NEGATE);
	}

	public static PureTransformation shift(final PrimitiveType type, final ShiftType shiftType) {
		return new PureTransformation(type, 2, shiftType);
	}

	public static PureTransformation bitwiseOperation(final PrimitiveType type, final BitwiseOperationType operation) {
		return new PureTransformation(type, 2, operation);
	}

	public static PureTransformation convert(final PrimitiveType from, final PrimitiveType to) {
		return new PureTransformation(from, 1, PureTransformationType.Convert.fromTargetType(to));
	}

	public static PureTransformation compare(final PrimitiveType type) {
		return new PureTransformation(type, 2, PureTransformationType.Compare.COMPARE);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + this.getType() + ", " + this.inputPorts + ", " + this.operation + ")";
	}

	@Override
	protected Value computeSingleOutput(final Map<PortId, ? extends Value> values) {
		if (values.size() != this.inputPorts) {
			throw new IllegalStateException();
		}
		if (this.operation instanceof BinaryOperationType) {
			final Value value0 = values.get(PortId.arg(0));
			final Value value1 = values.get(PortId.arg(1));
			if (value0.isConstant() && value1.isConstant()) {
				final Object arg0 = value0.getConstant();
				final Object arg1 = value1.getConstant();
				switch ((BinaryOperationType) this.operation) {
				case Add:
					return Value.integer((Integer) arg0 + (Integer) arg1);
				case Subtract:
					return Value.integer((Integer) arg0 - (Integer) arg1);
				case Multiply:
					return Value.integer((Integer) arg0 * (Integer) arg1);
				case Divide:
					return Value.integer((Integer) arg0 / (Integer) arg1);
				}
			} else if (value0.isConstant()) {
				final Object arg0 = value0.getConstant();
				if ((Integer) arg0 == 0 && this.operation == BinaryOperationType.Multiply) {
					return Value.integer(0);
				} else {
					return Value.unknown();
				}
			} else if (value1.isConstant()) {
				final Object arg1 = value1.getConstant();
				if ((Integer) arg1 == 0 && this.operation == BinaryOperationType.Multiply) {
					return Value.integer(0);
				} else {
					return Value.unknown();
				}
			} else {
				return Value.unknown();
			}
		} else if (this.operation instanceof Negate) {
			final Value value0 = values.get(PortId.arg(0));
			if (value0.isConstant()) {
				return Value.integer(-(Integer) value0.getConstant());
			} else {
				return Value.unknown();
			}
		} else if (this.operation instanceof Compare) {
			final Value value0 = values.get(PortId.arg(0));
			final Value value1 = values.get(PortId.arg(1));
			if (value0.isConstant() && value1.isConstant()) {
				final float arg0 = (Float) value0.getConstant();
				final float arg1 = (Float) value1.getConstant();
				return Value.integer(Float.compare(arg0, arg1));
			} else {
				return Value.unknown();
			}
		}
		throw new IllegalStateException("Invalid operation: " + this.operation);
	}

	public FieldType getReturnType() {
		// TODO
		return getType();
	}
}
