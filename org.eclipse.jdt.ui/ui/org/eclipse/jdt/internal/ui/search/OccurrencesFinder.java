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
package org.eclipse.jdt.internal.ui.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.PrefixExpression.Operator;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

import org.eclipse.ui.texteditor.MarkerUtilities;

import org.eclipse.search.ui.ISearchResultView;
import org.eclipse.search.ui.SearchUI;
import org.eclipse.search.ui.text.Match;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

public class OccurrencesFinder extends ASTVisitor implements IOccurrencesFinder {
	
	public static final String IS_WRITEACCESS= "writeAccess"; //$NON-NLS-1$
	public static final String IS_VARIABLE= "variable"; //$NON-NLS-1$
	
	private CompilationUnit fRoot;
	private Name fSelectedNode;
	private IBinding fTarget;
	private List fUsages= new ArrayList/*<ASTNode>*/();
	private List fWriteUsages= new ArrayList/*<ASTNode>*/();

	public OccurrencesFinder(IBinding target) {
		super(true);
		fTarget= target;
	}
	
	public OccurrencesFinder() {
		super(true);
	}
	
	public String initialize(CompilationUnit root, int offset, int length) {
		ASTNode node= NodeFinder.perform(root, offset, length);
		if (!(node instanceof Name))
			return SearchMessages.getString("OccurrencesFinder.no_element"); //$NON-NLS-1$
		fRoot= root;
		fSelectedNode= (Name)node;
		fTarget= fSelectedNode.resolveBinding();
		if (fTarget == null)
			return SearchMessages.getString("OccurrencesFinder.no_binding"); //$NON-NLS-1$
		return null;
	}
	
	public List perform() {
		fRoot.accept(this);
		return fUsages;
	}
	
	public IMarker[] createMarkers(IResource file, IDocument document) throws CoreException {
		List result= new ArrayList();
		boolean isVariable= fTarget instanceof IVariableBinding;
		for (Iterator each= fUsages.iterator(); each.hasNext();) {
			ASTNode node= (ASTNode) each.next();
			result.add(createMarker(file, document, node, fWriteUsages.contains(node), isVariable));
		}
		return (IMarker[]) result.toArray(new IMarker[result.size()]);
	}
	
