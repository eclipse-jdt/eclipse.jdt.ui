/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.base;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.internal.corext.refactoring.Assert;

/**
 * An immutable tuple (message, severity) representing an entry in the list in 
 * <code>RefactoringStatus</code>.
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
 */
public class RefactoringStatusEntry{
	
	private final String fMessage;
	private final int fSeverity;
	private final Context fContext;
	private final Object fData;
	private final int fCode;
	
	public RefactoringStatusEntry(String msg, int severity, Context context, Object data, int code){
		Assert.isTrue(severity == RefactoringStatus.INFO 
				   || severity == RefactoringStatus.WARNING
				   || severity == RefactoringStatus.ERROR
				   || severity == RefactoringStatus.FATAL);
		fMessage= msg;
		Assert.isNotNull(fMessage);
		fSeverity= severity;
		fContext= context;
		fData= data;
		fCode= code;
	}

	/**
	 * Creates an entry with the given severity.
	 * @param msg message
	 * @param severity severity
	 * @param context a context which can be used to show more detailed information
	 * 	about this error in the UI
	 */
	public RefactoringStatusEntry(String msg, int severity, Context context){
		this(msg, severity, context, null, RefactoringStatusCodes.NONE);
	}
	
	/**
	 * Creates an entry with the given severity. The corresponding resource and source range are set to <code>null</code>.
	 * @param severity severity
	 * @param msg message
	 */
	public RefactoringStatusEntry(String msg, int severity) {
		this(msg, severity, null);
	}
	
	/**
	 * Creates an entry with <code>RefactoringStatus.INFO</code> status.
	 * @param msg message
	 */
	public static RefactoringStatusEntry createInfo(String msg) {
		return new RefactoringStatusEntry(msg, RefactoringStatus.INFO);
	}
	
	/**
	 * Creates an entry with <code>RefactoringStatus.INFO</code> status.
	 * @param msg message
	 */
	public static RefactoringStatusEntry createInfo(String msg, Context context) {
		return new RefactoringStatusEntry(msg, RefactoringStatus.INFO, context);
	}

	/**
	 * Creates an entry with <code>RefactoringStatus.WARNING</code> status.
	 * @param msg message
	 */	
	public static RefactoringStatusEntry createWarning(String msg) {
		return new RefactoringStatusEntry(msg, RefactoringStatus.WARNING);
	}

	/**
	 * Creates an entry with <code>RefactoringStatus.WARNING</code> status.
	 * @param msg message
	 */	
	public static RefactoringStatusEntry createWarning(String msg, Context context) {
		return new RefactoringStatusEntry(msg, RefactoringStatus.WARNING, context);
	}
	
	/**
	 * Creates an entry with <code>RefactoringStatus.ERROR</code> status.
	 * @param msg message
	 */	
	public static RefactoringStatusEntry createError(String msg) {
		return new RefactoringStatusEntry(msg, RefactoringStatus.ERROR);
	}

	/**
	 * Creates an entry with <code>RefactoringStatus.ERROR</code> status.
	 * @param msg message
	 */		
	public static RefactoringStatusEntry createError(String msg, Context context) {
		return new RefactoringStatusEntry(msg, RefactoringStatus.ERROR, context);
	}
	
	/**
	 * Creates an entry with <code>RefactoringStatus.FATAL</code> status.
	 * @param msg message
	 */	
	public static RefactoringStatusEntry createFatal(String msg) {
		return new RefactoringStatusEntry(msg, RefactoringStatus.FATAL);
	}

	/**
	 * Creates an entry with <code>RefactoringStatus.FATAL</code> status.
	 * @param msg message
	 */	
	public static RefactoringStatusEntry createFatal(String msg, Context context) {
		return new RefactoringStatusEntry(msg, RefactoringStatus.FATAL, context);
	}
	
	/**
	 * @return <code>true</code> iff (severity == <code>RefactoringStatus.FATAL</code>).
	 */
	public boolean isFatalError() {
		return fSeverity == RefactoringStatus.FATAL;
	}
	
	/**
	 * @return <code>true</code> iff (severity == <code>RefactoringStatus.ERROR</code>).
	 */
	public boolean isError() {
		return fSeverity == RefactoringStatus.ERROR;
	}
	
	/**
	 * @return <code>true</code> iff (severity == <code>RefactoringStatus.WARNING</code>).
	 */
	public boolean isWarning() {
		return fSeverity == RefactoringStatus.WARNING;
	}
	
	/**
	 * @return <code>true</code> iff (severity == <code>RefactoringStatus.INFO</code>).
	 */
	public boolean isInfo() {
		return fSeverity == RefactoringStatus.INFO;
	}

	/**
	 * @return message.
	 */
	public String getMessage() {
		return fMessage;
	}

	/**
	 * @return severity level.
	 * @see RefactoringStatus#INFO
	 * @see RefactoringStatus#WARNING
	 * @see RefactoringStatus#ERROR
	 * @see RefactoringStatus#FATAL
	 */	
	public int getSeverity() {
		return fSeverity;
	}

	/**
	 * Returns the context which can be used to show more detailed information
	 * regarding this status entry in the UI. The method may return <code>null
	 * </code> indicating that no context is available.
	 * 
	 * @return the status entry's context
	 */
	public Context getContext() {
		return fContext;
	}

	public Object getData() {
		return fData;
	}

	public int getCode() {
		return fCode;
	}
	
	/* non java-doc
	 * for debugging only
	 */
	public String toString() {
		String contextString= fContext == null ? "<Unspecified context>": fContext.toString(); //$NON-NLS-1$
		return 	"\n" //$NON-NLS-1$
				+ RefactoringStatus.getSeverityString(fSeverity) 
				+ ": "  //$NON-NLS-1$
				+ fMessage 
				+ "\nContext: " //$NON-NLS-1$
				+ contextString
				+ "\nData: "  //$NON-NLS-1$
				+ getData()
				+"\ncode: "  //$NON-NLS-1$
				+ fCode
				+ "\n";  //$NON-NLS-1$
	}
}
