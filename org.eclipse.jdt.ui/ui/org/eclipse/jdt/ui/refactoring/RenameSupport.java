/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.ui.refactoring;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.corext.refactoring.rename.RenameCompilationUnitRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameFieldRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameJavaProjectRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameMethodRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenamePackageRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameSourceFolderRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameTypeRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IRenameRefactoring;

import org.eclipse.jdt.internal.ui.refactoring.RefactoringSupport;
import org.eclipse.jdt.internal.ui.reorg.IRefactoringRenameSupport;

/**
 * Central access point to launch rename refactorings. Refactorings triggered
 * through this class will perform the following steps:
 * <ul>
 *   <li>check if the element can be renamed</li>
 *   <li>save all unsaved files</li>
 *   <li>open a corresponding refactoring dialog to gather user input</li>
 *   <li>execute or cancel the refactoring depending on user action</li>
 * </ul>
 * 
 * @since 2.1
 */
public class RenameSupport {

	private IRefactoringRenameSupport fSupport;
	private IJavaElement fElement;
	
	/**
	 * Checks whether the rename support can actually rename the Java
	 * element passed to the create method.
	 * @return <code>true</code> if the element can be renamed; otherwise
	 * <code>false</code> is returned.
	 * @throws CoreException if an unexpected exception occurs while checking
	 * renaming.
	 */
	public boolean canRename() throws CoreException {
		return fSupport.canRename(fElement);
	}
	
	/**
	 * Opens the refactoring dialog for this rename support.
	 * 
	 * @param parent a shell used as a parent for the refactoring dialog.
	 * @throws CoreException if an unexpected exception occurs while opening the
	 * dialog.
	 */
	public void openDialog(Shell parent) throws CoreException {
		fSupport.rename(parent, fElement);
	}
	
	/** Flag indicating that the refactoring has been performed. */
	public static final int PERFORMED= 0;
	
	/** Flag indicating that the refactoring has been canceled by the user. */
	public static final int CANCELED= 1;
	
	/** Flag indication that an unexpected exception has occured during the execution. */
	public static final int EXCEPTION= 2;
	
	/**
	 * Executes the rename is a quasi "headless" manner. This means that no
	 * input dialog pops up, but if needed a saving dialog and a dialog
	 * presenting problems collected during precondition checking is shown.
	 */
	public int run(Shell parent, IProgressMonitor pm, int stopSeverity) {
		return CANCELED;
	}

	/** Flag indication that no additional update is to be performed. */
	public static final int NONE= 0;
	
	/** Flag indicating that references are to be updated as well. */
	public static final int UPDATE_REFERENCES= 1 << 0;
	
	/** Flag indicating that Java doc comments are to be updated as well. */
	public static final int UPDATE_JAVADOC_COMMENTS= 1 << 1;
	
	/** Flag indicating that regular comments are to be updated as well. */
	public static final int UPDATE_REGULAR_COMMENTS= 1 << 2;
	
	/** Flag indicating that string literals are to be updated as well. */
	public static final int UPDATE_STRING_LITERALS= 1 << 3;

	/** Flag indicating that the getter method is to be updated as well. */
	public static final int UPDATE_GETTER_METHOD= 1 << 4;

	/** Flag indicating that the setter method is to be updated as well. */
	public static final int UPDATE_SETTER_METHOD= 1 << 5;

	private RenameSupport(IRefactoringRenameSupport support, IJavaElement element) {
		fSupport= support;
		fElement= element;
	}

	/**
	 * Creates a new rename support for the given <tt>IJavaProject</tt>.
	 * 
	 * @param project the <tt>IJavaProject</tt> to be renamed.
	 * @param newName the project's new name. <code>null</code> is a valid
	 * value indicating that no new name is provided.
	 * @param flags flags controlling additional parameters. Valid flags are
	 * <code>UPDATE_REFERENCES</code> or <code>NONE</code>.
	 * @return the <tt>RenameSupport</tt>.
	 * @throws CoreException if an unexpected error occured while creating
	 * the <tt>RenameSupport</tt>.
	 */
	public static RenameSupport create(IJavaProject project, String newName, int flags) throws CoreException {
		RefactoringSupport.JavaProject support= new RefactoringSupport.JavaProject(project);
		RenameJavaProjectRefactoring refactoring= support.getSpecificRefactoring();
		setNewName(refactoring, newName);
		refactoring.setUpdateReferences(updateReferences(flags));
		return new RenameSupport(support, project);
	}
	
	/**
	 * Creates a new rename support for the given <tt>IPackageFragmentRoot</tt>.
	 * 
	 * @param root the <tt>IPackageFragmentRoot</tt> to be renamed.
	 * @param newName the package fragment roor's new name. <code>null</code> is
	 * a valid value indicating that no new name is provided.
	 * @return the <tt>RenameSupport</tt>.
	 * @throws CoreException if an unexpected error occured while creating
	 * the <tt>RenameSupport</tt>.
	 */
	public static RenameSupport create(IPackageFragmentRoot root, String newName) throws CoreException {
		RefactoringSupport.SourceFolder support= new RefactoringSupport.SourceFolder(root);
		RenameSourceFolderRefactoring refactoring= support.getSpecificRefactoring();
		setNewName(refactoring, newName);
		return new RenameSupport(support, root);
	}
	
