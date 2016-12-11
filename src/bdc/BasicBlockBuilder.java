package bdc;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import bdc.ConstantPool.ClassReference;
import bdc.ConstantPool.FieldReference;
import bdc.ConstantPool.MethodReference;
import bdc.Node.NodeType;
import bdc.Type.MethodType;
import bdc.Type.PrimitiveType;

public class BasicBlockBuilder {

    enum BinaryOperationType {
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

    enum ShiftType {
	Left, Arithmetic, Logical;

	public static ShiftType fromId(final int id) {
	    return values()[id];
	}
    }

    enum BitwiseOperationType {
	And, Or, Xor;
	public static BitwiseOperationType fromId(final int id) {
	    return values()[id];
	}
    }

    enum CompareType {
	EQ, NE, LT, GE, GT, LE;

	public static CompareType fromId(final int id) {
	    return values()[id];
	}
    }

    interface InstructionPrinter<T> {
	List<T> print(Node operation);
    }

    class Constant extends Node {
	public Constant(final Type type, final Object value) {
	    super(Arrays.asList("const", type, value), NodeType.CONSTANT, 1, Collections.emptyList());
	}
    }

    interface Terminator extends InputNode {
	List<? extends Object> getDescription();

	String getId();
    }

    private final Node inputNode = new Node(Arrays.asList(this), NodeType.INIT, 1);
    private OutputPort environment = this.inputNode.getOutput(0);
    private Terminator terminator = null;
    private Set<BasicBlockBuilder> jumpsOut = new HashSet<>();
    private final Set<BasicBlockBuilder> jumpsIn = new HashSet<>();

    private static int nextId = 0;

    private final int id = nextId++;

    public void putLocal(final int id, final OutputPort value) {
	final Node operation = new Node(Arrays.asList("store_local", id), NodeType.STORE_LOCAL, 1, this.environment,
		value);
	this.environment = operation.getOutput(0);
    }

    public OutputPort getLocal(final int id) {
	final Node operation = new Node(Arrays.asList("load_local", id), NodeType.LOAD_LOCAL, 1, this.environment);
	return operation.getOutput(0);
    }

    public OutputPort pop() {
	final Node operation = new Node(Arrays.asList("pop"), NodeType.POP, 2, this.environment);
	this.environment = operation.getOutput(0);
	return operation.getOutput(1);

    }

    public void push(final OutputPort value) {
	this.environment = new Node(Arrays.asList("push"), NodeType.PUSH, 1, this.environment, value).getOutput(0);
    }

    public OutputPort nullConstant() {
	return new Constant(PrimitiveType.Reference, null).getOutput(0);
    }

    public OutputPort integerConstant(final int value) {
	return new Constant(PrimitiveType.Integer, value).getOutput(0);
    }

    public OutputPort longConstant(final long value) {
	return new Constant(PrimitiveType.Long, value).getOutput(0);
    }

    public OutputPort floatConstant(final float value) {
	return new Constant(PrimitiveType.Float, value).getOutput(0);
    }

    public OutputPort doubleConstant(final double value) {
	return new Constant(PrimitiveType.Double, value).getOutput(0);
    }

    public OutputPort stringConstant(final String value) {
	return new Constant(Type.string(), value).getOutput(0);
    }

    public OutputPort binaryOperation(final PrimitiveType type, final BinaryOperationType op, final OutputPort left,
	    final OutputPort right) {
	return new Node(Arrays.asList("binary_operation", type, op), NodeType.BINARY_OPERATION, 1, left, right)
		.getOutput(0);
    }

    public OutputPort negate(final PrimitiveType type, final OutputPort value) {
	return new Node(Arrays.asList("negate", type), NodeType.NEGATE, 1, value).getOutput(0);
    }

    public OutputPort shift(final PrimitiveType type, final ShiftType shiftType, final OutputPort left,
	    final OutputPort right) {
	return new Node(Arrays.asList("shift", type, shiftType), NodeType.SHIFT, 1, left, right).getOutput(0);
    }

