package ai.vital.vitalsigns.block;

import ai.vital.vitalsigns.model.GraphObject
import java.util.List


public interface VitalBlockListener {

	public void onVitalBlock(String URI, List<GraphObject> block)
	
}
