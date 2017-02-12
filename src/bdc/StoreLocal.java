package bdc;

public class StoreLocal implements NodeOperation {

	private final int index;

	public StoreLocal(final int index) {
		this.index = index;
	}

	public int getIndex() {
		return this.index;
	}

	@Override
	public String toString() {
		return "StoreLocal(" + this.index + ")";
	}

}
