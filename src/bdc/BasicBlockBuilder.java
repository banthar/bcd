package bdc;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import bdc.ConstantPool.ClassReference;
import bdc.ConstantPool.FieldReference;
import bdc.ConstantPool.MethodReference;
import bdc.PortId.PortType;
import bdc.Type.FieldType;
import bdc.Type.MethodType;
import bdc.Type.PrimitiveType;

public class BasicBlockBuilder {

	enum BinaryOperationType implements PureTransformationType {
		Add("+"), Subtract("-"), Multiply("*"), Divide("/"), Remainder("%");
		private final String symbol;

		private BinaryOperationType(final String symbol) {
			this.symbol = symbol;
		}

		public String getSymbol() {
			return this.symbol;
		}

		public static BinaryOperationType fromId(final int id) {
			return values()[id];
		}
	}

	enum ShiftType implements PureTransformationType {
		Left, Arithmetic, Logical;

		public static ShiftType fromId(final int id) {
			return values()[id];
		}
	}

	enum BitwiseOperationType implements PureTransformationType {
		And, Or, Xor;
		public static BitwiseOperationType fromId(final int id) {
			return values()[id];
		}
	}

	enum CompareType implements PureTransformationType {
		EQ, NE, LT, GE, GT, LE;

		public static CompareType fromId(final int id) {
			return values()[id];
		}
	}

	interface InstructionPrinter<T> {
		List<T> print(Node operation);
	}

	final Node inputNode;
	private OutputPort environment;
	Node terminator = null;
	List<BasicBlockBuilder> jumpsOut = new ArrayList<>();
	Set<BasicBlockBuilder> jumpsIn = new HashSet<>();

	private static int nextId = 0;

	private final int id = nextId++;

	private BasicBlockBuilder() {
		this.inputNode = new Node(null, true, 0, null);
		this.environment = this.inputNode.getOutputEnvironment();
	}

	public void putLocal(final int id, final OutputPort value) {
		final Node operation = new Node(new StoreLocal(id), true, 0, this.environment, value);
		this.environment = operation.getOutputEnvironment();
	}

	public OutputPort getLocal(final int id) {
		final Node operation = new Node(new LoadLocal(id), false, 1, this.environment);
		return operation.getOutputArg(0);
	}

	public OutputPort pop() {
		final Node operation = new Node(new Pop(), true, 1, this.environment);
		this.environment = operation.getOutputEnvironment();
		return operation.getOutputArg(0);

	}

	public void push(final OutputPort value) {
		this.environment = new Node(new Push(), true, 0, this.environment, value).getOutputEnvironment();
	}

	private OutputPort constant(final FieldType type, final Object value) {
		return Node.constant(type, value);
	}

	public OutputPort nullConstant() {
		return constant(PrimitiveType.Reference, null);
	}

	public OutputPort integerConstant(final int value) {
		return constant(PrimitiveType.Integer, value);
	}

	public OutputPort longConstant(final long value) {
		return constant(PrimitiveType.Long, value);
	}

	public OutputPort floatConstant(final float value) {
		return constant(PrimitiveType.Float, value);
	}

	public OutputPort doubleConstant(final double value) {
		return constant(PrimitiveType.Double, value);
	}

	public OutputPort stringConstant(final String value) {
		return constant(Type.string(), value);
	}

	private OutputPort pureOperation(final PureTransformation operation, final OutputPort... args) {
		return Node.pureOperation(operation, args);
	}

	public OutputPort binaryOperation(final PrimitiveType type, final BinaryOperationType op, final OutputPort left,
			final OutputPort right) {
		return pureOperation(PureTransformation.binaryOperation(type, op), left, right);
	}

	public OutputPort negate(final PrimitiveType type, final OutputPort value) {
		return pureOperation(PureTransformation.negate(type), value);
	}

	public OutputPort shift(final PrimitiveType type, final ShiftType shiftType, final OutputPort left,
			final OutputPort right) {
		return pureOperation(PureTransformation.shift(type, shiftType), left, right);
	}

