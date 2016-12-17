package x86_64;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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

	public static void main(final String[] args) throws Exception {
		try (DataOutputStream out = new DataOutputStream(new FileOutputStream("a.out"))) {
			new Elf().write(out, true, true,
					new byte[] { (byte) 0xB8, 0x3C, 0x00, 0x00, 0x00, 0x31, (byte) 0xFF, 0x0F, 0x05 });
		}
	}

}
