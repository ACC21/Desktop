package org.docear.plugin.core.features;

import java.net.URI;
import java.util.Locale;

public class AnnotationID implements Comparable<AnnotationID>{
	
	private String id;
	private URI uri;
	private Integer objectNumber;
	
	public AnnotationID(URI absoluteUri, Integer objectNumber) throws IllegalArgumentException{
		this.setId(absoluteUri, objectNumber);
	}

	public String getId() {
		return id.toLowerCase(Locale.ENGLISH);
	}

	public void setId(URI absoluteUri, Integer objectNumber) throws IllegalArgumentException{
		if(absoluteUri == null){
			throw new IllegalArgumentException(this.getClass().getName() + ": Uri can not be null."); //$NON-NLS-1$
		}
		if(objectNumber == null){
			throw new IllegalArgumentException(this.getClass().getName() + ": Object number can not be null."); //$NON-NLS-1$
		}
		
		String uri = absoluteUri.getPath().toLowerCase(Locale.ENGLISH);
		uri = uri.trim();
		this.id = uri + " " + objectNumber; //$NON-NLS-1$
		this.objectNumber = objectNumber;
		this.uri = absoluteUri;
	}
	
	public URI getUri(){		
		return this.uri;
	}
	
	public Integer getObjectNumber(){		
		return this.objectNumber;
	}
	
	public boolean equals(Object object){
		if(object instanceof AnnotationID){
			return this.getUri().getPath().toLowerCase(Locale.ENGLISH).equals(((AnnotationID) object).getUri().getPath().toLowerCase(Locale.ENGLISH)) && this.getObjectNumber().equals(((AnnotationID) object).getObjectNumber());
		}
		else{
			return super.equals(object);
		}
	}

	public int compareTo(AnnotationID id) {
		if (id.getId() == null && this.getId() == null) {
	      return 0;
	    }
	    if (this.getId() == null) {
	      return 1;
	    }
	    if (id.getId() == null) {
	      return -1;
	    }
	    return this.getId().compareTo(id.getId());
	}
	
	public int hashCode(){		
		return this.getId().hashCode();
		
	}
	
	

}
