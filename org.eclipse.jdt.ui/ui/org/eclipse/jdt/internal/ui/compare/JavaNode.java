/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000,2001
 */
package org.eclipse.jdt.internal.ui.compare;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IDocument;

import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DocumentRangeNode;

import org.eclipse.jdt.internal.core.JavaElement;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * Extends the DocumentRangeNode with method signature information.
 */
class JavaNode extends DocumentRangeNode implements ITypedElement {
	
	public static final int CU= 0;
	public static final int PACKAGE= 1;
	public static final int IMPORT_CONTAINER= 2;
	public static final int IMPORT= 3;
	public static final int INTERFACE= 4;
	public static final int CLASS= 5;
	public static final int FIELD= 6;
	public static final int INIT= 7;
	public static final int CONSTRUCTOR= 8;
	public static final int METHOD= 9;

	private static Map fgImages= new Hashtable(13);	// maps the names below to SWT Images

	private int fInitializerCount= 1;
	private boolean fIsEditable;

	public JavaNode(JavaNode parent, int type, String name, IDocument doc, int start, int length) {
		super(type, buildID(type, name), doc, start, length);
		if (parent != null) {
			parent.addChild(this);
			fIsEditable= parent.isEditable();
		}
	}	
		
	public JavaNode(IDocument doc, boolean editable) {
		super(CU, buildID(CU, "root"), doc, 0, doc.getLength());
		fIsEditable= editable;
	}	

	private static String buildID(int type, String name) {
		StringBuffer sb= new StringBuffer();
		switch (type) {
		case CU:
			sb.append(JavaElement.JEM_COMPILATIONUNIT);
			break;
		case CLASS:
		case INTERFACE:
			sb.append(JavaElement.JEM_TYPE);
			sb.append(name);
			break;
		case FIELD:
			sb.append(JavaElement.JEM_FIELD);
			sb.append(name);
			break;
		case CONSTRUCTOR:
		case METHOD:
			sb.append(JavaElement.JEM_METHOD);
			sb.append(name);
			break;
		case INIT:
			sb.append(JavaElement.JEM_INITIALIZER);
			sb.append(name);
			break;
		case PACKAGE:
			sb.append(JavaElement.JEM_PACKAGEDECLARATION);
			break;
		case IMPORT:
			sb.append(JavaElement.JEM_IMPORTDECLARATION);
			sb.append(name);
			break;
		case IMPORT_CONTAINER:
			sb.append('<');
			break;
		default:
			System.out.println("JavaNode.buildID: Should not happen");
			break;
		}
		return sb.toString();
	}

	public String getInitializerCount() {
		return Integer.toString(fInitializerCount++);
	}
	
	/**
	 * Used for smart matching.
	 */
	public String getMethodName() {
		String id= getId();
		int pos= id.indexOf('(');
		if (pos > 0)
			return id.substring(1, pos);
		return id.substring(1);
	}
	
	/**
	 * Used for smart matching.
	 */
	public String getSignature() {
		String id= getId();
		int pos= id.indexOf('(');
		if (pos >= 0)
			return id.substring(pos+1);
		return id.substring(1);
	}
	
	/**
	 * Returns a name which is used in the UI.
	 * @see ITypedInput@getName
	 */
	public String getName() {
		
		switch (getTypeCode()) {
		case INIT:
			return "initializer";
		case IMPORT_CONTAINER:
			return "import declarations";
		case CU:
			return "compilation unit";
		case PACKAGE:
			return "package declaration";
		}
		return getId().substring(1);
	}
	
	/**
	 * @see ITypedInput@getType
	 */
	public String getType() {
		return "java2";
	}
	
	/* (non Javadoc)
	 * see IEditableContent.isEditable
	 */
	public boolean isEditable() {
		return fIsEditable;
	}
		
	/**
	 * Returns null if no Image exists for the type code.
	 * @see ITypedInput@getImage
	 */
	public Image getImage() {
				
		int id= getTypeCode();
		Integer key= new Integer(id);
		Image image= (Image) fgImages.get(key);
		
		if (image == null) {
			ImageDescriptor d= null;
			
			try {
				switch (getTypeCode()) {
				case CU:
					d= JavaPluginImages.DESC_OBJS_CUNIT;
					break;
				case PACKAGE:
					d= JavaPluginImages.DESC_OBJS_PACKDECL;
					break;
				case IMPORT:
					d= JavaPluginImages.DESC_OBJS_IMPDECL;
					break;
				case IMPORT_CONTAINER:
					d= JavaPluginImages.DESC_OBJS_IMPCONT;
					break;
				case CLASS:
					d= JavaPluginImages.DESC_OBJS_CLASS;
					break;
				case INTERFACE:
					d= JavaPluginImages.DESC_OBJS_INTERFACE;
					break;
				case INIT:
				case METHOD:
				case CONSTRUCTOR:
					d= JavaCompareUtilities.getImageDescriptor("obj16/compare_method.gif");
					break;
				case FIELD:
					d= JavaCompareUtilities.getImageDescriptor("obj16/compare_field.gif");
					break;					
				default:
					break;
				}
			} catch (NullPointerException ex) {
			} catch (ExceptionInInitializerError ex) {
			} catch (NoClassDefFoundError ex) {
			}
	
			if (d == null)
				d= ImageDescriptor.getMissingImageDescriptor();
			image= d.createImage();
			fgImages.put(key, image);
		}
		return image;
	}

	public static void disposeImages() {
		Iterator i= fgImages.values().iterator();
		while (i.hasNext()) {
			Image image= (Image) i.next();
			if (!image.isDisposed())
				image.dispose();
		}
		fgImages.clear();
	}
}

