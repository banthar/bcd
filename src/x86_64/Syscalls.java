package x86_64;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Syscalls {

	public static @interface Syscall {
		int value();
	}

	@Syscall(60)
	public static void exit(final int code) {
		System.exit(code);
	}

	@Syscall(0)
	public static long read(final int fd, final byte[] buf, final long offset, final long count) throws IOException {
		return getInputStream(fd).read(buf, (int) offset, (int) count);
	}

	private static InputStream getInputStream(final int fd) {
		switch (fd) {
		case 0:
			return System.in;
		default:
			throw new IllegalArgumentException();
		}
	}

	private static OutputStream getOutputStream(final int fd) {
		switch (fd) {
		case 1:
			return System.out;
		case 2:
			return System.err;
		default:
			throw new IllegalArgumentException();
		}
	}

	@Syscall(1)
	public static long write(final int fd, final byte[] buf, final long offset, final long count) throws IOException {
		getOutputStream(fd).write(buf, (int) offset, (int) count);
		return count;
	}

}
