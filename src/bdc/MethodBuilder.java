package bdc;

public class MethodBuilder {

    public MethodBuilder() {
    }

    public BasicBlockBuilder createBasicBlock() {
	return new BasicBlockBuilder();
    }

}
