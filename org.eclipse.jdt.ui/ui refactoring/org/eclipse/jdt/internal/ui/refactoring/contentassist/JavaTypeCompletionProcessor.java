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

package org.eclipse.jdt.internal.ui.refactoring.contentassist;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;

public class JavaTypeCompletionProcessor extends CUPositionCompletionProcessor {
	
	private static final String DUMMY_CLASS_NAME= "$$__$$"; //$NON-NLS-1$
	
	/**
	 * The CU name to be used if no parent ICompilationUnit is available.
	 * The main type of this class will be filtered out from the proposals list.
	 */
	public static final String DUMMY_CU_NAME= DUMMY_CLASS_NAME + ".java"; //$NON-NLS-1$
	
	private static final String CU_START= "public class " + DUMMY_CLASS_NAME + " { ";  //$NON-NLS-1$ //$NON-NLS-2$
	private static final String CU_END= " }"; //$NON-NLS-1$
	
	/**
	 * Creates a <code>JavaTypeCompletionProcessor</code>.
	 * The completion context must be set via {@link #setPackageFragment(IPackageFragment)}.
	 * 
	 * @param enableBaseTypes complete java base types iff <code>true</code>
	 * @param enableVoid complete <code>void</code> base type iff <code>true</code>
	 */
	public JavaTypeCompletionProcessor(boolean enableBaseTypes, boolean enableVoid) {
		super(new TypeCompletionRequestor(enableBaseTypes, enableVoid));
	}

	/**
	 * @param packageFragment the new completion context
	 */
	public void setPackageFragment(IPackageFragment packageFragment) {
		//TODO: Some callers have a better completion context and should include imports
		// and nested classes of their declaring CU in WC's source.
		setCompletionContext(packageFragment.getCompilationUnit(DUMMY_CU_NAME), CU_START, CU_END);
	}
	
	public void setExtendsCompletionContext(IPackageFragment packageFragment) {
		ICompilationUnit cu= packageFragment.getCompilationUnit(DUMMY_CU_NAME);
		setCompletionContext(cu, "public class " + DUMMY_CLASS_NAME + " extends ", " {}"); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
	}

	public void setImplementsCompletionContext(IPackageFragment packageFragment) {
		ICompilationUnit cu= packageFragment.getCompilationUnit(DUMMY_CU_NAME);
		setCompletionContext(cu, "public class " + DUMMY_CLASS_NAME + " implements ", " {}"); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
	}
	
	protected static class TypeCompletionRequestor extends CUPositionCompletionRequestor {
		private static final String VOID= "void"; //$NON-NLS-1$
		private static final List BASE_TYPES= Arrays.asList(
			new String[] {"boolean", "byte", "char", "double", "float", "int", "long", "short"});  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
		
		private boolean fEnableBaseTypes;
		private boolean fEnableVoid;
		
		public TypeCompletionRequestor(boolean enableBaseTypes, boolean enableVoid) {
			fEnableBaseTypes= enableBaseTypes;
			fEnableVoid= enableVoid;
		}

		public void acceptClass(char[] packageName, char[] typeName, char[] completionName, int modifiers, int start, int end, int relevance) {
			if (isDummyClass(typeName))
				return;
			ImageDescriptor descriptor= JavaElementImageProvider.getTypeImageDescriptor(false, false, false, modifiers);
			addAdjustedTypeCompletion(packageName, typeName, completionName, start, end, relevance, descriptor);
		}
		
		public void acceptInterface(char[] packageName, char[] typeName, char[] completionName, int modifiers, int start, int end, int relevance) {
			if (isDummyClass(typeName))
				return;
			ImageDescriptor descriptor= JavaElementImageProvider.getTypeImageDescriptor(true, false, false, modifiers);
			addAdjustedTypeCompletion(packageName, typeName, completionName, start, end, relevance, descriptor);
		}
		
		public void acceptKeyword(char[] keywordName, int start, int end, int relevance) {
			if (! fEnableBaseTypes)
				return;
			String keyword= new String(keywordName);
			if ( (fEnableVoid && VOID.equals(keyword)) || (fEnableBaseTypes && BASE_TYPES.contains(keyword)) )
				addAdjustedCompletion(keyword, keyword, start, end, relevance, null);
		}
		
		public void acceptPackage(char[] packageName, char[] completionName, int start, int end, int relevance) {
			addAdjustedCompletion(new String(packageName), new String(completionName), start, end, relevance, JavaPluginImages.DESC_OBJS_PACKAGE);
		}
		
		public void acceptType(char[] packageName, char[] typeName, char[] completionName, int start, int end, int relevance) {
			if (isDummyClass(typeName))
				return;
			addAdjustedTypeCompletion(packageName, typeName, completionName, start, end, relevance, JavaPluginImages.DESC_OBJS_CLASS);
		}
		
		private static boolean isDummyClass(char[] typeName) {
			return new String(typeName).equals(DUMMY_CLASS_NAME);
		}
	}
}