	public OutputPort bitwiseOperation(final PrimitiveType type, final BitwiseOperationType operation,
			final OutputPort left, final OutputPort right) {
		return pureOperation(PureTransformation.bitwiseOperation(type, operation), left, right);
	}

	public OutputPort convert(final PrimitiveType from, final PrimitiveType to, final OutputPort value) {
		return pureOperation(PureTransformation.convert(from, to), value);
	}

	public OutputPort compare(final PrimitiveType type, final OutputPort right, final OutputPort left) {
		return pureOperation(PureTransformation.compare(type), left, right);
	}

	public OutputPort loadElement(final PrimitiveType elementType, final OutputPort arrayref, final OutputPort index) {
		final Node operation = new Node(Arrays.asList("load_element", elementType), false, 1, this.environment,
				arrayref, index);
		return operation.getOutputArg(0);
	}

	public void storeElement(final OutputPort arrayref, final OutputPort index) {
		final Node operation = new Node(Arrays.asList("store_element"), true, 0, this.environment, arrayref, index);
		this.environment = operation.getOutputEnvironment();
	}

	public OutputPort checkedCast(final ClassReference type, final OutputPort value) {
		final Node operation = new Node(Arrays.asList("checked_cast", type), false, 1, null, value);
		return operation.getOutputArg(0);
	}

	public OutputPort instanceOf(final ClassReference type, final OutputPort value) {
		final Node operation = new Node(Arrays.asList("instance_of", type), false, 1, null, value);
		return operation.getOutputArg(0);
	}

	public void monitorEnter(final OutputPort monitor) {
		final Node operation = new Node(Arrays.asList("monitor_enter"), true, 1, this.environment, monitor);
		this.environment = operation.getOutputEnvironment();
	}

	public void monitorExit(final OutputPort monitor) {
		final Node operation = new Node(Arrays.asList("monitor_exit"), true, 1, this.environment, monitor);
		this.environment = operation.getOutputEnvironment();
	}

	public OutputPort loadStaticField(final FieldReference fieldReference) {
		final Node operation = new Node(Arrays.asList("load_static_field", fieldReference), false, 1, this.environment);
		return operation.getOutputArg(0);
	}

	public void storeStaticField(final FieldReference fieldReference, final OutputPort value) {
		final Node operation = new Node(Arrays.asList("store_static_field", fieldReference), true, 1, this.environment,
				value);
		this.environment = operation.getOutputEnvironment();
	}

	public OutputPort loadField(final FieldReference fieldReference, final OutputPort target) {
		final Node operation = new Node(Arrays.asList("load_field", fieldReference), false, 1, this.environment,
				target);
		return operation.getOutputArg(0);
	}

	public void storeField(final FieldReference fieldReference, final OutputPort target, final OutputPort value) {
		final Node operation = new Node(Arrays.asList("store_field", fieldReference), true, 1, this.environment, target,
				value);
		this.environment = operation.getOutputEnvironment();
	}

	public List<OutputPort> invokeVirtual(final MethodReference methodReference, final OutputPort target,
			final List<OutputPort> args) {
		return invoke("invoke_virtual", methodReference, target, args);
	}

	private List<OutputPort> invoke(final String type, final MethodReference methodReference, final OutputPort target,
			final List<OutputPort> args) {
		final MethodType methodType = methodReference.getType();
		final List<OutputPort> input = new ArrayList<>();
		if (target != null) {
			input.add(target);
		}
		input.addAll(args);
		final Node operation = new Node(methodReference, true, methodType.isVoid() ? 0 : 1, this.environment, input);
		this.environment = operation.getOutputEnvironment();
		if (methodType.isVoid()) {
			return Collections.emptyList();
		} else {
			return Arrays.asList(operation.getOutputArg(0));
		}
	}

	public List<OutputPort> invokeSpecial(final MethodReference methodReference, final OutputPort target,
			final List<OutputPort> args) {
		return invoke("invoke_special", methodReference, target, args);
	}

