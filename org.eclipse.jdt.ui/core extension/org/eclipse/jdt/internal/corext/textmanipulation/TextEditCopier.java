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

import org.eclipse.jdt.internal.corext.Assert;

/**
 * Copies a tree of text edits.
 * 
 * @since 3.0
 */
public class TextEditCopier {
	
	public static final int EXCLUDE= 1;
	public static final int INCLUDE= 2;

	private TextEdit fEdit;
	private TextEdit[] fExcludes;
	private TextEdit[] fIncludes;
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
	 * Constructs a new <code>TextEditCopier</code> for the
	 * given edit. The actual copy is done by calling <code>
	 * perform</code>.
	 * 
	 * @param edit the edit to copy
	 * @param mode either <code>INCLUDE</code> or <code>EXCLUDE</code>
	 * @param list the list of edits to include in or exclude from
	 *  the copy depending on the <code>mode</code> argument. 
	 *  <p>
	 *  If <code>INCLUDE<code> is specified then only those edits are 
	 *  part of the copy that are contained in <code>list</code>. If a 
	 *  child edit but not the corresponding parent edit is part of the 
	 *  list then a <code>MultiTextEdit</code> is created to represent 
	 *  the parent edit. This ensure that the copied tree preserves its 
	 *  structure.
	 *  <p>
	 *  If <code>EXCLUDE</code> is specified then only those edits are
	 *  part of the copy that aren't contained in <code>list</code>. 
	 *  Excluding a parent edit doesn't automatically exclude all 
	 *  child edits. If a parent edit is excluded and there are still
	 *  child edits to be copied a <code>MultiTextEdit<code> is
	 *  created to represent the parent edit. This ensures that the
	 *  copied tree preserves its structure.
	 * 
	 * @see #perform()
	 */
	public TextEditCopier(TextEdit edit, int mode, TextEdit[] list) {
		this(edit);
		Assert.isNotNull(list);
		Assert.isTrue(mode == EXCLUDE || mode == INCLUDE);
		if (mode == EXCLUDE)
			fExcludes= list;
		else
			fIncludes= list;
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
		List children= edit.internalGetChildren();
		List newChildren= null;
		if (children != null) {
			for (Iterator iter= children.iterator(); iter.hasNext();) {
				TextEdit element= (TextEdit)iter.next();
				TextEdit copy= doCopy(element);
				if (copy != null) {
					if (newChildren == null)
						newChildren= new ArrayList();
					newChildren.add(copy);
				}
			}
		}
		TextEdit result= null;
		if (considerEdit(edit)) {
			result= edit.copy0();	
		}
		if (newChildren != null) {
			if (result == null) {
				result= edit.createPlaceholder();
			}
			result.internalSetChildren(newChildren);
			for (Iterator iter= newChildren.iterator(); iter.hasNext();) {
				TextEdit child= (TextEdit)iter.next();
				child.setParent(result);
			}
		}
		if (result != null) {
			addCopy(edit, result);
		}
		return result;
	}
	
	private void addCopy(TextEdit original, TextEdit copy) {
		fCopies.put(original, copy);
	}
	
	private boolean considerEdit(TextEdit edit) {
		if (fExcludes != null) {
			for (int i= 0; i < fExcludes.length; i++) {
				if (edit.equals(fExcludes[i]))
					return false;
			}
			return true;
		}
		if (fIncludes != null) {
			for (int i= 0; i < fIncludes.length; i++) {
				if (edit.equals(fIncludes[i]))
					return true;
			}
			return false;
		}
		return true;
	}
}
