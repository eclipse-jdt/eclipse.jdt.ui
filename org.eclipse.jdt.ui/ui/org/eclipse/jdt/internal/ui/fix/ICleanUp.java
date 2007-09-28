/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.IFix;

/**
 * A clean up can solve problems in a compilation unit.
 * <p>
 * The clean up is asked for its requirements through a call to
 * {@link #getRequirements()}. The clean up can i.e. request
 * an AST and define how to build this AST. It can base its
 * requirements on the options passed through {@link #setOptions(CleanUpOptions)}.
 * </p>
 * <p>
 * A context containing the information requested by the 
 * requirements are passed to {@link #createFix(CleanUpContext)}.
 * A fix capable of fixing the problems is returned by this function
 * if {@link #checkPreConditions(IJavaProject, ICompilationUnit[], IProgressMonitor)}
 * has returned a non fatal error status.
 * </p>
 * <p>
 * At the end {@link #checkPostConditions(IProgressMonitor)} is called.
 * </p>
 * <p>
 * Clients can implement this interface but should extend {@link AbstractCleanUp}
 * if possible.
 * 
 * @since 3.2
 */
public interface ICleanUp {
	
	/**
	 * A collection of clean up requirements.
	 * Instances of this class are returned by {@link ICleanUp#getRequirements()}
	 * 
	 * @since 3.4
	 */
	public class CleanUpRequirements {
		
		private final boolean fRequiresAST;
		private final Map fCompilerOptions;
		private final boolean fRequiresFreshAST;

		/**
		 * Create a new requirement collection
		 * 
		 * @param requiresAST true if an AST is required
		 * @param requiresFreshAST true if a fresh AST is required
		 * @param compilerOptions map of compiler options or <b>null</b> if no requirements
		 */
		protected CleanUpRequirements(boolean requiresAST, boolean requiresFreshAST, Map compilerOptions) {
			fRequiresAST= requiresAST;
			fRequiresFreshAST= requiresFreshAST;
			fCompilerOptions= compilerOptions;
		}
		
		/**
		 * Does this clean up require an AST? If <code>true</code> 
		 * then the clean up context passed to create fix 
		 * will have an AST attached. 
		 * <p>
		 * <strong>This should return <code>false</code> whenever possible 
		 * because creating an AST is expensive.</strong>
		 * </p>
		 * 
		 * @return true if createFix will need an AST
		 */
		public boolean requiresAST() {
			return fRequiresAST;
		}
		
		/**
		 * If true a fresh AST, containing all the changes from previous clean ups,
		 * will be created and passed in the context.
		 * <p>
		 * Has no effect if {@link #requiresAST()} returns <code>false</code>.
		 * </p>
		 * 
		 * @return true if the caller needs an up to date AST
		 */
		public boolean requiresFreshAST() {
			return fRequiresFreshAST;
		}
		
		/**
		 * Required compiler options.
		 * <p>
		 * Has no effect if {@link #requiresAST()} returns <code>false</code>.
		 * </p>
		 * 
		 * @return The options as map or <b>null</b>
		 * @see JavaCore 
		 */
		public Map getCompilerOptions() {
			return fCompilerOptions;
		}
		
	}
	
	/**
	 * Clean up requirement for save actions. They can request changed
	 * regions.
	 */
	public class SaveActionRequirements extends CleanUpRequirements {

		private final boolean fRequiresChangedRegions;

		protected SaveActionRequirements(boolean requiresAST, boolean requiresFreshAST, Map compilerOptions, boolean requiresChangedRegions) {
			super(requiresAST, requiresFreshAST, compilerOptions);
			fRequiresChangedRegions= requiresChangedRegions;
		}
		
		/**
		 * Does this clean up required to be informed about changed regions?
		 * The changed regions are the regions which have been changed between
		 * the last save state of the compilation unit and its current state.
		 * <p>
		 * Has only an effect if the clean up is used as save action.
		 * </p>
		 * <p>
		 * <strong>This should return <code>false</code> whenever possible
		 * because calculating the changed regions is expensive.</code>
		 * </p>
		 * 
		 * @return true if context must contain changed regions
		 */
		public boolean requiresChangedRegions() {
			return fRequiresChangedRegions;
		}
		
	}
	
