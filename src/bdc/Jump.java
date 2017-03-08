package bdc;

import java.util.Map;

public class Jump extends BlockTerminator {

	public Value compute(final Map<PortId, ? extends Value> constantInput) {
		// TODO compute direct/conditional jump value
		return Value.unknown();
	}

}
