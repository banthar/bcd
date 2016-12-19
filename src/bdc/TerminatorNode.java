package bdc;

import java.util.List;

public class TerminatorNode extends Node {

	public TerminatorNode(final List<Object> description, final NodeType type, final OutputPort environment,
			final OutputPort... input) {
		super(description, type, false, 0, environment, input);
	}

	public List<? extends Object> getDescription() {
		return null;
	}

	public String getId() {
		return getNodeId();
	}

}
