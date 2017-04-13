package bdc;

public class Field {

	private final int accessFlags;
	private final String name;
	private final Type type;
	private final Type signature;

	public Field(final int accessFlags, final String name, final String descriptor, final String signature)
			throws ClassFormatException {
		this.accessFlags = accessFlags;
		this.name = name;
		this.type = Type.fromDescriptor(descriptor);
		this.signature = Type.fromSignature(signature);
	}

	@Override
	public String toString() {
		return "Field [accessFlags=" + this.accessFlags + ", name=" + this.name + ", type=" + this.type + ", signature="
				+ this.signature + "]";
	}

}
