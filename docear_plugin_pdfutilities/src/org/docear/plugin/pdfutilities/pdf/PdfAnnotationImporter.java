package org.docear.plugin.pdfutilities.pdf;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.docear.plugin.core.features.AnnotationID;
import org.docear.plugin.core.features.AnnotationModel;
import org.docear.plugin.core.features.IAnnotation;
import org.docear.plugin.core.features.IAnnotation.AnnotationType;
import org.docear.plugin.core.mindmap.AnnotationController;
import org.docear.plugin.core.mindmap.IAnnotationImporter;
import org.docear.plugin.core.util.Tools;
import org.docear.plugin.pdfutilities.PdfUtilitiesController;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;

import de.intarsys.pdf.cds.CDSNameTreeEntry;
import de.intarsys.pdf.cds.CDSNameTreeNode;
import de.intarsys.pdf.cds.CDSRectangle;
import de.intarsys.pdf.cos.COSArray;
import de.intarsys.pdf.cos.COSCatalog;
import de.intarsys.pdf.cos.COSDictionary;
import de.intarsys.pdf.cos.COSName;
import de.intarsys.pdf.cos.COSNull;
import de.intarsys.pdf.cos.COSObject;
import de.intarsys.pdf.cos.COSRuntimeException;
import de.intarsys.pdf.cos.COSString;
import de.intarsys.pdf.parser.COSLoadException;
import de.intarsys.pdf.pd.PDAnnotation;
import de.intarsys.pdf.pd.PDAnyAnnotation;
import de.intarsys.pdf.pd.PDDocument;
import de.intarsys.pdf.pd.PDExplicitDestination;
import de.intarsys.pdf.pd.PDHighlightAnnotation;
import de.intarsys.pdf.pd.PDOutline;
import de.intarsys.pdf.pd.PDOutlineItem;
import de.intarsys.pdf.pd.PDOutlineNode;
import de.intarsys.pdf.pd.PDPage;
import de.intarsys.pdf.pd.PDTextAnnotation;
import de.intarsys.pdf.pd.PDTextMarkupAnnotation;
import de.intarsys.tools.locator.FileLocator;

public class PdfAnnotationImporter implements IAnnotationImporter {
	
	private URI currentFile;
	private boolean importAll = false;
	private boolean setPDObject = false;
	
	public PdfAnnotationImporter(){
		//AnnotationController.addAnnotationImporter(this);
	}
	
	
	public Map<URI, List<AnnotationModel>> importAnnotations(List<URI> files) throws IOException, COSLoadException, COSRuntimeException{
		Map<URI, List<AnnotationModel>> annotationMap = new HashMap<URI, List<AnnotationModel>>();
		
		for(URI file : files){
			annotationMap.put(file, this.importAnnotations(file));
		}
		
		return annotationMap;
	}	
	
	public List<AnnotationModel> importAnnotations(URI uri) throws IOException, COSLoadException, COSRuntimeException{
		List<AnnotationModel> annotations = new ArrayList<AnnotationModel>();
		
		this.currentFile = uri;
		PDDocument document = getPDDocument(uri);
		if(document == null){
			return annotations;
		}
		
		try{
			annotations.addAll(this.importAnnotations(document));					
			annotations.addAll(this.importBookmarks(document.getOutline()));
			
		} catch(ClassCastException e){
			try{
				//LogUtils.warn("first try: "+ e.getMessage()+" -> " +uri);
				//LogUtils.warn(e);
				PDOutlineItem outline = (PDOutlineItem)PDOutline.META.createFromCos(document.getCatalog().cosGetOutline());
				annotations.addAll(this.importBookmarks(outline));
			} catch(Exception ex){				
				LogUtils.warn("org.docear.plugin.pdfutilities.pdf.PdfAnnotationImporter.importAnnotations: " + ex.getMessage()+" -> " +uri);
				return annotations;
			}
		} catch(Exception e){
			LogUtils.warn(e);
			return annotations;
		} finally {
			if(document != null){				
				document.close();				
				document = null;				
			}
		}
        
		return annotations;
	}
	
