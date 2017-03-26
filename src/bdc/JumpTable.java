package bdc;

import java.util.Map;

public class JumpTable extends Jump {

	private final int defaultOffset;
	private final Map<Integer, Integer> lookupTable;

	public JumpTable(final int defaultOffset, final Map<Integer, Integer> lookupTable) {
		this.defaultOffset = defaultOffset;
		this.lookupTable = lookupTable;
	}

	@Override
	public Value compute(final Map<PortId, ? extends Value> input) {
		final Value value = input.get(PortId.arg(0));
		if (value.isConstant()) {
			return Value.integer(compute(value.getConstant()));
		} else {
			return Value.unknown();
		}
	}

	public int compute(final Object index) {
		return this.lookupTable.getOrDefault(index, this.defaultOffset);
	}

}
