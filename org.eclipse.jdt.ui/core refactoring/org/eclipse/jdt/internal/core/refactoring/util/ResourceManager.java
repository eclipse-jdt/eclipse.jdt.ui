/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.util;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.core.refactoring.breakers.ResourcesManagerImpl;

public abstract class ResourceManager {

	private static ResourceManager fgInstance= new ResourcesManagerImpl();

	/**
	 * Returns the currently managed working copies.
	 * 
	 * @return the currently managed working copies
	 */
	public static ICompilationUnit[] getWorkingCopies() {
		return fgInstance.doGetWorkingCopies();
	}
	
	protected abstract ICompilationUnit[] doGetWorkingCopies();
}

