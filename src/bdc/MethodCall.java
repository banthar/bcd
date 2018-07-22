package bdc;

import java.io.IOException;

import bdc.ConstantPool.MethodReference;

public class MethodCall implements NodeOperation {

	private final MethodReference reference;

	private AbstractMethod method;

	public MethodCall(final MethodReference reference) {
		this.reference = reference;
	}

	@Override
	public void resolve(final URLClassParser bytecodeLoader, final Node node) throws IOException, ClassFormatException {
		try {
			final AbstractMethod method = resolveMethod(bytecodeLoader);
			this.method = method;
			method.addCaller(node);
		} catch (final ClassNotFoundException | NoSuchMethodException e) {
			System.err.println(e);
		}
	}

	private AbstractMethod resolveMethod(final URLClassParser bytecodeLoader)
			throws NoSuchMethodException, IOException, ClassFormatException, ClassNotFoundException {
		if (this.reference.getOwnerClass().getName().equals("java/lang/System")) {
			System.out.println(this.reference.getName() + " " + this.reference.getType());
			return new NativeMethod();
		} else {
			final Method method = bytecodeLoader.loadClass(this.reference.getOwnerClass().getName())
					.getMethod(this.reference.getName(), this.reference.getDescriptor());
			method.parse();
			return method;
		}
	}

	@Override
	public Value getValue(final Node node, final PortId portId) {
		return Value.unknown(Type.unknown());
	}

	public AbstractMethod getMethod() {
		assert this.method != null;
		return this.method;
	}

}
