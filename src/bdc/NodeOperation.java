package bdc;

import java.io.IOException;

public interface NodeOperation {

	default Value getValue(final Node node, final PortId portId) {
		throw new IllegalStateException(this.getClass().toString());
	}

	static NodeOperation unimplemented(final Object... args) {
		return new NodeOperation() {
		};
	}

	default void resolve(final URLClassParser bytecodeLoader, final Node node)
			throws IOException, ClassFormatException {
	}

}
