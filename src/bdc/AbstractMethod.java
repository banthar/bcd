package bdc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractMethod {

	private final List<Node> callers = new ArrayList<>();

	public List<Node> getCallers() {
		return this.callers;
	}

	public void addCaller(final Node node) {
		this.callers.add(node);
	}

	public abstract Map<PortId, Value> compute(final Map<PortId, Value> constantInput);

}
