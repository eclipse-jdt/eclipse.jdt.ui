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
import org.eclipse.jdt.core.dom.ThrowStatement;
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
import org.eclipse.jdt.internal.corext.dom.NodeFinder;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

public class ExceptionOccurrencesFinder extends ASTVisitor implements IOccurrencesFinder {

	public static final String IS_EXCEPTION= "isException"; //$NON-NLS-1$
	
	private AST fAST;
	private Name fSelectedName;
	
	private ITypeBinding fException;
	private ASTNode fStart;
	private List fResult;
	
	public ExceptionOccurrencesFinder() {
		fResult= new ArrayList();
	}
	
	public String initialize(CompilationUnit root, int offset, int length) {
		fAST= root.getAST();
		ASTNode node= NodeFinder.perform(root, offset, length);
		if (!(node instanceof Name)) {
			return SearchMessages.getString("ExceptionOccurrencesFinder.no_exception");  //$NON-NLS-1$
		}
		fSelectedName= ASTNodes.getTopMostName((Name)node);
		ASTNode parent= fSelectedName.getParent();
		if (parent instanceof MethodDeclaration) {
			MethodDeclaration decl= (MethodDeclaration)parent;
			if (decl.thrownExceptions().contains(fSelectedName)) {
				fException= fSelectedName.resolveTypeBinding();
				fStart= decl.getBody();
			}
		} else if (parent instanceof Type) {
			parent= parent.getParent();
			if (parent instanceof SingleVariableDeclaration && parent.getParent() instanceof CatchClause) {
				CatchClause catchClause= (CatchClause)parent.getParent();
				TryStatement tryStatement= (TryStatement)catchClause.getParent();
				if (tryStatement != null) {
					IVariableBinding var= catchClause.getException().resolveBinding();
					if (var != null && var.getType() != null) {
						fException= var.getType();
						fStart= tryStatement.getBody();
					}
				}
			}
		}
		if (fException == null || fStart == null)
			return SearchMessages.getString("ExceptionOccurrencesFinder.no_exception");  //$NON-NLS-1$
		return null;
	}
	
	public List perform() {
		fStart.accept(this);
		if (fResult.size() > 0 && fSelectedName != null) {
			fResult.add(fSelectedName);
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
 	
	private IMarker createMarker(IResource file, IDocument document, ASTNode node) throws CoreException {
		Map attributes= new HashMap(10);
		IMarker marker= file.createMarker(SearchUI.SEARCH_MARKER);
	
		int startPosition= node.getStartPosition();
		MarkerUtilities.setCharStart(attributes, startPosition);
		MarkerUtilities.setCharEnd(attributes, startPosition + node.getLength());
		
		if (node == fSelectedName) {
			attributes.put(IS_EXCEPTION, Boolean.TRUE);
		}
		
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
	
	public void searchStarted(ISearchResultView view, String inputName) {
		String elementName= ASTNodes.asString(fSelectedName);
		view.searchStarted(
			null,
			SearchMessages.getFormattedString(
				"ExceptionOccurrencesFinder.label.singular", //$NON-NLS-1$
				new Object[] { elementName, "{0}", inputName}), //$NON-NLS-1$
			SearchMessages.getFormattedString(
				"ExceptionOccurrencesFinder.label.plural", //$NON-NLS-1$
				new Object[] { elementName, "{0}", inputName}), //$NON-NLS-1$
			JavaPluginImages.DESC_OBJS_SEARCH_REF,
			"org.eclipse.jdt.ui.JavaFileSearch", //$NON-NLS-1$
			new ExceptionOccurrencesLabelProvider(),
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
	
	public boolean visit(ThrowStatement node) {
		if (matches(node.getExpression().resolveTypeBinding())) {
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
			if(matches(exception))
				return true;
		}
		return false;
	}

	private boolean matches(ITypeBinding exception) {
		if (exception == null)
			return false;
		while (exception != null) {
			if (Bindings.equals(fException, exception))
				return true;
			exception= exception.getSuperclass();
		}
		return false;
	}
}
