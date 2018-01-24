package bdc;

public enum BinaryOperationType implements PureTransformationType {
	Add("+"), Subtract("-"), Multiply("*"), Divide("/"), Remainder("%");
	private final String symbol;

	private BinaryOperationType(final String symbol) {
		this.symbol = symbol;
	}

	public String getSymbol() {
		return this.symbol;
	}

	public static BinaryOperationType fromId(final int id) {
		return values()[id];
	}
}