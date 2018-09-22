package x86_64;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import bdc.AbstractMethod;
import bdc.BasicBlockBuilder;
import bdc.BinaryOperationType;
import bdc.InputPort;
import bdc.Iterables;
import bdc.LoadConstantOperation;
import bdc.Method;
import bdc.MethodCall;
import bdc.MethodInit;
import bdc.NativeMethod;
import bdc.Node;
import bdc.OutputPort;
import bdc.PortId;
import bdc.PureTransformation;
import bdc.ReturnValues;

public class Codegen {

	private final Set<Method> methods;
	private final Deque<Method> methodsToVisit;
	private final Map<Method, Integer> symbols;
	private final InstructionGenerator instructions;

	public Codegen() {
		this.methods = new HashSet<>();
		this.methodsToVisit = new ArrayDeque<>();
		this.symbols = new HashMap<>();
		this.instructions = new InstructionGenerator(this);
	}

	public static byte[] codegen(final Method method) {
		final Codegen codegen = new Codegen();
		codegen.addMethod(method);
		while (!codegen.methodsToVisit.isEmpty()) {
			codegen.genMethod(codegen.methodsToVisit.remove());
		}
		return codegen.instructions.toBytes();
	}

	private void genMethod(final Method method) {
		this.symbols.put(method, this.instructions.out.position());
		System.out.println(method.getName() + " = " + this.instructions.out.position());
		final BasicBlockBuilder block = method.getBlock();
		final Map<OutputPort, Register> outputRegisters = new HashMap<>();
		allocateArgumentRegisters(outputRegisters, block.getInputNode());
		final Map<InputPort, Register> inputRegisters = new HashMap<>();
		emitNode(this.instructions, block.getTerminator(), inputRegisters, outputRegisters, Collections.emptyMap());
	}

	public void addMethod(final Method method) {
		if (this.methods.add(method)) {
			this.methodsToVisit.add(method);
		}
	}

	private static void allocateArgumentRegisters(final Map<OutputPort, Register> outputRegisters, final Node init) {
		final Iterator<Register> integerRegisters = Arrays
				.asList(Register.EDI, Register.ESI, Register.EDX, Register.ECX, Register.R8, Register.R9).iterator();
		for (final Entry<PortId, ? extends OutputPort> entry : init.getAllOutputPorts().entrySet()) {
			switch (entry.getKey().type) {
			case ENV:
				outputRegisters.put(entry.getValue(), Register.ENV);
				break;
			case LOCAL:
				outputRegisters.put(entry.getValue(), integerRegisters.next());
				break;
			default:
				throw new IllegalStateException("Unsupported port: " + entry.getKey());
			}
		}
	}

	private static void assignInputRegister(final InstructionGenerator instructions,
			final Map<InputPort, Register> inputRegisters, final Map<OutputPort, Register> outputRegisters,
			final InputPort input, final Register register) {
		inputRegisters.put(input, register);
		final Set<Register> registers = getAssignedOutputRegisters(inputRegisters, input);
		if (registers == null) {
			return;
		}
		final Register allocatedInputRegister = Iterables.getOnlyElement(registers);
		final Register allocatedOutputRegister = outputRegisters.get(input.getSource());

		if (allocatedOutputRegister != null) {
			if (!allocatedOutputRegister.equals(allocatedInputRegister)) {
				instructions.move(allocatedInputRegister, allocatedOutputRegister);
				return;
			}
		} else {
			outputRegisters.put(input.getSource(), allocatedInputRegister);
		}

		final Map<OutputPort, Register> sourceNodeOutputRegisters = new HashMap<>();
		final Node sourceNode = input.getSource().getNode();
		for (final OutputPort source : sourceNode.getAllOutputPorts().values()) {
			final Register mappedRegister = outputRegisters.get(source);
			if (mappedRegister == null) {
				return;
			} else {
				sourceNodeOutputRegisters.put(source, mappedRegister);
			}
		}
		emitNode(instructions, sourceNode, inputRegisters, outputRegisters, sourceNodeOutputRegisters);

	}

	private static void emitNode(final InstructionGenerator instructions, final Node sourceNode,
			final Map<InputPort, Register> inputRegisters, final Map<OutputPort, Register> outputRegisters,
			final Map<OutputPort, Register> sourceNodeOutputRegisters) {
		if (sourceNode.getData() instanceof ReturnValues) {
			final InputPort inputRegister = sourceNode.getInput(PortId.arg(0));
			if (inputRegister != null) {
				assignInputRegister(instructions, inputRegisters, outputRegisters, inputRegister, Register.EAX);
			}
			assignInputRegister(instructions, inputRegisters, outputRegisters,
					sourceNode.getInput(PortId.environment()), Register.ENV);
			instructions.functionReturn();
		} else if (sourceNode.getData() instanceof LoadConstantOperation) {
			final Object value = ((LoadConstantOperation) sourceNode.getData()).getValue();
			instructions.loadConstant(sourceNodeOutputRegisters.get(sourceNode.getOutput(PortId.arg(0))),
					(Integer) value);
		} else if (sourceNode.getData() instanceof PureTransformation) {
			final PureTransformation transformation = (PureTransformation) sourceNode.getData();
			final Register destination = sourceNodeOutputRegisters.get(sourceNode.getOutput(PortId.arg(0)));
			assignInputRegister(instructions, inputRegisters, outputRegisters, sourceNode.getInput(PortId.arg(0)),
					destination);
			assignInputRegister(instructions, inputRegisters, outputRegisters, sourceNode.getInput(PortId.arg(1)),
					Register.EBX);
			if (transformation.getOperation() == BinaryOperationType.Add) {
				instructions.add(destination, Register.EBX);
			} else {
				throw new IllegalStateException("Unsupported operation: " + transformation.getOperation());
			}

		} else if (sourceNode.getData() instanceof MethodInit) {
		} else if (sourceNode.getData() instanceof MethodCall) {
			final AbstractMethod method = ((MethodCall) sourceNode.getData()).getMethod();
			if (method instanceof Method) {
				instructions.call((Method) method);
			} else if (method instanceof NativeMethod) {
				assignInputRegister(instructions, inputRegisters, outputRegisters,
						sourceNode.getInput(PortId.environment()), Register.ENV);
				assignInputRegister(instructions, inputRegisters, outputRegisters, sourceNode.getInput(PortId.arg(0)),
						Register.EDI);
				instructions.loadConstant(Register.EAX, 60);
				instructions.syscall();
			} else {
				throw new IllegalStateException("Unknown method type: " + method);
			}
		} else {
			throw new IllegalStateException("Unsupported node: " + sourceNode.getData().getClass());
		}

	}

	private static Set<Register> getAssignedOutputRegisters(final Map<InputPort, Register> inputRegisters,
			final InputPort output) {
		final Set<Register> registers = new HashSet<>();
		for (final InputPort target : output.getSource().getTargets()) {
			final Register mappedRegister = inputRegisters.get(target);
			if (mappedRegister == null) {
				return null;
			} else {
				registers.add(mappedRegister);
			}
		}
		return registers;
	}

}
