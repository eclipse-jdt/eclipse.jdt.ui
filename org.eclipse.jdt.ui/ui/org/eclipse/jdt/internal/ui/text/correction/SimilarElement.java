/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.Arrays;

public class SimilarElement {
	
	private final int fKind;
	private final String fName;
	private final String[] fParamTypes;
	private final int fRelevance;

	public SimilarElement(int kind, String name, int relevance) {
		this(kind, name, null, relevance);
	}
	
	public SimilarElement(int kind, String name, String[] paramTypes, int relevance) {
		fKind= kind;
		fName= name;
		fParamTypes= paramTypes;
		fRelevance= relevance;
	}	

	/**
	 * Gets the kind.
	 * @return Returns a int
	 */
	public int getKind() {
		return fKind;
	}
	
	/**
	 * Gets the parameter types.
	 * @return Returns a int
	 */
	public String[] getParameterTypes() {
		return fParamTypes;
	}	

	/**
	 * Gets the name.
	 * @return Returns a String
	 */
	public String getName() {
		return fName;
	}

	/**
	 * Gets the relevance.
	 * @return Returns a int
	 */
	public int getRelevance() {
		return fRelevance;
	}
	
	/* (non-Javadoc)
	 * @see Object#equals(Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof SimilarElement) {
			SimilarElement elem= (SimilarElement) obj;
			return fName.equals(elem.fName) && fKind == elem.fKind && Arrays.equals(fParamTypes, elem.fParamTypes);
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see Object#hashCode()
	 */
	public int hashCode() {
		return fName.hashCode() + fKind;
	}	
}
