/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.w3c.dom.Element;

/**
 * History storing {@link TypeInfo}s as objects and 
 * {@link TypeInfo#getFullyQualifiedName()}s as keys.
 */
public class TypeInfoHistory extends History {
	
	private static final String NODE_ROOT= "typeInfoHistroy"; //$NON-NLS-1$
	private static final String NODE_TYPE_INFO= "typeInfo"; //$NON-NLS-1$
	private static final String NODE_NAME= "name"; //$NON-NLS-1$
	private static final String NODE_PACKAGE= "package"; //$NON-NLS-1$
	private static final String NODE_ENCLOSING_NAMES= "enclosingTypes"; //$NON-NLS-1$
	private static final String NODE_PATH= "path"; //$NON-NLS-1$
	private static final String NODE_MODIFIERS= "modifiers";  //$NON-NLS-1$
	
	private static final char[][] EMPTY_ENCLOSING_NAMES= new char[0][0];
	
	private final TypeInfoFactory fTypeInfoFactory;
	
	protected TypeInfoHistory(String fileName) {
		super(fileName, NODE_ROOT, NODE_TYPE_INFO);
		fTypeInfoFactory= new TypeInfoFactory();
		load();
	}
	
	public synchronized boolean contains(TypeInfo type) {
		return super.contains(type);
	}
	
	public synchronized void accessed(TypeInfo info) {
		super.accessed(info);
	}
	
	public synchronized TypeInfo remove(TypeInfo info) {
		return (TypeInfo)super.remove(info);
	}
	
	public synchronized TypeInfo[] getTypeInfos() {
		Collection values= getValues();
		int size= values.size();
		TypeInfo[] result= new TypeInfo[size];
		int i= size - 1;
		for (Iterator iter= values.iterator(); iter.hasNext();) {
			result[i]= (TypeInfo)iter.next();
			i--;
		}
		return result;
	}
	
	public synchronized TypeInfo[] getFilteredTypeInfos(TypeInfoFilter filter) {
		Collection values= getValues();
		List result= new ArrayList();
		for (Iterator iter= values.iterator(); iter.hasNext();) {
			TypeInfo type= (TypeInfo)iter.next();
			if ((filter == null || filter.matchesHistoryElement(type)) && !TypeFilter.isFiltered(type.getFullyQualifiedName()))
				result.add(type);
		}
		Collections.reverse(result);
		return (TypeInfo[])result.toArray(new TypeInfo[result.size()]);
		
	}
	
	protected Object getKey(Object object) {
		return ((TypeInfo)object).getFullyQualifiedName();
	}

	protected Object createFromElement(Element type) {
		String name= type.getAttribute(NODE_NAME);
		String pack= type.getAttribute(NODE_PACKAGE);
		char[][] enclosingNames= getEnclosingNames(type);
		String path= type.getAttribute(NODE_PATH);
		int modifiers= 0;
		try {
			modifiers= Integer.parseInt(type.getAttribute(NODE_MODIFIERS));
		} catch (NumberFormatException e) {
			// take zero
		}
		TypeInfo info= fTypeInfoFactory.create(
			pack.toCharArray(), name.toCharArray(), enclosingNames, modifiers, path);
		return info;
	}
	
	protected void setAttributes(Object object, Element typeElement) {
		TypeInfo type= (TypeInfo)object;
		typeElement.setAttribute(NODE_NAME, type.getTypeName());
		typeElement.setAttribute(NODE_PACKAGE, type.getPackageName());
		typeElement.setAttribute(NODE_ENCLOSING_NAMES, type.getEnclosingName());
		typeElement.setAttribute(NODE_PATH, type.getPath());
		typeElement.setAttribute(NODE_MODIFIERS, Integer.toString(type.getModifiers()));
	}
	
	private char[][] getEnclosingNames(Element type) {
		String enclosingNames= type.getAttribute(NODE_ENCLOSING_NAMES);
		if (enclosingNames.length() == 0)
			return EMPTY_ENCLOSING_NAMES;
		StringTokenizer tokenizer= new StringTokenizer(enclosingNames, "."); //$NON-NLS-1$
		List names= new ArrayList();
		while(tokenizer.hasMoreTokens()) {
			String name= tokenizer.nextToken();
			names.add(name.toCharArray());
		}
		return (char[][])names.toArray(new char[names.size()][]);
	}

}
