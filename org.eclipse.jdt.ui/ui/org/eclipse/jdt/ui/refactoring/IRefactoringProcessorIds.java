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
package org.eclipse.jdt.ui.refactoring;

import org.eclipse.jdt.internal.corext.refactoring.rename.RenameCompilationUnitProcessor;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameFieldProcessor;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameJavaProjectProcessor;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameMethodProcessor;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenamePackageProcessor;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameResourceProcessor;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameSourceFolderProcessor;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameTypeProcessor;
import org.eclipse.jdt.internal.corext.refactoring.reorg.JavaDeleteProcessor;
import org.eclipse.jdt.internal.corext.refactoring.reorg.JavaMoveProcessor;

/**
 * Interface to define the processor ids provided by the JDT refactoring.
 * 
 * @since 3.0
 */
public interface IRefactoringProcessorIds {

	/**
	 * Processor ID of the rename Java project processor
	 * (value <code>"org.eclipse.jdt.ui.renameJavaProjectProcessor"</code>)
	 */
	public static String RENAME_JAVA_PROJECT_PROCESSOR= RenameJavaProjectProcessor.IDENTIFIER;
	
	/**
	 * Processor ID of the rename source folder
	 * (value <code>"org.eclipse.jdt.ui.renameSourceFolderProcessor"</code>)
	 */
	public static String RENAME_SOURCE_FOLDER_PROCESSOR= RenameSourceFolderProcessor.IDENTIFIER;
	
	/**
	 * Processor ID of the rename package fragment processor
	 * (value <code>"org.eclipse.jdt.ui.renamePackageProcessor"</code>)
	 */
	public static String RENAME_PACKAGE_FRAGMENT_PROCESSOR= RenamePackageProcessor.IDENTIFIER;
	
	/**
	 * Processor ID of the rename compilation unit processor
	 * (value <code>"org.eclipse.jdt.ui.renameCompilationUnitProcessor"</code>)
	 */
	public static String RENAME_COMPILATION_UNIT_PROCESSOR= RenameCompilationUnitProcessor.IDENTIFIER;
	
	/**
	 * Processor ID of the rename type processor
	 * (value <code>"org.eclipse.jdt.ui.renameTypeProcessor"</code>)
	 */
	public static String RENAME_TYPE_PROCESSOR= RenameTypeProcessor.IDENTIFIER;
	
	/**
	 * Processor ID of the rename method processor
	 * (value <code>"org.eclipse.jdt.ui.renameMethodProcessor"</code>)
	 */
	public static String RENAME_METHOD_PROCESSOR= RenameMethodProcessor.IDENTIFIER;
	
	/**
	 * Processor ID of the rename type processor
	 * (value <code>"org.eclipse.jdt.ui.renameFieldProcessor"</code>)
	 */
	public static String RENAME_FIELD_PROCESSOR= RenameFieldProcessor.IDENTIFIER;
	
	/**
	 * Processor ID of the rename resource processor
	 * (value <code>"org.eclipse.jdt.ui.renameResourceProcessor"</code>)
	 */
	public static String RENAME_RESOURCE_PROCESSOR= RenameResourceProcessor.IDENTIFIER;
	
	/**
	 * Processor ID of the move resource processor
	 * (value <code>"org.eclipse.jdt.ui.MoveProcessor"</code>)
	 */
	public static String MOVE_PROCESSOR= JavaMoveProcessor.IDENTIFIER;
	
	/**
	 * Processor ID of the move static member processor
	 * (value <code>"org.eclipse.jdt.ui.MoveStaticMemberProcessor"</code>)
	 */
	public static String MOVE_STATIC_MEMBERS_PROCESSOR= "org.eclipse.jdt.ui.MoveStaticMemberProcessor"; //$NON-NLS-1$
	
	/**
	 * Processor ID of the delete resource processor
	 * (value <code>"org.eclipse.jdt.ui.DeleteProcessor"</code>)
	 */
	public static String DELETE_PROCESSOR= JavaDeleteProcessor.IDENTIFIER;
}
