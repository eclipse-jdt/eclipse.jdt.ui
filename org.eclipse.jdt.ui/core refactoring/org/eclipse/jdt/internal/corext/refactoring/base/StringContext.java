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
package org.eclipse.jdt.internal.corext.refactoring.base;

import org.eclipse.jdt.core.ISourceRange;

import org.eclipse.jdt.internal.corext.Assert;

public class StringContext extends Context {
	private String fSource;
	private ISourceRange fSourceRange;
	
	public StringContext(String source, ISourceRange sourceRange){
		Assert.isNotNull(source);
		Assert.isNotNull(sourceRange);
		fSource= source;
		fSourceRange= sourceRange;
	}
	
	public String getSource() {
		return fSource;
	}

	public ISourceRange getSourceRange() {
		return fSourceRange;
	}
}
