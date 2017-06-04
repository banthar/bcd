package bdc;

public class BlockInit implements NodeOperation {

	private final BasicBlockBuilder block;

	public BlockInit(final BasicBlockBuilder block) {
		this.block = block;
	}

	public BasicBlockBuilder getBlock() {
		return this.block;
	}

}
