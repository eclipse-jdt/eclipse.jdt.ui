/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.nls.search;

import java.util.Set;

import org.eclipse.jface.util.Assert;


class Properties extends java.util.Properties {
	
	private Set fDuplicateKeys;

	public Properties() {
	}
	
	public Properties(Set duplicateKeys) {
		super();
		Assert.isNotNull(duplicateKeys);
		fDuplicateKeys= duplicateKeys;
	}
	
	public Properties (Properties properties, Set duplicateKeys) {
		super(properties);
		Assert.isNotNull(duplicateKeys);
		fDuplicateKeys= duplicateKeys;
	}
	/**
	 * @see Map#put(Object, Object)
	 */
	public Object put(Object arg0, Object arg1) {
		if (arg0 != null && containsKey(arg0))
			fDuplicateKeys.add(arg0);
		return super.put(arg0, arg1);
	}
}
