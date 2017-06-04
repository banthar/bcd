package bdc;

import java.io.IOException;

import bdc.ConstantPool.MethodReference;

public class MethodCall implements NodeOperation {

	private final MethodReference reference;

	private Method method;

	public MethodCall(final MethodReference reference) {
		this.reference = reference;
	}

	@Override
	public void resolve(final URLClassParser bytecodeLoader, final Node node) throws IOException, ClassFormatException {
		try {
			final Method method = bytecodeLoader.loadClass(this.reference.getOwnerClass().getName())
					.getMethod(this.reference.getName(), this.reference.getDescriptor());
			method.parse();
			this.method = method;
			method.addCaller(node);
		} catch (final ClassNotFoundException | NoSuchMethodException e) {
			System.err.println(e);
		}
	}

	@Override
	public Value getValue(final Node node, final PortId portId) {
		return Value.unknown(Type.unknown());
	}

	public Method getMethod() {
		assert this.method != null;
		return this.method;
	}

}