	public List<OutputPort> invokeStatic(final MethodReference methodReference, final List<OutputPort> args) {
		return invoke("invoke_static", methodReference, null, args);

	}

	public List<OutputPort> invokeInterface(final MethodReference methodReference, final OutputPort target,
			final List<OutputPort> args) {
		return invoke("invoke_interface", methodReference, target, args);

	}

	public OutputPort newInstance(final ClassReference classReference) {
		final Node operation = new Node(Arrays.asList("new_instance", classReference), true, 1, this.environment);
		this.environment = operation.getOutputEnvironment();
		return operation.getOutputArg(0);
	}

	public OutputPort newPrimitiveArray(final PrimitiveType type, final OutputPort size) {
		final Node operation = new Node(Arrays.asList("new_primitive_array", type), true, 2, this.environment, size);
		this.environment = operation.getOutputEnvironment();
		return operation.getOutputArg(0);
	}

	public OutputPort newArray(final ClassReference type, final OutputPort size) {
		final Node operation = new Node(Arrays.asList("new_array", type), true, 2, this.environment, size);
		this.environment = operation.getOutputEnvironment();
		return operation.getOutputArg(0);

	}

	public OutputPort arrayLength(final OutputPort array) {
		final Node operation = new Node(Arrays.asList("array_length"), false, 1, this.environment, array);
		return operation.getOutputArg(0);
	}

	public void returnValue(final PrimitiveType type, final OutputPort ref) {
		terminate(Node.terminator(new ReturnValues(type), this.environment, ref));

	}

	public void returnVoid() {
		terminate(Node.terminator(new ReturnValues(), this.environment));
	}

	private void referenceTo(final BasicBlockBuilder target) {
		this.jumpsOut.add(target);
		target.jumpsIn.add(this);
	}

	public void jumpIf(final PrimitiveType type, final BasicBlockBuilder then, final BasicBlockBuilder otherwise,
			final CompareType compareType, final OutputPort left, final OutputPort right) {
		terminate(new Node(new ConditionalJump(type, compareType), false, 0, this.environment, left, right), then,
				otherwise);
	}

	public void jumpTable(final OutputPort value, final int defaultOffset, final Map<Integer, Integer> lookupTable,
			final List<BasicBlockBuilder> targets) {
		terminate(Node.terminator(new JumpTable(defaultOffset, lookupTable), this.environment, value),
				targets.toArray(new BasicBlockBuilder[0]));
	}

	public void jump(final BasicBlockBuilder target) {
		terminate(Node.terminator(new UnconditionalJump(), this.environment), target);
	}

	public void returnError(final OutputPort exception) {
		terminate(Node.terminator(new ReturnError(), this.environment, exception));
	}

	private void terminate(final Node terminator, final BasicBlockBuilder... targets) {
		if (!this.jumpsOut.isEmpty()) {
			throw new IllegalStateException();
		}
		if (this.terminator != null || this.environment == null) {
			throw new IllegalStateException();
		}
		if (new HashSet<>(Arrays.asList(targets)).size() != targets.length) {
			throw new IllegalStateException("Duplicate targets");
		}
		for (final BasicBlockBuilder target : targets) {
			referenceTo(target);
		}
		this.terminator = terminator;
		this.environment = null;
	}

	public void simplifyJump(final BasicBlockBuilder newTarget) {
		final Node oldTerminator = removeTerminator();
		jump(newTarget);
		for (final Entry<PortId, ? extends InputPort> entry : oldTerminator.getAllInputPorts().entrySet()) {
			if (entry.getKey().type == PortType.LOCAL || entry.getKey().type == PortType.STACK) {
				final OutputPort oldLink = entry.getValue().unlink();
				if (newTarget.inputNode.getOutput(entry.getKey()) != null) {
					this.terminator.addInput(entry.getKey(), oldLink);
				}
			} else if (entry.getKey().type == PortType.ARG) {
				entry.getValue().unlink();
			} else if (entry.getKey().type == PortType.ENV) {
				if (entry.getValue().getSource() != null) {
					throw new IllegalStateException();
				}
			} else {
				throw new IllegalStateException();
			}
		}
	}

