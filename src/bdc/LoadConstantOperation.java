package bdc;

import java.util.Arrays;
import java.util.Map;

import bdc.Type.FieldType;

public class LoadConstantOperation extends PureOperation {

	private final Object value;

	public LoadConstantOperation(final FieldType type, final Object value) {
		super(type);
		this.value = value;
	}

	public Object getValue() {
		return this.value;
	}

	@Override
	public String toString() {
		return "LoadConstantOperation(" + getType() + ", " + Arrays.deepToString(new Object[] { this.value }) + ")";
	}

	public Value toValue() {
		return Value.of(getType(), this.value);
	}

	@Override
	public Value getValue(final Node node, final PortId portId) {
		if (portId.equals(PortId.arg(0))) {
			return toValue();
		} else {
			throw new IllegalStateException();
		}
	}

	@Override
	protected Value computeSingleOutput(final Map<PortId, ? extends Value> values) {
		if (!values.isEmpty()) {
			throw new IllegalStateException("Expected no arguments: " + values);
		}
		return toValue();
	}

}
