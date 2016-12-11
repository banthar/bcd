package bdc;

final class InputPort {
    private final Node node;
    private final int n;
    private OutputPort source;

    InputPort(final Node node, final int n, final OutputPort remotePort) {
	this.node = node;
	this.n = n;
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

    public void unlink() {
	if (!this.source.targets.remove(this)) {
	    throw new IllegalStateException();
	}
	this.source = null;
    }

    public Node getNode() {
	return this.node;
    }

    public String getId() {
	return "in" + String.valueOf(this.n);
    }

    public OutputPort getSource() {
	return this.source;
    }

    @Override
    public String toString() {
	return getId() + "{" + getNode() + "}";
    }

}