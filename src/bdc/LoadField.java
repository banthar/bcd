package bdc;

import java.util.Map;

import bdc.ConstantPool.FieldReference;

public class LoadField extends PureOperation {

	public LoadField(final FieldReference field) {
		super(field.getType());
	}

	@Override
	protected Value computeSingleOutput(final Map<PortId, ? extends Value> values) {
		final Value source = values.get(PortId.arg(0));
		if (source.isConstant()) {
			throw new IllegalStateException();
		} else {
			return Value.unknown(getType());
		}
	}

}
