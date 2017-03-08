package bdc;

import java.util.List;
import java.util.Map.Entry;

public class ProgramTransformations {

	public static void optimizeMainMethod(final Method mainMethod) {
		removeDirectlyReturnedValuesFromCallees(mainMethod);
		removeUnusedArgumentsFromCallees(mainMethod);
		removeUnusedArguments(mainMethod);
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
			System.out.println(unusedArguments);
			System.out.println(caller);
			System.out.println(caller.getAllInputPorts());
			for (final PortId argumentId : unusedArguments) {
				caller.removeInput(argumentId);
			}
		}
		System.out.println();
	}
}
