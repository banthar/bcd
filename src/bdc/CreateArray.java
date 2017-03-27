package bdc;

import java.util.Map;

import bdc.Type.FieldType;

public class CreateArray extends PureOperation {

	private final FieldType elementType;

	public CreateArray(final FieldType elementType) {
		super(Type.array(elementType));
		this.elementType = elementType;
	}

	@Override
	protected ValueArray computeSingleOutput(final Map<PortId, ? extends Value> values) {
		return Value.array(this.elementType, values.get(PortId.arg(0)));
	}

}
