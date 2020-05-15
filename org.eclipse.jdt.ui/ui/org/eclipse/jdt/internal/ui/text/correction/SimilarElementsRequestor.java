/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.HashSet;

import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.CompletionRequestor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.manipulation.TypeKinds;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.util.TypeFilter;

import org.eclipse.jdt.internal.ui.text.CompletionTimeoutProgressMonitor;

public class SimilarElementsRequestor extends CompletionRequestor {

	private static final String[] PRIM_TYPES= { "boolean", "byte", "char", "short", "int", "long", "float", "double" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$

	private int fKind;
	private String fName;

	private HashSet<SimilarElement> fResult;
	private boolean fExcludeTestCode;

	private static boolean isTestSource(ICompilationUnit cu) {
		try {
			IJavaProject javaProject= cu.getJavaProject();
			if(javaProject==null) {
				return false;
			}
			IClasspathEntry[] resolvedClasspath= javaProject.getResolvedClasspath(true);
			final IPath resourcePath= cu.getResource().getFullPath();
			for (IClasspathEntry e : resolvedClasspath) {
				if (e.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					if (e.isTest()) {
						if (e.getPath().isPrefixOf(resourcePath)) {
							return true;
						}
					}
				}
			}
		} catch (JavaModelException e) {
			return false;
		}
		return false;
	}

	public static SimilarElement[] findSimilarElement(ICompilationUnit cu, Name name, int kind) throws JavaModelException {
		int pos= name.getStartPosition();
		int nArguments= -1;

		String identifier= ASTNodes.getSimpleNameIdentifier(name);
		String returnType= null;
		ICompilationUnit preparedCU= null;

		try {
			if (name.isQualifiedName()) {
				pos= ((QualifiedName) name).getName().getStartPosition();
			} else {
				pos= name.getStartPosition() + 1; // first letter must be included, other
			}
			Javadoc javadoc=  (Javadoc) ASTNodes.getParent(name, ASTNode.JAVADOC);
			if (javadoc != null) {
				preparedCU= createPreparedCU(cu, javadoc, name.getStartPosition());
				cu= preparedCU;
			}

			SimilarElementsRequestor requestor= new SimilarElementsRequestor(identifier, kind, nArguments, returnType, !isTestSource(cu));
			requestor.setIgnored(CompletionProposal.ANONYMOUS_CLASS_DECLARATION, true);
			requestor.setIgnored(CompletionProposal.ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION, true);
			requestor.setIgnored(CompletionProposal.KEYWORD, true);
			requestor.setIgnored(CompletionProposal.LABEL_REF, true);
			requestor.setIgnored(CompletionProposal.METHOD_DECLARATION, true);
			requestor.setIgnored(CompletionProposal.PACKAGE_REF, true);
			requestor.setIgnored(CompletionProposal.MODULE_REF, true);
			requestor.setIgnored(CompletionProposal.MODULE_DECLARATION, true);
			requestor.setIgnored(CompletionProposal.VARIABLE_DECLARATION, true);
			requestor.setIgnored(CompletionProposal.METHOD_REF, true);
			requestor.setIgnored(CompletionProposal.CONSTRUCTOR_INVOCATION, true);
			requestor.setIgnored(CompletionProposal.METHOD_REF_WITH_CASTED_RECEIVER, true);
			requestor.setIgnored(CompletionProposal.FIELD_REF, true);
			requestor.setIgnored(CompletionProposal.FIELD_REF_WITH_CASTED_RECEIVER, true);
			requestor.setIgnored(CompletionProposal.LOCAL_VARIABLE_REF, true);
			requestor.setIgnored(CompletionProposal.VARIABLE_DECLARATION, true);
			requestor.setIgnored(CompletionProposal.VARIABLE_DECLARATION, true);
			requestor.setIgnored(CompletionProposal.POTENTIAL_METHOD_DECLARATION, true);
			requestor.setIgnored(CompletionProposal.METHOD_NAME_REFERENCE, true);
			return requestor.process(cu, pos);
		} finally {
			if (preparedCU != null) {
				preparedCU.discardWorkingCopy();
			}
		}
	}

	private static ICompilationUnit createPreparedCU(ICompilationUnit cu, Javadoc comment, int wordStart) throws JavaModelException {
		int startpos= comment.getStartPosition();
		boolean isTopLevel= comment.getParent().getParent() instanceof CompilationUnit;
		char[] content= cu.getBuffer().getCharacters().clone();
		if (isTopLevel && (wordStart + 6 < content.length)) {
			content[startpos++]= 'i'; content[startpos++]= 'm'; content[startpos++]= 'p';
			content[startpos++]= 'o'; content[startpos++]= 'r'; content[startpos++]= 't';
		}
		if (wordStart < content.length) {
			for (int i= startpos; i < wordStart; i++) {
				content[i]= ' ';
			}
		}

		/*
		 * Explicitly create a new non-shared working copy.
		 */
		ICompilationUnit newCU= cu.getWorkingCopy(null);
		newCU.getBuffer().setContents(content);
		return newCU;
	}


	/**
	 * Constructor for SimilarElementsRequestor.
	 * @param name the name
	 * @param kind the type kind
	 * @param nArguments the number of arguments
	 * @param preferredType the preferred type
	 * @param excludeTestCode if true, exclude results in test code
	 */
	private SimilarElementsRequestor(String name, int kind, int nArguments, String preferredType, boolean excludeTestCode) {
		super();
		fName= name;
		fKind= kind;
		fExcludeTestCode= excludeTestCode;

		fResult= new HashSet<>();
		// nArguments and preferredType not yet used
	}

	private void addResult(SimilarElement elem) {
		fResult.add(elem);
	}

	private SimilarElement[] process(ICompilationUnit cu, int pos) throws JavaModelException {
		try {
			cu.codeComplete(pos, this, new CompletionTimeoutProgressMonitor());
			processKeywords();
			return fResult.toArray(new SimilarElement[fResult.size()]);
		} finally {
			fResult.clear();
		}
	}

	private boolean isKind(int kind) {
		return (fKind & kind) != 0;
	}

	/**
	 * Method addPrimitiveTypes.
	 */
	private void processKeywords() {
		if (isKind(TypeKinds.PRIMITIVETYPES)) {
			for (String t : PRIM_TYPES) {
				if (NameMatcher.isSimilarName(fName, t)) {
					addResult(new SimilarElement(TypeKinds.PRIMITIVETYPES, t, 50));
				}
			}
		}
		if (isKind(TypeKinds.VOIDTYPE)) {
			String voidType= "void"; //$NON-NLS-1$
			if (NameMatcher.isSimilarName(fName, voidType)) {
				addResult(new SimilarElement(TypeKinds.PRIMITIVETYPES, voidType, 50));
			}
		}
	}

	private static final int getKind(int flags, char[] typeNameSig) {
		if (Signature.getTypeSignatureKind(typeNameSig) == Signature.TYPE_VARIABLE_SIGNATURE) {
			return TypeKinds.VARIABLES;
		}
		if (Flags.isAnnotation(flags)) {
			return TypeKinds.ANNOTATIONS;
		}
		if (Flags.isInterface(flags)) {
			return TypeKinds.INTERFACES;
		}
		if (Flags.isEnum(flags)) {
			return TypeKinds.ENUMS;
		}
		return TypeKinds.CLASSES;
	}


	private void addType(char[] typeNameSig, int flags, int relevance) {
		int kind= getKind(flags, typeNameSig);
		if (!isKind(kind)) {
			return;
		}
		String fullName= new String(Signature.toCharArray(Signature.getTypeErasure(typeNameSig)));
		if (TypeFilter.isFiltered(fullName)) {
			return;
		}
		if (NameMatcher.isSimilarName(fName, Signature.getSimpleName(fullName))) {
			addResult(new SimilarElement(kind, fullName, relevance));
		}
	}


	@Override
	public void accept(CompletionProposal proposal) {
		if (proposal.getKind() == CompletionProposal.TYPE_REF) {
			addType(proposal.getSignature(), proposal.getFlags(), proposal.getRelevance());
		}
	}


	@Override
	public boolean isTestCodeExcluded() {
		return fExcludeTestCode;
	}
}
