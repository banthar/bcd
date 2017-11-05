package x86_64;

import java.nio.ByteBuffer;
import java.util.Set;

public class ProgramHeader {
	final ProgramHeaderType p_type;
	final Set<ProgramHeaderFlag> p_flags;
	final long p_offset;
	final long p_vaddr;
	final long p_paddr;
	final long p_filesz;
	final long p_memsz;
	final long p_align;

	public ProgramHeader(final ByteBuffer input, final boolean is64) {
		this.p_type = ProgramHeaderType.fromId(input.getInt());
		int flags = 0;
		if (is64) {
			flags = input.getInt();
		}
		this.p_offset = Elf.readPointer(input, is64);
		this.p_vaddr = Elf.readPointer(input, is64);
		this.p_paddr = Elf.readPointer(input, is64);
		this.p_filesz = Elf.readPointer(input, is64);
		this.p_memsz = Elf.readPointer(input, is64);
		if (!is64) {
			flags = input.getInt();
		}
		this.p_flags = ProgramHeaderFlag.fromMask(flags);
		this.p_align = Elf.readPointer(input, is64);

	}

	@Override
	public String toString() {
		final StringBuilder out = new StringBuilder();
		out.append(String.format("%s:\n", this.p_type));
		out.append(String.format("\tp_flags: %s\n", this.p_flags));
		out.append(String.format("\tp_offset: %08x\n", this.p_offset));
		out.append(String.format("\tp_vaddr: %08x\n", this.p_vaddr));
		out.append(String.format("\tp_paddr: %08x\n", this.p_paddr));
		out.append(String.format("\tp_filesz: %08x\n", this.p_filesz));
		out.append(String.format("\tp_memsz: %08x\n", this.p_memsz));
		out.append(String.format("\tp_align: %08x\n", this.p_align));
		return out.toString();
	}

}
