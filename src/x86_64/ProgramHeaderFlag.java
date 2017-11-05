package x86_64;

import java.util.EnumSet;
import java.util.Set;

public enum ProgramHeaderFlag {
	EXECUTE(0x1),

	WRITE(0x2),

	READ(0x4),

	;

	int id;

	ProgramHeaderFlag(final int id) {
		this.id = id;
	}

	public static Set<ProgramHeaderFlag> fromMask(final int p_flags) {
		final EnumSet<ProgramHeaderFlag> set = EnumSet.noneOf(ProgramHeaderFlag.class);
		for (final ProgramHeaderFlag flag : values()) {
			if ((flag.id & p_flags) != 0) {
				set.add(flag);
			}
		}
		return set;
	}

}
