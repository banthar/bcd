package bdc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import bdc.PortId.PortType;

public class BlockTransformations {

	public static void removeDirectJumps(final BasicBlockBuilder block) {
		removeDirectJumps(block, new HashSet<>());
	}

	private static void removeDirectJumps(final BasicBlockBuilder block, final HashSet<BasicBlockBuilder> visited) {
		if (visited.add(block)) {
			while (true) {
				if (block.jumpsOut.size() == 1) {
					final BasicBlockBuilder target = Iterables.getOnlyElement(block.jumpsOut);
					if (target.jumpsIn.size() == 1 && target != block) {
						if (!target.jumpsIn.contains(block)) {
							throw new IllegalStateException();
						}
						Node.merge(block.terminator, target.inputNode);
						block.terminator = target.terminator;
						block.jumpsOut = target.jumpsOut;
						for (final BasicBlockBuilder newTarget : block.jumpsOut) {
							if (!newTarget.jumpsIn.remove(target)) {
								throw new IllegalStateException();
							}
							if (!newTarget.jumpsIn.add(block)) {
								throw new IllegalStateException();
							}
						}
						continue;
					}
				}
				break;
			}
			for (final BasicBlockBuilder target : block.jumpsOut) {
				removeDirectJumps(target, visited);
			}
		}
	}

	public static void removeDirectStackWrites(final BasicBlockBuilder startBlock) {
		for (final BasicBlockBuilder block : startBlock.getAllTargetBlocks()) {
			final Node source = block.inputNode;
			removePush(block, source, null, 0, new HashMap<>());
		}
		addMissingBlockPorts(startBlock, new HashSet<>());
		removeExtraBlockPorts(startBlock, new HashSet<>());
	}

	private static void addMissingBlockPorts(final BasicBlockBuilder startBlock,
			final HashSet<BasicBlockBuilder> visited) {
		if (visited.add(startBlock)) {
			for (final BasicBlockBuilder target : startBlock.jumpsOut) {
				addMissingBlockPorts(target, visited);
				for (final PortId portId : target.inputNode.getAllOutputPorts().keySet()) {
					if (startBlock.terminator.getInput(portId) == null) {
						if (portId.type == PortType.LOCAL) {
							startBlock.terminator.addInput(portId, startBlock.inputNode.provideOutput(portId));
						} else if (portId.type == PortType.STACK) {
							startBlock.terminator.addInput(portId, startBlock.inputNode.provideOutput(portId));
						} else {
							throw new IllegalStateException();
						}
					}
				}
			}
			visited.remove(startBlock);
		}
	}

	private static void removeExtraBlockPorts(final BasicBlockBuilder startBlock,
			final HashSet<BasicBlockBuilder> visited) {
		if (visited.add(startBlock)) {
			final Set<PortId> usedPorts = new HashSet<>();
			for (final BasicBlockBuilder target : startBlock.jumpsOut) {
				removeExtraBlockPorts(target, visited);
				usedPorts.addAll(target.inputNode.getAllOutputPorts().keySet());
			}
			for (final Entry<? extends PortId, ? extends InputPort> e : new ArrayList<>(
					startBlock.terminator.getAllInputPorts().entrySet())) {
				final PortId port = e.getKey();
				if (port.type == PortType.LOCAL) {
					if (!usedPorts.contains(port)) {
						startBlock.terminator.removeInput(port);
					}
				}
			}
			visited.remove(startBlock);
		}
	}

