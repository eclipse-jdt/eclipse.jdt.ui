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
package org.eclipse.ltk.internal.refactoring.core;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.ltk.refactoring.core.RenameData;


public class RenameFieldData extends RenameData {
	
	private String fNewGetterName;
	private String fNewSetterName;

	public RenameFieldData(String newName, boolean updateReferences, String newGetterName, String newSetterName) {
		super(newName, updateReferences);
		Assert.isNotNull(newGetterName);
		Assert.isNotNull(newSetterName);
		fNewGetterName= newGetterName;
		fNewSetterName= newSetterName;
	}
	public String getNewGetterName() {
		return fNewGetterName;
	}
	public String getNewSetterName() {
		return fNewSetterName;
	}
}
