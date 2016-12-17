package bdc;

import java.util.HashMap;
import java.util.Map;

public enum ConstantType {
	Class(7), Fieldref(9), Methodref(10), InterfaceMethodref(11), String(8), Integer(3), Float(4), Long(5), Double(
			6), NameAndType(12), Utf8(1), MethodHandle(15), MethodType(16), InvokeDynamic(18);

	private static final Map<Integer, ConstantType> TYPES = createMap();

	private int id;

	private ConstantType(final int id) {
		this.id = id;
	}

	private static Map<java.lang.Integer, ConstantType> createMap() {
		final HashMap<java.lang.Integer, ConstantType> map = new HashMap<>();
		for (final ConstantType t : values()) {
			map.put(t.id, t);
		}
		return map;
	}

	public static ConstantType fromId(final int id) {
		final ConstantType type = TYPES.get(id);
		if (type == null) {
			throw new IllegalStateException("Unknown constant id: " + id);
		} else {
			return type;
		}
	}
}