    public OutputPort bitwiseOperation(final PrimitiveType type, final BitwiseOperationType operation,
	    final OutputPort left, final OutputPort right) {
	return new Node(Arrays.asList("bitwise_operation", type, operation), NodeType.BITWISE_OPERATION, 1, left, right)
		.getOutput(0);
    }

    public OutputPort convert(final PrimitiveType from, final PrimitiveType to, final OutputPort value) {
	return new Node(Arrays.asList("convert", from, to), NodeType.CONVERT, 1, value).getOutput(0);
    }

    public OutputPort compare(final PrimitiveType type, final OutputPort left, final OutputPort right) {
	return new Node(Arrays.asList("compare", type), NodeType.COMPARE, 1, left, right).getOutput(0);
    }

    public OutputPort loadElement(final PrimitiveType elementType, final OutputPort arrayref, final OutputPort index) {
	final Node operation = new Node(Arrays.asList("load_element", elementType), NodeType.LOAD_ELEMENT, 1,
		this.environment, arrayref, index);
	return operation.getOutput(0);
    }

    public void storeElement(final OutputPort arrayref, final OutputPort index) {
	final Node operation = new Node(Arrays.asList("store_element"), NodeType.STORE_ELEMENT, 1, this.environment,
		arrayref, index);
	this.environment = operation.getOutput(0);
    }

    public OutputPort checkedCast(final ClassReference type, final OutputPort value) {
	final Node operation = new Node(Arrays.asList("checked_cast", type), NodeType.CHECKED_CAST, 1, value);
	return operation.getOutput(0);
    }

    public OutputPort instanceOf(final ClassReference type, final OutputPort value) {
	final Node operation = new Node(Arrays.asList("instance_of", type), NodeType.INSTANCE_OF, 1, value);
	return operation.getOutput(0);
    }

    public void monitorEnter(final OutputPort monitor) {
	final Node operation = new Node(Arrays.asList("monitor_enter"), NodeType.MONITOR_ENTER, 1, monitor);
	this.environment = operation.getOutput(0);
    }

    public void monitorExit(final OutputPort monitor) {
	final Node operation = new Node(Arrays.asList("monitor_exit"), NodeType.MONITOR_EXIT, 1, monitor);
	this.environment = operation.getOutput(0);
    }

    public OutputPort loadStaticField(final FieldReference fieldReference) {
	final Node operation = new Node(Arrays.asList("load_static_field", fieldReference), NodeType.LOAD_STATIC_FIELD,
		1, this.environment);
	return operation.getOutput(0);
    }

    public void storeStaticField(final FieldReference fieldReference, final OutputPort value) {
	final Node operation = new Node(Arrays.asList("store_static_field", fieldReference),
		NodeType.STORE_STATIC_FIELD, 1, this.environment, value);
	this.environment = operation.getOutput(0);
    }

    public OutputPort loadField(final FieldReference fieldReference, final OutputPort target) {
	final Node operation = new Node(Arrays.asList("load_field", fieldReference), NodeType.LOAD_FIELD, 1,
		this.environment, target);
	return operation.getOutput(0);
    }

    public void storeField(final FieldReference fieldReference, final OutputPort target, final OutputPort value) {
	final Node operation = new Node(Arrays.asList("store_field", fieldReference), NodeType.STORE_FIELD, 1,
		this.environment, target, value);
	this.environment = operation.getOutput(0);
    }

    public List<OutputPort> invokeVirtual(final MethodReference methodReference, final OutputPort target,
	    final List<OutputPort> args) {
	return invoke("invoke_virtual", methodReference, target, args);
    }

