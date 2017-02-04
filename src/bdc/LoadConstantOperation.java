package bdc;

public class LoadConstantOperation extends PureOperation {

	private final Object value;

	public LoadConstantOperation(final Type type, final Object value) {
		super(type, 0);
		this.value = value;
	}

	public Object getValue() {
		return this.value;
	}

	@Override
	public String toString() {
		return "LoadConstantOperation(" + getType() + ", " + this.value + ")";
	}

}
