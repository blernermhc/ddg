package laser.ddg;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import laser.ddg.r.RDataInstanceNode;

public class ScriptNode implements Node {

	// SIN name
	private final String nameOfSN;
	
	// The id of the node is set to zero if no id is assigned.
	private int scrid = 0;

	private Map<String, ScriptNode> inputs;

	private Map<String, ScriptNode> outputs;

	private Map<String, SourcePos> sourcePositions;
	
	private Serializable value;
	
	private List<RDataInstanceNode> fileNodes;
	
	// Attribute-value pairs to allow arbitrary extensions
	private Map<String, Object> attributeValues = new TreeMap<>();

	// Time when a Script Node is created
	private final String timeCreated;

	// CPU time that this opeartion took
	private double elapsedTime;

	public ScriptNode(double elapsedTime, String name) {
		this.timeCreated = Calendar.getInstance().toString();
		this.elapsedTime = elapsedTime;
		this.nameOfSN = name;
		this.fileNodes = new ArrayList<RDataInstanceNode>();
	}
	
	public String getName() {
		return nameOfSN;
	}

	@Override
	public String getCreatedTime() {
		return timeCreated;
	}

	@Override
	public double getElapsedTime() {
		return elapsedTime;
	}

	@Override
	public int getId() {
		return scrid;
	}

	@Override
	public Set<DataInstanceNode> getProcessOutputsDerived() {
		return null;
	}

	@Override
	public Set<DataInstanceNode> getProcessInputsDerived() {
		return null;
	}

	@Override
	public Object getAttributeValue(String name) {
		return attributeValues.get(name);
	}

	@Override
	public void setAttribute(String name, Object value) {
		attributeValues.put(name, value);
	}

	@Override
	public Iterator<String> attributes() {
		return attributeValues.keySet().iterator();
	}

	public List<RDataInstanceNode> getWorkflowNodes() {
		return fileNodes;
	}

	public void addWorkflowNode(RDataInstanceNode toAdd) {
		this.fileNodes.add(toAdd);
	}

	public String getType() {
		return "Script";
	}

	public Serializable getValue() {
		return value;
	}

	public void setValue(Serializable value) {
		this.value = value;
	}

}