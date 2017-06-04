package bdc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ProgramTransformations {

	public static void optimizeMainMethod(final Method mainMethod) {
		removeDirectlyReturnedValuesFromCallees(mainMethod);
		removeUnusedArgumentsFromCallees(mainMethod);
		removeUnusedArguments(mainMethod);
		do {
			BlockTransformations.removeDeadBlocks(mainMethod.getBlock());
			BlockTransformations.removeDirectJumps(mainMethod.getBlock());
			BlockTransformations.removeUnnecessaryHeapAllocations(mainMethod.getBlock());
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
		final Map<PortId, Value> constantInput = new HashMap<>();
		for (final Entry<PortId, ? extends InputPort> port : node.getAllInputPorts().entrySet()) {
			blocksRemoved |= propagateConstants(block, port.getValue().getSource().getNode());
			constantInput.put(port.getKey(), port.getValue().getSource().getConstantValue());
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
		} else if (node.getData() instanceof PureOperation && !(node.getData() instanceof LoadConstantOperation)) {
			final PureOperation operation = (PureOperation) node.getData();
			final Map<PortId, ? extends Value> output = operation.compute(constantInput);
			if (output.size() != 1) {
				throw new IllegalStateException();
			}
			final Value computedValue = output.get(PortId.arg(0));
			if (computedValue.isConstant()) {
				node.getOutput(PortId.arg(0))
						.replaceWith(Node.constant(operation.getType(), computedValue.getConstant()));
			}
		} else if (node.getData() instanceof Method) {
			if (constantInput != null) {
				final Method calee = (Method) node.getData();
				final Map<PortId, Value> values = calee.compute(constantInput);
				for (final Entry<PortId, Value> entry : values.entrySet()) {
					final Value value = entry.getValue();
					if (value.isConstant()) {
						node.getOutput(entry.getKey()).replaceWith(Node.constant(value.getType(), value.getConstant()));
					}
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
