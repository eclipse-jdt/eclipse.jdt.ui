/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

import org.eclipse.search.ui.text.Match;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;


/**
 * Finds all implement occurrences of an extended class or an implemented interface.
 * 
 * @since 3.1
 */
public class ImplementOccurrencesFinder implements org.eclipse.jdt.internal.ui.search.IOccurrencesFinder {
	
	
	private class MethodVisitor extends ASTVisitor {
		
		/*
		 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.MethodDeclaration)
		 */
		public boolean visit(MethodDeclaration node) {
			IMethodBinding binding= node.resolveBinding();
			if (binding != null) {
				IMethodBinding method= Bindings.findOverriddenMethodInHierarchy(fSelectedType, binding);
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
	private ASTNode fSelectedNode;
	private ITypeBinding fSelectedType;
	
	public ImplementOccurrencesFinder() {
		fResult= new ArrayList();
	}
	
	public String initialize(CompilationUnit root, int offset, int length) {
		return initialize(root, NodeFinder.perform(root, offset, length));
	}
	
	public String initialize(CompilationUnit root, ASTNode node) {
		if (!(node instanceof Name))
			return SearchMessages.ImplementOccurrencesFinder_invalidTarget;  
		
		fSelectedNode= ASTNodes.getNormalizedNode(node);
		if (!(fSelectedNode instanceof Type))
			return SearchMessages.ImplementOccurrencesFinder_invalidTarget;
		
		ASTNode typeDeclaration= fSelectedNode.getParent();
		if (!(typeDeclaration instanceof AbstractTypeDeclaration))
			return SearchMessages.ImplementOccurrencesFinder_invalidTarget;  
		
		fSelectedType= ((Type)fSelectedNode).resolveBinding();
		if (fSelectedType == null)
			return SearchMessages.ImplementOccurrencesFinder_invalidTarget;  

		fStart= typeDeclaration;
		return null;
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.search.IOccurrencesFinder#perform()
	 */
	public List perform() {
		fStart.accept(new MethodVisitor());
		if (fSelectedNode != null)
			fResult.add(fSelectedNode);
		
		return fResult;
	}
	
	public void collectOccurrenceMatches(IJavaElement element, IDocument document, Collection resultingMatches) {
		for (Iterator iter= fResult.iterator(); iter.hasNext();) {
			ASTNode node= (ASTNode) iter.next();
			int startPosition= node.getStartPosition();
			int length= node.getLength();
			try {
				int line= document.getLineOfOffset(startPosition);
				IRegion region= document.getLineInformation(line);
				String lineContents= document.get(region.getOffset(), region.getLength()).trim();
				JavaElementLine groupKey= new JavaElementLine(element, line, lineContents);
				resultingMatches.add(new Match(groupKey, startPosition, length));
			} catch (BadLocationException e) {
				//nothing
			}
		}
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.search.IOccurrencesFinder#getJobLabel()
	 */
	public String getJobLabel() {
		return SearchMessages.ImplementOccurrencesFinder_searchfor ; 
	}
	
	public String getElementName() {
		if (fSelectedNode != null) {
			return ASTNodes.asString(fSelectedNode);
		}
		return null;
	}
	
	public String getUnformattedPluralLabel() {
		return SearchMessages.ImplementOccurrencesFinder_label_plural;
	}
	
	public String getUnformattedSingularLabel() {
		return SearchMessages.ImplementOccurrencesFinder_label_singular;
	}
	
	public void releaseAST() {
		fStart= null;
		fSelectedType= null;
	}
	
}
