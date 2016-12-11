package bdc;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import bdc.Node.NodeType;
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
			}
			if (portId.type == PortType.STACK) {
			    System.out.println("missing stack port: " + portId);
			}
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

	if (node.getType() == NodeType.TERMINATOR) {
	    int i = 0;
	    Chain<OutputPort> frame = stack;
	    while (frame != null) {
		node.addInput(PortId.stack(i++), frame.head);
		frame = frame.tail;
	    }
	}
	final Set<? extends InputPort> targets;
	if (node.getOutputEnvironment() != null) {
	    targets = node.getOutputEnvironment().getTargets();
	} else {
	    targets = Collections.emptySet();
	}
	if (node.getType() == NodeType.PUSH) {
	    final OutputPort value = node.getInput(PortId.arg(0)).unlink();
	    node.getOutputEnvironment().replaceWith(node.getInputEnvironment().unlink());
	    newStack = Chain.append(value, stack);
	} else if (node.getType() == NodeType.STORE_LOCAL) {
	    final OutputPort value = node.getInput(PortId.arg(0)).unlink();
	    newLocals.put((Integer) node.getData(), value);
	    node.getOutputEnvironment().replaceWith(node.getInputEnvironment().unlink());

	} else if (node.getType() == NodeType.POP) {
	    node.getOutputEnvironment().replaceWith(node.getInputEnvironment().unlink());
	    if (stack != null) {
		node.getOutputArg(0).replaceWith(stack.head);
		newStack = stack.tail;
	    } else {
		node.getOutputArg(0).replaceWith(block.inputNode.addOutput(PortId.stack(newStackArgs++)));
	    }
	} else if (node.getType() == NodeType.LOAD_LOCAL) {
	    final int localId = ((Integer) node.getData()).intValue();
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
}
