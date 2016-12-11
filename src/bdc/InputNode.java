package bdc;

import java.util.List;

interface InputNode {
    List<? extends InputPort> getAllInputPorts();

    InputPort getInputEnvironment();

    InputPort getExtraInput(final int index);

}