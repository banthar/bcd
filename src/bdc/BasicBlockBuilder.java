package bdc;

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import bdc.ConstantPool.ClassReference;
import bdc.ConstantPool.FieldReference;
import bdc.ConstantPool.MethodReference;
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

	T blockInput();
    }

    interface Port {
	<T> T dump(InstructionPrinter<T> printer);

	Node getNode();
    }

    interface InputNode {
	default Port getInput(final int n) {
	    return getInput().get(n);
	}

	default int getInputSize() {
	    return getInput().size();
	}

	List<Port> getInput();

    }

    interface OutputNode {
	default int getOutputSize() {
	    return getOutput().size();
	}

	default Port getOutput(final int n) {
	    return getOutput().get(n);
	}

	List<Port> getOutput();

    }

    class Node implements InputNode, OutputNode {

	private List<Object> description;
	private final List<Port> output;
	private final List<Port> input;

	public Node(final List<Object> description, final int outputs, final List<Port> input) {
	    this.description = description;
	    this.input = input;
	    this.output = new ArrayList<>();
	    for (int i = 0; i < outputs; i++) {
		final int n = i;
		this.output.add(new Port() {
		    @Override
		    public <T> T dump(final InstructionPrinter<T> printer) {
			return printer.print(Node.this).get(n);
		    }

		    @Override
		    public Node getNode() {
			return Node.this;
		    }
		});
	    }
	}

	public Node(final List<Object> description, final int outputs, final Port... input) {
	    this(description, outputs, Arrays.asList(input));
	}

	@Override
	public List<Port> getInput() {
	    return this.input;
	}

	@Override
	public List<Port> getOutput() {
	    return this.output;
	}
    }

    class Constant extends Node {
	public Constant(final Type type, final Object value) {
	    super(Arrays.asList("const", type, value), 1, Collections.emptyList());
	}
    }

    interface Terminator extends InputNode {
	List<? extends Object> getDescription();
    }

    class RegisterReference implements Port {
	Port target;

	@Override
	public <T> T dump(final InstructionPrinter<T> printer) {
	    if (this.target != null) {
		return this.target.dump(printer);
	    } else {
		return printer.blockInput();
	    }
	}

	@Override
	public Node getNode() {
	    if (this.target != null) {
		return this.target.getNode();
	    } else {
		return null;
	    }
	}
    }

    private final RegisterReference inputEnvironment = new RegisterReference();
    private Port environment = this.inputEnvironment;
    private Terminator terminator = null;
    private Set<BasicBlockBuilder> jumpsOut = new HashSet<>();
    private final Set<BasicBlockBuilder> jumpsIn = new HashSet<>();

    public void putLocal(final int id, final Port value) {
	final Node operation = new Node(Arrays.asList("store_local", id), 1, this.environment, value);
	this.environment = operation.getOutput(0);
    }

    public Port getLocal(final int id) {
	final Node operation = new Node(Arrays.asList("load_local", id), 1, this.environment);
	return operation.getOutput(0);
    }

    public Port pop() {
	final Node operation = new Node(Arrays.asList("pop"), 2, this.environment);
	this.environment = operation.getOutput(0);
	return operation.getOutput(1);

    }

    public void push(final Port value) {
	this.environment = new Node(Arrays.asList("push"), 1, this.environment, value).getOutput(0);
    }

    public Port nullConstant() {
	return new Constant(PrimitiveType.Reference, null).getOutput(0);
    }

    public Port integerConstant(final int value) {
	return new Constant(PrimitiveType.Integer, value).getOutput(0);
    }

    public Port longConstant(final long value) {
	return new Constant(PrimitiveType.Long, value).getOutput(0);
    }

    public Port floatConstant(final float value) {
	return new Constant(PrimitiveType.Float, value).getOutput(0);
    }

    public Port doubleConstant(final double value) {
	return new Constant(PrimitiveType.Double, value).getOutput(0);
    }

    public Port stringConstant(final String value) {
	return new Constant(Type.string(), value).getOutput(0);
    }

    public Port binaryOperation(final PrimitiveType type, final BinaryOperationType op, final Port left,
	    final Port right) {
	return new Node(Arrays.asList("binary_operation", type, op), 1, left, right).getOutput(0);
    }

    public Port negate(final PrimitiveType type, final Port value) {
	return new Node(Arrays.asList("negate", type), 1, value).getOutput(0);
    }

    public Port shift(final PrimitiveType type, final ShiftType shiftType, final Port left, final Port right) {
	return new Node(Arrays.asList("shift", type, shiftType), 1, left, right).getOutput(0);
    }

    public Port bitwiseOperation(final PrimitiveType type, final BitwiseOperationType operation, final Port left,
	    final Port right) {
	return new Node(Arrays.asList("bitwise_operation", type, operation), 1, left, right).getOutput(0);
    }

    public Port convert(final PrimitiveType from, final PrimitiveType to, final Port value) {
	return new Node(Arrays.asList("convert", from, to), 1, value).getOutput(0);
    }

    public Port compare(final PrimitiveType type, final Port left, final Port right) {
	return new Node(Arrays.asList("compare", type), 1, left, right).getOutput(0);
    }

    public Port loadElement(final PrimitiveType elementType, final Port arrayref, final Port index) {
	final Node operation = new Node(Arrays.asList("load_element", elementType), 1, this.environment, arrayref,
		index);
	return operation.getOutput(0);
    }

    public void storeElement(final Port arrayref, final Port index) {
	final Node operation = new Node(Arrays.asList("store_element"), 1, this.environment, arrayref, index);
	this.environment = operation.getOutput(0);
    }

    public Port checkedCast(final ClassReference type, final Port value) {
	final Node operation = new Node(Arrays.asList("checked_cast", type), 1, value);
	return operation.getOutput(0);
    }

    public Port instanceOf(final ClassReference type, final Port value) {
	final Node operation = new Node(Arrays.asList("instance_of", type), 1, value);
	return operation.getOutput(0);
    }

    public void monitorEnter(final Port monitor) {
	final Node operation = new Node(Arrays.asList("monitor_enter"), 1, monitor);
	this.environment = operation.getInput(0);
    }

    public void monitorExit(final Port monitor) {
	final Node operation = new Node(Arrays.asList("monitor_exit"), 1, monitor);
	this.environment = operation.getInput(0);
    }

    public Port loadStaticField(final FieldReference fieldReference) {
	final Node operation = new Node(Arrays.asList("load_static_field", fieldReference), 1, this.environment);
	return operation.getOutput(0);
    }

    public void storeStaticField(final FieldReference fieldReference, final Port value) {
	final Node operation = new Node(Arrays.asList("store_static_field", fieldReference), 1, this.environment,
		value);
	this.environment = operation.getOutput(0);
    }

    public Port loadField(final FieldReference fieldReference, final Port target) {
	final Node operation = new Node(Arrays.asList("load_static_field", fieldReference), 1, this.environment,
		target);
	return operation.getOutput(0);
    }

    public void storeField(final FieldReference fieldReference, final Port target, final Port value) {
	final Node operation = new Node(Arrays.asList("store_field", fieldReference), 1, this.environment, target,
		value);
	this.environment = operation.getOutput(0);
    }

    public List<Port> invokeVirtual(final MethodReference methodReference, final Port target, final List<Port> args) {
	return invoke("invoke_virtual", methodReference, target, args);
    }

    private List<Port> invoke(final String type, final MethodReference methodReference, final Port target,
	    final List<Port> args) {
	final MethodType methodType = methodReference.getType();
	final List<Port> input = new ArrayList<>();
	input.add(this.environment);
	if (target != null) {
	    input.add(target);
	}
	input.addAll(args);
	final Node operation = new Node(Arrays.asList(type, methodReference), methodType.isVoid() ? 1 : 2, input);
	this.environment = operation.getOutput(0);
	if (methodType.isVoid()) {
	    return Collections.emptyList();
	} else {
	    return Arrays.asList(operation.getOutput(1));
	}
    }

    public List<Port> invokeSpecial(final MethodReference methodReference, final Port target, final List<Port> args) {
	return invoke("invoke_special", methodReference, target, args);
    }

    public List<Port> invokeStatic(final MethodReference methodReference, final List<Port> args) {
	return invoke("invoke_static", methodReference, null, args);

    }

    public List<Port> invokeInterface(final MethodReference methodReference, final Port target, final List<Port> args) {
	return invoke("invoke_interface", methodReference, target, args);

    }

    public Port newInstance(final ClassReference classReference) {
	final Node operation = new Node(Arrays.asList("new_instance", classReference), 2, this.environment);
	this.environment = operation.getOutput(0);
	return operation.getOutput(1);
    }

    public Port newPrimitiveArray(final PrimitiveType type, final Port size) {
	final Node operation = new Node(Arrays.asList("new_primitive_array", type), 2, this.environment, size);
	this.environment = operation.getOutput(0);
	return operation.getOutput(1);
    }

    public Port newArray(final ClassReference type, final Port size) {
	final Node operation = new Node(Arrays.asList("new_array", type), 2, this.environment, size);
	this.environment = operation.getOutput(0);
	return operation.getOutput(1);

    }

    public Port arrayLength(final Port array) {
	final Node operation = new Node(Arrays.asList("array_length"), 1, this.environment, array);
	return operation.getOutput(0);
    }

    class ReturnValue implements Terminator {

	private final List<? extends Object> description;
	private final List<Port> input;

	public ReturnValue(final Port state) {
	    this.description = Arrays.asList("return_void");
	    this.input = Arrays.asList(state);
	}

	public ReturnValue(final Port state, final PrimitiveType type, final Port ref) {
	    this.description = Arrays.asList("return_value", type);
	    this.input = Arrays.asList(state, ref);
	}

	public ReturnValue(final Port state, final Port exception) {
	    this.description = Arrays.asList("return_error");
	    this.input = Arrays.asList(state, exception);
	}

	@Override
	public List<? extends Object> getDescription() {
	    return this.description;
	}

	@Override
	public List<Port> getInput() {
	    return this.input;
	}

    }

    public void returnValue(final PrimitiveType type, final Port ref) {
	terminate(new ReturnValue(this.environment, type, ref));
    }

    public void returnVoid() {
	terminate(new ReturnValue(this.environment));
    }

    class JumpIf implements Terminator {
	private final List<? extends Object> description;
	private final List<Port> input;

	public JumpIf(final Port state, final PrimitiveType type, final Port left, final CompareType compareType,
		final Port right) {
	    this.description = Arrays.asList("jump_if", type, compareType);
	    this.input = Arrays.asList(state, left, right);

	}

	@Override
	public List<? extends Object> getDescription() {
	    return this.description;
	}

	@Override
	public List<Port> getInput() {
	    return this.input;
	}
    }

    private void referenceTo(final BasicBlockBuilder target) {
	this.jumpsOut.add(target);
	target.jumpsIn.add(this);
    }

    public void jumpIf(final PrimitiveType type, final BasicBlockBuilder then, final BasicBlockBuilder otherwise,
	    final CompareType compareType, final Port left, final Port right) {
	referenceTo(then);
	referenceTo(otherwise);
	terminate(new JumpIf(this.environment, type, left, compareType, right));
    }

    public void jumpTable(final Port value, final int defaultOffset, final Map<Integer, Integer> table) {
	throw new IllegalStateException();
    }

    class Jump implements Terminator {
	private final List<? extends Object> description;
	private final List<Port> input;

	public Jump(final Port state) {
	    this.description = Arrays.asList("jump");
	    this.input = Arrays.asList(state);
	}

	@Override
	public List<? extends Object> getDescription() {
	    return this.description;
	}

	@Override
	public List<Port> getInput() {
	    return this.input;
	}

    }

    public void jump(final BasicBlockBuilder target) {
	referenceTo(target);
	terminate(new Jump(this.environment));
    }

    public void returnError(final Port exception) {
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
	final Iterator<T> iterator = iterable.iterator();
	if (!iterator.hasNext()) {
	    throw new IllegalStateException();
	}
	final T t = iterator.next();
	if (iterator.hasNext()) {
	    throw new IllegalStateException();
	}
	return t;
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
			target.inputEnvironment.target = onlyElement(this.terminator.getInput());
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

    public void removeStack() {
	final Set<BasicBlockBuilder> visited = new HashSet<>();
	removeStack(visited);
    }

    private void removeStack(final Set<BasicBlockBuilder> visited) {
	if (visited.add(this)) {
	    removeStack(this.terminator);
	    for (final BasicBlockBuilder block : this.jumpsOut) {
		block.removeStack(visited);
	    }
	}
    }

    private void removeStack(final InputNode node) {
	for (final Port r : node.getInput()) {
	    final Node op = r.getNode();
	    if (op != null) {
		removeStack(op);
	    }
	}
    }

    public void dump(final PrintStream out, final String name) {
	final Set<BasicBlockBuilder> printed = new HashSet<>();
	final Deque<BasicBlockBuilder> toPrint = new ArrayDeque<>();
	toPrint.add(this);
	printed.add(this);
	while (!toPrint.isEmpty()) {
	    final BasicBlockBuilder block = toPrint.removeFirst();

	    out.print("    \"block" + block.hashCode() + "\" [");
	    out.print("shape = \"record\" label = \"");
	    out.print("<start> block #" + block.hashCode());
	    out.println("|{<0> 0}\"];");

	    final InstructionPrinter<String> instructionPrinter = new InstructionPrinter<String>() {
		private final IdentityHashMap<Node, List<String>> map = new IdentityHashMap<>();

		@Override
		public String blockInput() {
		    return "\"block" + block.hashCode() + "\":0";
		}

		@Override
		public List<String> print(final Node operation) {
		    final List<String> registers = this.map.get(operation);
		    if (registers != null) {
			return registers;
		    } else {
			final List<String> inputRegisters = new ArrayList<>();
			final String operationId = "operation" + operation.hashCode();
			for (int i = 0; i < operation.getInputSize(); i++) {
			    inputRegisters.add(operation.getInput(i).dump(this));
			}
			final List<String> outputRegisters = new ArrayList<>();
			for (int i = 0; i < operation.getOutputSize(); i++) {
			    outputRegisters.add("\"" + operationId + "\":out" + String.valueOf(i));
			}
			out.print("    \"" + operationId + "\" [");
			out.print("shape = \"record\" label = \"");
			out.print("{in");
			for (int i = 0; i < inputRegisters.size(); i++) {
			    out.format("|<in%d> #%d", i, i);
			}
			out.print("}|");
			out.print(operation.description.toString().replace('|', '_').replace('"', ' ').replace('}', '_')
				.replace('{', '_').replace('$', ' '));
			out.print("|");
			out.print("{out");
			for (int i = 0; i < outputRegisters.size(); i++) {
			    out.format("|<out%d> #%d", i, i);
			}
			out.println("}\"];");

			for (int i = 0; i < inputRegisters.size(); i++) {
			    out.println("    " + inputRegisters.get(i) + " -> \"" + operationId + "\":in" + i + ";");
			}

			this.map.put(operation, outputRegisters);
			return outputRegisters;
		    }
		}
	    };

	    final List<String> inputRegisters = new ArrayList<>();
	    for (final Port in : block.terminator.getInput()) {
		inputRegisters.add(in.dump(instructionPrinter));
	    }

	    final String terminatorId = "terminator" + block.terminator.hashCode();
	    out.print("    \"" + terminatorId + "\" [");
	    out.print("shape = \"record\" label = \"");
	    out.print("{in");
	    for (int i = 0; i < inputRegisters.size(); i++) {
		out.format("|<%d> #%d", i, i);
	    }
	    out.print("}|<end> ");
	    out.print(block.terminator.getDescription().toString().replace('|', '_').replace('"', ' ').replace('}', '_')
		    .replace('{', '_').replace('$', ' '));
	    out.println("\"];");

	    for (int i = 0; i < inputRegisters.size(); i++) {
		out.println("    " + inputRegisters.get(i) + " -> \"" + terminatorId + "\":" + i + ";");
	    }

	    for (final BasicBlockBuilder target : block.jumpsOut) {
		if (!printed.contains(target)) {
		    printed.add(target);
		    toPrint.add(target);
		}
	    }
	}
	for (final BasicBlockBuilder source : printed) {
	    for (final BasicBlockBuilder target : source.jumpsOut) {
		out.println("    \"terminator" + source.terminator.hashCode() + "\":end -> \"block" + target.hashCode()
			+ "\":start;");
	    }
	}
    }
}
