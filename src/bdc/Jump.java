package bdc;

import java.util.Map;

public abstract class Jump extends BlockTerminator {
	public abstract Value compute(final Map<PortId, ? extends Value> input);
}
