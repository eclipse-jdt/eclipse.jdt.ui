/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.template;

import java.util.Iterator;
import java.util.Vector;
import org.eclipse.jdt.internal.core.Assert;

/**
 * Used by <code>TemplateEditorPopup</code>.
 */
public class TemplateModel {
	
	// element types
	public static final int NON_EDITABLE_TEXT= 1;
	public static final int EDITABLE_TEXT= 2;
	public static final int CARET= 3;
	
	private static class Element {
		public final String text;
		public int size;
		public int type;
		
		public Element(String text, int type) {
			this.text= text;
			this.type= type;
			this.size= text.length();
		}
	}	

	private Vector fElements= new Vector();
	private StringBuffer fBuffer= new StringBuffer();

	public TemplateModel() {		
	}

	public void reset() {
		fElements.clear();
	}
	
	public void append(String name, int type) {
		fElements.add(new Element(name, type));
		fBuffer.append(name);
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
		return fBuffer.toString();
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

	public void setSize(int index, int size) {
		Element element= (Element) fElements.get(index);
		
		if (element.type == EDITABLE_TEXT)
			element.size= size;
	}

	public int getSize(int index) {
		if (index == fElements.size())
			return 0;
			
		Element element= (Element) fElements.get(index);		
		return element.size;
	}

	public int getOffset(int index) {
		int offset= 0;
		int i= 0;
				
		Iterator iterator= fElements.iterator();
		while (iterator.hasNext()) {
			Element element = (Element) iterator.next();
			
			if (i == index)
				return offset;

			offset += element.size;
			i++;
		}
		
		return -1;
	}

	public boolean shareSameModel(int index0, int index1) {
		Element element0 = (Element) fElements.get(index0);
		Element element1 = (Element) fElements.get(index1);
		
		return element0.text.equals(element1.text);
	}
/*	
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

			offset += element.size;
		}
		
		if (selection[1] == -1)
			selection[1]= selection[0];
		
		return selection;
	}
*/	
	public boolean exceeds(int index, int offset, int length) {
		Element element= (Element) fElements.get(index);
		int elementOffset= getOffset(index);

		return
			(offset < elementOffset) ||
			(offset + length > elementOffset + element.size);
	}

	public int[] getPositions() {
		int[] positions= new int[fElements.size() + 1];
		
		int i= 0;
		int offset= 0;
		
		for (Iterator iterator = fElements.iterator(); iterator.hasNext();) {
			Element element = (Element) iterator.next();
			
			positions[i]= offset;
			offset += element.size;					
			
			i++;
		}
		
		positions[i]= offset;
		
		return positions;
	}
	
	public void update(String string, int[] positions) {
		Assert.isTrue(positions.length == fElements.size() + 1);		

		fBuffer.setLength(0);
		fBuffer.append(string);
		
		int i= 0;
		int offset= 0;
		for (Iterator iterator = fElements.iterator(); iterator.hasNext();) {
			Element element = (Element) iterator.next();
			
			int size= positions[i + 1] - positions[i];
			element.size= size;
			
			// XXX workaround for insufficient positioning of code formatter
			if (element.type == EDITABLE_TEXT) {
				String subString= string.substring(offset, offset + size);
				String trimmedString= subString.trim();
				
				int delta= trimmedString.length() - subString.length();
				if (delta < 0) {					
					element.size += delta;
					positions[i + 1] += delta;	
				}
			}
			
			// offset
			if (i == 0)
				element.size += positions[0];

			offset += element.size;
			i++;
		}
	}

	public int getCaretOffset() {
		int offset= 0;		
		for (Iterator iterator = fElements.iterator(); iterator.hasNext();) {
			Element element = (Element) iterator.next();
			
			if (element.type == CARET)
				break;
				
			offset += element.size;
		}

		return offset;
	}
	
	public int getTotalSize() {
		int size= 0;
		
		for (Iterator iterator = fElements.iterator(); iterator.hasNext();) {
			Element element = (Element) iterator.next();
			size += element.size;
		}
		
		return size;		
	}
}

