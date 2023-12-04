/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copied from original {@link JavaModelUtil#getStaticImportFavorites(ICompilationUnit, String, boolean, String[])}
 *
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.util;

import java.util.HashSet;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.CompletionRequestor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

/**
 * Copied from {@link JavaModelUtil#getStaticImportFavorites(ICompilationUnit, String, boolean, String[])}
 * If code completion needs to be called multiple times on a given compilation unit,
 * this class will perform the operations more efficiently by re-using a working copy.
 */
public class StaticImportFavoritesCompletionInvoker {
	private ICompilationUnit fCu;
	private ICompilationUnit fNewCU;
	private String[] fFavourites;

	public StaticImportFavoritesCompletionInvoker (ICompilationUnit cu, String[] favourites) {
		fCu= cu;
		fFavourites= favourites;
	}

	public void destroy () throws JavaModelException {
		if (fNewCU != null) {
			fNewCU.discardWorkingCopy();
		}
	}

	public String [] getStaticImportFavorites (final String elementName, boolean isMethod) throws JavaModelException {
		if (fNewCU == null) {
			fNewCU= fCu.getWorkingCopy(null);
		}

		String[] res = createNewCompilationUnit(elementName);
		if (res.length != 2) {
			return new String[0];
		}
		String dummyCuContent = res[0];
		int offset= Integer.parseInt(res[1]);
		fNewCU.getBuffer().setContents(dummyCuContent);
		final HashSet<String> result= new HashSet<>();
		CompletionRequestor requestor= new CompletionRequestor(true) {
			@Override
			public void accept(CompletionProposal proposal) {
				if (elementName.equals(new String(proposal.getName()))) {
					for (CompletionProposal curr : proposal.getRequiredProposals()) {
						if (curr.getKind() == CompletionProposal.METHOD_IMPORT || curr.getKind() == CompletionProposal.FIELD_IMPORT) {
							result.add(JavaModelUtil.concatenateName(Signature.toCharArray(curr.getDeclarationSignature()), curr.getName()));
						}
					}
				}
			}
		};

		if (isMethod) {
			requestor.setIgnored(CompletionProposal.METHOD_REF, false);
			requestor.setAllowsRequiredProposals(CompletionProposal.METHOD_REF, CompletionProposal.METHOD_IMPORT, true);
		} else {
			requestor.setIgnored(CompletionProposal.FIELD_REF, false);
			requestor.setAllowsRequiredProposals(CompletionProposal.FIELD_REF, CompletionProposal.FIELD_IMPORT, true);
		}
		requestor.setFavoriteReferences(fFavourites);
		fNewCU.codeComplete(offset, requestor, new NullProgressMonitor());
		return result.toArray(new String[result.size()]);
	}

	private String[] createNewCompilationUnit (final String elementName) {
		StringBuilder dummyCU= new StringBuilder();
		String packName= fCu.getParent().getElementName();
		IType type= fCu.findPrimaryType();
		if (type == null)
			return new String[0];

		if (packName.length() > 0) {
			dummyCU.append("package ").append(packName).append(';'); //$NON-NLS-1$
		}
		dummyCU.append("public class ").append(type.getElementName()).append("{\n static {\n").append(elementName); // static initializer  //$NON-NLS-1$//$NON-NLS-2$
		String offset = Integer.toString(dummyCU.length());
		dummyCU.append("\n}\n }"); //$NON-NLS-1$
		return new String [] {dummyCU.toString(), offset};
	}
}
