/*
 * (c) Copyright 2002 IBM Corporation.
 * All Rights Reserved.
 */
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
