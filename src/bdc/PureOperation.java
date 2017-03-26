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

	public final Map<PortId, ? extends Value> compute(final Map<PortId, ? extends Value> values) {
		return Collections.singletonMap(PortId.arg(0), computeSingleOutput(values));
	}

	protected abstract Value computeSingleOutput(final Map<PortId, ? extends Value> values);
}
