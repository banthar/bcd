package bdc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import bdc.Type.FieldType;

public class Node implements InputNode, OutputNode {

	static int nextId = 0;

	private final int id = nextId++;

	NodeOperation data;

	private final Map<PortId, InputPort> input;

	private final Map<PortId, OutputPort> output;

	public Node(final NodeOperation data, final boolean writesMemory, final int outputs,
			final OutputPort inputEnvironment, final List<? extends OutputPort> input) {
		this.data = data;

		this.input = new HashMap<>();
		if (inputEnvironment != null) {
			this.input.put(PortId.environment(), new InputPort(this, inputEnvironment));
		}
		for (int i = 0; i < input.size(); i++) {
			final InputPort port = new InputPort(this, input.get(i));
			this.input.put(PortId.arg(i), port);
		}

		this.output = new HashMap<>();
		if (writesMemory) {
			addOutput(PortId.environment());
		}
		for (int i = 0; i < outputs; i++) {
			addOutput(PortId.arg(i));
		}
	}

	public Node(final NodeOperation data, final boolean writesMemory, final int outputs,
			final OutputPort inputEnvironment, final OutputPort... input) {
		this(data, writesMemory, outputs, inputEnvironment, Arrays.asList(input));
	}

	@Override
	public Map<PortId, ? extends InputPort> getAllInputPorts() {
		return this.input;
	}

	@Override
	public Map<PortId, ? extends OutputPort> getAllOutputPorts() {
		return this.output;
	}

	public String getNodeId() {
		return "node" + this.id;
	}

	public void unlinkInput() {
		for (final InputPort input : getAllInputPorts().values()) {
			input.unlink();
		}
	}

	public Set<Node> getAllLinkedNodes() {
		final Set<Node> nodes = new HashSet<>();
		getAllLinkedNodes(nodes);
		return nodes;

	}

	private void getAllLinkedNodes(final Set<Node> nodes) {
		nodes.add(this);
		for (final OutputPort port : getAllOutputPorts().values()) {
			for (final InputPort targetPort : port.getTargets()) {
				final Node targetNode = targetPort.getNode();
				if (nodes.add(targetNode)) {
					targetNode.getAllLinkedNodes(nodes);
				}
			}
		}
		for (final InputPort port : getAllInputPorts().values()) {
			final OutputPort source = port.getSource();
			if (source != null) {
				final Node sourceNode = source.getNode();
				if (nodes.add(sourceNode)) {
					sourceNode.getAllLinkedNodes(nodes);
				}
			}
		}
	}

	public static void merge(final InputNode inputNode, final OutputNode outputNode) {
		final Set<PortId> ports = new HashSet<>();
		ports.addAll(inputNode.getAllInputPorts().keySet());
		ports.addAll(outputNode.getAllOutputPorts().keySet());
		for (final PortId port : ports) {
			final InputPort inputValue = inputNode.getAllInputPorts().get(port);
			final OutputPort outputValue = outputNode.getAllOutputPorts().get(port);
			if (inputValue == null) {
				throw new IllegalStateException("Missing input value: " + port);
			} else if (outputValue == null) {
				throw new IllegalStateException("Missing output value: " + port);
			} else {
				merge(inputValue, outputValue);
			}
		}
	}

	public static void merge(final InputPort input, final OutputPort output) {
		output.replaceWith(input.getSource());
		Iterables.assertEmpty(output.getTargets());
		input.unlink();
	}

	@Override
	public InputPort getInputEnvironment() {
		return this.input.get(PortId.environment());
	}

	public InputPort getInput(final PortId id) {
		return this.input.get(id);
	}

	public void addInput(final PortId id, final OutputPort remotePort) {
		if (this.input.put(id, new InputPort(this, remotePort)) != null) {
			throw new IllegalStateException();
		}
	}

	public void removeInput(final PortId port) {
		final InputPort removed = this.input.remove(port);
		if (removed == null) {
			throw new IllegalStateException("No such input port: " + port);
		}
		removed.unlink();
	}

	public void removeOutput(final PortId port) {
		final OutputPort removed = this.output.remove(port);
		if (removed == null) {
			throw new IllegalStateException("Port doesn't exist: " + port);
		}
		if (!removed.getTargets().isEmpty()) {
			throw new IllegalStateException("Removing used output ports: " + removed);
		}
		if (getAllOutputPorts().isEmpty()) {
			unlinkInput();
		}
	}

	@Override
	public OutputPort getOutputEnvironment() {
		return this.output.get(PortId.environment());
	}

	public OutputPort getOutputArg(final int index) {
		return getOutput(PortId.arg(index));
	}

	public OutputPort getOutput(final PortId id) {
		return this.output.get(id);
	}

	public OutputPort addOutput(final PortId portId) {
		final OutputPort newPort = new OutputPort(this, portId);
		if (this.output.put(portId, newPort) != null) {
			throw new IllegalStateException();
		}
		return newPort;
	}

	public OutputPort provideOutput(final PortId id) {
		final OutputPort existingOutput = getOutput(id);
		if (existingOutput == null) {
			return addOutput(id);
		} else {
			return existingOutput;
		}
	}

	@Override
	public String toString() {
		return "Node(" + getNodeId() + ", " + getData() + ")";
	}

	public NodeOperation getData() {
		return this.data;
	}

	public static OutputPort pureOperation(final PureOperation operation, final OutputPort... args) {
		return new Node(operation, false, 1, null, args).getOutputArg(0);
	}

	public static OutputPort constant(final FieldType type, final Object value) {
		return pureOperation(new LoadConstantOperation(type, value));
	}

	public static Node terminator(final BlockTerminator operation, final OutputPort environment,
			final OutputPort... input) {
		return new Node(operation, false, 0, environment, input);
	}

}