	/**
	 * Creates a new rename support for the given <tt>IPackageFragment</tt>.
	 * 
	 * @param fragment the <tt>IPackageFragment</tt> to be renamed.
	 * @param newName the package fragement's new name. <code>null</code> is a
	 * valid value indicating that no new name is provided.
	 * @param flags flags controlling additional parameters. Valid flags are
	 * <code>UPDATE_REFERENCES</code>, <code>UPDATE_JAVADOC_COMMENTS</code>,
	 * <code>UPDATE_REGULAR_COMMENTS</code> and
	 * <code>UPDATE_STRING_LITERALS</code>, or their bitwise OR, or
	 * <code>NONE</code>.
	 * @return the <tt>RenameSupport</tt>.
	 * @throws CoreException if an unexpected error occured while creating
	 * the <tt>RenameSupport</tt>.
	 */
	public static RenameSupport create(IPackageFragment fragment, String newName, int flags) throws CoreException {
		RefactoringSupport.PackageFragment support= new RefactoringSupport.PackageFragment(fragment);
		RenamePackageRefactoring refactoring= support.getSpecificRefactoring();
		setNewName(refactoring, newName);
		refactoring.setUpdateReferences(updateReferences(flags));
		refactoring.setUpdateJavaDoc(updateJavadocComments(flags));
		refactoring.setUpdateComments(updateRegularComments(flags));
		refactoring.setUpdateStrings(updateStringLiterals(flags));
		return new RenameSupport(support, fragment);
	}
	
	/**
	 * Creates a new rename support for the given <tt>ICompilationUnit</tt>.
	 * 
	 * @param unit the <tt>ICompilationUnit</tt> to be renamed.
	 * @param newName the compilation unit's new name. <code>null</code> is a
	 * valid value indicating that no new name is provided.
	 * @param flags flags controlling additional parameters. Valid flags are
	 * <code>UPDATE_REFERENCES</code>, <code>UPDATE_JAVADOC_COMMENTS</code>,
	 * <code>UPDATE_REGULAR_COMMENTS</code> and
	 * <code>UPDATE_STRING_LITERALS</code>, or their bitwise OR, or
	 * <code>NONE</code>.
	 * @return the <tt>RenameSupport</tt>.
	 * @throws CoreException if an unexpected error occured while creating
	 * the <tt>RenameSupport</tt>.
	 */
	public static RenameSupport create(ICompilationUnit unit, String newName, int flags) throws CoreException {
		RefactoringSupport.CompilationUnit support= new RefactoringSupport.CompilationUnit(unit);
		RenameCompilationUnitRefactoring refactoring= support.getSpecificRefactoring();
		setNewName(refactoring, newName);
		refactoring.setUpdateReferences(updateReferences(flags));
		refactoring.setUpdateJavaDoc(updateJavadocComments(flags));
		refactoring.setUpdateComments(updateRegularComments(flags));
		refactoring.setUpdateStrings(updateStringLiterals(flags));
		return new RenameSupport(support, unit);
	}
	
	/**
	 * Creates a new rename support for the given <tt>IType</tt>.
	 * 
	 * @param type the <tt>IType</tt> to be renamed.
	 * @param newName the type's new name. <code>null</code> is a valid value
	 * indicating that no new name is provided.
	 * @param flags flags controlling additional parameters. Valid flags are
	 * <code>UPDATE_REFERENCES</code>, <code>UPDATE_JAVADOC_COMMENTS</code>,
	 * <code>UPDATE_REGULAR_COMMENTS</code> and
	 * <code>UPDATE_STRING_LITERALS</code>, or their bitwise OR, or
	 * <code>NONE</code>.
	 * @return the <tt>RenameSupport</tt>.
	 * @throws CoreException if an unexpected error occured while creating
	 * the <tt>RenameSupport</tt>.
	 */
	public static RenameSupport create(IType type, String newName, int flags) throws CoreException {
		RefactoringSupport.Type support= new RefactoringSupport.Type(type);
		RenameTypeRefactoring refactoring= support.getSpecificRefactoring();
		setNewName(refactoring, newName);
		refactoring.setUpdateReferences(updateReferences(flags));
		refactoring.setUpdateJavaDoc(updateJavadocComments(flags));
		refactoring.setUpdateComments(updateRegularComments(flags));
		refactoring.setUpdateStrings(updateStringLiterals(flags));
		return new RenameSupport(support, type);
	}
	