	private Node removeTerminator() {
		if (this.terminator == null || this.environment != null) {
			throw new IllegalStateException();
		}
		for (final BasicBlockBuilder target : this.jumpsOut) {
			target.jumpsIn.remove(this);
		}
		this.jumpsOut.clear();
		final Node removedTerminator = this.terminator;
		this.terminator = null;
		this.environment = removedTerminator.getInput(PortId.environment()).unlink();
		return removedTerminator;
	}

	public boolean isTerminated() {
		return this.terminator != null;
	}

	public Set<BasicBlockBuilder> getAllTargetBlocks() {
		final Set<BasicBlockBuilder> visited = new HashSet<>(Arrays.asList(this));
		getAllTargetBlocks(visited);
		return visited;
	}

	private void getAllTargetBlocks(final Set<BasicBlockBuilder> visited) {
		for (final BasicBlockBuilder out : this.jumpsOut) {
			if (visited.add(out)) {
				out.getAllTargetBlocks(visited);
			}
		}
	}

	public Set<BasicBlockBuilder> getAllLinkedBlocks() {
		final Set<BasicBlockBuilder> visited = new HashSet<>(Arrays.asList(this));
		getAllLinkedBlocks(visited);
		return visited;
	}

	private void getAllLinkedBlocks(final Set<BasicBlockBuilder> visited) {
		for (final BasicBlockBuilder out : this.jumpsOut) {
			if (visited.add(out)) {
				out.getAllLinkedBlocks(visited);
			}
		}
		for (final BasicBlockBuilder out : this.jumpsIn) {
			if (visited.add(out)) {
				out.getAllLinkedBlocks(visited);
			}
		}
	}

	public void dump(final PrintStream out, final String name) {
		for (final BasicBlockBuilder block : getAllLinkedBlocks()) {
			out.println("    subgraph cluster_" + block.getBlockId() + " {");
			out.println("      label=\"" + block.getBlockId() + "\";");
			out.print("      " + quote(block.inputNode.getNodeId()) + " [");
			out.print("shape = \"record\" label = \"" + block.getBlockId() + " init");
			out.print("|{out");
			for (final Entry<? extends PortId, ? extends OutputPort> port : block.inputNode.getAllOutputPorts()
					.entrySet()) {
				out.print("|<" + port.getValue().getId() + "> #" + port.getKey());
			}
			out.println("}\"];");

			for (final Node node : block.getNodes()) {
				out.print("      \"" + node.getNodeId() + "\" [");
				out.print("shape = \"record\" label = \"{");
				out.print("{<in>in");
				for (final Entry<? extends PortId, ? extends InputPort> entry : node.getAllInputPorts().entrySet()) {
					out.format("|<%s> %s", entry.getValue().getId(), entry.getKey() + ": " + entry.getValue().getId());
				}
				out.print("}|{");
				out.print(String.join("|", node.getNodeId(),
						node.getData().toString().replaceAll("([<>\"\\\\\\]\\[{}])", "\\\\$1")));
				out.print("}|{");
				out.print("<out>out");
				for (final Entry<? extends PortId, ? extends OutputPort> entry : node.getAllOutputPorts().entrySet()) {
					out.format("|<%s> %s", entry.getValue().getId(), entry.getKey() + ": " + entry.getValue().getId());
				}
				out.println("}}\"];");
			}

			for (final Node node : block.getNodes()) {
				for (final Entry<? extends PortId, ? extends OutputPort> entry : node.getAllOutputPorts().entrySet()) {
					final OutputPort sourcePort = entry.getValue();
					for (final InputPort targetPort : sourcePort.getTargets()) {
						out.println("      " + sourcePort.getNode().getNodeId() + ":" + sourcePort.getId() + " -> "
								+ targetPort.getNode().getNodeId() + ":" + targetPort.getId() + ";");
					}
				}
			}
			out.println("    }");

		}
		for (final BasicBlockBuilder source : getAllLinkedBlocks()) {
			for (final BasicBlockBuilder target : source.jumpsOut) {
				out.println(
						"    " + source.terminator.getNodeId() + ":out -> " + target.inputNode.getNodeId() + ":in;");
			}
			if (source.terminator.getData() == "return_value") {
				out.println("    " + source.terminator.getNodeId() + ":out -> " + name + "_end;");

			}
		}
		out.println("    " + name + "_start -> " + this.inputNode.getNodeId() + ":in;");

	}