	public AnnotationModel importPdf(URI uri) throws IOException, COSLoadException, COSRuntimeException{
		Collection<AnnotationModel> importedAnnotations = new ArrayList<AnnotationModel>();
		try{
			importedAnnotations = importAnnotations(uri);
		} catch(IOException e){
			LogUtils.info("IOexception during update file: "+ uri); //$NON-NLS-1$
		} catch(COSRuntimeException e){
			LogUtils.info("COSRuntimeException during update file: "+ uri); //$NON-NLS-1$
		} catch(COSLoadException e){
			LogUtils.info("COSLoadException during update file: "+ uri); //$NON-NLS-1$
		}
		AnnotationModel root = new AnnotationModel(new AnnotationID(Tools.getAbsoluteUri(uri), 0), AnnotationType.PDF_FILE);
		root.setTitle(Tools.getFilefromUri(Tools.getAbsoluteUri(uri)).getName());
		root.getChildren().addAll(importedAnnotations);	
		return root;
	}
	
	public boolean renameAnnotation(IAnnotation annotation, String newTitle) throws COSRuntimeException, IOException, COSLoadException{
		List<AnnotationModel> annotations = new ArrayList<AnnotationModel>();
		boolean ret = false;
		this.currentFile = annotation.getUri();
		PDDocument document = getPDDocument(annotation.getUri());
		if(document == null){
			ret = false;
		}
		try{
			this.setImportAll(true);
			this.setPDObject(true);
			annotations.addAll(this.importAnnotations(document));					
			annotations.addAll(this.importBookmarks(document.getOutline()));
			
		} catch(ClassCastException e){		
			try{
				PDOutlineItem outline = (PDOutlineItem)PDOutline.META.createFromCos(document.getCatalog().cosGetOutline());
				annotations.addAll(this.importBookmarks(outline));
			} catch(Exception ex){
				LogUtils.warn(ex);
			}
		} catch(Exception e){
			LogUtils.warn(e);
		} finally {
			this.setImportAll(true);
			this.setPDObject(false);
			AnnotationModel result = this.searchAnnotation(annotations, annotation);
			if(result != null){
				Object annotationObject = result.getAnnotationObject();
				if(annotationObject != null && annotationObject instanceof PDOutlineItem){
					((PDOutlineItem)annotationObject).setTitle(newTitle);
					ret = true;
				}
				if(annotationObject != null && annotationObject instanceof PDAnnotation){
					((PDAnnotation)annotationObject).setContents(newTitle);
					ret = true;
				}
				document.save();		
			}
			if(document != null)
			document.close();			
		}
        
		return ret;
	}

	private void setPDObject(boolean b) {
		setPDObject = b;		
	}
	
	private boolean setPDObject() {
		return setPDObject;		
	}


	public PDDocument getPDDocument(URI uri) throws IOException,	COSLoadException, COSRuntimeException {
		MapModel map = Controller.getCurrentController().getMap();
		if(uri == null || Tools.getFilefromUri(Tools.getAbsoluteUri(uri, map)) == null || !Tools.exists(uri, map) || !new PdfFileFilter().accept(uri)){
			return null;
		}
		File file = Tools.getFilefromUri(Tools.getAbsoluteUri(uri, map));
		
		FileLocator locator = new FileLocator(file);		
		PDDocument document = PDDocument.createFromLocator(locator);
		locator = null;
		return document;
	}
	
	private List<AnnotationModel> importBookmarks(PDOutlineNode parent) throws IOException, COSLoadException, COSRuntimeException{
		List<AnnotationModel> annotations = new ArrayList<AnnotationModel>();
		
		if(!this.importAll && !ResourceController.getResourceController().getBooleanProperty(PdfUtilitiesController.IMPORT_BOOKMARKS_KEY)){
			return annotations;
		}	
		if(parent == null) return annotations;
		@SuppressWarnings("unchecked")
		List<PDOutlineItem> children = parent.getChildren();
		for(PDOutlineItem child : children){
			Integer objectNumber = child.cosGetObject().getIndirectObject().getObjectNumber();
			AnnotationModel annotation = new AnnotationModel(new AnnotationID(this.currentFile, objectNumber));
			if(this.setPDObject()){
				annotation.setAnnotationObject(child);
        	}
			annotation.setTitle(child.getTitle());			
			annotation.setAnnotationType(getAnnotationType(child));			
			annotation.setGenerationNumber(child.cosGetObject().getIndirectObject().getGenerationNumber());
			annotation.getChildren().addAll(this.importBookmarks(child));
			
			
			if(annotation.getAnnotationType() == AnnotationType.BOOKMARK_WITH_URI){
				annotation.setDestinationUri(this.getAnnotationDestinationUri(child));
			}
			if(annotation.getAnnotationType() == AnnotationType.BOOKMARK){
				annotation.setPage(this.getAnnotationDestinationPage(child));
			}			
			
			annotations.add(annotation);
		}
		
		return annotations;
	}
	
