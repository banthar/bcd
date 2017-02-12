package bdc;

import java.util.Map.Entry;

public class ProgramTransformations {
	public static void removeDirectlyReturnedValues(final bdc.Method mainMethod) {
		for (final Method target : mainMethod.getTargetMethods()) {
			removeDirectlyReturnedValuesFromMethod(target);
		}
	}

	private static void removeDirectlyReturnedValuesFromMethod(final Method method) {
		for (final Entry<PortId, PortId> entry : BlockTransformations.getConstantOutputs(method.getBlock())
				.entrySet()) {
			for (final Node caller : method.getCallers()) {
				final PortId outputPort = entry.getValue();
				final PortId inputPort = entry.getKey();
				caller.getOutput(outputPort).replaceWith(caller.getInput(inputPort).getSource());
				caller.removeOutput(entry.getKey());
			}
		}
	}
}
