package bdc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class Node implements InputNode, OutputNode {

    private final List<InputPort> input;
    List<Object> description;
    private final List<OutputPort> output;

    public Node(final List<Object> description, final int outputs, final List<? extends OutputPort> input) {
	this.description = description;
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

    public Node(final List<Object> description, final int outputs, final OutputPort... input) {
	this(description, outputs, Arrays.asList(input));
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

    public void mergeOutput(final InputNode terminator) {
	System.out.println(terminator.getInput());
	System.out.println(getOutput());
    }

    public static void merge(final InputNode inputNode, final OutputNode outputNode) {
	if (inputNode instanceof OutputNode) {
	    Iterables.assertEmpty(((OutputNode) inputNode).getOutput());
	}
	if (outputNode instanceof InputNode) {
	    Iterables.assertEmpty(((InputNode) outputNode).getInput());
	}
	final InputPort input = Iterables.getOnlyElement(inputNode.getInput());
	final OutputPort output = Iterables.getOnlyElement(outputNode.getOutput());
	output.replaceWith(input.getSource());

	Iterables.assertEmpty(output.getTargets());
	input.unlink();
    }
}
