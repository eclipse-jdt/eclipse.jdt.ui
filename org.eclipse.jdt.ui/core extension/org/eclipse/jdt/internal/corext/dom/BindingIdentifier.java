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

import org.eclipse.jdt.core.dom.IBinding;

import org.eclipse.jdt.internal.corext.Assert;

public class BindingIdentifier {
	
	private IBinding fBinding;
	private String fKey;
	
	public BindingIdentifier(IBinding binding) {
		Assert.isNotNull(binding);
		fBinding= binding;
		fKey= fBinding.getKey();
	}
	
	public boolean matches(IBinding otherBinding) {
		if (otherBinding == null)
			return false;
		if (fBinding == otherBinding)
			return true;
			
		String otherKey= otherBinding.getKey();
				
		if (fKey != null && otherKey != null)
			return fKey.equals(otherKey);
			
		return false;
	}
}
