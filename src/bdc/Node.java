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
    List<Object> description;

    InputPort inputEnvironment;
    private final List<InputPort> input;

    OutputPort outputEnvironment;
    private final List<OutputPort> output;

    public Node(final List<Object> description, final NodeType type, final boolean writesMemory, final int outputs,
	    final OutputPort inputEnvironment, final List<? extends OutputPort> input) {
	this.description = description;
	this.type = type;

	if (inputEnvironment != null) {
	    this.inputEnvironment = new InputPort(this, inputEnvironment);
	}
	this.input = new ArrayList<>();
	for (int i = 0; i < input.size(); i++) {
	    final InputPort port = new InputPort(this, input.get(i));
	    this.input.add(port);
	}

	if (writesMemory) {
	    this.outputEnvironment = new OutputPort(this);
	}
	this.output = new ArrayList<>();
	for (int i = 0; i < outputs; i++) {
	    final OutputPort port = new OutputPort(this);
	    this.output.add(port);
	}
    }

    public Node(final List<Object> description, final NodeType type, final boolean writesMemory, final int outputs,
	    final OutputPort inputEnvironment, final OutputPort... input) {
	this(description, type, writesMemory, outputs, inputEnvironment, Arrays.asList(input));
    }

    public NodeType getType() {
	return this.type;
    }

    @Override
    public List<? extends InputPort> getAllInputPorts() {
	final ArrayList<InputPort> list = new ArrayList<>();
	if (this.inputEnvironment != null) {
	    list.add(this.inputEnvironment);
	}
	list.addAll(this.input);
	return list;
    }

    @Override
    public List<? extends OutputPort> getAllOutputPorts() {
	final ArrayList<OutputPort> list = new ArrayList<>();
	if (this.outputEnvironment != null) {
	    list.add(this.outputEnvironment);
	}
	list.addAll(this.output);
	return list;
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
	for (final OutputPort port : getAllOutputPorts()) {
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
	for (final InputPort port : getAllInputPorts()) {
	    final Node sourceNode = port.getSource().getNode();
	    if (nodes.add(sourceNode)) {
		sourceNode.addAllTargetNodes(nodes);
	    }
	}
    }

    public static void merge(final InputNode inputNode, final OutputNode outputNode) {
	final List<? extends InputPort> input = inputNode.getAllInputPorts();
	final List<? extends OutputPort> output = outputNode.getAllOutputPorts();
	if (input.size() != output.size()) {
	    throw new IllegalStateException();
	}
	merge(inputNode.getInputEnvironment(), outputNode.getOutputEnvironment());
    }

    public static void merge(final InputPort input, final OutputPort output) {
	output.replaceWith(input.getSource());
	Iterables.assertEmpty(output.getTargets());
	input.unlink();
    }

    @Override
    public InputPort getInputEnvironment() {
	return this.inputEnvironment;
    }

    @Override
    public InputPort getExtraInput(final int index) {
	return this.input.get(index);
    }

    @Override
    public OutputPort getOutputEnvironment() {
	return this.outputEnvironment;
    }

    @Override
    public OutputPort getExtraOutput(final int index) {
	return this.output.get(index);
    }

    @Override
    public String toString() {
	return getNodeId() + "{" + getType() + "}";
    }

}
