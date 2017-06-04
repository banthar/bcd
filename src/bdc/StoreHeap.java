package bdc;

public class StoreHeap implements NodeOperation {

	@Override
	public Value getValue(final Node node, final PortId portId) {
		if (portId.equals(PortId.environment())) {
			return Value.unknownEnvironment();
		} else if (portId.equals(PortId.arg(0))) {
			return Value.unknownHeapReference();
		} else {
			throw new IllegalStateException();
		}
	}

}
