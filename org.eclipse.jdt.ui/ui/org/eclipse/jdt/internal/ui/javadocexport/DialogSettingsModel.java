/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.javadocexport;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @version 	1.0
 * @author
 */
class DialogSettingsModel implements IDialogSettings {
	
	private boolean fFromAnt;
	private Element fElement;
	private IDialogSettings fSettings;
	
	
	public DialogSettingsModel(Element element){
		fFromAnt= true;
		fElement= element;	
	}
	
	public DialogSettingsModel(IDialogSettings settings){
		fFromAnt= false;
		fSettings= settings;
	}


	public DialogSettingsModel(IDialogSettings settings, Element element){
		fElement= element;
		fSettings= settings;	
	}
	/*
	 * @see IDialogSettings#addNewSection(String)
	 */
	public IDialogSettings addNewSection(String name) {
		return null;
	}

	/*
	 * @see IDialogSettings#addSection(IDialogSettings)
	 */
	public void addSection(IDialogSettings section) {
		if(fFromAnt) {
			//making an assumption
			if(section instanceof DialogSettingsModel)
				fElement.appendChild(((DialogSettingsModel)section).getElement());	

		} else
			fSettings.addSection(section);
	}

	/*
	 * @see IDialogSettings#get(String)
	 */
	public String get(String key) {
		if(fFromAnt){
			String str= fElement.getAttribute(key);
			if(str.equals(""))
				return null;
			else return str;
		}
		return fSettings.get(key);
	}

	/*
	 * @see IDialogSettings#getArray(String)
	 */
	public String[] getArray(String key) {
		if(fFromAnt) {
			String value= fElement.getAttribute(key);
			if(value!=null){
				StringTokenizer tokenizer= new StringTokenizer(value, ",");
				ArrayList list= new ArrayList();
				while(tokenizer.hasMoreElements()) {
					list.add(tokenizer.nextElement());
				}
				return (String[]) list.toArray(new String[list.size()]);	
			} else return null;
		}else return fSettings.getArray(key);
		
	}		

	/*
	 * @see IDialogSettings#getBoolean(String)
	 */
	public boolean getBoolean(String key) {
		if(fFromAnt){
				String bool= fElement.getAttribute(key);
				if(bool.equals("true"))
					return true;
				else return false;
		} else return fSettings.getBoolean(key);
	}

	/*
	 * @see IDialogSettings#getDouble(String)
	 */
	public double getDouble(String key) throws NumberFormatException {
		return 0;
	}

	/*
	 * @see IDialogSettings#getFloat(String)
	 */
	public float getFloat(String key) throws NumberFormatException {
		return 0;
	}

	/*
	 * @see IDialogSettings#getInt(String)
	 */
	public int getInt(String key) throws NumberFormatException {
		return 0;
	}

	/*
	 * @see IDialogSettings#getLong(String)
	 */
	public long getLong(String key) throws NumberFormatException {
		return 0;
	}

	/*
	 * @see IDialogSettings#getName()
	 */
	public String getName() {
		return null;
	}

	/*
	 * @see IDialogSettings#getSection(String)
	 */
	public IDialogSettings getSection(String sectionName) {
		if (fFromAnt) {
			NodeList list = fElement.getChildNodes();
			for (int i = 0; i < list.getLength(); i++) {
				Node child = list.item(i);
				if (child.getNodeName().equals(sectionName)) {
					return new DialogSettingsModel((Element) child);
				}
			}
			return null; 
		} else {
			IDialogSettings section= fSettings.getSection(sectionName);
			if(section== null)
				return section;
			else return new DialogSettingsModel(section);
		}
	}
	/*
	 * @see IDialogSettings#getSections()
	 */
	public IDialogSettings[] getSections() {
		return null;
	}

	/*
	 * @see IDialogSettings#load(Reader)
	 */
	public void load(Reader reader) throws IOException {
	}

	/*
	 * @see IDialogSettings#load(String)
	 */
	public void load(String fileName) throws IOException {
	}

	/*
	 * @see IDialogSettings#put(String, String[])
	 */
	public void put(String key, String[] value) {
		if(fFromAnt){
			for (int i = 0; i < value.length; i++) {
				fElement.setAttribute(key, value[i]);
			}
		}	
		 else 
			fSettings.put(key, value);
	}

	/*
	 * @see IDialogSettings#put(String, double)
	 */
	public void put(String key, double value) {
	}

	/*
	 * @see IDialogSettings#put(String, float)
	 */
	public void put(String key, float value) {
	}

	/*
	 * @see IDialogSettings#put(String, int)
	 */
	public void put(String key, int value) {
	}

	/*
	 * @see IDialogSettings#put(String, long)
	 */
	public void put(String key, long value) {
	}

	/*
	 * @see IDialogSettings#put(String, String)
	 */
	public void put(String key, String value) {
		if (fFromAnt) {
			fElement.setAttribute(key, value);
		} else
			fSettings.put(key, value);
	}

	/*
	 * @see IDialogSettings#put(String, boolean)
	 */
	public void put(String key, boolean value) {
		if(fFromAnt) {
			if(value==true) {
					fElement.setAttribute(key, "true");
			}else fElement.setAttribute(key, "false");
		} else fSettings.put(key, value);
	}

	/*
	 * @see IDialogSettings#save(Writer)
	 */
	public void save(Writer writer) throws IOException {
	}

	/*
	 * @see IDialogSettings#save(String)
	 */
	public void save(String fileName) throws IOException {
	}

	public Element getElement() {
		return fElement;
	}
	
	public IDialogSettings getSettings(){
		return fSettings;
	}

}
