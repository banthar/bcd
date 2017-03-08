package bdc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

public class ProgramTransformations {

	public static void optimizeMainMethod(final Method mainMethod) {
		removeDirectlyReturnedValuesFromCallees(mainMethod);
		removeUnusedArgumentsFromCallees(mainMethod);
		removeUnusedArguments(mainMethod);
		do {
			BlockTransformations.removeDeadBlocks(mainMethod.getBlock());
			BlockTransformations.removeDirectJumps(mainMethod.getBlock());
		} while (propagateConstants(mainMethod.getBlock()));
	}

	private static void removeDirectlyReturnedValuesFromCallees(final Method mainMethod) {
		for (final Method target : mainMethod.getCallees()) {
			removeDirectlyReturned(target);
		}
	}

	private static void removeDirectlyReturned(final Method method) {
		removeDirectlyReturnedValuesFromCallees(method);
		for (final Entry<PortId, PortId> entry : BlockTransformations.removeDirectOutputs(method.getBlock())
				.entrySet()) {
			for (final Node caller : method.getCallers()) {
				final PortId outputPort = entry.getValue();
				final PortId inputPort = entry.getKey();
				caller.getOutput(outputPort).replaceWith(caller.getInput(inputPort).getSource());
				caller.removeOutput(entry.getKey());
			}
		}
	}

	private static void removeUnusedArgumentsFromCallees(final Method mainMethod) {
		for (final Method target : mainMethod.getCallees()) {
			removeUnusedArguments(target);
		}
	}

	private static void removeUnusedArguments(final Method mainMethod) {
		final List<PortId> unusedArguments = BlockTransformations.removeUnusedArguments(mainMethod.getBlock());
		for (final Node caller : mainMethod.getCallers()) {
			for (final PortId argumentId : unusedArguments) {
				caller.removeInput(argumentId);
			}
		}
	}

	public static boolean propagateConstants(final BasicBlockBuilder block) {
		return propagateConstants(block, block.terminator);
	}

	private static boolean propagateConstants(final BasicBlockBuilder block, final Node node) {
		boolean blocksRemoved = false;
		boolean constantInput = true;
		for (final Entry<PortId, ? extends InputPort> port : node.getAllInputPorts().entrySet()) {
			blocksRemoved |= propagateConstants(block, port.getValue().getSource().getNode());
			if (!(port.getValue().getSource().getNode().getData() instanceof LoadConstantOperation)) {
				constantInput = false;
			}
		}
		if (node.getData() instanceof ConditionalJump) {
			if (block.terminator != node) {
				throw new IllegalStateException();
			}
			if (block.jumpsOut.size() != 2) {
				throw new IllegalStateException();
			}
			final ConditionalJump jump = (ConditionalJump) node.getData();
			final Object left = getConstantPortValue(node, 0);
			final Object right = getConstantPortValue(node, 1);
			if (left != null && right != null) {
				final int n = jump.compute(left, right);
				block.simplifyJump(block.getTarget(n));
				blocksRemoved = true;
			}
		} else if (node.getData() instanceof JumpTable) {
			if (block.terminator != node) {
				throw new IllegalStateException();
			}
			final JumpTable jump = (JumpTable) node.getData();
			final Object index = getConstantPortValue(node, 0);
			if (index != null) {
				final int n = jump.compute(index);
				block.simplifyJump(block.getTarget(n));
				blocksRemoved = true;
			}
		} else if (constantInput && node.getData() instanceof PureOperation
				&& !(node.getData() instanceof LoadConstantOperation)) {
			final List<Object> values = new ArrayList<>();
			final PureOperation operation = (PureOperation) node.getData();
			for (int i = 0; i < operation.getInputPorts(); i++) {
				final LoadConstantOperation argValue = (LoadConstantOperation) node.getInput(PortId.arg(i)).getSource()
						.getNode().data;
				values.add(argValue.getValue());
			}
			node.getOutput(PortId.arg(0))
					.replaceWith(Node.constant(operation.getReturnType(), operation.compute(values)));
		} else if (node.getData() instanceof Method) {
			if (constantInput) {
				final Method calee = (Method) node.getData();
				final Node terminator = calee.getBlock().terminator;
				if (terminator.getData() instanceof ReturnValues) {
					if (!terminator.getAllInputPorts().keySet().equals(Collections.singleton(PortId.arg(0)))) {
						throw new IllegalStateException();
					}
					final OutputPort source = terminator.getInput(PortId.arg(0)).getSource();
					if (source.getNode().getData() instanceof LoadConstantOperation) {
						final LoadConstantOperation constant = (LoadConstantOperation) source.getNode().getData();
						node.getOutput(PortId.arg(0))
								.replaceWith(Node.constant(constant.getType(), constant.getValue()));
					} else {
						throw new IllegalStateException();
					}
				} else if (terminator.getData() instanceof Jump) {
					throw new IllegalStateException("Unsupported node: " + terminator);
				} else {
					throw new IllegalStateException("Unsupported node: " + terminator);
				}
			}
		}
		return blocksRemoved;
	}

	private static Object getConstantPortValue(final Node node, final int argId) {
		final Object data = node.getInput(PortId.arg(argId)).getSource().getNode().getData();
		if (data instanceof LoadConstantOperation) {
			return ((LoadConstantOperation) data).getValue();
		} else {
			return null;
		}
	}

}
