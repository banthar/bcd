package x86_64;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.StandardOpenOption;

public class Elf {

	public void write(final OutputStream out, final boolean is64, final boolean isLittleEndian, final byte[] code)
			throws IOException {

		final long baseAddress = 0x0000000000400000;

		final ByteBuffer programHeader = ByteBuffer.allocate(is64 ? 56 : 32);
		final ByteBuffer fileHeader = ByteBuffer.allocate(is64 ? 64 : 52);

		programHeader.order(isLittleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
		programHeader.putInt(1); // p_type
		programHeader.putInt(1); // p_flags
		programHeader.putLong(0); // p_offset
		programHeader.putLong(baseAddress); // p_vaddr
		programHeader.putLong(baseAddress); // p_paddr
		programHeader.putLong(code.length); // p_filesz
		programHeader.putLong(code.length); // p_memsz
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
		final int sectionHeaderNum = input.getShort();
		final int sectionHeaderNamesIndex = input.getShort();

		System.out.println("is64: " + is64);
		System.out.println("isBigEndian: " + isBigEndian);
		System.out.println("version: " + version);
		System.out.println("osabi: " + osabi);
		System.out.println("abiversion: " + abiversion);
		System.out.println("type: " + type);
		System.out.println("d1: " + machine);
		System.out.println("d2: " + e_version);

		System.out.format("entry: 0x%08x\n", entry);
		System.out.format("programHeaderOffset: 0x%08x\n", programHeaderOffset);
		System.out.format("sectionHeaderOffset: 0x%08x\n", sectionHeaderOffset);

		System.out.format("flags: 0x%08x\n", flags);
		System.out.format("headerSize: %d\n", headerSize);
		System.out.format("programHeaderEntsize: %d\n", programHeaderSize);
		System.out.format("programHeaderNum: %d\n", programHeaderCount);
		System.out.format("sectionHeaderSize: %d\n", sectionHeaderSize);
		System.out.format("sectionHeaderNum: %d\n", sectionHeaderNum);
		System.out.format("sectionHeaderNamesIndex: %d\n", sectionHeaderNamesIndex);

		for (int i = 0; i < programHeaderCount; i++) {
			input.position((int) programHeaderOffset + i * programHeaderSize);
			final int pType = input.getInt();
			System.out.format("pType: 0x%08x\n", pType);
		}

		System.out.println("END of ELF");

	}

	private static long readPointer(final MappedByteBuffer input, final boolean is64) {
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

	public static void main(final String[] args) throws Exception {
		final File file = new File("a.out");
		try (DataOutputStream out = new DataOutputStream(new FileOutputStream(file))) {
			file.setExecutable(true);
			new Elf().write(out, true, true,
					new byte[] { (byte) 0xB8, 0x3C, 0x00, 0x00, 0x00, 0x31, (byte) 0xFF, 0x0F, 0x05 });
		}
		read(file);
		read(new File("/bin/sh"));
	}

	private static void read(final File file) throws IOException {
		try (FileChannel input = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
			read(input.map(MapMode.READ_ONLY, 0, input.size()));
		}
	}
}
