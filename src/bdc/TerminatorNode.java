package bdc;

import java.util.List;

public class TerminatorNode extends Node implements bdc.BasicBlockBuilder.Terminator {

    public TerminatorNode(final List<Object> description, final OutputPort... input) {
	super(description, 0, input);
    }

    @Override
    public List<? extends Object> getDescription() {
	return null;
    }

    @Override
    public String getId() {
	return getNodeId();
    }

}
