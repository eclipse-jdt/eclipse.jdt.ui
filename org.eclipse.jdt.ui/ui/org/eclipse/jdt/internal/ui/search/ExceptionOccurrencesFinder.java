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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

import org.eclipse.ui.texteditor.MarkerUtilities;

import org.eclipse.search.ui.ISearchResultView;
import org.eclipse.search.ui.SearchUI;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

public class ExceptionOccurrencesFinder extends ASTVisitor implements IOccurrencesFinder {
	
	private ITypeBinding fException;
	private AST fAST;
	private List fResult;
	
	public ExceptionOccurrencesFinder() {
		fResult= new ArrayList();
	}
	
	public static List perform(Name name) {
		ExceptionOccurrencesFinder finder= new ExceptionOccurrencesFinder();
		return finder.perform((CompilationUnit) name.getRoot(), name);
	}
	
	public List perform(CompilationUnit root, Name name) {
		fAST= root.getAST();
		ITypeBinding typeBinding= null;
		ASTNode start= null;
		ASTNode reference= null;
		Name topMost= ASTNodes.getTopMostName(name);
		ASTNode parent= topMost.getParent();
		if (parent instanceof MethodDeclaration) {
			MethodDeclaration decl= (MethodDeclaration)parent;
			if (decl.thrownExceptions().contains(topMost)) {
				typeBinding= topMost.resolveTypeBinding();
				reference= topMost;
				start= decl.getBody();
			}
		} else if (parent instanceof Type) {
			parent= parent.getParent();
			if (parent instanceof SingleVariableDeclaration && parent.getParent() instanceof CatchClause) {
				CatchClause catchClause= (CatchClause)parent.getParent();
				TryStatement tryStatement= (TryStatement)catchClause.getParent();
				if (tryStatement != null) {
					IVariableBinding var= catchClause.getException().resolveBinding();
					if (var != null && var.getType() != null) {
						typeBinding= var.getType();
						start= tryStatement.getBody();
						reference= catchClause.getException().getType();
					}
				}
			}
		}
		if (start == null || typeBinding == null)
			return new ArrayList();
		fException= typeBinding;
		start.accept(this);
		if (fResult.size() > 0 && reference != null) {
			fResult.add(reference);
		}
		return fResult;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.search.IPositionFinder#createMarkers(org.eclipse.core.resources.IResource, org.eclipse.jface.text.IDocument)
	 */
	public IMarker[] createMarkers(IResource file, IDocument document) throws CoreException {
		List result= new ArrayList();
		for (Iterator iter = fResult.iterator(); iter.hasNext();) {
			ASTNode node = (ASTNode) iter.next();
			result.add(createMarker(file, document, node));
		}
		return (IMarker[]) result.toArray(new IMarker[result.size()]);
	}
 	
	private static IMarker createMarker(IResource file, IDocument document, ASTNode node) throws CoreException {
		Map attributes= new HashMap(10);
		IMarker marker= file.createMarker(SearchUI.SEARCH_MARKER);
	
		int startPosition= node.getStartPosition();
		MarkerUtilities.setCharStart(attributes, startPosition);
		MarkerUtilities.setCharEnd(attributes, startPosition + node.getLength());
		
		try {
			int line= document.getLineOfOffset(startPosition);
			MarkerUtilities.setLineNumber(attributes, line);
			IRegion region= document.getLineInformation(line);
			String lineContents= document.get(region.getOffset(), region.getLength());
			MarkerUtilities.setMessage(attributes, lineContents.trim());
		} catch (BadLocationException e) {
		}
		marker.setAttributes(attributes);
		return marker;
	}
	
	public void searchStarted(ISearchResultView view, String inputName, Name name) {
		String elementName= ASTNodes.asString(name);
		view.searchStarted(
			null,
			"Occurrences of " + elementName + " - {0} match in " + inputName,
			"Occurrences of " + elementName + " - {0} matches in " + inputName,
			JavaPluginImages.DESC_OBJS_SEARCH_REF,
			"org.eclipse.jdt.ui.JavaFileSearch", //$NON-NLS-1$
			new OccurrencesInFileLabelProvider(),
			new GotoMarkerAction(), 
			new SearchGroupByKeyComputer(),
			null
		);
	}

	public boolean visit(CastExpression node) {
		if ("java.lang.ClassCastException".equals(fException.getQualifiedName())) //$NON-NLS-1$
			fResult.add(node.getType());
		return super.visit(node);
	}
	
	public boolean visit(ClassInstanceCreation node) {
		if (matches(node.resolveConstructorBinding())) {
			fResult.add(node.getName());
		}
		return super.visit(node);
	}
	
	public boolean visit(ConstructorInvocation node) {
		if (matches(node.resolveConstructorBinding())) {
			SimpleName name= fAST.newSimpleName("xxxx"); //$NON-NLS-1$
			name.setSourceRange(node.getStartPosition(), 4);
			fResult.add(name);
		}
		return super.visit(node);
	}
	
	public boolean visit(MethodInvocation node) {
		if (matches(node.resolveMethodBinding()))
			fResult.add(node.getName());
		return super.visit(node);
	}
	
	public boolean visit(SuperConstructorInvocation node) {
		if (matches(node.resolveConstructorBinding())) {
			SimpleName name= fAST.newSimpleName("xxxxx"); //$NON-NLS-1$
			name.setSourceRange(node.getStartPosition(), 5);
			fResult.add(name);
		}
		return super.visit(node);
	}
	
	public boolean visit(SuperMethodInvocation node) {
		if (matches(node.resolveMethodBinding())) {
			SimpleName name= fAST.newSimpleName("xxxxx"); //$NON-NLS-1$
			name.setSourceRange(node.getStartPosition(), 5);
			fResult.add(name);
		}
		return super.visit(node);
	}
	
	private boolean matches(IMethodBinding binding) {
		if (binding == null)
			return false;
		ITypeBinding[] exceptions= binding.getExceptionTypes();
		for (int i = 0; i < exceptions.length; i++) {
			ITypeBinding exception= exceptions[i];
			while (exception != null) {
				if (Bindings.equals(fException, exception))
					return true;
				exception= exception.getSuperclass();
			}
		}
		return false;
	}
}
