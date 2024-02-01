package ai.vital.vitalsigns.block;

import java.io.IOException
import java.util.ArrayList
import java.util.List

import ai.vital.vitalsigns.model.GraphObject
import ai.vital.vitalsigns.block.BlockCompactStringSerializer
import ai.vital.vitalsigns.block.CompactStringSerializer

/**
 * Vital block parser. It should be always closed not to lose the last block.
 *
 */
public class VitalBlockParser {

	/*
	public static interface VitalBlockListener {
		
		public void onVitalBlock(String URI, List<GraphObject> block)
		
	}
	*/
	
	private VitalBlockListener vitalBlockListener

	private List<GraphObject> currentBlock = new ArrayList<GraphObject>()
	
	public VitalBlockParser(VitalBlockListener vitalBlockListener) {
		super()
		if(vitalBlockListener == null) throw new NullPointerException("Vital block listener cannot be null")
		this.vitalBlockListener = vitalBlockListener
	}
	
	public void addLine(String line) throws IOException {
		
		if(line.startsWith("#")) return
		
		if(line.equals(BlockCompactStringSerializer.BLOCK_SEPARATOR)) {
			checkBlock()
			return;
		}
		
		GraphObject go = CompactStringSerializer.fromString(line)
		
		if(go != null) {
			currentBlock.add(go)
		}
		
	}
	
	void checkBlock() {

		if(currentBlock.size() > 0) {
			
			List<GraphObject> copy = new ArrayList<GraphObject>(currentBlock)
			
			vitalBlockListener.onVitalBlock(copy.get(0).getURI(), copy)
			
			currentBlock.clear()
			
		}
				
	}
	
	public void close() {
		checkBlock()
	}

}
