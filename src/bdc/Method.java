package bdc;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import bdc.ConstantPool.ClassReference;
import bdc.ConstantPool.MethodReference;
import bdc.Node.NodeType;
import bdc.Type.MethodType;
import bdc.Type.PrimitiveType;

public class Method {

	private final URLClassParser bytecodeLoader;
	private final ConstantPool constantPool;
	private final ClassReference selfType;
	private final int accessFlags;
	private final String name;
	private final String descriptor;
	private final byte[] code;
	private final List<ClassReference> exceptions;
	private final String signature;
	private BasicBlockBuilder block;

	public Method(final URLClassParser bytecodeLoader, final ConstantPool constantPool, final ClassReference selfType,
			final int accessFlags, final String name, final String descriptor, final byte[] code,
			final List<ClassReference> exceptions, final String signature) {
		this.bytecodeLoader = bytecodeLoader;
		this.constantPool = constantPool;
		this.selfType = selfType;
		this.accessFlags = accessFlags;
		this.name = name;
		this.descriptor = descriptor;
		this.code = code;
		this.exceptions = exceptions;
		this.signature = signature;
	}

	public String getDescriptor() {
		return this.descriptor;
	}

	public String getName() {
		return this.name;
	}

	public void parse() throws IOException, ClassFormatException {
		if (this.block != null) {
			return;
		}
		if (this.code == null) {
			System.err.println("Abstract method: " + this.selfType + " " + this.name);
			return;
		}
		final DataInputStream dataInput = new DataInputStream(new ByteArrayInputStream(this.code));
		final BasicBlockBuilder block = InstructionParser.parseCode(dataInput, this.constantPool,
				this.selfType.getType(), MethodType.fromDescriptor(this.descriptor));
		if (dataInput.read() != -1) {
			throw new ClassFormatException("Extra bytes at end of method code");
		}
		do {
			BlockTransformations.removeDeadBlocks(block);
			BlockTransformations.removeDirectJumps(block);
			BlockTransformations.removeDirectStackWrites(block);
		} while (BlockTransformations.propagateConstants(block));
		this.block = block;
		resolve();
	}

	public void resolve() throws IOException, ClassFormatException {
		for (final BasicBlockBuilder block : this.block.getAllTargetBlocks()) {
			for (final Node node : block.inputNode.getAllLinkedNodes()) {
				if (node.getType() == NodeType.INVOKE) {
					final MethodReference reference = (MethodReference) node.getData();
					try {
						final Method method = this.bytecodeLoader.loadClass(reference.getOwnerClass().getName())
								.getMethod(reference.getName(), reference.getDescriptor());
						method.parse();
					} catch (final ClassNotFoundException | NoSuchMethodException e) {
						System.err.println(e);
					}
				}
			}
		}
	}

	public void dump(final PrintStream out) {
		if (this.block != null) {
			final String methodName = (this.selfType.getName() + "_" + getName()).replace('<', '_').replace('>', '_')
					.replace('$', '_').replace('/', '_');
			out.println("  subgraph cluster_" + methodName + " {");
			out.println("    label=\"" + getName() + "\";");
			this.block.dump(out, methodName);
			out.println("  }");
		}
	}

	public BasicBlockBuilder getBlock() {
		return this.block;
	}

	@Override
	public String toString() {
		return "Method [accessFlags=" + this.accessFlags + ", name=" + this.name + ", descriptor=" + this.descriptor
				+ ", code=" + Arrays.toString(this.code) + ", exceptions=" + this.exceptions + ", signature="
				+ this.signature + "]";
	}

	public void assertReturnsConstant(final Object expectedValue) {
		if (this.block.terminator.getType() != NodeType.TERMINATOR) {
			throw new AssertionError("Expected straight return not: " + this.block.terminator);
		}
		if (!this.block.jumpsOut.isEmpty()) {
			throw new AssertionError("Expected no out blocks");
		}
		if (this.block.terminator.getAllInputPorts().size() != 2) {
			throw new IllegalStateException("Terminator node should have exactly one input: " + this.block.terminator
					+ ": " + this.block.terminator.getAllInputPorts());
		}

		final Node environmentSource = this.block.terminator.getInput(PortId.environment()).getSource().getNode();
		if (environmentSource.getType() != NodeType.INIT) {
			throw new IllegalStateException("Method modifies environment: " + environmentSource);
		}
		final Node sourceNode = this.block.terminator.getInput(PortId.arg(0)).getSource().getNode();
		if (sourceNode.getData() instanceof LoadConstantOperation) {
			final LoadConstantOperation data = (LoadConstantOperation) sourceNode.getData();
			if (data.getType() != PrimitiveType.Integer) {
				throw new IllegalStateException();
			}
			final Object actualValue = data.getValue();
			if (!isEqual(actualValue, expectedValue)) {
				throw new AssertionError("expected: " + toString(expectedValue) + " but was " + toString(actualValue));
			}
		} else {
			throw new IllegalStateException();
		}

	}

	private String toString(final Object value) {
		if (value == null) {
			return "null";
		} else {
			return value.getClass().getSimpleName() + "(" + value + ")";
		}
	}

	private boolean isEqual(final Object actualValue, final Object expectedValue) {
		final Object registerValue;
		if (expectedValue instanceof Boolean) {
			registerValue = ((Boolean) expectedValue).booleanValue() ? 1 : 0;
		} else {
			registerValue = expectedValue;
		}
		return Objects.equals(actualValue, registerValue);
	}

}
