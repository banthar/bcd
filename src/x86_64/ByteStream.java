package x86_64;

import x86_64.MemoryView.InvalidAddressException;

public class ByteStream {

	private final MemoryView memory;
	private long position;

	public ByteStream(final MemoryView memory, final long offset) {
		this.memory = memory;
		this.position = offset;
	}

	public int getUnsignedByte() throws InvalidAddressException {
		return 0xff & getByte();
	}

	public byte getByte() throws InvalidAddressException {
		return this.memory.get(this.position++);
	}

	public int getInt() throws InvalidAddressException {
		final int n = 0;
		return getUnsignedByte() + getUnsignedByte() + getUnsignedByte() + getUnsignedByte();
	}

	public long getPosition() {
		return this.position;
	}
}
