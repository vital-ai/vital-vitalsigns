package ai.vital.query.querybuilder

import java.lang.reflect.Field
import java.util.Map
import java.util.Map.Entry

import ai.vital.query.SELECT;
import ai.vital.query.Value;
import ai.vital.vitalservice.query.VitalSortProperty;
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.model.VitalSegment
import ai.vital.vitalsigns.model.property.IProperty
import ai.vital.vitalsigns.model.property.URIProperty;
import groovy.util.AbstractFactory;
import groovy.util.FactoryBuilderSupport;

class ValueFactory extends AbstractFactory {

	@Override
	public boolean isLeaf() {
		return true
	}
	
	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes
	) throws InstantiationException, IllegalAccessException {

		Value val = new Value()
		
		if(attributes == null || attributes.size() == 0) throw new RuntimeException("value attributes map mustn't be null nor empty")
		
		val.attributes = attributes
		
		return val;
		
	}
	
	@Override
	public boolean onHandleNodeAttributes(FactoryBuilderSupport builder,
			Object node, Map attributes) {
		return false
	}

	@Override
	public void setParent(FactoryBuilderSupport builder, Object parent,
			Object child) {

		Value val = (Value)child;
		
		for(Entry entry : val.attributes.entrySet()) {
			
			if( segmentsListCase(parent, entry.key, entry.value) ) {
			} else if( sortPropertiesCase(parent, entry.key, entry.value) ) {
			} else {
			
				parent[entry.key] = entry.value
			}
		}
			
	}
			
	public static boolean sortPropertiesCase(Object parent, String name, Object value) {
        
        if(name == 'sortProperties') {
            
            List newList = []
            
            if(value instanceof List) {
                
                for(Object v : (List)value) {
                    
                    if(v instanceof VitalSortProperty) {
                        
                        newList.add(v)
                        
                    } else if(v instanceof Class){
                    
                        VitalSortProperty vsp = VitalSortProperty.get(v)
                    
                        newList.add(vsp)
            
                    } else if(v instanceof String || v instanceof GString) {
                    
                        String s = v.toString()
                        
                        if(parent instanceof SELECT) {
                            
                            IProperty prop = s.contains(':') ? VitalSigns.get().getProperty(URIProperty.withString(s)) : VitalSigns.get().getProperty(s) 
                            if(prop == null) throw new RuntimeException("Sort Property with name: ${s} not found or ambiguous")
                            
                            VitalSortProperty vsp = new VitalSortProperty(prop, false)
                            newList.add(vsp)            
                            
                        } else {
                            VitalSortProperty vsp = VitalSortProperty.get(s)
                            newList.add(vsp)            
                        }
                        
                        
                    } else {
                        throw new RuntimeException("Unexpected value in sort properties list: " + v?.getClass()?.getCanonicalName())
                    }
                    
                }
                
            } else if(value instanceof VitalSortProperty) {
            
                newList.add(value)
                
            } else if(value instanceof Class) {
            
                VitalSortProperty vsp = VitalSortProperty.get(value)
            
                newList.add(vsp)
                
            
            } else if(value instanceof String || value instanceof GString) {
            
                String s = value.toString()
            
                if(parent instanceof SELECT) {
                     
                    IProperty prop = s.contains(":") ? VitalSigns.get().getProperty(URIProperty.withString(s)) : VitalSigns.get().getProperty(s) 
                    if(prop == null) throw new RuntimeException("Sort Property with name: ${s} not found or ambiguous")
                                
                    VitalSortProperty vsp = new VitalSortProperty(prop, false)
                    newList.add(vsp)      
                            
                 } else {
                     VitalSortProperty vsp = VitalSortProperty.get(s)
                     newList.add(vsp)
                 }
                
            } else {
            
                throw new RuntimeException("Expected list value of " + name);
            
            }
            
            parent[name] = newList
            
            return true
            
        }
        
        return false
        
    }
    
	public static boolean segmentsListCase(Object parent, String name, Object value) {

		if(name == 'segments') {
			
//			Field field = parent.getClass().getDeclaredField(name);
			
			if( true /*List.class.isAssignableFrom( field.getType() )*/ ) {
			
				List<VitalSegment> newSegmentsList = []
				
				if(value instanceof List) {
					
					List l = value
					
					for(Object x : l) {
						
						if(x instanceof VitalSegment) {
							
							newSegmentsList.add(x)
							
						} else if(x instanceof String || x instanceof GString) {
						
							String s = x.toString()
							
							if(s == '*') {
								
								if(l.size() > 1) {
									throw new RuntimeException("all-segments value (*) must be a single parameter")
								}
								
								newSegmentsList = VitalSegment.getAllSegments()
								
							} else {
								newSegmentsList.add(VitalSegment.withId(s))
							}
						
						} else {
						
							throw new RuntimeException("Unexpected element of segments list attribute: " + x?.getClass())
							
						}
						
					}
					
				} else if(value instanceof String || value instanceof GString) {
				
					String v = value.toString()
					
					if(v == '*') {
						
						newSegmentsList = VitalSegment.getAllSegments()
						
					} else {
					
						newSegmentsList.add(VitalSegment.withId(v))
					
					}
					
				} else if(value instanceof VitalSegment) {
				
					newSegmentsList.add(value)
				
				} else {
					throw new RuntimeException("Unexpected type of segments attribute: " + value?.getClass())
				}
				
				
				parent[name] = newSegmentsList
				
				return true;
					
			}
			
		}
		
		return false;
	}
		
}