	public Set<Node> getNodes() {
		final HashSet<Node> nodes = new HashSet<>();
		nodes.addAll(this.inputNode.getAllLinkedNodes());
		nodes.addAll(this.terminator.getAllLinkedNodes());
		return nodes;
	}

	public void dump(final String name) {
		try {
			try (final PrintStream out = new PrintStream(new File(name + ".dot"))) {
				out.println("digraph G {");
				dump(out, name);
				out.println("}");
			}
		} catch (final IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private String getBlockId() {
		return "block" + this.id;
	}

	@Override
	public String toString() {
		return getBlockId();
	}

	static String quote(final String s) {
		if (s == null) {
			return "null";
		} else {
			return "\"" + s.toString() + "\"";
		}
	}

	static String getObjectId(final Object o) {
		return String.format("%08x", o.hashCode());
	}

	public BasicBlockBuilder getTarget(final int n) {
		return this.jumpsOut.get(n);
	}

	public void unlink() {
		if (!this.jumpsIn.isEmpty()) {
			throw new IllegalStateException();
		}
		removeTerminator();
	}

	public static BasicBlockBuilder createBlock() {
		final BasicBlockBuilder block = new BasicBlockBuilder();
		block.inputNode.data = new BlockInit(block);
		return block;
	}

	public static BasicBlockBuilder createMethodInitBlock(final Method method) {
		final BasicBlockBuilder block = new BasicBlockBuilder();
		block.inputNode.data = new MethodInit(method);
		return block;
	}

	private Map<PortId, Value> compute(final Map<OutputPort, Value> computedValues, final Node node) {
		final Map<PortId, Value> input = new HashMap<>();
		for (final Entry<PortId, ? extends InputPort> entry : node.getAllInputPorts().entrySet()) {
			final OutputPort source = entry.getValue().getSource();
			if (!computedValues.containsKey(source)) {
				compute(computedValues, source.getNode());
			}
			input.put(entry.getKey(), computedValues.get(source));
		}
		if (node.getData() instanceof PureOperation) {
			final Map<PortId, ? extends Value> output = ((PureOperation) node.getData()).compute(input);
			for (final Entry<PortId, ? extends OutputPort> entry : node.getAllOutputPorts().entrySet()) {
				computedValues.put(entry.getValue(), output.get(entry.getKey()));
			}
			return null;
		} else if (node.getData() instanceof ReturnValues) {
			return input;
		} else if (node.getData() instanceof Jump) {
			final Value branch = ((Jump) node.getData()).compute(input);
			if (branch.isConstant()) {
				final int targetIndex = ((Integer) branch.getConstant()).intValue();
				return getTarget(targetIndex).compute(input);
			} else {
				throw new IllegalStateException();
			}
		} else {
			throw new IllegalStateException("Unsupported node: " + node.getData());
		}
	}

	public Map<PortId, Value> compute(final Map<PortId, Value> constantInput) {
		final Map<OutputPort, Value> computedValues = new HashMap<>();
		for (final Entry<PortId, Value> entry : constantInput.entrySet()) {
			computedValues.put(this.inputNode.getOutput(entry.getKey()), entry.getValue());
		}
		return compute(computedValues, this.terminator);
	}
}
