package bdc;

import java.util.Map;

interface OutputNode {

	Map<? extends PortId, ? extends OutputPort> getAllOutputPorts();

	OutputPort getOutputEnvironment();

}