package bdc;

import java.util.Map;

import bdc.ConstantPool.FieldReference;

public class LoadField extends PureOperation {

	private final FieldReference field;

	public LoadField(final FieldReference field) {
		super(field.getType());
		this.field = field;
	}

	@Override
	protected Value computeSingleOutput(final Map<PortId, ? extends Value> values) {
		final Value source = values.get(PortId.arg(0));
		if (source.isConstant()) {
			return source.as(ValueObject.class).getConstant().get(this.field);
		} else {
			return Value.unknown(getType());
		}
	}

}
