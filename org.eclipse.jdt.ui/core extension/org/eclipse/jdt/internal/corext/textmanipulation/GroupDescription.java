/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.textmanipulation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.internal.corext.Assert;

public class GroupDescription {

	private String fDescription;
	private List fEdits;

	public GroupDescription(String description) {
		super();
		Assert.isNotNull(description);
		fDescription= description;
		fEdits= new ArrayList(3);
	}

	public GroupDescription(String description, TextEdit[] edits) {
		super();
		Assert.isNotNull(description);
		Assert.isNotNull(edits);
		fDescription= description;
		fEdits= new ArrayList(Arrays.asList(edits));
	}

	public void addTextEdit(TextEdit edit) {
		fEdits.add(edit);
	}
	
	public TextEdit[] getTextEdits() {
		return (TextEdit[]) fEdits.toArray(new TextEdit[fEdits.size()]);
	}
	
	public TextRange getTextRange() {
		int size= fEdits.size();
		if (size == 0)
			return TextRange.UNDEFINED;
		if (size == 1) {
			return ((TextEdit)fEdits.get(0)).getTextRange();
		} else {
			return TextEdit.getTextRange(fEdits);
		}
	}
	
	public Object getModifiedElement() {
		int size= fEdits.size();
		switch (size) {
			case 0:
				return null;
			case 1:
				return ((TextEdit)fEdits.get(0)).getModifiedElement();
			default:
				return null;
		}
	}
	
	public String getName() {
		return fDescription;
	}
}
