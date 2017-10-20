package x86_64;

public enum SectionHeaderType {

	SHT_NULL(0),

	SHT_PROGBITS(1),

	SHT_SYMTAB(2),

	SHT_STRTAB(3),

	SHT_RELA(4),

	SHT_HASH(5),

	SHT_DYNAMIC(6),

	SHT_NOTE(7),

	SHT_NOBITS(8),

	SHT_REL(9),

	SHT_SHLIB(10),

	SHT_DYNSYM(11),

	SHT_INIT_ARRAY(14),

	SHT_FINI_ARRAY(15),

	SHT_PREINIT_ARRAY(16),

	SHT_GROUP(17),

	SHT_SYMTAB_SHNDX(18),

	SHT_NUM(19),

	SHT_LOOS(0x60000000),

	SHT_GNU_HASH(0x6ffffff6),

	SHT_GNU_verdef(0x6ffffffd),

	SHT_GNU_verneed(0x6ffffffe),

	SHT_GNU_versym(0x6fffffff),

	SHT_LOPROC(0x70000000),

	SHT_HIPROC(0x7fffffff),

	SHT_LOUSER(0x80000000),

	SHT_HIUSER(0xffffffff),

	SHT_AMD64_UNWIND(0x70000001),

	;

	private int id;

	private SectionHeaderType(final int id) {
		this.id = id;
	}

	public static SectionHeaderType fromId(final int id) {
		for (final SectionHeaderType type : values()) {
			if (type.id == id) {
				return type;
			}
		}
		throw new IllegalArgumentException(String.format("Unknown section header type: 0x%08x", id));
	}

}
