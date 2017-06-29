package laser.ddg.commands;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import laser.ddg.DataInstanceNode;
import laser.ddg.DataNodeVisitor;
import laser.ddg.ProvenanceData;
import laser.ddg.ScriptNode;
import laser.ddg.Workflow;
import laser.ddg.gui.DDGExplorer;
import laser.ddg.persist.JenaWriter;
import laser.ddg.r.RDataInstanceNode;
import laser.ddg.visualizer.WorkflowGraphBuilder;

/**
 * Command to examine a DDG and determine if any of the MD5 hashes of its
 * file nodes match with the MD5 hashes contained in the hashtable.csv file.
 * 
 * @author Connor Gregorich-Trevor
 * @version June 13, 2017
 *
 */

public class FindIdenticalObjectsCommand implements ActionListener {

	private static final JFileChooser FILE_CHOOSER = new JFileChooser(System.getProperty("user.home"));
	private ArrayList<ScriptNode> scrnodes = new ArrayList<ScriptNode>();
	private ArrayList<RDataInstanceNode> fileNodes = new ArrayList<RDataInstanceNode>();
	private int index = 0;

	@Override
	public void actionPerformed(ActionEvent args0) {
		
		DataNodeVisitor dataNodeVisitor = new DataNodeVisitor();
		ArrayList<DataInstanceNode> dins = dataNodeVisitor.getDins();
		ArrayList<String[]> matches = new ArrayList<String[]>();
		dataNodeVisitor.visitNodes();
		
		JenaWriter jenaWriter = JenaWriter.getInstance();
		WorkflowGraphBuilder builder = new WorkflowGraphBuilder(false, jenaWriter);
		builder.buildNodeAndEdgeTables();
		
		ArrayList<String> nodehashes = new ArrayList<String>();
		for (int i = 0; i < dins.size(); i++) {
			nodehashes.add(dins.get(i).getHash());
		}

		ProvenanceData currDDG = DDGExplorer.getInstance().getCurrentDDG();
		try {
			readHashtable(currDDG.getSourcePath(), nodehashes, matches);
			generateFileNodes(matches, builder);
		} catch (Exception e) {
			DDGExplorer ddgExplorer = DDGExplorer.getInstance();
			e.printStackTrace(System.err);
			JOptionPane.showMessageDialog(ddgExplorer,
					"Unable to load the file: " + e.getMessage(),
					"Error loading file", JOptionPane.ERROR_MESSAGE);
		}

		
		Workflow flow = new Workflow(currDDG.getProcessName(), currDDG.getTimestamp());
		flow.setFileNodeList(fileNodes);
		flow.setScriptNodeList(scrnodes);
		
		
		for (ScriptNode node : flow.getScriptNodeList()) {
			builder.addNode(node, node.getId());
		}
		// The value could use some fixing
		for (RDataInstanceNode node : flow.getFileNodeList()) {
			builder.addNode(node.getType(), node.getId(), node.getName(), "value", 
					node.getCreatedTime(), node.getLocation(), null);
		}
		builder.drawGraph();
		
		DDGExplorer ddgExplorer = DDGExplorer.getInstance();
		ddgExplorer.addTab("Script Workflow", builder.getPanel());
		
	}

	private void readHashtable(String currDDGDir, ArrayList<String> nodehashes, ArrayList<String[]> csvmap) throws IOException {
		// https://www.mkyong.com/java/how-to-read-and-parse-csv-file-in-java/
		String home = System.getProperty("user.home");
		File hashtable = new File(home + "/.ddg/hashtable.csv");
		if (!hashtable.exists()) {
			DDGExplorer ddgExplorer = DDGExplorer.getInstance();
			if (FILE_CHOOSER.showOpenDialog(ddgExplorer) == JFileChooser.APPROVE_OPTION) {
				hashtable = FILE_CHOOSER.getSelectedFile();
			}
		}
		String line = "";
		FileReader fr = new FileReader(hashtable);
		BufferedReader br = new BufferedReader(fr);
		br.readLine();
		while((line = br.readLine()) != null) {
			String[] entries = line.replaceAll("\"", "").split(","); 
			String hash = entries[5];
			if (nodehashes.contains(hash)) {
				csvmap.add(entries);
			}
		}
		br.close();
	}

	private ArrayList<RDataInstanceNode> generateFileNodes(ArrayList<String[]> matches, WorkflowGraphBuilder builder) {
		for (String[] match : matches) {
			String name = match[1].substring(match[1].lastIndexOf('/') + 1);
			RDataInstanceNode file = new RDataInstanceNode("File", name, match[8], match[7], match[1], match[5], match[0]);
			ScriptNode scrnode = generateScriptNode(scrnodes, file, match[0]);
			
			int foundindex = -1;
			for (int i = 0; i < fileNodes.size(); i++) {
				if (fileNodes.get(i).getHash().equals(file.getHash()) && 
						fileNodes.get(i).getName().equals(file.getName())) {
					foundindex = i;
					System.out.println("hashmatch");
				}
			}
			if (foundindex == -1) {
				fileNodes.add(file);
				file.setId(index++);
				if (match[6].equals("read")) {
					System.out.println("read");
					builder.addEdge("SF", file.getId(), scrnode.getId());
				} else if (match[6].equals("write")) {
					System.out.println("write");
					builder.addEdge("SF", scrnode.getId(), file.getId());
				}
			} else {
				if (match[6].equals("read")) {
					System.out.println("read");
					builder.addEdge("SF", fileNodes.get(foundindex).getId(), scrnode.getId());
				} else if (match[6].equals("write")) {
					System.out.println("write");
					builder.addEdge("SF", scrnode.getId(), fileNodes.get(foundindex).getId());
				}
			}
		}
		return fileNodes;
	}

	private ScriptNode generateScriptNode(ArrayList<ScriptNode> scrnodes, RDataInstanceNode file, String path) {
		ScriptNode ret = new ScriptNode(0.0, path);
		if (scrnodes.size() == 0) {
			ScriptNode toAdd = new ScriptNode(0.0, path);
			toAdd.setId(index++);
			toAdd.addWorkflowNode(file);
			scrnodes.add(toAdd);
			ret = toAdd;
		} else {
			boolean added = false;
			for (ScriptNode scrnode : scrnodes) {
				if (scrnode.getName().equals(path)) {
					scrnode.addWorkflowNode(file);
					added = true;
					ret = scrnode;
				}
			}
			if (!added) {
				ScriptNode toAdd = new ScriptNode(0.0, path);
				toAdd.addWorkflowNode(file);
				toAdd.setId(index++);
				scrnodes.add(toAdd);
				ret = toAdd;
			}
		}
		return ret;
	}

	private void connectNodes(WorkflowGraphBuilder builder) {
		// Institute some kind of check to see if a node similar to this one has been
		// iterated through before.
		
		for (RDataInstanceNode filenode : fileNodes) {
			for (ScriptNode scriptnode : scrnodes) {
				if (filenode.getScrloc().equals(scriptnode.getName())) { // check if script is the same. May have to change if not doing full script path as name.
					if (filenode.getRw().equals("read")) {
						builder.addEdge("SF", filenode.getId(), scriptnode.getId());
					} else {
						builder.addEdge("SF", scriptnode.getId(), filenode.getId());
					}
				}
			}
		}
	}
	
}
