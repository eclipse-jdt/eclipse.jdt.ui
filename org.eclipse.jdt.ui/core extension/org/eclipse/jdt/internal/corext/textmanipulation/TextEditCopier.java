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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.Assert;

/**
 * Copies a tree of text edits.
 * 
 * @since 3.0
 */
public class TextEditCopier {
	
	private TextEdit fEdit;
	private Map fCopies;

	/**
	 * Constructs a new <code>TextEditCopier</code> for the
	 * given edit. The actual copy is done by calling <code>
	 * perform</code>.
	 * 
	 * @param edit the edit to copy
	 * 
	 * @see #perform()
	 */
	public TextEditCopier(TextEdit edit) {
		super();
		Assert.isNotNull(edit);
		fEdit= edit;
		fCopies= new HashMap();
	}

	/**
	 * Performs the actual copy. This method returns <code>null
	 * </code> if all edits in the tree are excluded from copying.
	 * 
	 * @return the copy or <code>null</code> if all edits are
	 *  excluded from copying
	 */
	public TextEdit perform() {
		TextEdit result= doCopy(fEdit);
		if (result != null) {
			for (Iterator iter= fCopies.keySet().iterator(); iter.hasNext();) {
				TextEdit edit= (TextEdit)iter.next();
				edit.postProcessCopy(this);
			}
		}
		return result;
	}
	
	/**
	 * Returns the copy for the original text edit.
	 * 
	 * @param original the original for which the copy
	 *  is requested
	 * @return the copy of the original edit.
	 */
	public TextEdit getCopy(TextEdit original) {
		if (original == null)
			return null;
		return (TextEdit)fCopies.get(original);
	}
	
	//---- helper methods --------------------------------------------
		
	private TextEdit doCopy(TextEdit edit) {
		TextEdit result= edit.copy0();
		List children= edit.internalGetChildren();
		if (children != null) {
			List newChildren= new ArrayList(children.size());
			for (Iterator iter= children.iterator(); iter.hasNext();) {
				TextEdit childCopy= doCopy((TextEdit)iter.next());
				childCopy.setParent(result);
				newChildren.add(childCopy);
			}
			result.internalSetChildren(newChildren);
		}
		addCopy(edit, result);
		return result;
	}
	
	private void addCopy(TextEdit original, TextEdit copy) {
		fCopies.put(original, copy);
	}	
}