	private URI getAnnotationDestinationUri(PDOutlineItem bookmark) {
		if(bookmark != null && !(bookmark.cosGetField(PDOutlineItem.DK_A) instanceof COSNull)){
			COSDictionary cosDictionary = (COSDictionary)bookmark.cosGetField(PDOutlineItem.DK_A);
			if(!(cosDictionary.get(COSName.create("URI")) instanceof COSNull)){ //$NON-NLS-1$
				COSObject destination = cosDictionary.get(COSName.create("URI")); //$NON-NLS-1$
		        if(destination instanceof COSString && destination.getValueString(null) != null && destination.getValueString(null).length() > 0){
		        	try {
						return new URI(destination.getValueString(null));						
					} catch (URISyntaxException e) {
						LogUtils.warn("Bookmark Destination Uri Syntax incorrect.", e); //$NON-NLS-1$
					}
		        }
			}            
		}		
		return null;
	}

	private AnnotationType getAnnotationType(PDOutlineItem bookmark) {
		if(bookmark != null && (bookmark.cosGetField(PDOutlineItem.DK_A) instanceof COSNull)){			
			Integer page = null;
			try {
				 page = getAnnotationDestinationPage(bookmark);
			}
			catch (Exception e) {				
			}
			if (page == null) {			
				return AnnotationType.BOOKMARK_WITHOUT_DESTINATION;
			}			
		}
		if(bookmark != null && !(bookmark.cosGetField(PDOutlineItem.DK_A) instanceof COSNull)){			
			COSDictionary cosDictionary = (COSDictionary)bookmark.cosGetField(PDOutlineItem.DK_A);
			if(!(cosDictionary.get(COSName.create("URI")) instanceof COSNull)){ //$NON-NLS-1$
				return AnnotationType.BOOKMARK_WITH_URI;
			}            
		}
		return AnnotationType.BOOKMARK;
	}

