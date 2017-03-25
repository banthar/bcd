package bdc;

import java.util.Map;

import bdc.BasicBlockBuilder.BinaryOperationType;
import bdc.BasicBlockBuilder.BitwiseOperationType;
import bdc.BasicBlockBuilder.ShiftType;
import bdc.Type.FieldType;
import bdc.Type.PrimitiveType;

public class PureOperation implements NodeOperation {

	private final FieldType type;
	private final int inputPorts;
	private final Object operation;

	public PureOperation(final FieldType type, final int inputPorts, final Object operation) {
		this.type = type;
		this.inputPorts = inputPorts;
		this.operation = operation;
	}

	public PureOperation(final FieldType type, final int inputPorts) {
		this(type, inputPorts, null);
	}

	public int getInputPorts() {
		return this.inputPorts;
	}

	public FieldType getType() {
		return this.type;
	}

	public static PureOperation binaryOperation(final PrimitiveType type, final BinaryOperationType op) {
		return new PureOperation(type, 2, op);
	}

	public static PureOperation negate(final PrimitiveType type) {
		return new PureOperation(type, 1);
	}

	public static PureOperation shift(final PrimitiveType type, final ShiftType shiftType) {
		return new PureOperation(type, 2, shiftType);
	}

	public static PureOperation bitwiseOperation(final PrimitiveType type, final BitwiseOperationType operation) {
		return new PureOperation(type, 2, operation);
	}

	public static PureOperation convert(final PrimitiveType from, final PrimitiveType to) {
		return new PureOperation(from, 1, to);
	}

	public static PureOperation compare(final PrimitiveType type) {
		return new PureOperation(type, 2);
	}

	@Override
	public String toString() {
		return "PureOperation(" + this.type + ", " + this.inputPorts + ", " + this.operation + ")";
	}

	public Value compute(final Map<PortId, ? extends Value> values) {
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
		}
		throw new IllegalStateException("Invalid operation: " + this.operation);
	}

	public FieldType getReturnType() {
		// TODO
		return getType();
	}
}
