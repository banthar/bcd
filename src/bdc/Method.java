package bdc;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

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
		BlockTransformations.removeDirectJumps(block);
		BlockTransformations.removeDirectStackWrites(block);
		BlockTransformations.propagateConstants(block);
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

	public void assertReturnsConstantInt(final int expectedValue) {
		if (this.block.terminator.getType() != NodeType.RETURN) {
			throw new IllegalStateException();
		}
		if (this.block.terminator.getAllInputPorts().size() != 2) {
			throw new IllegalStateException();
		}

		if (this.block.terminator.getInput(PortId.environment()).getSource().getNode().getType() != NodeType.INIT) {
			throw new IllegalStateException();
		}
		final Node sourceNode = this.block.terminator.getInput(PortId.arg(0)).getSource().getNode();
		if (sourceNode.getData() instanceof LoadConstantOperation) {
			final LoadConstantOperation data = (LoadConstantOperation) sourceNode.getData();
			if (data.getType() != PrimitiveType.Integer) {
				throw new IllegalStateException();
			}
			if (data.getValue() instanceof Integer) {
				final int actualValue = ((Integer) data.getValue()).intValue();
				if (actualValue != expectedValue) {
					throw new AssertionError("expected: " + expectedValue + " but was " + actualValue);
				}
			} else {
				throw new IllegalStateException();
			}
		} else {
			throw new IllegalStateException();
		}

	}

}
