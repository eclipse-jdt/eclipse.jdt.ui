/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.breakers;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.core.refactoring.util.ResourceManager;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitDocumentProvider;

public class ResourcesManagerImpl extends ResourceManager {

	protected ICompilationUnit[] doGetWorkingCopies() {
		CompilationUnitDocumentProvider provider= JavaPlugin.getDefault().getCompilationUnitDocumentProvider();
		return provider.getAllWorkingCopies();
	}	
}

