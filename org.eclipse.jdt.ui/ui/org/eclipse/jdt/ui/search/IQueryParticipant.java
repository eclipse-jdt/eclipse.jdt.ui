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
package org.eclipse.jdt.ui.search;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.internal.ui.search.QuerySpecification;

/**
 * This is the interface expected of extension to the extension point.
 * <code>org.eclipse.jdt.ui.queryParticipants</code>.
 * <p> 
 * A <code>IQueryParticipant</code> is called during the execution of a 
 * Java search query. It can report matches via an <code>ISearchRequestor</code> and 
 * may contribute a <code>ISearchUIParticipant</code> to help render the elements it contributes.
 * </p>
 * <p>
 * Clients may implement this interface.
 * </p>
 *
 * @since 3.0
 */
public interface IQueryParticipant {
	/**
	 * Executes the search described by the given <code>querySpecification</code>. And reports
	 * any matches through the given <code>requestor</code>.
	 * The interpretation of what a given Java search (e.g. "References to class Foo" means is up to the 
	 * participant).
	 * @param requestor The requestor to report matches to.
	 * @param querySpecification The specification of the query to run.
	 * @param monitor A monitor to report progress on.
	 * @throws CoreException
	 */
	void search(ISearchRequestor requestor, QuerySpecification querySpecification, IProgressMonitor monitor) throws CoreException;
	/**
	 * Returns the number of units of work estimated. The returned number should be normalized such
	 * that the number of ticks for the original java search job is 1000. For example if the participant
	 * uses the same amount of time as the java search, it should return 1000, if it uses half the time,
	 * it should return 500, etc.
	 * This method is supposed to give a quick estimate of the work to be done and is assumed
	 * to be much faster than the actual query.
	 * @return The number of ticks estimated
	 */
	int estimateTicks(QuerySpecification data);
	/**
	 * Gets the UI participant responsible for handling the display of any elements that can't 
	 * be handled by the java serach UI, i.e. elements that are not implementors if <code>IJavaElement</code>
	 * of <code>IResource</code>.
	 * A participant may return null if matches are only reported against <code>IResources</code> or <code>IJavaElements</code>.
	 * @return The UI participant for this query participant or <code>null</code>.
	 */
	ISearchUIParticipant getUIParticipant();
}
