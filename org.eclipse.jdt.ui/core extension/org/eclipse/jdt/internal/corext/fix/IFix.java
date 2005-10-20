package org.eclipse.jdt.internal.corext.fix;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ltk.core.refactoring.TextChange;

import org.eclipse.jdt.core.ICompilationUnit;

/**
 * An <code>IFix</code> can calculate a <code>TextChange</code>
 * which applayed to a <code>ICompilationUnit</code> will fix
 * one or several problems.
 * 
 * @since 3.2
 */
public interface IFix {
	
	/**
	 * A String describing what the <code>TextChange</code> returned by
	 * <code>createChange</code> will do.
	 * 
	 * @return The description, not null
	 */
	public abstract String getDescription();
	
	/**
	 * A <code>TextChange</code> which applayed to <code>getCompilationUnit</code>
	 * will fix a problem.
	 * 
	 * @return The change or null if no fix possible
	 * @throws CoreException
	 */
	public abstract TextChange createChange() throws CoreException;
	
	/**
	 * The <code>ICompilationUnit</code> on which <code>createChange</code> should
	 * be applayed to fix a problem.
	 * 
	 * @return The ICompilationUnit, not null
	 */
	public abstract ICompilationUnit getCompilationUnit();
}
