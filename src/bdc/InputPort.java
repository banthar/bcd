package bdc;

final class InputPort {
    private static int nextId = 0;

    private final Node node;
    private final int id;
    private OutputPort source;

    InputPort(final Node node, final OutputPort remotePort) {
	this.node = node;
	this.id = nextId++;
	link(remotePort);
    }

    public final void link(final OutputPort remotePort) {
	if (this.source != null) {
	    if (!this.source.targets.remove(this)) {
		throw new IllegalStateException();
	    }
	}
	this.source = remotePort;
	if (!remotePort.targets.add(this)) {
	    throw new IllegalStateException();
	}
    }

    public OutputPort unlink() {
	if (!this.source.targets.remove(this)) {
	    throw new IllegalStateException();
	}
	final OutputPort source = this.source;
	this.source = null;
	return source;
    }

    public Node getNode() {
	return this.node;
    }

    public String getId() {
	return "in" + String.valueOf(this.id);
    }

    public OutputPort getSource() {
	return this.source;
    }

    @Override
    public String toString() {
	return getId() + "{" + getNode() + "}";
    }

}