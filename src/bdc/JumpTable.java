package bdc;

import java.util.Map;

public class JumpTable extends Jump {

	private final int defaultOffset;
	private final Map<Integer, Integer> lookupTable;

	public JumpTable(final int defaultOffset, final Map<Integer, Integer> lookupTable) {
		this.defaultOffset = defaultOffset;
		this.lookupTable = lookupTable;
	}

	public int compute(final Object index) {
		return this.lookupTable.getOrDefault(index, this.defaultOffset);
	}

}
