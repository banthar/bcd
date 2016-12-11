package bdc;

import java.util.List;

interface InputNode {
    default InputPort getInput(final int n) {
	return getInput().get(n);
    }

    default int getInputSize() {
	return getInput().size();
    }

    List<? extends InputPort> getInput();

}