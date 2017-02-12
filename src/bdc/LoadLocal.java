package bdc;

public class LoadLocal implements NodeOperation {

	private final int index;

	public LoadLocal(final int index) {
		this.index = index;
	}

	public int getIndex() {
		return this.index;
	}

	@Override
	public String toString() {
		return "LoadLocal(" + this.index + ")";
	}

}
