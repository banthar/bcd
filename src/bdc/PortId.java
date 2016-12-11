package bdc;

public class PortId {

    private static enum PortType {
	ENV,

	ARG,

	STACK,

	LOCAL,
    }

    private final PortType type;

    private final int index;

    private PortId(final PortType type, final int id) {
	this.type = type;
	this.index = id;
    }

    public static PortId environment() {
	return new PortId(PortType.ENV, 0);
    }

    public static PortId arg(final int id) {
	return new PortId(PortType.ARG, id);
    }

    public static PortId stack(final int id) {
	return new PortId(PortType.STACK, id);
    }

    public static PortId local(final int id) {
	return new PortId(PortType.LOCAL, id);
    }

    @Override
    public boolean equals(final Object obj) {
	final PortId other = (PortId) obj;
	return this.type == other.type && this.index == other.index;
    }

    @Override
    public int hashCode() {
	return this.index;
    }

    @Override
    public String toString() {
	return this.type.name().toLowerCase() + this.index;
    }

}
