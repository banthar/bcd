package wasm;

import java.io.DataOutputStream;
import java.io.IOException;

public class WASMGen implements AutoCloseable {

	private final DataOutputStream output;

	public WASMGen(final DataOutputStream output) {
		this.output = output;
	}

	public void run() throws IOException {

		final byte[] data = new byte[] { 0, 97, 115, 109, 1, 0, 0, 0, 1, 7, 1, 96, 2, 127, 127, 1, 127, 3, 2, 1, 0, 7,
				10, 1, 6, 97, 100, 100, 84, 119, 111, 0, 0, 10, 9, 1, 7, 0, 32, 0, 32, 1, 106, 11, 0, 25, 4, 110, 97,
				109, 101, 1, 9, 1, 0, 6, 97, 100, 100, 84, 119, 111, 2, 7, 1, 0, 2, 0, 0, 1, 0 };

		System.out.println(new String(data));

		this.output.write(data);
		if (1 == 1) {
			return;
		}

		this.output.writeInt(0x0061736d);
		this.output.writeInt(0x01000000);
		writeSection();
	}

	public void writeSection() throws IOException {

	}

	private void writeVarUInt(int n) throws IOException {
		do {
			final int m = n & 0b01111111;
			n >>>= 7;
			this.output.writeByte(m | (n == 0 ? 0b00000000 : 0b10000000));
		} while (n != 0);
	}

	@Override
	public void close() throws IOException {
		this.output.close();

	}

	public static void main(final String[] args) throws Exception {

	}

}
