package bdc;

import java.util.HashSet;
import java.util.Set;

public class MethodInit implements NodeOperation {

	private final Method method;

	public MethodInit(final Method method) {
		this.method = method;
	}

	public Method getMethod() {
		return this.method;
	}

	public Iterable<? extends PortId> getReturnedPorts() {
		Set<PortId> returnedPorts = null;
		for (final BasicBlockBuilder block : this.method.getBlock().getAllLinkedBlocks()) {
			if (block.terminator.getData() instanceof FunctionTerminator) {
				final Set<PortId> ports = block.terminator.getAllInputPorts().keySet();
				if (returnedPorts == null) {
					returnedPorts = new HashSet<>(ports);
				} else if (!returnedPorts.equals(ports)) {
					throw new IllegalStateException("Incompatible returns: " + ports + " " + returnedPorts);
				}
			}
		}
		return returnedPorts;
	}

	@Override
	public Value getValue(final Node node, final PortId portId) {
		if (portId.equals(PortId.environment())) {
			return Value.unknown(Type.unknown());
		} else {
			// TODO refine type
			return Value.unknown(Type.unknown());
		}
	}
}
