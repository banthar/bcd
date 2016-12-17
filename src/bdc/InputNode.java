package bdc;

import java.util.Map;

interface InputNode {
	Map<? extends PortId, ? extends InputPort> getAllInputPorts();

	InputPort getInputEnvironment();
}