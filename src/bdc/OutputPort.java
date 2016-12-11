package bdc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

final class OutputPort {
    private static int nextId = 0;

    private final Node node;
    private final int id;
    final Set<InputPort> targets = new HashSet<>();

    OutputPort(final Node node) {
	this.node = node;
	this.id = nextId++;
    }

    public Node getNode() {
	return this.node;
    }

    public String getId() {
	return "out" + String.valueOf(this.id);
    }

    public Set<? extends InputPort> getTargets() {
	return new HashSet<>(this.targets);
    }

    public void replaceWith(final OutputPort source) {
	for (final InputPort target : new ArrayList<>(this.targets)) {
	    target.link(source);
	}
    }

    @Override
    public String toString() {
	return getNode() + "." + getId();
    }
}