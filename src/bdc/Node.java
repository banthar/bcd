package bdc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class Node implements InputNode, OutputNode {

    public enum NodeType {
	INIT,

	POP,

	PUSH,

	STORE_LOCAL,

	LOAD_LOCAL,

	CONSTANT,

	INVOKE,

	LOAD_ELEMENT,

	STORE_ELEMENT,

	CHECKED_CAST,

	INSTANCE_OF,

	MONITOR_ENTER,

	MONITOR_EXIT,

	COMPARE,

	CONVERT,

	BINARY_OPERATION,

	NEGATE,

	SHIFT,

	BITWISE_OPERATION,

	LOAD_STATIC_FIELD,

	STORE_STATIC_FIELD,

	LOAD_FIELD,

	STORE_FIELD,

	NEW_INSTANCE,

	NEW_PRIMITIVE_ARRAY,

	NEW_ARRAY,

	ARRAY_LENGTH,

	TERMINATOR,
    }

    private final NodeType type;
    private final List<InputPort> input;
    List<Object> description;
    private final List<OutputPort> output;

    public Node(final List<Object> description, final NodeType type, final int outputs,
	    final List<? extends OutputPort> input) {
	this.description = description;
	this.type = type;
	this.input = new ArrayList<>();
	this.output = new ArrayList<>();
	for (int i = 0; i < outputs; i++) {
	    final OutputPort port = new OutputPort(this, i);
	    this.output.add(port);
	}
	for (int i = 0; i < input.size(); i++) {
	    final InputPort port = new InputPort(this, i, input.get(i));
	    this.input.add(port);
	}
    }

    public Node(final List<Object> description, final NodeType type, final int outputs, final OutputPort... input) {
	this(description, type, outputs, Arrays.asList(input));
    }

    public NodeType getType() {
	return this.type;
    }

    @Override
    public List<? extends InputPort> getInput() {
	return this.input;
    }

    @Override
    public List<? extends OutputPort> getOutput() {
	return this.output;
    }

    public String getNodeId() {
	return "node" + BasicBlockBuilder.getObjectId(this);
    }

    public Set<Node> getAllTargetNodes() {
	final Set<Node> nodes = new HashSet<>();
	addAllTargetNodes(nodes);
	return nodes;

    }

    private void addAllTargetNodes(final Set<Node> nodes) {
	for (final OutputPort port : getOutput()) {
	    for (final InputPort targetPort : port.getTargets()) {
		final Node targetNode = targetPort.getNode();
		if (nodes.add(targetNode)) {
		    targetNode.addAllTargetNodes(nodes);
		}
	    }
	}
    }

    public Set<Node> getAllSourceNodes() {
	final Set<Node> nodes = new HashSet<>();
	addAllSourceNodes(nodes);
	return nodes;

    }

    private void addAllSourceNodes(final Set<Node> nodes) {
	for (final InputPort port : getInput()) {
	    final Node sourceNode = port.getSource().getNode();
	    if (nodes.add(sourceNode)) {
		sourceNode.addAllTargetNodes(nodes);
	    }
	}
    }

    public static void merge(final InputNode inputNode, final OutputNode outputNode) {
	final List<? extends InputPort> input = inputNode.getInput();
	final List<? extends OutputPort> output = outputNode.getOutput();
	if (input.size() != output.size()) {
	    throw new IllegalStateException();
	}
	for (int i = 0; i < input.size(); i++) {
	    merge(input.get(i), output.get(i));
	}
    }

    public static void merge(final InputPort input, final OutputPort output) {
	output.replaceWith(input.getSource());
	Iterables.assertEmpty(output.getTargets());
	input.unlink();
    }

    @Override
    public String toString() {
	return getNodeId() + "{" + getType() + "}";
    }

}
