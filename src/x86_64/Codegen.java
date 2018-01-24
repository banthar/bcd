package x86_64;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import bdc.BasicBlockBuilder;
import bdc.BinaryOperationType;
import bdc.FunctionTerminator;
import bdc.InputPort;
import bdc.Iterables;
import bdc.LoadConstantOperation;
import bdc.Method;
import bdc.MethodInit;
import bdc.Node;
import bdc.OutputPort;
import bdc.PortId;
import bdc.PureTransformation;

public class Codegen {

	enum Register {
		EAX(0),

		ECX(1),

		EDX(2),

		EBX(3),

		SIB(4),

		IP(5),

		ESI(6),

		EDI(7),

		R8(8),

		R9(9),

		R10(10),

		R11(11),

		SIB2(12),

		IP2(13),

		R14(14),

		R15(15),

		ENV(-1),

		;

		int index;

		private Register(final int index) {
			this.index = index;
		}

		public int getIndex() {
			return this.index;
		}
	}

	static class InstructionGenerator {

		ByteBuffer out = ByteBuffer.allocate(1024);

		public void loadConstant(final Register register, final int value) {
			this.out.put((byte) (0xb8 | register.getIndex()));
			this.out.putInt(value);
		}

		public void functionReturn() {
			this.out.put((byte) 0xc3);
		}

		public void add(final Register destination, final Register addend) {
			this.out.put((byte) 0x01);
			this.out.put((byte) (addend.getIndex() << 3 | destination.getIndex() | 0xc0));
		}

		public void move(final Register target, final Register source) {
			this.out.put((byte) 0x89);
			this.out.put((byte) ((target.getIndex() << 3) + source.getIndex() | 0xc0));
		}

		byte[] toBytes() {
			final byte[] bytes = new byte[this.out.position()];
			this.out.rewind();
			this.out.get(bytes);
			return bytes;
		}

	}

	public static byte[] codegen(final Method method) {
		final BasicBlockBuilder block = method.getBlock();

		final Map<OutputPort, Register> outputRegisters = new HashMap<>();
		allocateArgumentRegisters(outputRegisters, block.getInputNode());

		final Map<InputPort, Register> inputRegisters = new HashMap<>();

		final InstructionGenerator instructions = new InstructionGenerator();

		emitNode(instructions, block.getTerminator(), inputRegisters, outputRegisters, Collections.emptyMap());

		return instructions.toBytes();
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
		if (sourceNode.getData() instanceof FunctionTerminator) {
			assignInputRegister(instructions, inputRegisters, outputRegisters, sourceNode.getInput(PortId.arg(0)),
					Register.EAX);
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
