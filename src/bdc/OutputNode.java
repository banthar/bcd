package bdc;

import java.util.List;

interface OutputNode {
    default int getOutputSize() {
	return getOutput().size();
    }

    default OutputPort getOutput(final int n) {
	return getOutput().get(n);
    }

    List<? extends OutputPort> getOutput();

}