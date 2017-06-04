package bdc;

public class AllocHeap implements NodeOperation {

	@Override
	public Value getValue(final Node node, final PortId portId) {
		if (portId.equals(PortId.environment())) {
			return Value.unknown(Type.unknown());
		} else if (portId.equals(PortId.arg(0))) {
			return Value.unknown(Type.unknown());
		} else {
			throw new IllegalStateException("");
		}
	}

}
