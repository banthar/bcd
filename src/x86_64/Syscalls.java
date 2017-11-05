package x86_64;

public class Syscalls {

	public static @interface Syscall {
		int value();
	}

	@Syscall(60)
	public static native void exit(int code);

}
