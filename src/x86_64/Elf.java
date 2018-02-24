package x86_64;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.StandardOpenOption;

import x86_64.MemoryView.InvalidAddressException;

public class Elf {

	public void write(final OutputStream out, final boolean is64, final boolean isLittleEndian, final byte[] code)
			throws IOException {

		final long baseAddress = 0x0000000000400000;

		final ByteBuffer programHeader = ByteBuffer.allocate(is64 ? 56 : 32);
		final ByteBuffer fileHeader = ByteBuffer.allocate(is64 ? 64 : 52);
		final long codeOffset = programHeader.capacity() + fileHeader.capacity();

		programHeader.order(isLittleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
		programHeader.putInt(1); // p_type
		programHeader.putInt(1); // p_flags
		programHeader.putLong(0); // p_offset
		programHeader.putLong(baseAddress); // p_vaddr
		programHeader.putLong(baseAddress); // p_paddr
		programHeader.putLong(code.length + codeOffset); // p_filesz
		programHeader.putLong(code.length + codeOffset); // p_memsz
		programHeader.putLong(0); // p_align

		fileHeader.put(new byte[] { 0x7f, 'E', 'L', 'F' }); // EI_MAG
		fileHeader.put((byte) (is64 ? 2 : 1)); // EI_CLASS
		fileHeader.put((byte) (isLittleEndian ? 1 : 2));
		fileHeader.put((byte) 1); // EI_VERSION
		fileHeader.put((byte) 0); // EI_OSABI
		fileHeader.put((byte) 0); // EI_ABIVERSION
		fileHeader.put(new byte[] { 0, 0, 0, 0, 0, 0, 0 }); // EI_PAD
		fileHeader.order(isLittleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
		fileHeader.putShort((byte) 2); // e_type = executable
		fileHeader.putShort((short) (is64 ? 0x003E : 0x0003));
		fileHeader.putInt(1); // e_version = 1
		writePointer(fileHeader, is64, baseAddress + programHeader.capacity() + fileHeader.capacity()); // e_entry
		writePointer(fileHeader, is64, fileHeader.capacity()); // e_phoff
		writePointer(fileHeader, is64, 0); // e_shoff
		fileHeader.putInt(0); // e_flags
		fileHeader.putShort((short) fileHeader.capacity()); // e_hsize
		fileHeader.putShort((short) programHeader.capacity()); // e_phentsize
		fileHeader.putShort((short) 1); // e_phnum
		fileHeader.putShort((short) 0); // e_shentsize
		fileHeader.putShort((short) 0); // e_shnum
		fileHeader.putShort((short) 0); // e_shstrndx

		out.write(fileHeader.array());
		out.write(programHeader.array());
		out.write(code);
	}

	private void writePointer(final ByteBuffer buf, final boolean is64, final long l) {
		if (is64) {
			buf.putLong(l);
		} else {
			if ((int) l != l) {
				throw new IllegalStateException();
			}
			buf.putInt((int) l);
		}
	}

	private static void read(final MappedByteBuffer input) {
		final int magic = input.getInt();
		if (magic != 0x7f454c46) {
			throw new IllegalStateException(String.format("Invalid magic number: 0x%08x", magic));
		}
		final boolean is64 = readBoolean(input);
		final boolean isBigEndian = readBoolean(input);
		final int version = input.get();
		final int osabi = input.get();
		final int abiversion = input.get();
		for (int i = 0; i < 7; i++) {
			if (input.get() != 0) {
				throw new IllegalStateException();
			}
		}
		input.order(isBigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
		final int type = input.getShort();
		final int machine = input.getShort();
		final int e_version = input.getInt();

		final long entry = readPointer(input, is64);
		final long programHeaderOffset = readPointer(input, is64);
		final long sectionHeaderOffset = readPointer(input, is64);

		final int flags = input.getInt();
		final int headerSize = input.getShort();
		final int programHeaderSize = input.getShort();
		final int programHeaderCount = input.getShort();

		final int sectionHeaderSize = input.getShort();
		final int sectionHeaderCount = input.getShort();
		final int sectionHeaderNamesIndex = input.getShort();

		// System.out.println("is64: " + is64);
		// System.out.println("isBigEndian: " + isBigEndian);
		// System.out.println("version: " + version);
		// System.out.println("osabi: " + osabi);
		// System.out.println("abiversion: " + abiversion);
		// System.out.println("type: " + type);
		// System.out.println("d1: " + machine);
		// System.out.println("d2: " + e_version);
		//
		// System.out.format("entry: 0x%08x\n", entry);
		// System.out.format("programHeaderOffset: 0x%08x\n", programHeaderOffset);
		// System.out.format("sectionHeaderOffset: 0x%08x\n", sectionHeaderOffset);
		//
		// System.out.format("flags: 0x%08x\n", flags);
		// System.out.format("headerSize: %d\n", headerSize);
		// System.out.format("programHeaderEntsize: %d\n", programHeaderSize);
		// System.out.format("programHeaderNum: %d\n", programHeaderCount);
		// System.out.format("sectionHeaderSize: %d\n", sectionHeaderSize);
		// System.out.format("sectionHeaderCount: %d\n", sectionHeaderCount);
		// System.out.format("sectionHeaderNamesIndex: %d\n", sectionHeaderNamesIndex);

		MemoryView memory = MemoryView.empty();

		for (int i = 0; i < programHeaderCount; i++) {
			input.position((int) programHeaderOffset + i * programHeaderSize);
			final ProgramHeader programHeader = new ProgramHeader(input, is64);

			if (programHeader.p_type == ProgramHeaderType.PT_LOAD) {
				memory = memory.map(programHeader.p_vaddr, programHeader.p_memsz, programHeader.p_flags, input,
						programHeader.p_offset, programHeader.p_filesz);
			}
		}

		for (int i = 0; i < sectionHeaderCount; i++) {
			input.position((int) sectionHeaderOffset + i * sectionHeaderSize);
			// System.out.println("section name: " + input.getInt());
			// System.out.println(SectionHeaderType.fromId(input.getInt()));
		}

		System.out.println(disassemble(memory, entry));
	}

	private static String getRegisterName(final int n) {
		return "r" + n + "(" + new String[] { "ax", "cx", "dx", "bx", "[sib]", "ip", "si", "di", "r8", "r9", "r10",
				"r11", "[sib]", "ip2", "r14", "r15" }[n] + ")";
	}

	private static String disassemble(final MemoryView memory, final long entry) {
		String s = "";
		final ByteStream input = memory.createStream(entry);
		while (true) {
			final long startPosition = input.getPosition();

			if (startPosition > entry + 1024) {
				return s;
			}

			String line = "";
			try {
				int prefix = input.getUnsignedByte();
				final boolean rex_w;
				final boolean rex_r;
				final boolean rex_x;
				final boolean rex_b;

				if (prefix >= 0x40 && prefix < 0x50) {
					rex_w = (prefix & 8) != 0;
					rex_r = (prefix & 4) != 0;
					rex_x = (prefix & 2) != 0;
					rex_b = (prefix & 1) != 0;
					prefix = input.getUnsignedByte();
				} else {
					rex_w = false;
					rex_r = false;
					rex_x = false;
					rex_b = false;
				}

				final int opcode = prefix;
				switch (opcode) {
				case 0x01: {
					final int n = input.getUnsignedByte();
					final int dstReg = n & 0b00000111;
					final int srcReg = (n & 0b00111000) >>> 3;
					line = String.format("%s = %s + %s", getRegisterName(dstReg), getRegisterName(dstReg),
							getRegisterName(srcReg));
					break;
				}

				case 0x0f: {
					final int extendedOpcode = input.getUnsignedByte();
					switch (extendedOpcode) {
					case 0x05:
						line = "syscall";
					}
					break;
				}
				case 0x50:
				case 0x51:
				case 0x52:
				case 0x53:
				case 0x54:
				case 0x55:
				case 0x56:
				case 0x57: {
					final int srcReg = opcode & 0x07;
					line = String.format("push %s", getRegisterName(srcReg));
					break;
				}
				case 0x58:
				case 0x59:
				case 0x5a:
				case 0x5b:
				case 0x5c:
				case 0x5d:
				case 0x5e:
				case 0x5f: {
					final int dstReg = opcode & 0x07;
					line = String.format("%s = pop", getRegisterName(dstReg));
					break;
				}
				case 0xb8:
				case 0xb9:
				case 0xba:
				case 0xbb:
				case 0xbc:
				case 0xbd:
				case 0xbe:
				case 0xbf: {
					final int dstReg = opcode & 0x07;
					final int imm = input.getInt();
					line = String.format("%s = 0x%08x", getRegisterName(dstReg), imm);
					break;
				}
				case 0x89: {
					final int n = input.getUnsignedByte();
					final int dstReg = n & 0b00000111;
					final int srcReg = (n & 0b00111000) >>> 3;
					line = String.format("%s = %s", getRegisterName(dstReg), getRegisterName(srcReg));
					break;
				}

				case 0x31: {
					final int regs = input.getUnsignedByte();
					final int dstReg = regs & 0b00000111;
					final int srcReg = (regs & 0b00111000) >>> 3;
					final int extraR = (regs & 0x11000000) >>> 6;
					line = String.format("%s = %s %d", getRegisterName(dstReg), getRegisterName(srcReg), extraR);
					break;
				}
				case 0xc3: {
					line = String.format("ret (near)");
					break;
				}
				case 0x8d: {
					final int n = input.getUnsignedByte();
					final int mod = n >> 6;
					switch (mod) {
					case 0b00: {
						final int src = n & 0x7;
						final int dst = n >> 3 & 0x7;
						final int offset = input.getInt();
						line = String.format("%s = %s + 0x%08x", getRegisterName(dst), getRegisterName(src), offset);
						break;
					}
					case 0b01: {
						final int src = n & 0x7;
						final int dst = n >> 3 & 0x7;
						final int dummy = input.getUnsignedByte();
						final byte disp = input.getByte();
						line = String.format("%s = %s %d %d", getRegisterName(dst), getRegisterName(src), dummy, disp);
						break;
					}
					case 0b10:
						throw new IllegalStateException();
					case 0b11:
						throw new IllegalStateException();
					default:
						throw new IllegalStateException();
					}
					break;

				}
				default: {
					line = String.format("unknown opcode", opcode);
					break;
				}
				}

				String hex = "";
				for (long i = startPosition; i < input.getPosition(); i++) {
					hex += String.format(" %02x", memory.get(i));
				}
				s += String.format("%-32s # %s\n", line, hex);

			} catch (final InvalidAddressException e) {
				return s;
			}
		}
	}

	public static long readPointer(final ByteBuffer input, final boolean is64) {
		if (is64) {
			return input.getLong();
		} else {
			return input.getInt();
		}
	}

	private static boolean readBoolean(final MappedByteBuffer input) {
		switch (input.get()) {
		case 1:
			return false;
		case 2:
			return true;
		}
		throw new IllegalStateException();
	}

	public static void read(final File file) throws IOException {
		try (FileChannel input = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
			read(input.map(MapMode.READ_ONLY, 0, input.size()));
		}
	}
}
