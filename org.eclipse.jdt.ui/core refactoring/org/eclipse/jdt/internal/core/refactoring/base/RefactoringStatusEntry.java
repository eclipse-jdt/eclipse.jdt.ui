/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.base;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.internal.core.refactoring.Assert;

/**
 * An immutable tuple (message, severity) representing an entry in the list in <code>RefactoringStatus</code>.
 * Clients can instantiate.
 * This class is not intented to be subclassed.
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
 */
public class RefactoringStatusEntry{
	
	private String fMessage;
	private int fSeverity;
	private IResource fResource;
	private ISourceRange fSourceRange;
	
	/**
	 * Creates an entry with the given severity.
	 * @param severity severity
	 * @param msg message
	 */
	RefactoringStatusEntry(String msg, int severity, IResource resource, ISourceRange sourceRange){
		Assert.isTrue(severity == RefactoringStatus.INFO 
				   || severity == RefactoringStatus.WARNING
				   || severity == RefactoringStatus.ERROR
				   || severity == RefactoringStatus.FATAL);
		fMessage= msg;
		fSeverity= severity;
		fResource= resource;
		fSourceRange= sourceRange;
	}
	
	/**
	 * Creates an entry with the given severity. The corresponding resource and source range are set to <code>null</code>.
	 * @param severity severity
	 * @param msg message
	 */
	RefactoringStatusEntry(String msg, int severity){
		this(msg, severity, null, null);
	}
	
	/**
	 * Creates an entry with <code>RefactoringStatus.INFO</code> status.
	 * @param msg message
	 */
	public static RefactoringStatusEntry createInfo(String msg){
		return new RefactoringStatusEntry(msg, RefactoringStatus.INFO);
	}
	
	/**
	 * Creates an entry with <code>RefactoringStatus.INFO</code> status.
	 * @param msg message
	 */
	public static RefactoringStatusEntry createInfo(String msg, IResource resource, ISourceRange range){
		return new RefactoringStatusEntry(msg, RefactoringStatus.INFO, resource, range);
	}
	/**
	 * Creates an entry with <code>RefactoringStatus.WARNING</code> status.
	 * @param msg message
	 */	
	public static RefactoringStatusEntry createWarning(String msg){
		return new RefactoringStatusEntry(msg, RefactoringStatus.WARNING);
	}

	/**
	 * Creates an entry with <code>RefactoringStatus.WARNING</code> status.
	 * @param msg message
	 */	
	public static RefactoringStatusEntry createWarning(String msg, IResource resource, ISourceRange range){
		return new RefactoringStatusEntry(msg, RefactoringStatus.WARNING, resource, range);
	}
	
	/**
	 * Creates an entry with <code>RefactoringStatus.ERROR</code> status.
	 * @param msg message
	 */	
	public static RefactoringStatusEntry createError(String msg){
		return new RefactoringStatusEntry(msg, RefactoringStatus.ERROR);
	}

	/**
	 * Creates an entry with <code>RefactoringStatus.ERROR</code> status.
	 * @param msg message
	 */		
	public static RefactoringStatusEntry createError(String msg, IResource resource, ISourceRange range){
		return new RefactoringStatusEntry(msg, RefactoringStatus.ERROR, resource, range);
	}
	
	/**
	 * Creates an entry with <code>RefactoringStatus.FATAL</code> status.
	 * @param msg message
	 */	
	public static RefactoringStatusEntry createFatal(String msg){
		return new RefactoringStatusEntry(msg, RefactoringStatus.FATAL);
	}

	/**
	 * Creates an entry with <code>RefactoringStatus.FATAL</code> status.
	 * @param msg message
	 */	
	public static RefactoringStatusEntry createFatal(String msg, IResource resource, ISourceRange range){
		return new RefactoringStatusEntry(msg, RefactoringStatus.FATAL, resource, range);
	}
	
	/**
	 * @return <code>true</code> iff (severity == <code>RefactoringStatus.FATAL</code>).
	 */
	public boolean isFatalError(){
		return fSeverity == RefactoringStatus.FATAL;
	}
	
	/**
	 * @return <code>true</code> iff (severity == <code>RefactoringStatus.ERROR</code>).
	 */
	public boolean isError(){
		return fSeverity == RefactoringStatus.ERROR;
	}
	
	/**
	 * @return <code>true</code> iff (severity == <code>RefactoringStatus.WARNING</code>).
	 */
	public boolean isWarning(){
		return fSeverity == RefactoringStatus.WARNING;
	}
	
	/**
	 * @return <code>true</code> iff (severity == <code>RefactoringStatus.INFO</code>).
	 */
	public boolean isInfo(){
		return fSeverity == RefactoringStatus.INFO;
	}

	/**
	 * @return message.
	 */
	public String getMessage(){
		return fMessage;
	}
	/**
	 * @return severity level.
	 * @see RefactoringStatus.INFO
	 * @see RefactoringStatus.WARNING
	 * @see RefactoringStatus.ERROR
	 */	
	public int getSeverity() {
		return fSeverity;
	}

	public IResource getCorrespondingResource(){
		return fResource;
	}
	
	public ISourceRange getSourceRange(){
		return fSourceRange;
	} 
	/* non java-doc
	 * for debugging only
	 */
	public String toString(){
		String pathString= fResource == null ? "<Unspecified resource>": fResource.getFullPath().toString();
		String rangeString= fSourceRange == null ? "<Unspecified range>": fSourceRange.toString();	
		return RefactoringStatus.getSeverityString(fSeverity) + ": " + fMessage + " in " + pathString + " " + rangeString;
	}
}