	/**
	 * Creates a new rename support for the given <tt>IMethod</tt>.
	 * 
	 * @param method the <tt>IMethod</tt> to be renamed.
	 * @param newName the method's new name. <code>null</code> is a valid value
	 * indicating that no new name is provided.
	 * @param flags flags controlling additional parameters. Valid flags are
	 * <code>UPDATE_REFERENCES</code> or <code>NONE</code>.
	 * @return the <tt>RenameSupport</tt>.
	 * @throws CoreException if an unexpected error occured while creating
	 * the <tt>RenameSupport</tt>.
	 */
	public static RenameSupport create(IMethod method, String newName, int flags) throws CoreException {
		RefactoringSupport.Method support= new RefactoringSupport.Method(method);
		RenameMethodRefactoring refactoring= support.getSpecificRefactoring();
		setNewName(refactoring, newName);
		refactoring.setUpdateReferences(updateReferences(flags));
		return new RenameSupport(support, method);
	}
	
	/**
	 * Creates a new rename support for the given <tt>IField</tt>.
	 * 
	 * @param method the <tt>IField</tt> to be renamed.
	 * @param newName the field's new name. <code>null</code> is a valid value
	 * indicating that no new name is provided.
	 * @param flags flags controlling additional parameters. Valid flags are
	 * <code>UPDATE_REFERENCES</code>, <code>UPDATE_JAVADOC_COMMENTS</code>,
	 * <code>UPDATE_REGULAR_COMMENTS</code>,
	 * <code>UPDATE_STRING_LITERALS</code>, </code>UPDATE_GETTER_METHOD</code>
	 * and </code>UPDATE_SETTER_METHOD</code>, or their bitwise OR, or
	 * <code>NONE</code>.
	 * @return the <tt>RenameSupport</tt>.
	 * @throws CoreException if an unexpected error occured while creating
	 * the <tt>RenameSupport</tt>.
	 */
	public static RenameSupport create(IField field, String newName, int flags) throws CoreException {
		RefactoringSupport.Field support= new RefactoringSupport.Field(field);
		RenameFieldRefactoring refactoring= support.getSpecificRefactoring();
		setNewName(refactoring, newName);
		refactoring.setUpdateReferences(updateReferences(flags));
		refactoring.setUpdateJavaDoc(updateJavadocComments(flags));
		refactoring.setUpdateComments(updateRegularComments(flags));
		refactoring.setUpdateStrings(updateStringLiterals(flags));
		refactoring.setRenameGetter(updateGetterMethod(flags));
		refactoring.setRenameSetter(updateSetterMethod(flags));
		return new RenameSupport(support, field);
	}
	
	/**
	 * Creates a new <tt>RenameSupport</tt> for the given <tt>IJavaElement</tt>
	 * by forwarding the creation to one of the concrete create methods
	 * depending on the type of the given <tt>IJavaElement</tt>.
	 * @param element the <tt>IJavaElement</tt> to be renamed
	 * @param newName the Java element's new name. <code>null</code> is a valid
	 * value indicating that no new name is provided.
	 * @param flags flags controlling additional parameters. For a list of valid
	 * flags see the corresponding create methods of this class.
	 * @return the <tt>RenameSupport</tt>.
	 * @throws CoreException if an unexpected error occured while creating
	 * the <tt>RenameSupport</tt>.
	 * 
	 * @see #create(IJavaProject, int)
	 * @see #create(IPackageFragmentRoot)
	 * @see #create(IPackageFragment, int)
	 * @see #create(ICompilationUnit, int)
	 * @see #create(IType, int)
	 * @see #create(IMethod, int)
	 * @see #create(IField, int)
	 */
	public static RenameSupport create(IJavaElement element, String newName, int flags) throws CoreException {
		switch (element.getElementType()) {
			case IJavaElement.JAVA_PROJECT:
				return create((IJavaProject)element, newName, flags); 
			case IJavaElement.PACKAGE_FRAGMENT_ROOT:
				return create((IPackageFragmentRoot)element, newName); 
			case IJavaElement.PACKAGE_FRAGMENT:
				return create((IPackageFragment)element, newName, flags); 
			case IJavaElement.COMPILATION_UNIT:
				return create((ICompilationUnit)element, newName, flags); 
			case IJavaElement.TYPE:
				return create((IType)element, newName, flags); 
			case IJavaElement.METHOD:
				return create((IMethod)element, newName, flags); 
			case IJavaElement.FIELD:
				return create((IField)element, newName, flags); 
		}
		return null;
	}
	
	private static void setNewName(IRenameRefactoring refactoring, String newName) {
		if (newName != null)
			refactoring.setNewName(newName);
	}
	
	private static boolean updateReferences(int flags) {
		return (flags & UPDATE_REFERENCES) != 0;
	}
	
	private static boolean updateJavadocComments(int flags) {
		return (flags & UPDATE_JAVADOC_COMMENTS) != 0;
	}
	
	private static boolean updateRegularComments(int flags) {
		return (flags & UPDATE_REGULAR_COMMENTS) != 0;
	}
	
	private static boolean updateStringLiterals(int flags) {
		return (flags & UPDATE_STRING_LITERALS) != 0;
	}
	
	private static boolean updateGetterMethod(int flags) {
		return (flags & UPDATE_GETTER_METHOD) != 0;
	}
	
	private static boolean updateSetterMethod(int flags) {
		return (flags & UPDATE_SETTER_METHOD) != 0;
	}
}
