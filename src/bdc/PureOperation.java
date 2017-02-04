package bdc;

import java.util.List;

import bdc.BasicBlockBuilder.BinaryOperationType;
import bdc.BasicBlockBuilder.BitwiseOperationType;
import bdc.BasicBlockBuilder.ShiftType;
import bdc.Type.PrimitiveType;

public class PureOperation {

	private final Type type;
	private final int inputPorts;
	private final Object operation;

	public PureOperation(final Type type, final int inputPorts, final Object operation) {
		this.type = type;
		this.inputPorts = inputPorts;
		this.operation = operation;
	}

	public PureOperation(final Type type, final int inputPorts) {
		this(type, inputPorts, null);
	}

	public int getInputPorts() {
		return this.inputPorts;
	}

	public Type getType() {
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

	public Object compute(final List<Object> values) {
		if (values.size() != this.inputPorts) {
			throw new IllegalStateException();
		}
		if (this.operation instanceof BinaryOperationType) {
			switch ((BinaryOperationType) this.operation) {
			case Add:
				return (Integer) values.get(0) + (Integer) values.get(1);
			case Subtract:
				return (Integer) values.get(0) - (Integer) values.get(1);
			case Multiply:
				return (Integer) values.get(0) * (Integer) values.get(1);
			case Divide:
				return (Integer) values.get(0) / (Integer) values.get(1);
			}
		}
		throw new IllegalStateException("Invalid operation: " + this.operation);
	}

	public Type getReturnType() {
		// TODO
		return getType();
	}
}
