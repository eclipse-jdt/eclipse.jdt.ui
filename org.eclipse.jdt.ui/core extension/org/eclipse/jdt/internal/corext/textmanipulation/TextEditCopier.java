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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.internal.corext.Assert;

public class TextEditCopier {

	private TextEdit fEdit;
	private Map fCopies;

	public TextEditCopier(TextEdit edit) {
		super();
		Assert.isNotNull(edit);
		fEdit= edit;
		fCopies= new HashMap();
	}

	public TextEdit copy() {
		return fEdit.copy(this);
	}
	
	public TextEdit getCopy(TextEdit original) {
		if (original == null)
			return null;
		return (TextEdit)fCopies.get(original);
	}
	
	/* package */ void addCopy(TextEdit original, TextEdit copy) {
		fCopies.put(original, copy);
	}
}
