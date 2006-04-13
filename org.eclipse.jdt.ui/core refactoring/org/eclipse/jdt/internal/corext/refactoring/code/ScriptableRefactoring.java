/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.code;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;

import org.eclipse.jdt.internal.corext.refactoring.tagging.ICommentProvider;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IScriptableRefactoring;

/**
 * Partial implementation of a scriptable refactoring which provides a comment
 * for the history.
 * 
 * @since 3.2
 */
public abstract class ScriptableRefactoring extends Refactoring implements IScriptableRefactoring, ICommentProvider {

	/** The comment */
	private String fComment;

	/**
	 * {@inheritDoc}
	 */
	public boolean canEnableComment() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getComment() {
		return fComment;
	}

	/**
	 * {@inheritDoc}
	 */
	public RefactoringStatus initialize(RefactoringArguments arguments) {
		return new RefactoringStatus();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setComment(String comment) {
		fComment= comment;
	}
}