package bdc;

import java.util.Map;

import bdc.ConstantPool.FieldReference;

public class StoreField extends PureOperation {

	private final FieldReference field;

	public StoreField(final FieldReference field) {
		super(field.getTarget().getType());
		this.field = field;
	}

	@Override
	protected Value computeSingleOutput(final Map<PortId, ? extends Value> values) {
		final Value target = values.get(PortId.arg(0));
		final Value value = values.get(PortId.arg(1));
		if (target.isConstant() && value.isConstant()) {
			return ((ValueObject) target).put(this.field, value);
		} else {
			return Value.unknown(getType());
		}
	}
}
