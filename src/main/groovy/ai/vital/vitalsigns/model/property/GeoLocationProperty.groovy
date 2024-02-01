package ai.vital.vitalsigns.model.property;

import java.text.DecimalFormat

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion;

public class GeoLocationProperty implements IProperty {

	private static final long serialVersionUID = 4936584637858221232L;
	
	public final static DecimalFormat format = new DecimalFormat("0.000000000000");
	
	Double longitude;
	
	Double latitude;
	
	public GeoLocationProperty() {
		this.longitude = 0d;
		this.latitude = 0d;
	}
	
	public GeoLocationProperty(Double longitude, Double latitude) {
		super();
		if(longitude == null) throw new NullPointerException("Longitude cannot be null");
		if(latitude == null) throw new NullPointerException("Latitude cannot be null");
		if(longitude < -180d || longitude > 180d) throw new RuntimeException("Longitude must be a value in range: [-180, 180]");
		if(latitude < -90d || latitude > 90d ) throw new RuntimeException("Latitude must a value in range: [-90, 90]");
		this.longitude = longitude;
		this.latitude = latitude;
	}


	@Override
	public String toString() {
		return toRDFValue();
	}
	
	public String toRDFValue() {
		return "LON:${format.format(longitude)},LAT:${format.format(latitude)}";
	}
	
	public final static class GeoLocationFormatException extends RuntimeException {

		private static final long serialVersionUID = 5576696507655911746L;

		public GeoLocationFormatException() {
			super();
		}

		public GeoLocationFormatException(String msg) {
			super(msg);
		}

	}
	
	/*
	public static GeoLocationProperty fromJSONMap( Map value ) {
		return new GeoLocationProperty(value.longitude, value.latitude)
	}
	*/
	
	public static GeoLocationProperty fromRDFString(String lexicalForm) throws GeoLocationFormatException {
		
		String[] cols = lexicalForm.split(",");
		if (cols.length != 2) {
			throw new GeoLocationFormatException("two columns expected");
		}
		
		Double lon = null;
		Double lat = null;
		
		for(String c : cols) {
				
			c = c.trim();
				
			if(c.startsWith("LON:")) {
				if(lon != null) throw new GeoLocationFormatException("Exactly 1 LON: value expected");
				try {
					lon = Double.parseDouble(c.substring(4).trim());
				} catch(NumberFormatException e) {
					throw new GeoLocationFormatException("Longitude part invalid: " + c);
				}
			} else if(c.startsWith("LAT:")) {
			
				if(lat != null) throw new GeoLocationFormatException("Exactly 1 LAT: value expected");

				try {
					lat = Double.parseDouble(c.substring(4).trim());
				} catch(NumberFormatException e) {
					throw new GeoLocationFormatException("Longitude part invalid: " + c);
				}
							
			}
		
		}
		
		if(lon == null) throw new GeoLocationFormatException("No LON: part: " + lexicalForm);
		if(lat == null) throw new GeoLocationFormatException("No LAT: part: " + lexicalForm);
		
		return new GeoLocationProperty(lon, lat);
		
	}

	@Override
	public boolean equals(Object arg0) {
		
		if(!(arg0 instanceof GeoLocationProperty)) return false;
		GeoLocationProperty v = (GeoLocationProperty)arg0;
		return this.latitude.doubleValue() == v.latitude.doubleValue() && 
				this.longitude.doubleValue() == v.longitude.doubleValue();
	}
	
	@Override
    public int hashCode() {
        return this.latitude.hashCode() + 1000d * this.longitude.hashCode()
    }

    public Map<String, Object> toJSONMap() {
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put("longitude", longitude);
		m.put("latitude", latitude);
		return m;
	}
	
	public static GeoLocationProperty fromJSONMap(Map<String, Object> m) {
		GeoLocationProperty p = new GeoLocationProperty(m.get("longitude"), m.get("latitude"));
		return p;
	}
	

	@Override
	public Object rawValue() {
		return this;
	}
	
	@Override
	public IProperty unwrapped() {
		return this;
	}
	
	private String externalURI
	
	public String getURI() {
		if(externalURI != null) return externalURI
		throw new RuntimeException("getURI should be implemented by property trait");
	}
	
	public static GeoLocationProperty createInstance(String propertyURI, Double longitude, Double latitude) {
		GeoLocationProperty bp = new GeoLocationProperty(longitude, latitude)
		bp.externalURI = propertyURI
		return bp
	}
	
	/**
	 * creates external property criterion
	 * @param propertyURI
	 * @return
	 */
	public static VitalGraphQueryPropertyCriterion create(String propertyURI) {
		VitalGraphQueryPropertyCriterion c = new VitalGraphQueryPropertyCriterion(propertyURI);
		c.externalProperty = true
		return c;
	}
	

    @Override
    public void setExternalPropertyURI(String externalPropertyURI) {
        this.externalURI = externalPropertyURI;
    }
}
