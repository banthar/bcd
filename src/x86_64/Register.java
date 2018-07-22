package x86_64;

enum Register {
	EAX(0),

	ECX(1),

	EDX(2),

	EBX(3),

	SIB(4),

	IP(5),

	ESI(6),

	EDI(7),

	R8(8),

	R9(9),

	R10(10),

	R11(11),

	SIB2(12),

	IP2(13),

	R14(14),

	R15(15),

	ENV(-1),

	;

	int index;

	private Register(final int index) {
		this.index = index;
	}

	public int getIndex() {
		return this.index;
	}
}