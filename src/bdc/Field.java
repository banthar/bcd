package bdc;

import bdc.Type.FieldType;

public class Field {

	private final int accessFlags;
	private final String name;
	private final FieldType type;

	public Field(final int accessFlags, final String name, final String descriptor, final String signature)
			throws ClassFormatException {
		this.accessFlags = accessFlags;
		this.name = name;
		this.type = (FieldType) Type.fromDescriptor(signature != null ? signature : descriptor);
	}

	@Override
	public String toString() {
		return "Field [accessFlags=" + this.accessFlags + ", name=" + this.name + ", type=" + this.type + "]";
	}

	public FieldType getType() {
		return this.type;
	}

}
