/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.search;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;


/**
 * Finds all implement occurrences of an extended class or an implemented interface.
 * 
 * @since 3.1
 */
public class ImplementOccurrencesFinder implements IOccurrencesFinder {
	
	
	private class MethodVisitor extends ASTVisitor {
		
		/*
		 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.MethodDeclaration)
		 */
		public boolean visit(MethodDeclaration node) {
			IMethodBinding binding= node.resolveBinding();
			if (binding != null) {
				IMethodBinding method= Bindings.findMethodInHierarchy(fSelectedType, binding.getName(), binding.getParameterTypes());
				if (method != null)
					fResult.add(node.getName());
			}
			return super.visit(node);
		}
		
		/*
		 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.AnonymousClassDeclaration)
		 */
		public boolean visit(AnonymousClassDeclaration node) {
			// don't dive into anonymous type declarations.
			return false;
		}
		
		/*
		 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.TypeDeclarationStatement)
		 */
		public boolean visit(TypeDeclarationStatement node) {
			// don't dive into local type declarations.
			return false;
		}
	}
	
	
	private ASTNode fStart;
	private List fResult;
	private Name fSelectedName;
	private ITypeBinding fSelectedType;

	
	public ImplementOccurrencesFinder() {
		fResult= new ArrayList();
	}
	
	public String initialize(CompilationUnit root, int offset, int length) {
		ASTNode node= NodeFinder.perform(root, offset, length);
		if (!(node instanceof Name))
			return SearchMessages.getString("ImplementOccurrencesFinder.invalidTarget");  //$NON-NLS-1$

		fSelectedName= ASTNodes.getTopMostName((Name)node);
		ASTNode parent= fSelectedName.getParent();
		if (!(parent instanceof SimpleType))
			return SearchMessages.getString("ImplementOccurrencesFinder.invalidTarget");  //$NON-NLS-1$

		ASTNode typeDeclaration= parent.getParent();
		if (!(typeDeclaration instanceof TypeDeclaration))
			return SearchMessages.getString("ImplementOccurrencesFinder.invalidTarget");  //$NON-NLS-1$
		
		fSelectedType= fSelectedName.resolveTypeBinding();
		if (fSelectedType == null)
			return SearchMessages.getString("ImplementOccurrencesFinder.invalidTarget");  //$NON-NLS-1$

		fStart= typeDeclaration;
		return null;
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.search.IOccurrencesFinder#perform()
	 */
	public List perform() {
		fStart.accept(new MethodVisitor());
		if (fSelectedName != null)
			fResult.add(fSelectedName);
		
		return fResult;
	}
	
	public Match[] getOccurrenceMatches(IJavaElement element, IDocument document) {
		ArrayList matches= new ArrayList(fResult.size());
		
		for (Iterator iter= fResult.iterator(); iter.hasNext();) {
			ASTNode node= (ASTNode) iter.next();
			int startPosition= node.getStartPosition();
			int length= node.getLength();
			try {
				int line= document.getLineOfOffset(startPosition);
				IRegion region= document.getLineInformation(line);
				String lineContents= document.get(region.getOffset(), region.getLength()).trim();
				JavaElementLine groupKey= new JavaElementLine(element, line, lineContents);
				matches.add(new Match(groupKey, startPosition, length));
			} catch (BadLocationException e) {
				//nothing
			}
		}
		return (Match[]) matches.toArray(new Match[matches.size()]);
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.search.IOccurrencesFinder#getJobLabel()
	 */
	public String getJobLabel() {
		return SearchMessages.getString("ImplementOccurrencesFinder.searchfor") ; //$NON-NLS-1$
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.search.IOccurrencesFinder#getPluralLabelPattern(java.lang.String)
	 */
	public String getPluralLabelPattern(String documentName) {
		return getPluralLabelPattern(ASTNodes.asString(fSelectedName), documentName);
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.search.IOccurrencesFinder#getSingularLabel(java.lang.String)
	 */
	public String getSingularLabel(String documentName) {
		return getSingularLabel(ASTNodes.asString(fSelectedName), documentName);
	}
	
	private String getPluralLabelPattern(String nodeContents, String elementName) {
		String[] args= new String[] {nodeContents, "{0}", elementName}; //$NON-NLS-1$
		return SearchMessages.getFormattedString("ImplementOccurrencesFinder.label.plural", args); //$NON-NLS-1$
	}
	
	private String getSingularLabel(String nodeContents, String elementName) {
		String[] args= new String[] {nodeContents, elementName}; //$NON-NLS-1$
		return SearchMessages.getFormattedString("ImplementOccurrencesFinder.label.singular", args); //$NON-NLS-1$
	}
}
