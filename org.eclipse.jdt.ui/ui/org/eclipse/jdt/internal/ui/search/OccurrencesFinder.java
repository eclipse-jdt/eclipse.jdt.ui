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

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

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
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.PrefixExpression.Operator;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;

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
		Type type= node.getType();
		if (type instanceof ParameterizedType) {
			type= ((ParameterizedType) type).getType();
		}
		if (type instanceof SimpleType) {
			Name name= ((SimpleType) type).getName();
			if (name instanceof QualifiedName)
				name= ((QualifiedName)name).getName();
			match(name, fUsages, node.resolveConstructorBinding());
		}
		return super.visit(node);
	}
	
	public boolean visit(Assignment node) {
		Expression lhs= node.getLeftHandSide();
		SimpleName name= getSimpleName(lhs);
		if (name != null) 
			match(name, fWriteUsages, name.resolveBinding());	
		lhs.accept(this);
		node.getRightHandSide().accept(this);
		return false;
	}
	
	public boolean visit(SingleVariableDeclaration node) {
		match(node.getName(), fWriteUsages, node.resolveBinding());
		return super.visit(node);
	}

	public boolean visit(VariableDeclarationFragment node) {
		if (node.getParent().getNodeType() == ASTNode.FIELD_DECLARATION)
			match(node.getName(), fWriteUsages, node.resolveBinding());
		return super.visit(node);
	}

	public boolean visit(PrefixExpression node) {
		PrefixExpression.Operator operator= node.getOperator();	
		if (operator == Operator.INCREMENT || operator == Operator.DECREMENT) {
			Expression operand= node.getOperand();
			SimpleName name= getSimpleName(operand);
			if (name != null) 
				match(name, fWriteUsages, name.resolveBinding());				
		}
		return super.visit(node);
	}

	public boolean visit(PostfixExpression node) {
		Expression operand= node.getOperand();
		SimpleName name= getSimpleName(operand);
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

	private SimpleName getSimpleName(Expression expression) {
		if (expression instanceof SimpleName)
			return ((SimpleName)expression);
		else if (expression instanceof QualifiedName)
			return (((QualifiedName) expression).getName());
		else if (expression instanceof FieldAccess)
			return ((FieldAccess)expression).getName();
		return null;
	}	
}
