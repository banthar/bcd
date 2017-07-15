package bdc;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import bdc.ConstantPool.ClassReference;
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
	private final List<Node> callers = new ArrayList<>();

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
		final BasicBlockBuilder block = linkInitBlock(InstructionParser.parseCode(dataInput, this.constantPool,
				this.selfType.getType(), MethodType.fromDescriptor(this.descriptor)));
		if (dataInput.read() != -1) {
			throw new ClassFormatException("Extra bytes at end of method code");
		}
		BlockTransformations.optimizeBlock(block);
		this.block = block;
		resolve();
	}

	private BasicBlockBuilder linkInitBlock(final BasicBlockBuilder mainBlock) {
		final BasicBlockBuilder block = BasicBlockBuilder.createMethodInitBlock(this);
		block.jump(mainBlock);
		return block;

	}

	public void resolve() throws IOException, ClassFormatException {
		for (final BasicBlockBuilder block : this.block.getAllLinkedBlocks()) {
			for (final Node node : block.getNodes()) {
				node.data.resolve(this.bytecodeLoader, node);
			}
		}
	}

	public void display() {
		try {
			final Process exec = Runtime.getRuntime().exec(new String[] { "/usr/bin/env", "dot", "-Tx11" });
			try (final PrintStream out = new PrintStream(exec.getOutputStream())) {
				out.println("digraph G {");
				dumpRecursively(out);
				out.println("}");
			}
			final byte[] buf = new byte[1024];
			while (true) {
				final int len = exec.getErrorStream().read(buf);
				if (len < 0) {
					break;
				}
				System.err.write(buf, 0, len);
			}
		} catch (final IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public void dump(final PrintStream out) {
		if (this.block != null) {
			final String methodName = (this.selfType.getName() + "_" + getName() + "_" + getDescriptor())
					.replace('<', '_').replace('>', '_').replace('$', '_').replace('/', '_').replace('(', '_')
					.replace(')', '_').replace(';', '_');
			out.println("  subgraph \"cluster_" + methodName + "\" {");
			out.println("    label=\"" + getName() + getDescriptor() + "\";");
			this.block.dump(out, methodName);
			out.println("  }");
		}
	}

	public void dumpRecursively(final PrintStream out) {
		dumpRecursively(out, new HashSet<>());
	}

	private void dumpRecursively(final PrintStream out, final Set<Method> visited) {
		dump(out);
		for (final bdc.Method target : getCallees()) {
			if (visited.add(target)) {
				target.dumpRecursively(out);
			}
		}
	}

	public BasicBlockBuilder getBlock() {
		return this.block;
	}

	public Set<Method> getCallees() {
		final Set<Method> targets = new HashSet<>();
		for (final BasicBlockBuilder block : this.block.getAllLinkedBlocks()) {
			for (final Node node : block.getNodes()) {
				if (node.getData() instanceof MethodCall) {
					targets.add(((MethodCall) node.getData()).getMethod());
				}
			}
		}
		return targets;
	}

	@Override
	public String toString() {
		return "Method " + this.selfType + "." + this.name + getDescriptor();
	}

	public void assertReturnsConstant(final Object expectedValue) {
		if (!(this.block.terminator.getData() instanceof BlockTerminator)) {
			throw new AssertionError("Invalid terminator: " + this.block.terminator);
		}
		if (!this.block.jumpsOut.isEmpty()) {
			throw new AssertionError("Expected straight return not: " + this.block.terminator);
		}
		if (this.block.terminator.getAllInputPorts().size() != 2) {
			throw new IllegalStateException("Terminator node should have exactly one input: " + this.block.terminator
					+ ": " + this.block.terminator.getAllInputPorts());
		}
		final Node environmentSource = this.block.terminator.getInput(PortId.environment()).getSource().getNode();
		if (!(environmentSource.getData() instanceof MethodInit)) {
			throw new AssertionError("Method modifies environment: " + environmentSource);
		}
		final Node sourceNode = this.block.terminator.getInput(PortId.arg(0)).getSource().getNode();
		if (sourceNode.getData() instanceof LoadConstantOperation) {
			final LoadConstantOperation data = (LoadConstantOperation) sourceNode.getData();
			final Type expectedType = getExpectedType(expectedValue);
			if (data.getType() != expectedType) {
				throw new IllegalStateException("Expected type: " + expectedType + " != " + data.getType());
			}
			final Object actualValue = data.getValue();
			if (!isEqual(actualValue, expectedValue)) {
				throw new AssertionError("expected: " + toString(expectedValue) + " but was " + toString(actualValue));
			}
		} else {
			throw new AssertionError("Expected constant node: " + sourceNode);
		}

	}

	private Type getExpectedType(final Object expectedValue) {
		if (expectedValue instanceof Integer) {
			return PrimitiveType.Integer;
		} else if (expectedValue instanceof Boolean) {
			return PrimitiveType.Integer;
		} else if (expectedValue == null) {
			return PrimitiveType.Reference;
		} else {
			throw new IllegalStateException("Unknown type: " + expectedValue);
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

	public List<Node> getCallers() {
		return this.callers;
	}

	public static PortId calleToCallerPort(final PortId portId) {
		switch (portId.type) {
		case ENV:
			return portId;
		case LOCAL:
			return PortId.arg(portId.index);
		default:
			throw new IllegalStateException("Invalid callee argument port: " + portId);
		}
	}

	public PortId callerToCalleePort(final PortId portId) {
		switch (portId.type) {
		case ENV:
			return portId;
		case ARG:
			return PortId.local(portId.index);
		default:
			throw new IllegalStateException("Invalid callee argument port: " + portId);
		}
	}

	public Map<PortId, Value> compute(final Map<PortId, Value> constantInput) {
		final Map<PortId, Value> methodInput = new HashMap<>();
		for (final Entry<PortId, Value> entry : constantInput.entrySet()) {
			methodInput.put(callerToCalleePort(entry.getKey()), entry.getValue());
		}
		return getBlock().compute(methodInput);
	}

	public void addCaller(final Node node) {
		this.callers.add(node);
	}

	public MethodType getType() {
		try {
			return MethodType.fromDescriptor(getDescriptor());
		} catch (final ClassFormatException e) {
			throw new IllegalStateException(e);
		}
	}
}
