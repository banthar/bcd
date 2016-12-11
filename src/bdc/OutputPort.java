package bdc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

final class OutputPort {
    private final Node node;
    private final int n;
    final Set<InputPort> targets = new HashSet<>();

    OutputPort(final Node node, final int n) {
	this.node = node;
	this.n = n;
    }

    public Node getNode() {
	return this.node;
    }

    public String getId() {
	return "out" + String.valueOf(this.n);
    }

    public Set<? extends InputPort> getTargets() {
	return this.targets;
    }

    public void replaceWith(final OutputPort source) {
	for (final InputPort target : new ArrayList<>(this.targets)) {
	    target.link(source);
	}
    }
}