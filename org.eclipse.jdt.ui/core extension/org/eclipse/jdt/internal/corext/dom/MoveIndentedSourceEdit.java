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
package org.eclipse.jdt.internal.corext.dom;

import org.eclipse.jdt.internal.corext.textmanipulation.MoveSourceEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;

/**
 * Move source that changes the indention of the moved range.
  */
public final class MoveIndentedSourceEdit extends MoveSourceEdit {

	public MoveIndentedSourceEdit(int offset, int length) {
		super(offset, length);
		SourceModifier modifier= SourceModifier.createMoveModifier();
		modifier.initialize(-1, "", 4); //$NON-NLS-1$
		setSourceModifier(modifier);
	}

	private MoveIndentedSourceEdit(MoveIndentedSourceEdit other) {
		super(other);
	}
	
	public void initialize(int sourceIndentLevel, String destIndentString, int tabWidth) {
		((SourceModifier)getSourceModifier()).initialize(sourceIndentLevel, destIndentString, tabWidth);
	}
	
	public boolean isInitialized() {
		return ((SourceModifier)getSourceModifier()).isInitialized();
	}

	/* non Java-doc
	 * @see TextEdit#copy
	 */	
	protected TextEdit copy0() {
		return new MoveIndentedSourceEdit(this);
	}	
}
