/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java;

import java.io.IOException;
import java.io.Reader;

import org.eclipse.jface.text.Assert;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavadocContentAccess;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.javadoc.JavaDoc2HTMLTextReader;

/**
 * Proposal info that computes the javadoc lazily when it is queried.
 * <p>
 * TODO this class only subclasses ProposalInfo to be compatible - it does not
 * use any thing from it.
 * </p>
 *
 * @since 3.1
 */
public abstract class MemberProposalInfo extends ProposalInfo {
	/* configuration */
	protected final IJavaProject fJavaProject;
	protected final CompletionProposal fProposal;

	/* cache filled lazily */
	private boolean fJavadocResolved= false;
	private String fJavadoc= null;

	/**
	 * Creates a new proposal info.
	 *
	 * @param project the java project to reference when resolving types
	 * @param proposal the proposal to generate information for
	 */
	public MemberProposalInfo(IJavaProject project, CompletionProposal proposal) {
		super(null);
		Assert.isNotNull(project);
		Assert.isNotNull(proposal);
		fJavaProject= project;
		fProposal= proposal;
	}

	/**
	 * Gets the text for this proposal info formatted as HTML, or
	 * <code>null</code> if no text is available.
	 *
	 * @return the additional info text
	 */
	public final String getInfo() {
		if (!fJavadocResolved) {
			fJavadocResolved= true;
			fJavadoc= computeInfo();
		}
		return fJavadoc;
	}

	/**
	 * Gets the text for this proposal info formatted as HTML, or
	 * <code>null</code> if no text is available.
	 *
	 * @return the additional info text
	 */
	private String computeInfo() {
		try {
			return extractJavadoc(resolveMember());
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		} catch (IOException e) {
			JavaPlugin.log(e);
		}
		return null;
	}

	/**
	 * Extracts the javadoc for the given <code>IMember</code> and returns it
	 * as HTML.
	 *
	 * @param member the member to get the documentation for
	 * @return the javadoc for <code>member</code> or <code>null</code> if
	 *         it is not available
	 * @throws JavaModelException if accessing the javadoc fails
	 * @throws IOException if reading the javadoc fails
	 */
	private String extractJavadoc(IMember member) throws JavaModelException, IOException {
		if (member != null) {
			Reader reader= JavadocContentAccess.getContentReader(member, true);
			if (reader != null) {
				return new JavaDoc2HTMLTextReader(reader).getString();
			}
		}
		return null;
	}

	/**
	 * Resolves the member described by the receiver and returns it if found.
	 * Returns <code>null</code> if no corresponding member can be found.
	 *
	 * @return the resolved member or <code>null</code> if none is found
	 * @throws JavaModelException if accessing the java model fails
	 */
	protected abstract IMember resolveMember() throws JavaModelException;


}
