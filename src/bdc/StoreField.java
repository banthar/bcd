package bdc;

import java.util.Map;

import bdc.ConstantPool.FieldReference;

public class StoreField extends PureOperation {

	public StoreField(final FieldReference field) {
		super(field.getTarget().getType());
	}

	@Override
	protected Value computeSingleOutput(final Map<PortId, ? extends Value> values) {
		final Value source = values.get(PortId.arg(0));
		final Value value = values.get(PortId.arg(1));
		if (source.isConstant() && value.isConstant()) {
			throw new IllegalStateException();
		} else {
			return Value.unknown(getType());
		}
	}
}
