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
package org.eclipse.text.edits;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.text.IRegion;

import org.eclipse.text.edits.TextEdit;
import org.eclipse.jdt.internal.corext.Assert;

public class TextEditGroup {

	private String fDescription;
	private List fEdits;

	public TextEditGroup(String description) {
		Assert.isNotNull(description);
		fDescription= description;
		fEdits= new ArrayList(3);
	}

	public TextEditGroup(String description, TextEdit edit) {
		Assert.isNotNull(description);
		Assert.isNotNull(edit);
		fDescription= description;
		fEdits= new ArrayList(1);
		fEdits.add(edit);
	}
	
	public TextEditGroup(String description, TextEdit[] edits) {
		Assert.isNotNull(description);
		Assert.isNotNull(edits);
		fDescription= description;
		fEdits= new ArrayList(Arrays.asList(edits));
	}

	public String getName() {
		return fDescription;
	}
	
	public void addTextEdit(TextEdit edit) {
		fEdits.add(edit);
	}
	
	public boolean isEmpty() {
		return fEdits.isEmpty();
	}
	
	public TextEdit[] getTextEdits() {
		return (TextEdit[]) fEdits.toArray(new TextEdit[fEdits.size()]);
	}
	
	/**
	 * Returns the text region covered by the edits managed via this
	 * edit group. If the group doesn't manage any edits <code>null
	 * </code> is returned.
	 * 
	 * @return the text region covered by this edit group or <code>
	 *  null</code> if no edits are managed
	 */
	public IRegion getRegion() {
		int size= fEdits.size();
		if (size == 0) {
			return null;
		} else if (size == 1) {
			return ((TextEdit)fEdits.get(0)).getRegion();
		} else {
			return TextEdit.getCoverage((TextEdit[])fEdits.toArray(new TextEdit[fEdits.size()]));
		}
	}	
}
