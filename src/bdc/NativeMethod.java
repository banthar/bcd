package bdc;

import java.util.HashMap;
import java.util.Map;

public class NativeMethod extends AbstractMethod {

	@Override
	public Map<PortId, Value> compute(final Map<PortId, Value> constantInput) {
		final HashMap<PortId, Value> returnValues = new HashMap<>();
		returnValues.put(PortId.environment(), Value.unknownEnvironment());
		return returnValues;
	}

}
