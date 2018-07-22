package x86_64;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import bdc.Method;

class InstructionGenerator {

	ByteBuffer out = ByteBuffer.allocate(1024);

	List<Object> relocations = new ArrayList<>();

	private final Codegen codegen;

	public InstructionGenerator(final Codegen codegen) {
		this.codegen = codegen;
		this.out.order(ByteOrder.LITTLE_ENDIAN);
	}

	public void loadConstant(final Register register, final int value) {
		this.out.put((byte) (0xb8 | register.getIndex()));
		this.out.putInt(value);
	}

	public void functionReturn() {
		this.out.put((byte) 0xc3);
	}

	public void add(final Register destination, final Register addend) {
		this.out.put((byte) 0x01);
		this.out.put((byte) (addend.getIndex() << 3 | destination.getIndex() | 0xc0));
	}

	public void move(final Register target, final Register source) {
		this.out.put((byte) 0x89);
		this.out.put((byte) ((target.getIndex() << 3) + source.getIndex() | 0xc0));
	}

	byte[] toBytes() {
		final byte[] bytes = new byte[this.out.position()];
		this.out.rewind();
		this.out.get(bytes);
		return bytes;
	}

	public void call(final Method method) {
		this.out.put((byte) 0xe8);
		this.relocations.add(new Relocation(this.out.position(), method));
		this.codegen.addMethod(method);
		this.out.putInt(method.hashCode());
	}

	public void syscall() {
		this.out.put((byte) 0x0f);
		this.out.put((byte) 0x05);
	}
}