package ai.vital.vitalsigns.model.property

class MultiValueProperty<T extends IProperty> implements IProperty, Collection<T> {

	private static final long serialVersionUID = 1L;
	
	private List<T> innerList = [];
	
	public MultiValueProperty(Collection<T> values) {
		if(values != null && values.size() > 0) {
			innerList.addAll(values)
		}
		innerList = Collections.unmodifiableList(innerList)	
	}
	
	//empty constructor
	public MultiValueProperty() {
		
	}
	
	public IProperty getFirst() {
		if(innerList.size() > 0) return innerList[0]
		return null;
	} 

	@Override
	public Object rawValue() {
		HashSet raw = new HashSet();
		for(IProperty v : this.iterator() ) {
			raw.add(v.rawValue());
		}
		return raw;
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
	
	
	
	//collection methods
	@Override
	public boolean add(T arg0) {
		throw unsupported()
		
	}
	
	protected UnsupportedOperationException unsupported() {
		return new UnsupportedOperationException("multivalue property is immutable");
	}

	@Override
	public boolean addAll(Collection arg0) {
		throw unsupported()
	}

	@Override
	public void clear() {
		throw unsupported()
	}

	@Override
	public boolean contains(Object arg0) {
		return innerList.contains(arg0);
	}

	@Override
	public boolean containsAll(Collection<?> arg0) {
		return innerList.containsAll(arg0);
	}

	@Override
	public boolean isEmpty() {
		return innerList.isEmpty();
	}

	@Override
	public Iterator<T> iterator() {
		return innerList.iterator();
	}

	@Override
	public boolean remove(Object arg0) {
		throw unsupported()
	}

	@Override
	public boolean removeAll(Collection<?> arg0) {
		throw unsupported()
	}

	@Override
	public boolean retainAll(Collection<?> arg0) {
		throw unsupported()
	}

	@Override
	public int size() {
		return innerList.size();
	}

	// [Ljava.lang.Object; toArray(java.lang.Object[])
	
	@Override
	Object[] toArray() {
		return innerList.toArray();
	}

	
	// @Override
	T[] toArray(T[] arg0) {
		return innerList.toArray(arg0);
	}

	@Override
	Object[] toArray(Object[] arg0) {
		return innerList.toArray(arg0);
	}
	
	
	
	public static MultiValueProperty createInstance(String propertyURI) {
		MultiValueProperty bp = new MultiValueProperty()
		bp.externalURI = propertyURI
		return bp
	}

    @Override
    public void setExternalPropertyURI(String externalPropertyURI) {
        this.externalURI = externalPropertyURI;
    }
	
	@Override
	boolean equals(Object n) {

		//      if(n instanceof GeneratedGroovyProxy) {
		//          n = ((GeneratedGroovyProxy)n).getProxyTarget();
		//      }

		if(n instanceof IProperty) {
			n = n.rawValue()
		}

		return this.rawValue().equals(n)

				
	}
	
	
}