	private List<AnnotationModel> importAnnotations(PDDocument document){
		List<AnnotationModel> annotations = new ArrayList<AnnotationModel>();
		boolean importComments = false;
		boolean importHighlightedTexts = false;
		if(this.importAll){
			importComments = true;
			importHighlightedTexts = true;
		}
		else{
			importComments = ResourceController.getResourceController().getBooleanProperty(PdfUtilitiesController.IMPORT_COMMENTS_KEY);
			importHighlightedTexts = ResourceController.getResourceController().getBooleanProperty(PdfUtilitiesController.IMPORT_HIGHLIGHTED_TEXTS_KEY);
		}
		
		String lastString = ""; //$NON-NLS-1$
		
		// Process page at a time pages always have a page number annotations dont have to record the page
		for (PDPage pdPage = document.getPageTree().getFirstPage(); pdPage != null; pdPage = pdPage.getNextPage() )
		{
			//@SuppressWarnings("unchecked")
			List<PDAnnotation> pdAnnotations = pdPage.getAnnotations();
			// if there are annotation on this page
			if (pdAnnotations != null) {
				for(PDAnnotation annotation : pdAnnotations){
					// Avoid empty entries
					// support repligo highlights
					if (annotation.getClass() == PDHighlightAnnotation.class) {
						// ignore Highlight if Subject is "Highlight" and Contents is ""
						if (((PDHighlightAnnotation) annotation).getSubject() != null &&
								((PDHighlightAnnotation) annotation).getSubject().length() > 0 &&
								((PDHighlightAnnotation) annotation).getSubject().equals("Highlight") &&
								annotation.getContents().equals("") )
							continue;
					} else {
						// ignore annotations with Contents is ""
						if (annotation.getContents().equals("")
						/* && !annotation.isMarkupAnnotation() */
						)
							continue;
						//$NON-NLS-1$
		
						// Avoid double entries (Foxit Reader)
						if (annotation.getContents().equals(lastString)/*
																		 * &&
																		 * !annotation.
																		 * isMarkupAnnotation
																		 * ()
																		 */)
							continue;
						lastString = annotation.getContents();
					}
		            if((annotation.getClass() == PDAnyAnnotation.class ||annotation.getClass() == PDTextAnnotation.class) &&
		            		importComments){
		            	Integer objectNumber = annotation.cosGetObject().getIndirectObject().getObjectNumber();
		    			AnnotationModel pdfAnnotation = new AnnotationModel(new AnnotationID(this.currentFile, objectNumber));            	
		            	pdfAnnotation.setTitle(annotation.getContents()); 
		            	if(this.setPDObject()){
		            		pdfAnnotation.setAnnotationObject(annotation);
		            	}
		            	pdfAnnotation.setAnnotationType(AnnotationType.COMMENT);            	
		            	pdfAnnotation.setGenerationNumber(annotation.cosGetObject().getIndirectObject().getGenerationNumber());
		            	pdfAnnotation.setPage(pdPage.getNodeIndex()+1);
		    			annotations.add(pdfAnnotation);
		            }
		            if((annotation.getClass() == PDTextMarkupAnnotation.class || annotation.getClass() == PDHighlightAnnotation.class) && importHighlightedTexts){
		            	Integer objectNumber = annotation.cosGetObject().getIndirectObject().getObjectNumber();
		    			AnnotationModel pdfAnnotation = new AnnotationModel(new AnnotationID(this.currentFile, objectNumber));
		    			// prefer Title from Contents (So updates work)
						if (annotation.getContents() != null &&
								annotation.getContents().length() > 0) {
							pdfAnnotation.setTitle(annotation.getContents());
						} else {
			    			// support repligo highlights
							// set Title to Subject per repligo
							if (annotation.getClass() == PDHighlightAnnotation.class) {
								if (((PDHighlightAnnotation) annotation).getSubject() != null &&
										((PDHighlightAnnotation) annotation).getSubject().length() > 0) {
									if (!((PDHighlightAnnotation) annotation).getSubject().equals("Highlight"))
										pdfAnnotation.setTitle(((PDHighlightAnnotation) annotation).getSubject());
								}
							}
						}
						
		            	if(this.setPDObject()){
		            		pdfAnnotation.setAnnotationObject(annotation);
		            	}
		            	pdfAnnotation.setAnnotationType(AnnotationType.HIGHLIGHTED_TEXT);             	
		            	pdfAnnotation.setGenerationNumber(annotation.cosGetObject().getIndirectObject().getGenerationNumber());
		            	pdfAnnotation.setPage(pdPage.getNodeIndex()+1);
		    			annotations.add(pdfAnnotation);
		            }
				}
			}
		}
		
		return annotations;
	}	
	
	public Integer getAnnotationDestination(PDAnnotation pdAnnotation) {				
		
		if(pdAnnotation != null){
			PDPage page = pdAnnotation.getPage();			
			if(page != null)
				return page.getNodeIndex()+1;
		}
		
		return null;		
	}

	public Integer getAnnotationDestinationPage(PDOutlineItem bookmark) throws IOException, COSLoadException {
		
		PDDocument document = bookmark.getDoc();
		if(document == null || bookmark == null){
			return null;
		}		
		
		if(bookmark != null && bookmark.getDestination() != null){
            PDExplicitDestination destination = bookmark.getDestination().getResolvedDestination(document);
            if(destination != null){
                PDPage page = destination.getPage(document);
                return page.getNodeIndex()+1;
            }
        }
        if(bookmark != null && !(bookmark.cosGetField(PDOutlineItem.DK_A) instanceof COSNull)){        	
            
            COSDictionary cosDictionary = (COSDictionary)bookmark.cosGetField(PDOutlineItem.DK_A);            
            COSArray destination = getCOSArrayFromDestination(cosDictionary);
                        
            return getPageFromCOSArray(document, (COSArray)destination);           
        }
        
        return null;
	}

