package bdc;

import java.util.Collections;
import java.util.Map;

import bdc.Type.FieldType;

public abstract class PureOperation implements NodeOperation {
	private final FieldType type;

	public PureOperation(final FieldType type) {
		this.type = type;
	}

	public FieldType getType() {
		return this.type;
	}

	@Override
	public Value getValue(final Node node, final PortId portId) {
		return Value.unknown(getType());
	}

	public final Map<PortId, ? extends Value> compute(final Map<PortId, ? extends Value> values) {
		return Collections.singletonMap(PortId.arg(0), computeSingleOutput(values));
	}

	protected abstract Value computeSingleOutput(final Map<PortId, ? extends Value> values);
}
