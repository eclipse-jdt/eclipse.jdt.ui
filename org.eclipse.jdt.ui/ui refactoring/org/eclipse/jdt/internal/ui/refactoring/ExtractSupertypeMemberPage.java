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
package org.eclipse.jdt.internal.ui.refactoring;

/**
 * Wizard page for the extract supertype refactoring, which, apart from pull up
 * facilities, also allows to specify the types where to extract the supertype.
 * 
 * @since 3.2
 */
public final class ExtractSupertypeMemberPage extends PullUpMemberPage {

	/**
	 * Creates a new extract supertype member page.
	 * 
	 * @param page
	 *            the method page
	 */
	public ExtractSupertypeMemberPage(final PullUpMethodPage page) {
		super(page);
		setMessage(RefactoringMessages.ExtractSupertypeMemberPage_page_title);
	}
}