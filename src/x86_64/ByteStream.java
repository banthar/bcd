package x86_64;

import x86_64.MemoryView.InvalidAddressException;

public class ByteStream {

	private final MemoryView memory;
	private long position;

	public ByteStream(final MemoryView memory, final long offset) {
		this.memory = memory;
		this.position = offset;
	}

	public int getByte() throws InvalidAddressException {
		return 0xff & this.memory.get(this.position++);
	}

	public int getInt() throws InvalidAddressException {
		final int n = 0;
		return getByte() + getByte() + getByte() + getByte();
	}

	public long getPosition() {
		return this.position;
	}
}
