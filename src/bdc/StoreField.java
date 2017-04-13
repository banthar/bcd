package bdc;

import java.util.Map;

public class StoreField extends PureOperation {

	public StoreField(final Field field) {
		super(field.getTarget().getType());
	}

	@Override
	protected Value computeSingleOutput(final Map<PortId, ? extends Value> values) {
		final ValueObject target = (ValueObject) values.get(PortId.arg(0));
		final Value value = values.get(PortId.arg(1));
		if (target.isConstant() && value.isConstant()) {
			System.out.println(target.getConstant());
			throw new IllegalStateException();
		} else {
			return Value.unknown(getType());
		}
	}
}
