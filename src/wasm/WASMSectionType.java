package wasm;

enum WASMSectionType {
	TYPE(1),

	IMPORT(2),

	FUNCTION(3),

	TABLE(4),

	MEMORY(5),

	GLOBAL(6),

	EXPORT(7),

	START(8),

	ELEMENT(9),

	CODE(10),

	DATA(11),

	;

	final int id;

	private WASMSectionType(final int id) {
		this.id = id;
	}

}