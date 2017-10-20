package x86_64;

public enum ProgramHeaderType {

	PT_NULL(0),

	PT_LOAD(1),

	PT_DYNAMIC(2),

	PT_INTERP(3),

	PT_NOTE(4),

	PT_SHLIB(5),

	PT_PHDR(6),

	PT_TLS(7),

	PT_LOPROC(0x70000000),

	PT_HIPROC(0x7fffffff),

	PT_GNU_EH_FRAME(0x6474e550),

	PT_GNU_STACK(0x6474e551),

	PT_GNU_RELRO(0x6474e552),

	;

	private int id;

	private ProgramHeaderType(final int id) {
		this.id = id;
	}

	public static ProgramHeaderType fromId(final int id) {
		for (final ProgramHeaderType type : values()) {
			if (type.id == id) {
				return type;
			}
		}
		throw new IllegalArgumentException(String.format("Unknown program header type: %08x", id));
	}

}
