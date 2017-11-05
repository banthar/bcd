package x86_64;

import java.nio.MappedByteBuffer;
import java.util.Set;

public abstract class MemoryView {

	public static class InvalidAddressException extends Exception {

		public InvalidAddressException(final String msg) {
			super(msg);
		}

	}

	private MemoryView() {
	}

	public static MemoryView empty() {
		return new MemoryView() {
			@Override
			public byte get(final long address) throws InvalidAddressException {
				throw new InvalidAddressException(String.format("0x%08x is unmapped", address));
			}
		};
	}

	public MemoryView map(final long vaddr, final long vsize, final Set<ProgramHeaderFlag> flags,
			final MappedByteBuffer input, final long poffset, final long psize) {
		return new MemoryView() {
			@Override
			public byte get(final long address) throws InvalidAddressException {
				if (address >= vaddr) {
					final long offset = address - vaddr;
					if (offset < vsize) {
						if (offset < psize) {
							return input.get((int) (poffset + offset));
						} else {
							return 0;
						}
					} else {
						return MemoryView.this.get(address);
					}
				} else {
					return MemoryView.this.get(address);
				}
			}
		};
	}

	public abstract byte get(final long address) throws InvalidAddressException;

	public ByteStream createStream(final long offset) {
		return new ByteStream(this, offset);
	}

}