    private List<OutputPort> invoke(final String type, final MethodReference methodReference, final OutputPort target,
	    final List<OutputPort> args) {
	final MethodType methodType = methodReference.getType();
	final List<OutputPort> input = new ArrayList<>();
	input.add(this.environment);
	if (target != null) {
	    input.add(target);
	}
	input.addAll(args);
	final Node operation = new Node(Arrays.asList(type, methodReference), NodeType.INVOKE,
		methodType.isVoid() ? 1 : 2, input);
	this.environment = operation.getOutput(0);
	if (methodType.isVoid()) {
	    return Collections.emptyList();
	} else {
	    return Arrays.asList(operation.getOutput(1));
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
	final Node operation = new Node(Arrays.asList("new_instance", classReference), NodeType.NEW_INSTANCE, 2,
		this.environment);
	this.environment = operation.getOutput(0);
	return operation.getOutput(1);
    }

    public OutputPort newPrimitiveArray(final PrimitiveType type, final OutputPort size) {
	final Node operation = new Node(Arrays.asList("new_primitive_array", type), NodeType.NEW_PRIMITIVE_ARRAY, 2,
		this.environment, size);
	this.environment = operation.getOutput(0);
	return operation.getOutput(1);
    }

    public OutputPort newArray(final ClassReference type, final OutputPort size) {
	final Node operation = new Node(Arrays.asList("new_array", type), NodeType.NEW_ARRAY, 2, this.environment,
		size);
	this.environment = operation.getOutput(0);
	return operation.getOutput(1);

    }

    public OutputPort arrayLength(final OutputPort array) {
	final Node operation = new Node(Arrays.asList("array_length"), NodeType.ARRAY_LENGTH, 1, this.environment,
		array);
	return operation.getOutput(0);
    }

    class ReturnValue extends TerminatorNode {

	public ReturnValue(final OutputPort state) {
	    super(Arrays.asList("return_void"), state);
	}

	public ReturnValue(final OutputPort state, final PrimitiveType type, final OutputPort ref) {
	    super(Arrays.asList("return_value", type), state, ref);
	}

	public ReturnValue(final OutputPort state, final OutputPort exception) {
	    super(Arrays.asList("return_error"), state, exception);
	}

    }

    public void returnValue(final PrimitiveType type, final OutputPort ref) {
	terminate(new ReturnValue(this.environment, type, ref));
    }

    public void returnVoid() {
	terminate(new ReturnValue(this.environment));
    }

    class JumpIf extends TerminatorNode {
	public JumpIf(final OutputPort state, final PrimitiveType type, final OutputPort left,
		final CompareType compareType, final OutputPort right) {
	    super(Arrays.asList("jump_if", type, compareType), state, left, right);
	}
    }

    private void referenceTo(final BasicBlockBuilder target) {
	this.jumpsOut.add(target);
	target.jumpsIn.add(this);
    }

    public void jumpIf(final PrimitiveType type, final BasicBlockBuilder then, final BasicBlockBuilder otherwise,
	    final CompareType compareType, final OutputPort left, final OutputPort right) {
	referenceTo(then);
	referenceTo(otherwise);
	terminate(new JumpIf(this.environment, type, left, compareType, right));
    }

    public void jumpTable(final OutputPort value, final int defaultOffset, final Map<Integer, Integer> table) {
	throw new IllegalStateException();
    }

    class Jump extends TerminatorNode {
	public Jump(final OutputPort state) {
	    super(Arrays.asList("jump"), state);
	}
    }

    public void jump(final BasicBlockBuilder target) {
	referenceTo(target);
	terminate(new Jump(this.environment));
    }

    public void returnError(final OutputPort exception) {
	terminate(new ReturnValue(this.environment, exception));
    }

    private void terminate(final Terminator terminator) {
	if (this.terminator != null || this.environment == null) {
	    throw new IllegalStateException();
	}
	this.terminator = terminator;
	this.environment = null;
    }

    public boolean isTerminated() {
	return this.terminator != null;
    }

    public void removeDirectJumps() {
	removeDirectJumps(new HashSet<>());
    }

    private <T> T onlyElement(final Iterable<T> iterable) {
	return Iterables.getOnlyElement(iterable);
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

    private void removeDirectJumps(final HashSet<BasicBlockBuilder> visited) {
	if (visited.add(this)) {
	    while (true) {
		if (this.jumpsOut.size() == 1) {
		    final BasicBlockBuilder target = onlyElement(this.jumpsOut);
		    if (target.jumpsIn.size() == 1 && target != this) {
			if (!target.jumpsIn.contains(this)) {
			    throw new IllegalStateException();
			}
			Node.merge(this.terminator, target.inputNode);
			this.terminator = target.terminator;
			this.jumpsOut = target.jumpsOut;
			for (final BasicBlockBuilder newTarget : this.jumpsOut) {
			    if (!newTarget.jumpsIn.remove(target)) {
				throw new IllegalStateException();
			    }
			    if (!newTarget.jumpsIn.add(this)) {
				throw new IllegalStateException();
			    }
			}
			continue;
		    }
		}
		break;
	    }
	    for (final BasicBlockBuilder target : this.jumpsOut) {
		target.removeDirectJumps(visited);
	    }
	}
    }

    public void removeDirectStackWrites() {
	for (final BasicBlockBuilder block : getAllTargetBlocks()) {
	    for (final Node source : block.inputNode.getAllTargetNodes()) {
		if (source.getType() == NodeType.PUSH) {
		    final Set<? extends InputPort> targets = Iterables.getOnlyElement(source.getOutput()).getTargets();
		    if (targets.size() == 1) {
			final InputPort target = Iterables.getOnlyElement(targets);
			if (target.getNode().getType() == NodeType.POP) {
			    Node.merge(source, target.getNode());
			}
		    }
		}
	    }
	}
    }

    public void dump(final PrintStream out, final String name) {
	for (final BasicBlockBuilder block : getAllTargetBlocks()) {
	    out.print("    " + quote(block.inputNode.getNodeId()) + " [");
	    out.print("shape = \"record\" label = \"" + block.getBlockId() + " init");
	    out.print("|{out");
	    for (final OutputPort port : block.inputNode.getOutput()) {
		out.print("|<" + port.getId() + "> #" + port.getId());
	    }
	    out.println("}\"];");
	    out.print("    " + quote(block.getBlockId()) + " -> " + quote(block.inputNode.getNodeId()));

	    for (final Node node : block.inputNode.getAllTargetNodes()) {
		out.print("    \"" + node.getNodeId() + "\" [");
		out.print("shape = \"record\" label = \"");
		out.print("{in");
		for (int i = 0; i < node.getInput().size(); i++) {
		    out.format("|<%s> #%d", node.getInput().get(i).getId(), i);
		}
		out.print("}|");
		out.print(node.description.toString().replace('|', '_').replace('"', ' ').replace('}', '_')
			.replace('{', '_').replace('$', ' '));
		out.print("|");
		out.print("{out");
		for (int i = 0; i < node.getOutput().size(); i++) {
		    out.format("|<%s> #%d", node.getOutput().get(i).getId(), i);
		}
		out.println("}\"];");
	    }

	    final Set<Node> nodes = new HashSet<>(block.inputNode.getAllTargetNodes());
	    nodes.add(block.inputNode);
	    for (final Node node : nodes) {
		for (final OutputPort sourcePort : node.getOutput()) {
		    for (final InputPort targetPort : sourcePort.getTargets()) {
			out.println(sourcePort.getNode().getNodeId() + ":" + sourcePort.getId() + " -> "
				+ targetPort.getNode().getNodeId() + ":" + targetPort.getId());
		    }
		}
	    }
	}
	for (final BasicBlockBuilder source : getAllTargetBlocks()) {
	    for (final BasicBlockBuilder target : source.jumpsOut) {
		out.println(source.terminator.getId() + " -> " + target.getBlockId() + ";");
	    }
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
}
