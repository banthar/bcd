package bdc;

import java.util.List;

interface OutputNode {

    List<? extends OutputPort> getAllOutputPorts();

    OutputPort getOutputEnvironment();

    OutputPort getExtraOutput(final int index);

}