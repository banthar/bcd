package bdc;

import java.util.HashSet;
import java.util.Set;

public class MethodInit {

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

}