	/**
	 * A context containing all information required by a clean up
	 * to create a fix
	 * 
	 * @since 3.4
	 */
	public class CleanUpContext {
		
		private final ICompilationUnit fUnit;
		private final CompilationUnit fAst;

		public CleanUpContext(ICompilationUnit unit, CompilationUnit ast) {
			fUnit= unit;
			fAst= ast;
		}
		
		/**
		 * @return the compilation unit to fix
		 */
		public ICompilationUnit getCompilationUnit() {
			return fUnit;
		}
		
		/**
		 * An AST build from the compilation unit to fix.
		 * Is <b>null</b> if CleanUpRequirements#requiresAST()
		 * returned <code>false</code>.
		 * The AST is guaranteed to contain changes made by previous
		 * clean ups only if CleanUpRequirements#requiresFreshAST()
		 * returned <code>true</code>.
		 * 
		 * @return an AST or <b>null</b> if none requested.
		 */
		public CompilationUnit getAST() {
			return fAst;
		}
	}

	/**
	 * Constant for default options kind for clean up.
	 */
	public static final int DEFAULT_CLEAN_UP_OPTIONS= 1;
	
	/**
	 * Constant for default options kind for save actions.
	 */
	public static final int DEFAULT_SAVE_ACTION_OPTIONS= 2;
	
	/**
	 * Returns the default options for the specified kind of clean up.
	 * Implementors can reuse the same keys for each kind.
	 * 
	 * @param kind the kind for which to get the default options
	 * @return the default options
	 * 
	 * @see ICleanUp#DEFAULT_CLEAN_UP_OPTIONS
	 * @see ICleanUp#DEFAULT_SAVE_ACTION_OPTIONS
	 */
	public CleanUpOptions getDefaultOptions(int kind);
	
	/**
	 * @param options the options to use
	 */
	public void setOptions(CleanUpOptions options);
	
	/**
	 * Human readable description for each operation this clean up will execute.
	 * 
	 * @return descriptions or <b>null</b>
	 */
	public String[] getDescriptions();
	
	/**
	 * A code snippet which complies to the current settings.
	 * 
	 * @return A code snippet
	 */
	public abstract String getPreview();
	
	/**
	 * @return the requirements for used for {@link #createFix(CleanUpContext)} to work
	 */
	public CleanUpRequirements getRequirements();
	
	/**
	 * After call to checkPreConditions clients will start creating fixes for
	 * <code>compilationUnits</code> in <code>project</code> unless the
	 * result of checkPreConditions contains a fatal error
	 * 
	 * @param project
	 *            The project to clean up
	 * @param compilationUnits
	 *            The compilation Units to clean up, all member of project
	 * @param monitor
	 *            the monitor to show progress
	 * @return the result of the precondition check, not null
	 * @throws CoreException if an unexpected error occurred
	 */
	public abstract RefactoringStatus checkPreConditions(IJavaProject project, ICompilationUnit[] compilationUnits, IProgressMonitor monitor) throws CoreException;
	
	/**
	 * Create an <code>IFix</code> which fixes all problems in
	 * <code>context</code> or <b>null</b> if nothing to fix.
	 * 
	 * @param context 
	 * 		a context containing all information requested by {@link #getRequirements()}
	 * @return the fix for the problems or <b>null</b> if nothing to fix
	 * @throws CoreException if an unexpected error occurred
	 */
	public abstract IFix createFix(CleanUpContext context) throws CoreException;
	
	/**
	 * Called when done cleaning up.
	 * 
	 * @param monitor
	 *            the monitor to show progress
	 * @return the result of the postcondition check, not null
	 * @throws CoreException if an unexpected error occurred
	 */
	public abstract RefactoringStatus checkPostConditions(IProgressMonitor monitor) throws CoreException;
	
}
