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
package org.eclipse.ltk.core.refactoring;

import org.eclipse.jdt.internal.corext.Assert;

/**
 * A rename data object describes the data that a rename processor
 * provides to its participants. Participants 
 * 
 * @since 3.0
 */
public class RenameData {
	
	private String fNewName;
	private boolean fUpdateReferences;
	
	public RenameData(String newName, boolean updateReferences) {
		Assert.isNotNull(newName);
		fNewName= newName;
		fUpdateReferences= updateReferences;
	}
	
	public String getNewName() {
		return fNewName;
	}
	
	public boolean getUpdateReferences() {
		return fUpdateReferences;
	}
}
