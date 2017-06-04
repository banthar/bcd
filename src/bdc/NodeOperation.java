package bdc;

public interface NodeOperation {

	default Value getValue(final Node node, final PortId portId) {
		throw new IllegalStateException(this.getClass().toString());
	}

}
