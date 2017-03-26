package bdc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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

	private static void removeUnusedInterBlockPorts(final BasicBlockBuilder startBlock,
			final HashSet<BasicBlockBuilder> visited) {
		if (visited.add(startBlock)) {
			for (final BasicBlockBuilder target : startBlock.jumpsOut) {
				removeUnusedInterBlockPorts(target, visited);
			}
			if (startBlock.terminator.getData() instanceof Jump) {
				final Set<PortId> usedPorts = new HashSet<>();
				for (final BasicBlockBuilder target : startBlock.jumpsOut) {
					usedPorts.addAll(target.inputNode.getAllOutputPorts().keySet());
				}
				for (final Entry<PortId, ? extends InputPort> entry : new ArrayList<>(
						startBlock.terminator.getAllInputPorts().entrySet())) {
					if (!usedPorts.contains(entry.getKey()) && entry.getKey().type != PortType.ARG) {
						startBlock.terminator.removeInput(entry.getKey());
					}
				}
			}
			if (startBlock.inputNode.getData() instanceof BlockInit) {
				for (final OutputPort port : new ArrayList<>(startBlock.inputNode.getAllOutputPorts().values())) {
					if (port.getTargets().isEmpty()) {
						startBlock.inputNode.removeOutput(port.getPortId());
					}
				}
			}
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

	public static void removeDeadBlocks(final BasicBlockBuilder initBlock) {
		for (final BasicBlockBuilder block : initBlock.getAllLinkedBlocks()) {
			if (block != initBlock) {
				if (block.jumpsIn.isEmpty()) {
					block.unlink();
				}
			}
		}
	}

	public static Map<PortId, PortId> removeDirectOutputs(final BasicBlockBuilder init) {
		final Iterable<? extends PortId> returnedPorts = ((MethodInit) init.inputNode.getData()).getReturnedPorts();
		final Map<PortId, PortId> portsToInline = new HashMap<>();
		for (final PortId portId : returnedPorts) {
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
				portsToInline.put(portId, Method.calleToCallerPort(commonSource.getPortId()));
				for (final Node terminator : terminators) {
					terminator.removeInput(portId);
				}
			}
		}
		removeUnusedInterBlockPorts(init, new HashSet<>());
		removeExtraBlockPorts(init, new HashSet<>());
		return portsToInline;
	}

	private static boolean isEqual(final OutputPort commonPort, final OutputPort source) {
		return commonPort == source;
	}

	private static OutputPort getSource(final Node terminatorNode, final PortId portId) {
		final OutputPort source = terminatorNode.getInput(portId).getSource();
		if (source.getNode().getData() instanceof BlockInit) {
			final BasicBlockBuilder block = ((BlockInit) source.getNode().getData()).getBlock();
			OutputPort commonRoot = null;
			for (final BasicBlockBuilder sourceBlock : block.jumpsIn) {
				final OutputPort root = getSource(sourceBlock.terminator, source.getPortId());
				if (commonRoot == null || isEqual(commonRoot, root)) {
					commonRoot = root;
				} else {
					return null;
				}
			}
			return commonRoot;
		} else if (source.getNode().getData() instanceof MethodInit) {
			return source;
		} else if (source.getNode().getData() instanceof LoadConstantOperation) {
			return source;
		} else if (source.getNode().getData() instanceof PureTransformation) {
			return null;
		} else {
			throw new IllegalStateException("Unsupported node type: " + source.getNode());
		}
	}

	public static List<PortId> removeUnusedArguments(final BasicBlockBuilder block) {
		final List<PortId> removedPorts = new ArrayList<>();
		final Node node = block.inputNode;
		final Iterator<? extends Entry<PortId, ? extends OutputPort>> iterator = node.getAllOutputPorts().entrySet()
				.iterator();
		while (iterator.hasNext()) {
			final Entry<PortId, ? extends OutputPort> entry = iterator.next();
			final PortId id = entry.getKey();
			final OutputPort port = entry.getValue();
			if (port.getTargets().isEmpty()) {
				iterator.remove();
				removedPorts.add(Method.calleToCallerPort(id));
			}
		}
		return removedPorts;
	}
}
