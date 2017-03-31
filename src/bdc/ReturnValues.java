package bdc;

import bdc.Type.PrimitiveType;

public class ReturnValues extends FunctionTerminator {
	private final PrimitiveType type;

	public ReturnValues(final PrimitiveType type) {
		this.type = type;
	}

	public ReturnValues() {
		this.type = null;
	}

	@Override
	public String toString() {
		if (this.type != null) {
			return "return " + this.type;
		} else {
			return "return";
		}
	}
}
