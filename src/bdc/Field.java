package bdc;

public class Field {

	private final int accessFlags;
	private final String name;
	private final String descriptor;
	private final String signature;

	public Field(final int accessFlags, final String name, final String descriptor, final String signature) {
		this.accessFlags = accessFlags;
		this.name = name;
		this.descriptor = descriptor;
		this.signature = signature;
	}

	@Override
	public String toString() {
		return "Field [accessFlags=" + this.accessFlags + ", name=" + this.name + ", descriptor=" + this.descriptor
				+ ", signature=" + this.signature + "]";
	}

}