	private static IMarker createMarker(IResource file, IDocument document, ASTNode node, boolean writeAccess, boolean isVariable) throws CoreException {
		Map attributes= new HashMap(10);
		IMarker marker= file.createMarker(SearchUI.SEARCH_MARKER);

		int startPosition= node.getStartPosition();
		MarkerUtilities.setCharStart(attributes, startPosition);
		MarkerUtilities.setCharEnd(attributes, startPosition + node.getLength());
		
		if(writeAccess)
			attributes.put(IS_WRITEACCESS, new Boolean(true));

		if(isVariable)
			attributes.put(IS_VARIABLE, new Boolean(true));
			
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
	
	/*
	 * @see org.eclipse.jdt.internal.ui.search.IOccurrencesFinder#getOccurrenceMatches(org.eclipse.jdt.core.IJavaElement, org.eclipse.jface.text.IDocument)
	 */
	public Match[] getOccurrenceMatches(IJavaElement element, IDocument document) {
		boolean isVariable= fTarget instanceof IVariableBinding;
		ArrayList matches= new ArrayList(fUsages.size());
		HashMap lineToGroup= new HashMap();
		
		for (Iterator iter= fUsages.iterator(); iter.hasNext();) {
			ASTNode node= (ASTNode) iter.next();
			int startPosition= node.getStartPosition();
			int length= node.getLength();
			try {
				boolean isWriteAccess= fWriteUsages.contains(node);
				int line= document.getLineOfOffset(startPosition);
				Integer lineInteger= new Integer(line);
				OccurrencesGroupKey groupKey= (OccurrencesGroupKey) lineToGroup.get(lineInteger);
				if (groupKey == null) {
					IRegion region= document.getLineInformation(line);
					String lineContents= document.get(region.getOffset(), region.getLength()).trim();
					groupKey= new OccurrencesGroupKey(element, line, lineContents, isWriteAccess, isVariable);
					lineToGroup.put(lineInteger, groupKey);
				} else if (isWriteAccess) {
					// a line with read an write access is considered as write access:
					groupKey.setWriteAccess(true);
				}
				Match match= new Match(groupKey, startPosition, length);
				matches.add(match);
			} catch (BadLocationException e) {
				//nothing
			}
		}
		return (Match[]) matches.toArray(new Match[matches.size()]);
	}
	
	public void searchStarted(ISearchResultView view, String inputName) {
		String elementName= ASTNodes.asString(fSelectedNode);
		view.searchStarted(
			null,
			getSingularLabel(elementName, inputName),
			getPluralLabelPattern(elementName, inputName),
			JavaPluginImages.DESC_OBJS_SEARCH_REF,
			"org.eclipse.jdt.ui.JavaFileSearch", //$NON-NLS-1$
			new OccurrencesInFileLabelProvider(),
			new GotoMarkerAction(), 
			new SearchGroupByKeyComputer(),
			null
		);
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.search.IOccurrencesFinder#getJobLabel()
	 */
	public String getJobLabel() {
		return SearchMessages.getString("OccurrencesFinder.searchfor") ; //$NON-NLS-1$
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.search.IOccurrencesFinder#getPluralLabelPattern(java.lang.String)
	 */
	public String getPluralLabelPattern(String documentName) {
		return getPluralLabelPattern(ASTNodes.asString(fSelectedNode), documentName);
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.search.IOccurrencesFinder#getSingularLabel(java.lang.String)
	 */
	public String getSingularLabel(String documentName) {
		return getSingularLabel(ASTNodes.asString(fSelectedNode), documentName);
	}
	
	private String getPluralLabelPattern(String nodeContents, String elementName) {
		String[] args= new String[] {nodeContents, "{0}", elementName}; //$NON-NLS-1$
		return SearchMessages.getFormattedString("OccurrencesFinder.label.plural", args); //$NON-NLS-1$
	}
	
	private String getSingularLabel(String nodeContents, String elementName) {
		String[] args= new String[] {nodeContents, elementName}; //$NON-NLS-1$
		return SearchMessages.getFormattedString("OccurrencesFinder.label.singular", args); //$NON-NLS-1$
	}

	public boolean visit(QualifiedName node) {
		IBinding binding= node.resolveBinding();
		if (binding instanceof IVariableBinding && ((IVariableBinding)binding).isField()) {
			SimpleName name= node.getName();
			return !match(name, fUsages, name.resolveBinding());
		}
		return !match(node, fUsages, node.resolveBinding());
	}

	public boolean visit(SimpleName node) {
		return !match(node, fUsages, node.resolveBinding());
	}

	/*
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.ConstructorInvocation)
	 */
	public boolean visit(ClassInstanceCreation node) {
		// match with the constructor and the type.
		Name name= node.getName();
		if (name instanceof QualifiedName)
			name= ((QualifiedName)name).getName();
		match(name, fUsages, node.resolveConstructorBinding());
		return super.visit(node);
	}
	public boolean visit(Assignment node) {
		Expression lhs= node.getLeftHandSide();
		Name name= getName(lhs);
		if (name != null) 
			match(name, fWriteUsages, name.resolveBinding());	
		lhs.accept(this);
		node.getRightHandSide().accept(this);
		return false;
	}
	
	public boolean visit(SingleVariableDeclaration node) {
		if (node.getInitializer() != null)
			match(node.getName(), fWriteUsages, node.resolveBinding());
		return super.visit(node);
	}

	public boolean visit(VariableDeclarationFragment node) {
		if (node.getInitializer() != null)
			match(node.getName(), fWriteUsages, node.resolveBinding());
		return super.visit(node);
	}

	public boolean visit(PrefixExpression node) {
		PrefixExpression.Operator operator= node.getOperator();	
		if (operator == Operator.INCREMENT || operator == Operator.DECREMENT) {
			Expression operand= node.getOperand();
			Name name= getName(operand);
			if (name != null) 
				match(name, fWriteUsages, name.resolveBinding());				
		}
		return super.visit(node);
	}

	public boolean visit(PostfixExpression node) {
		Expression operand= node.getOperand();
		Name name= getName(operand);
		if (name != null) 
			match(name, fWriteUsages, name.resolveBinding());
		return super.visit(node);
	}
	
	private boolean match(Name node, List result, IBinding binding) {
		if (binding != null && Bindings.equals(binding, fTarget)) {
			result.add(node);
			return true;
		}
		return false;
	}

	private Name getName(Expression expression) {
		if (expression instanceof SimpleName)
			return ((SimpleName)expression);
		else if (expression instanceof QualifiedName)
			return ((QualifiedName)expression);
		else if (expression instanceof FieldAccess)
			return ((FieldAccess)expression).getName();
		return null;
	}	
}