	private static void removePush(final BasicBlockBuilder block, final Node node, final Chain<OutputPort> stack,
			final int stackArgs, final Map<Integer, OutputPort> locals) {
		Chain<OutputPort> newStack = stack;
		int newStackArgs = stackArgs;
		final Map<Integer, OutputPort> newLocals = new HashMap<>(locals);
		if (node.getData() instanceof Jump) {
			int i = 0;
			Chain<OutputPort> frame = stack;
			while (frame != null) {
				node.addInput(PortId.stack(i++), frame.head);
				frame = frame.tail;
			}
			for (final Entry<Integer, OutputPort> entry : locals.entrySet()) {
				final int localId = entry.getKey().intValue();
				final OutputPort storedValue = entry.getValue();
				node.addInput(PortId.local(localId), storedValue);
			}
		}
		final Set<? extends InputPort> targets;
		if (node.getOutputEnvironment() != null) {
			targets = node.getOutputEnvironment().getTargets();
		} else {
			targets = Collections.emptySet();
		}
		if (node.getData() instanceof Push) {
			final OutputPort value = node.getInput(PortId.arg(0)).unlink();
			node.getOutputEnvironment().replaceWith(node.getInputEnvironment().unlink());
			newStack = Chain.append(value, stack);
		} else if (node.getData() instanceof StoreLocal) {
			final OutputPort value = node.getInput(PortId.arg(0)).unlink();
			newLocals.put(((StoreLocal) node.getData()).getIndex(), value);
			node.getOutputEnvironment().replaceWith(node.getInputEnvironment().unlink());
		} else if (node.getData() instanceof Pop) {
			node.getOutputEnvironment().replaceWith(node.getInputEnvironment().unlink());
			if (stack != null) {
				node.getOutputArg(0).replaceWith(stack.head);
				newStack = stack.tail;
			} else {
				node.getOutputArg(0).replaceWith(block.inputNode.addOutput(PortId.stack(newStackArgs++)));
			}
		} else if (node.getData() instanceof LoadLocal) {
			final int localId = ((LoadLocal) node.getData()).getIndex();
			OutputPort storedValue = locals.get(localId);
			if (storedValue == null) {
				storedValue = block.inputNode.provideOutput(PortId.local(localId));
			}
			node.getOutput(PortId.arg(0)).replaceWith(storedValue);
			node.getInputEnvironment().unlink();
		}
		for (final InputPort port : targets) {
			removePush(block, port.getNode(), newStack, newStackArgs, newLocals);
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

	public static void removeDeadBlocks(final BasicBlockBuilder initBlock) {
		for (final BasicBlockBuilder block : initBlock.getAllLinkedBlocks()) {
			if (block != initBlock) {
				if (block.jumpsIn.isEmpty()) {
					block.unlink();
				}
			}
		}
	}

	public static Map<PortId, PortId> removeConstantOutputs(final BasicBlockBuilder init) {
		final Map<PortId, PortId> portsToInline = new HashMap<>();
		for (final PortId portId : Arrays.asList(PortId.environment(), PortId.arg(0))) {
			final List<Node> terminators = new ArrayList<>();
			OutputPort commonSource = null;
			for (final BasicBlockBuilder block : init.getAllLinkedBlocks()) {
				if (block.terminator.getData() instanceof FunctionTerminator) {
					final Node terminatorNode = block.terminator;
					final OutputPort source = getSource(terminatorNode, portId);
					if (commonSource == null || isEqual(commonSource, source)) {
						commonSource = source;
						terminators.add(terminatorNode);
					} else {
						commonSource = null;
						break;
					}
				}
			}
			if (commonSource != null && commonSource.getNode() == init.inputNode) {
				portsToInline.put(portId, calleToCallerPort(commonSource.getPortId()));
				for (final Node terminator : terminators) {
					terminator.removeInput(portId);
				}
			}
		}
		return portsToInline;
	}

	private static PortId calleToCallerPort(final PortId portId) {
		switch (portId.type) {
		case ENV:
			return portId;
		case LOCAL:
			return PortId.arg(portId.index);
		default:
			throw new IllegalStateException("Invalid calee argument port: " + portId);
		}
	}

	private static boolean isEqual(final OutputPort commonPort, final OutputPort source) {
		return commonPort == source;
	}

	private static OutputPort getSource(final Node terminatorNode, final PortId portId) {
		return terminatorNode.getInput(portId).getSource();
	}
}