	private COSArray getCOSArrayFromDestination(COSDictionary cosDictionary) {
		COSObject cosObject = cosDictionary.get(COSName.create("D")); //$NON-NLS-1$
		if(cosObject instanceof COSArray){
			return (COSArray)cosObject;
		}
		if(cosObject instanceof COSString){
			String destinationName = cosObject.getValueString(null);
			if(destinationName == null || destinationName.length() <= 0){
				return null;
			}
				
        	COSDictionary dests = cosDictionary.getDoc().getCatalog().cosGetDests();
    		if (dests != null) {
    			for (Iterator<?> i = dests.keySet().iterator(); i.hasNext();) {
    				COSName key = (COSName) i.next();
    				if(key.stringValue().equals(destinationName)){
    					cosDictionary = (COSDictionary)dests.get(key);
    					cosObject = cosDictionary.get(COSName.create("D")); //$NON-NLS-1$
    					if(cosObject instanceof COSArray){
    						return (COSArray)cosObject;
    					}
    				}
    			}
    		}
    		
    		COSDictionary names = cosDictionary.getDoc().getCatalog().cosGetNames();
    		if (names != null) {
    			COSDictionary destsDict = names.get(COSCatalog.DK_Dests).asDictionary();
    			if (destsDict != null) {
    				CDSNameTreeNode destsTree = CDSNameTreeNode.createFromCos(destsDict);
    				for (Iterator<?> i = destsTree.iterator(); i.hasNext();) {
    					CDSNameTreeEntry entry = (CDSNameTreeEntry) i.next();        					
    					if(entry.getName().stringValue().equals(destinationName)){
    						if(entry.getValue() instanceof COSDictionary){
	    						cosDictionary = (COSDictionary)entry.getValue();
	    						cosObject = cosDictionary.get(COSName.create("D")); //$NON-NLS-1$
	        					if(cosObject instanceof COSArray){
	        						return (COSArray)cosObject;
	        					}
    						}
    						else if(entry.getValue() instanceof COSArray){
    							return (COSArray)entry.getValue();
    						}
        				}
    				}
    			}
    		}
    		
    		
		}
		return null;
	}

	private Integer getPageFromCOSArray(PDDocument document, COSArray destination) {
		//DOCEAR: fallback if no etry was found
		if(destination == null) {
			return 1;
		}
		Iterator<?> it = destination.iterator();
	    while(it.hasNext()){
	         COSObject o = (COSObject)it.next();
	         if(o.isIndirect()){  //the page is indirect referenced
	             o.dereference();
	         }
	         PDPage page = document.getPageTree().getFirstPage();
	         while(page != null){
	             if(page.cosGetObject().equals(o)){
	                 return page.getNodeIndex() + 1;
	             }
	             page = page.getNextPage();
	         }	                 
	    }
	    return null;
	}

	public AnnotationModel searchAnnotation(URI uri, NodeModel node) throws Exception {
		this.currentFile = uri;
		if(!this.isImportAll()) this.setImportAll(true);
		List<AnnotationModel> annotations = this.importAnnotations(uri);
		this.setImportAll(false);
		return searchAnnotation(annotations, node);        
   }
	
	public AnnotationModel searchAnnotation(List<AnnotationModel> annotations, NodeModel node) {
		for(AnnotationModel annotation : annotations){           
			AnnotationModel extensionModel = AnnotationController.getModel(node, false);
			if(extensionModel == null){
				if(annotation.getTitle().equals(node.getText())){
					//TODO: DOCEAR is Update nodeModel good here??
					//TODO: DOCEAR How to deal with nodes without extension(and object number) and changed annotation title ??
					AnnotationController.setModel(node, annotation);
					return annotation;
				}
				else{
					AnnotationModel searchResult = searchAnnotation(annotation.getChildren(), node);
					if(searchResult != null) return searchResult;
				}
			}
			else{
				return searchAnnotation(annotations, extensionModel);
			}
           
       }
		return null;
	}
	
	public AnnotationModel searchAnnotation(List<AnnotationModel> annotations, IAnnotation target) {
		for(AnnotationModel annotation : annotations){ 
			if(annotation.getObjectNumber().equals(target.getObjectNumber())){
				return annotation;
			}
			else{
				AnnotationModel searchResult = searchAnnotation(annotation.getChildren(), target);
				if(searchResult != null) return searchResult;
			}
		}
		return null;
	}

	public boolean isImportAll() {
		return importAll;
	}

	public void setImportAll(boolean importAll) {
		this.importAll = importAll;
	}


}
