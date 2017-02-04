package bdc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

		PURE_OPERATION,

		LOAD_STATIC_FIELD,

		STORE_STATIC_FIELD,

		LOAD_FIELD,

		STORE_FIELD,

		NEW_INSTANCE,

		NEW_PRIMITIVE_ARRAY,

		NEW_ARRAY,

		ARRAY_LENGTH,

		BRANCH,

		RETURN,

	}

	static int nextId = 0;

	private final int id = nextId++;

	private final NodeType type;
	Object data;

	private final Map<PortId, InputPort> input;

	private final Map<PortId, OutputPort> output;

	public Node(final Object data, final NodeType type, final boolean writesMemory, final int outputs,
			final OutputPort inputEnvironment, final List<? extends OutputPort> input) {
		this.data = data;
		this.type = type;

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
			this.output.put(PortId.environment(), new OutputPort(this));
		}
		for (int i = 0; i < outputs; i++) {
			final OutputPort port = new OutputPort(this);
			this.output.put(PortId.arg(i), port);
		}
	}

	public Node(final Object data, final NodeType type, final boolean writesMemory, final int outputs,
			final OutputPort inputEnvironment, final OutputPort... input) {
		this(data, type, writesMemory, outputs, inputEnvironment, Arrays.asList(input));
	}

	public NodeType getType() {
		return this.type;
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

	public Set<Node> getAllLinkedNodes() {
		final Set<Node> nodes = new HashSet<>();
		getAllLinkedNodes(nodes);
		return nodes;

	}

	private void getAllLinkedNodes(final Set<Node> nodes) {
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
		this.input.remove(port).unlink();
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

	public OutputPort addOutput(final PortId id) {
		final OutputPort newPort = new OutputPort(this);
		if (this.output.put(id, newPort) != null) {
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
		return "Node(" + getNodeId() + ", " + getType() + ", " + getData() + ")";
	}

	public Object getData() {
		return this.data;
	}

	public static OutputPort pureOperation(final PureOperation operation, final OutputPort... args) {
		return new Node(operation, NodeType.PURE_OPERATION, false, 1, null, args).getOutputArg(0);
	}

	public static OutputPort constant(final Type type, final Object value) {
		return pureOperation(new LoadConstantOperation(type, value));
	}
}
