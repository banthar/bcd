package bdc;

import java.util.Map;

public class UnconditionalJump extends Jump {

	@Override
	public Value compute(final Map<PortId, ? extends Value> input) {
		throw new IllegalStateException();
	}

}
