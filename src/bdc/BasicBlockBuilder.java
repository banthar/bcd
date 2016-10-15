package bdc;

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

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
	List<T> print(Operation operation);
    }

    interface Register {
	<T> T dump(InstructionPrinter<T> printer);
    }

    class Operation {

	private List<Object> description;
	private final List<Register> output;
	private final List<Register> input;

	public Operation(final List<Object> description, final int outputs, final List<Register> input) {
	    this.description = description;
	    this.input = input;
	    this.output = new ArrayList<>();
	    for (int i = 0; i < outputs; i++) {
		final int n = i;
		this.output.add(new Register() {
		    @Override
		    public <T> T dump(final InstructionPrinter<T> printer) {
			return printer.print(Operation.this).get(n);
		    }
		});
	    }
	}

	public Operation(final List<Object> description, final int outputs, final Register... input) {
	    this(description, outputs, Arrays.asList(input));
	}

	public Register getInput(final int n) {
	    return this.input.get(n);
	}

	public Register getOutput(final int n) {
	    return this.output.get(n);
	}

	public int getOutputSize() {
	    return this.output.size();
	}

	public int getInputSize() {
	    return this.input.size();
	}

    }

    class Constant extends Operation {
	public Constant(final Type type, final Object value) {
	    super(Arrays.asList("const", type, value), 1, Collections.emptyList());
	}
    }

    interface Terminator {
	Iterable<? extends BasicBlockBuilder> targets();

	List<Register> getInput();

	List<? extends Object> getDescription();
    }

    private final List<BasicBlockBuilder> sources = new ArrayList<>();
    private Register environment = new Register() {
	@Override
	public <T> T dump(final InstructionPrinter<T> printer) {
	    return null;
	}
    };
    private Terminator terminator = null;

    public void putLocal(final int id, final Register value) {
	final Operation operation = new Operation(Arrays.asList("store_local", id), 1, this.environment, value);
	this.environment = operation.getOutput(0);
    }

    public Register getLocal(final int id) {
	final Operation operation = new Operation(Arrays.asList("load_local", id), 1, this.environment);
	return operation.getOutput(0);
    }

    public Register pop() {
	final Operation operation = new Operation(Arrays.asList("pop"), 2, this.environment);
	this.environment = operation.getOutput(0);
	return operation.getOutput(1);

    }

    public void push(final Register value) {
	this.environment = new Operation(Arrays.asList("push"), 1, this.environment, value).getOutput(0);
    }

    public Register nullConstant() {
	return new Constant(PrimitiveType.Reference, null).getOutput(0);
    }

    public Register integerConstant(final int value) {
	return new Constant(PrimitiveType.Integer, value).getOutput(0);
    }

    public Register longConstant(final long value) {
	return new Constant(PrimitiveType.Long, value).getOutput(0);
    }

    public Register floatConstant(final float value) {
	return new Constant(PrimitiveType.Float, value).getOutput(0);
    }

    public Register doubleConstant(final double value) {
	return new Constant(PrimitiveType.Double, value).getOutput(0);
    }

    public Register stringConstant(final String value) {
	return new Constant(Type.string(), value).getOutput(0);
    }

    public Register binaryOperation(final PrimitiveType type, final BinaryOperationType op, final Register left,
	    final Register right) {
	return new Operation(Arrays.asList("binary_operation", type, op), 1, left, right).getOutput(0);
    }

    public Register negate(final PrimitiveType type, final Register value) {
	return new Operation(Arrays.asList("negate", type), 1, value).getOutput(0);
    }

    public Register shift(final PrimitiveType type, final ShiftType shiftType, final Register left,
	    final Register right) {
	return new Operation(Arrays.asList("shift", type, shiftType), 1, left, right).getOutput(0);
    }

    public Register bitwiseOperation(final PrimitiveType type, final BitwiseOperationType operation,
	    final Register left, final Register right) {
	return new Operation(Arrays.asList("bitwise_operation", type, operation), 1, left, right).getOutput(0);
    }

    public Register convert(final PrimitiveType from, final PrimitiveType to, final Register value) {
	return new Operation(Arrays.asList("convert", from, to), 1, value).getOutput(0);
    }

    public Register compare(final PrimitiveType type, final Register left, final Register right) {
	return new Operation(Arrays.asList("compare", type), 1, left, right).getOutput(0);
    }

    public Register loadElement(final PrimitiveType elementType, final Register arrayref, final Register index) {
	final Operation operation = new Operation(Arrays.asList("load_element", elementType), 1, this.environment,
		arrayref, index);
	return operation.getOutput(0);
    }

    public void storeElement(final Register arrayref, final Register index) {
	final Operation operation = new Operation(Arrays.asList("store_element"), 1, this.environment, arrayref, index);
	this.environment = operation.getOutput(0);
    }

    public Register checkedCast(final ClassReference type, final Register value) {
	final Operation operation = new Operation(Arrays.asList("checked_cast", type), 1, value);
	return operation.getOutput(0);
    }

    public Register instanceOf(final ClassReference type, final Register value) {
	final Operation operation = new Operation(Arrays.asList("instance_of", type), 1, value);
	return operation.getOutput(0);
    }

    public void monitorEnter(final Register monitor) {
	final Operation operation = new Operation(Arrays.asList("monitor_enter"), 1, monitor);
	this.environment = operation.getInput(0);
    }

    public void monitorExit(final Register monitor) {
	final Operation operation = new Operation(Arrays.asList("monitor_exit"), 1, monitor);
	this.environment = operation.getInput(0);
    }

    public Register loadStaticField(final FieldReference fieldReference) {
	final Operation operation = new Operation(Arrays.asList("load_static_field", fieldReference), 1,
		this.environment);
	return operation.getOutput(0);
    }

    public void storeStaticField(final FieldReference fieldReference, final Register value) {
	final Operation operation = new Operation(Arrays.asList("store_static_field", fieldReference), 1,
		this.environment, value);
	this.environment = operation.getOutput(0);
    }

    public Register loadField(final FieldReference fieldReference, final Register target) {
	final Operation operation = new Operation(Arrays.asList("load_static_field", fieldReference), 1,
		this.environment, target);
	return operation.getOutput(0);
    }

    public void storeField(final FieldReference fieldReference, final Register target, final Register value) {
	final Operation operation = new Operation(Arrays.asList("store_field", fieldReference), 1, this.environment,
		target, value);
	this.environment = operation.getOutput(0);
    }

    public List<Register> invokeVirtual(final MethodReference methodReference, final Register target,
	    final List<Register> args) {
	return invoke("invoke_virtual", methodReference, target, args);
    }

    private List<Register> invoke(final String type, final MethodReference methodReference, final Register target,
	    final List<Register> args) {
	final MethodType methodType = methodReference.getType();
	final List<Register> input = new ArrayList<>();
	input.add(this.environment);
	if (target != null) {
	    input.add(target);
	}
	input.addAll(args);
	final Operation operation = new Operation(Arrays.asList(type, methodReference), 1, input);
	this.environment = operation.getOutput(0);
	if (methodType.isVoid()) {
	    return Collections.emptyList();
	} else {
	    return Arrays.asList(operation.getOutput(1));
	}
    }

    public List<Register> invokeSpecial(final MethodReference methodReference, final Register target,
	    final List<Register> args) {
	return invoke("invoke_special", methodReference, target, args);
    }

    public List<Register> invokeStatic(final MethodReference methodReference, final List<Register> args) {
	return invoke("invoke_static", methodReference, null, args);

    }

    public List<Register> invokeInterface(final MethodReference methodReference, final Register target,
	    final List<Register> args) {
	return invoke("invoke_interface", methodReference, target, args);

    }

    public Register newInstance(final ClassReference classReference) {
	final Operation operation = new Operation(Arrays.asList("new_instance", classReference), 2, this.environment);
	this.environment = operation.getOutput(0);
	return operation.getOutput(1);
    }

    public Register newPrimitiveArray(final PrimitiveType type, final Register size) {
	final Operation operation = new Operation(Arrays.asList("new_primitive_array", type), 2, this.environment,
		size);
	this.environment = operation.getOutput(0);
	return operation.getOutput(1);
    }

    public Register newArray(final ClassReference type, final Register size) {
	final Operation operation = new Operation(Arrays.asList("new_array", type), 2, this.environment, size);
	this.environment = operation.getOutput(0);
	return operation.getOutput(1);

    }

    public Register arrayLength(final Register array) {
	final Operation operation = new Operation(Arrays.asList("array_length"), 1, this.environment, array);
	return operation.getOutput(0);
    }

    class ReturnValue implements Terminator {

	private final List<? extends Object> description;
	private final List<Register> input;

	public ReturnValue(final Register state, final PrimitiveType type, final Register ref) {
	    this.description = Arrays.asList("return_value", type);
	    this.input = Arrays.asList(state, ref);
	}

	@Override
	public Iterable<? extends BasicBlockBuilder> targets() {
	    return Collections.emptyList();
	}

	@Override
	public List<? extends Object> getDescription() {
	    return this.description;
	}

	@Override
	public List<Register> getInput() {
	    return this.input;
	}

    }

    public void returnValue(final PrimitiveType type, final Register ref) {
	terminate(new ReturnValue(this.environment, type, ref));
    }

    class ReturnVoid implements Terminator {

	private final List<? extends Object> description;
	private final List<Register> input;

	public ReturnVoid(final Register state) {
	    this.description = Arrays.asList("return_void");
	    this.input = Arrays.asList(state);
	}

	@Override
	public Iterable<? extends BasicBlockBuilder> targets() {
	    return Collections.emptyList();
	}

	@Override
	public List<? extends Object> getDescription() {
	    return this.description;
	}

	@Override
	public List<Register> getInput() {
	    return this.input;
	}
    }

    public void returnVoid() {
	terminate(new ReturnVoid(this.environment));
    }

    class JumpIf implements Terminator {
	private final List<? extends Object> description;
	private final List<Register> input;

	private final BasicBlockBuilder then;
	private final BasicBlockBuilder otherwise;

	public JumpIf(final Register state, final PrimitiveType type, final BasicBlockBuilder then,
		final BasicBlockBuilder otherwise, final Register left, final CompareType compareType,
		final Register right) {
	    this.description = Arrays.asList("jump_if", type, compareType);
	    this.input = Arrays.asList(state, left, right);
	    this.then = then;
	    this.otherwise = otherwise;

	}

	@Override
	public Iterable<? extends BasicBlockBuilder> targets() {
	    return Arrays.asList(this.then, this.otherwise);
	}

	@Override
	public List<? extends Object> getDescription() {
	    return this.description;
	}

	@Override
	public List<Register> getInput() {
	    return this.input;
	}
    }

    public void jumpIf(final PrimitiveType type, final BasicBlockBuilder then, final BasicBlockBuilder otherwise,
	    final CompareType compareType, final Register left, final Register right) {
	terminate(new JumpIf(this.environment, type, then, otherwise, left, compareType, right));
    }

    public void jumpTable(final Register value, final int defaultOffset, final Map<Integer, Integer> table) {
	throw new IllegalStateException();
    }

    class Jump implements Terminator {
	private final List<? extends Object> description;
	private final List<Register> input;
	private final BasicBlockBuilder target;

	public Jump(final Register state, final BasicBlockBuilder target) {
	    this.description = Arrays.asList("jump");
	    this.input = Arrays.asList(state);
	    this.target = target;
	}

	@Override
	public Iterable<? extends BasicBlockBuilder> targets() {
	    return Arrays.asList(this.target);
	}

	@Override
	public List<? extends Object> getDescription() {
	    return this.description;
	}

	@Override
	public List<Register> getInput() {
	    return this.input;
	}

    }

    public void jump(final BasicBlockBuilder target) {
	terminate(new Jump(this.environment, target));
    }

    class ReturnError implements Terminator {

	private final List<? extends Object> description;
	private final List<Register> input;

	public ReturnError(final Register state, final Register exception) {
	    this.description = Arrays.asList("return_error");
	    this.input = Arrays.asList(state, exception);
	}

	@Override
	public Iterable<? extends BasicBlockBuilder> targets() {
	    return Collections.emptyList();
	}

	@Override
	public List<? extends Object> getDescription() {
	    return this.description;
	}

	@Override
	public List<Register> getInput() {
	    return this.input;
	}
    }

    public void returnError(final Register exception) {
	terminate(new ReturnError(this.environment, exception));
    }

    private void terminate(final Terminator terminator) {
	if (this.terminator != null || this.environment == null) {
	    throw new IllegalStateException();
	}
	for (final BasicBlockBuilder target : terminator.targets()) {
	    target.addSource(this);
	}
	this.terminator = terminator;
	this.environment = null;
    }

    private void addSource(final BasicBlockBuilder source) {
	this.sources.add(source);
    }

    public Iterable<BasicBlockBuilder> getSources() {
	return this.sources;
    }

    public boolean isTerminated() {
	return this.terminator != null;
    }

    public void dump(final PrintStream out) {
	int n = 0;
	final HashMap<BasicBlockBuilder, Integer> printed = new HashMap<>();
	final Deque<BasicBlockBuilder> toPrint = new ArrayDeque<>();
	toPrint.add(this);
	printed.put(this, n++);
	while (!toPrint.isEmpty()) {
	    final BasicBlockBuilder block = toPrint.removeFirst();
	    out.print("  \"block" + printed.get(block) + "\" [");
	    out.print("shape = \"record\" label = \"<start> #" + printed.get(block));
	    out.print("|");

	    final InstructionPrinter<Integer> instructionPrinter = new InstructionPrinter<Integer>() {
		private final IdentityHashMap<Operation, List<Integer>> map = new IdentityHashMap<>();
		private int n = 0;

		@Override
		public List<Integer> print(final Operation operation) {
		    final List<Integer> registers = this.map.get(operation);
		    if (registers != null) {
			return registers;
		    } else {
			final List<Integer> inputRegisters = new ArrayList<>();
			for (int i = 0; i < operation.getInputSize(); i++) {
			    inputRegisters.add(operation.getInput(i).dump(this));
			}
			final List<Integer> outputRegisters = new ArrayList<>();
			for (int i = 0; i < operation.getOutputSize(); i++) {
			    outputRegisters.add(this.n++);
			}
			out.print(outputRegisters);
			out.print(" = ");
			out.print(operation.description);
			out.print(" ");
			out.print(inputRegisters);
			out.print("|");
			this.map.put(operation, outputRegisters);
			return outputRegisters;
		    }
		}
	    };
	    final List<Integer> inputRegisters = new ArrayList<>();
	    for (final Register in : block.terminator.getInput()) {
		inputRegisters.add(in.dump(instructionPrinter));
	    }
	    out.print("<end> ");
	    out.print(block.terminator.getDescription());
	    out.print(" ");
	    out.print(inputRegisters);
	    for (final BasicBlockBuilder opcode : block.terminator.targets()) {
		out.print(" ");
		Integer blockId = printed.get(opcode);
		if (blockId == null) {
		    blockId = n++;
		    toPrint.add(opcode);
		}
		printed.put(opcode, blockId);
		out.print(blockId);
	    }
	    out.println("\"];");
	}
	for (final BasicBlockBuilder bbb : printed.keySet()) {
	    for (final BasicBlockBuilder opcode : bbb.terminator.targets()) {
		out.println(
			" \"block" + printed.get(bbb) + "\":end -> " + "\"block" + printed.get(opcode) + "\":start;");
	    }

	}
    }
}
