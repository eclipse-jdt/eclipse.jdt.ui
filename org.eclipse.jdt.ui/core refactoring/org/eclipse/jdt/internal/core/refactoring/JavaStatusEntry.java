/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatusEntry;
import org.eclipse.jdt.internal.core.refactoring.util.Binding2JavaModel;

/**
 * A special <code>RefactoringStatusEntry</code> that knows about
 * IJavaElements.
 */
public class JavaStatusEntry {

	//---- Helpers for IMember ----------------------------------------------------

	/**
	 * Creates an entry with <code>RefactoringStatus.INFO</code> status.
	 * @param msg message
	 * @param member the member that has caused the error
	 */
	public static RefactoringStatusEntry createInfo(String msg, IMember member) {
		return create(msg, RefactoringStatus.INFO, member);
	}
	
	/**
	 * Creates an entry with <code>RefactoringStatus.WARNING</code> status.
	 * @param msg message
	 * @param member the member that has caused the error
	 */
	public static RefactoringStatusEntry createWarning(String msg, IMember member) {
		return create(msg, RefactoringStatus.WARNING, member);
	}
	
	/**
	 * Creates an entry with <code>RefactoringStatus.ERROR</code> status.
	 * @param msg message
	 * @param member the member that has caused the error
	 */
	public static RefactoringStatusEntry createError(String msg, IMember member) {
		return create(msg, RefactoringStatus.ERROR, member);
	}
	
	/**
	 * Creates an entry with <code>RefactoringStatus.FATAL</code> status.
	 * @param msg message
	 * @param member the member that has caused the error
	 */
	public static RefactoringStatusEntry createFatal(String msg, IMember member) {
		return create(msg, RefactoringStatus.FATAL, member);
	}
	
	private static RefactoringStatusEntry create(String msg, int severity, IMember member) {
		Object resource= getResource(member);
		ISourceRange range= getSourceRange(member);
		if (resource != null && range != null)
			return new RefactoringStatusEntry(msg, severity, resource, range);
		else
			return new RefactoringStatusEntry(msg, severity);
	}
	
	private static Object getResource(IMember member) {
		if (member == null)
			return null;
		if (member.isBinary())
			return member.getClassFile();
		else
			return member.getCompilationUnit();	
	}
	
	private static ISourceRange getSourceRange(IMember member) {
		try {
			return member == null ? null : member.getSourceRange();
		} catch (JavaModelException e) {
		}
		return null;
	}	
	
	//---- Helpers for MethodBinding -------------------------------------------------------------
	
	/**
	 * Creates an entry with <code>RefactoringStatus.INFO</code> status.
	 * @param msg message
	 * @param method the method that has caused the error.
	 * @param project the project which class path is used to convert the method binding
	 * 	into an <code>IMethod<.code>
	 */
	public static RefactoringStatusEntry createInfo(String msg, MethodBinding method, IJavaProject project) {
		return create(msg, RefactoringStatus.INFO, method, project);
	}
	
	/**
	 * Creates an entry with <code>RefactoringStatus.WARNING</code> status.
	 * @param msg message
	 * @param method the method that has caused the error.
	 * @param project the project which class path is used to convert the method binding
	 * 	into an <code>IMethod<.code>
	 */
	public static RefactoringStatusEntry createWarning(String msg, MethodBinding method, IJavaProject project) {
		return create(msg, RefactoringStatus.WARNING, method, project);
	}
	
	/**
	 * Creates an entry with <code>RefactoringStatus.ERROR</code> status.
	 * @param msg message
	 * @param method the method that has caused the error.
	 * @param project the project which class path is used to convert the method binding
	 * 	into an <code>IMethod<.code>
	 */
	public static RefactoringStatusEntry createError(String msg, MethodBinding method, IJavaProject project) {
		return create(msg, RefactoringStatus.ERROR, method, project);
	}
	
	/**
	 * Creates an entry with <code>RefactoringStatus.FATAL</code> status.
	 * @param msg message
	 * @param method the method that has caused the error.
	 * @param project the project which class path is used to convert the method binding
	 * 	into an <code>IMethod<.code>
	 */
	public static RefactoringStatusEntry createFatal(String msg, MethodBinding method, IJavaProject project) {
		return create(msg, RefactoringStatus.FATAL, method, project);
	}
	
	private static RefactoringStatusEntry create(String msg, int severity, MethodBinding method, IJavaProject scope) {
		ReferenceBinding declaringClass= method.declaringClass;
		IMethod mr= null;
		try {
			IType resource= Binding2JavaModel.find(declaringClass, scope);
			if (resource != null)
				mr= Binding2JavaModel.find(method, resource);
		} catch (JavaModelException e) {
		}
		return create(msg, severity, mr);
	}
}

