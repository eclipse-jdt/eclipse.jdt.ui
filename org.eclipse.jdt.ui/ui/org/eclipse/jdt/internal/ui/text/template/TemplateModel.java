/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.template;

import java.util.Iterator;
import java.util.Vector;

/**
 * Used by <code>TemplateEditorPopup</code>.
 */
public class TemplateModel {
	
	// element types
	public static final int NON_EDITABLE_TEXT= 1;
	public static final int EDITABLE_TEXT= 2;
	public static final int SELECTION_START= 3;
	public static final int SELECTION_END= 4;
	
	private static class Element {
		public String text;
		public int type;
		
		public Element(String text, int type) {
			this.text= text;
			this.type= type;
		}
	}	

	private Vector fElements= new Vector();

	public TemplateModel() {		
	}

	private Element findEditableText(String name) {
		Iterator iterator= fElements.iterator();
		while (iterator.hasNext()) {
			Element element= (Element) iterator.next();

			if ((element.type == EDITABLE_TEXT) && element.text.equals(name))
				return element;
		}
		return null;
		
	}

	public void reset() {
		fElements.clear();
	}

	public void append(String name, int type) {
		Element element= null;
		
		if (type == EDITABLE_TEXT)
			element= findEditableText(name);

		if (element == null)
			element= new Element(name, type);

		fElements.add(element);
	}

	public int[] getEditableTexts() {
		int[] indices= new int[getEditableCount()];
		
		int i= 0;
		int j= 0;
		Iterator iterator= fElements.iterator();
		while (iterator.hasNext()) {
			Element element = (Element) iterator.next();
			if (element.type == EDITABLE_TEXT)
				indices[i++]= j;
			j++;
		}
		
		return indices;
	}

	/**
	 * Returns a concatenated string representation of the ranges.
	 */
	public String toString() {
		StringBuffer buffer= new StringBuffer();
		
		Iterator iterator= fElements.iterator();
		while (iterator.hasNext()) {
			Element element = (Element) iterator.next();
			buffer.append(element.text);			
		}

		return buffer.toString();
	}
	
	public int getEditableCount() {
		int count= 0;
		
		Iterator iterator= fElements.iterator();
		while (iterator.hasNext()) {
			Element element = (Element) iterator.next();
			if (element.type == EDITABLE_TEXT)
				count++;
		}
		
		return count;
	}

	public void setText(int index, String text) {
		Element element= (Element) fElements.get(index);
		
		if (element.type == EDITABLE_TEXT)
			element.text= text;
	}

	public String getText(int index) {
		return ((Element) fElements.get(index)).text;
	}

	public int getOffset(int index) {
		int offset= 0;
		int i= 0;
				
		Iterator iterator= fElements.iterator();
		while (iterator.hasNext()) {
			Element element = (Element) iterator.next();
			
			if (i == index)
				return offset;

			offset += element.text.length();
			i++;
		}
		
		return -1;
	}

	public boolean shareSameModel(int index0, int index1) {
		Element element0 = (Element) fElements.get(index0);
		Element element1 = (Element) fElements.get(index1);
		
		return element0 == element1;
	}
	
	public int[] getSelection() {
		int[] selection= new int[] {-1, -1};		
		int offset= 0;
		
		Iterator iterator= fElements.iterator();
		while (iterator.hasNext()) {
			Element element = (Element) iterator.next();
			
			if (element.type == SELECTION_START)
				selection[0]= offset;
			else if (element.type == SELECTION_END)
				selection[1]= offset;

			offset += element.text.length();
		}
		
		if (selection[1] == -1)
			selection[1]= selection[0];
		
		return selection;
	}
}

