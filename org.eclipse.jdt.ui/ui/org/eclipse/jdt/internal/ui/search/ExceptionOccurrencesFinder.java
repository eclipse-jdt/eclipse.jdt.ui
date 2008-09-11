/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Javadoc;
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
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.viewsupport.BasicElementLabels;

public class ExceptionOccurrencesFinder extends ASTVisitor implements IOccurrencesFinder {

	public static final String ID= "ExceptionOccurrencesFinder"; //$NON-NLS-1$

	public static final String IS_EXCEPTION= "isException"; //$NON-NLS-1$

	private CompilationUnit fASTRoot;
	private Name fSelectedName;

	private ITypeBinding fException;
	private ASTNode fStart;
	private List fResult;
	private String fDescription;

	public ExceptionOccurrencesFinder() {
		fResult= new ArrayList();
	}

	public String initialize(CompilationUnit root, int offset, int length) {
		return initialize(root, NodeFinder.perform(root, offset, length));
	}

	public String initialize(CompilationUnit root, ASTNode node) {
		fASTRoot= root;
		if (!(node instanceof Name)) {
			return SearchMessages.ExceptionOccurrencesFinder_no_exception;
		}
		fSelectedName= ASTNodes.getTopMostName((Name)node);
		ASTNode parent= fSelectedName.getParent();
		MethodDeclaration decl= resolveMethodDeclaration(parent);
		if (decl != null && methodThrowsException(decl, fSelectedName)) {
			fException= fSelectedName.resolveTypeBinding();
			fStart= decl.getBody();
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
			return SearchMessages.ExceptionOccurrencesFinder_no_exception;
		fDescription= Messages.format(SearchMessages.ExceptionOccurrencesFinder_occurrence_description, BasicElementLabels.getJavaElementName(fException.getName()));
		return null;
	}

	private MethodDeclaration resolveMethodDeclaration(ASTNode node) {
		if (node instanceof MethodDeclaration)
			return (MethodDeclaration)node;
		Javadoc doc= (Javadoc) ASTNodes.getParent(node, ASTNode.JAVADOC);
		if (doc == null)
			return null;
		if (doc.getParent() instanceof MethodDeclaration)
			return (MethodDeclaration) doc.getParent();
		return null;
	}

	private boolean methodThrowsException(MethodDeclaration method, Name exception) {
		ASTMatcher matcher = new ASTMatcher();
		for (Iterator iter = method.thrownExceptions().iterator(); iter.hasNext();) {
			Name thrown = (Name)iter.next();
			if (exception.subtreeMatch(matcher, thrown))
				return true;
		}
		return false;
	}

	private void performSearch() {
		fStart.accept(this);
		if (fSelectedName != null) {
			fResult.add(new OccurrenceLocation(fSelectedName.getStartPosition(), fSelectedName.getLength(), F_EXCEPTION_DECLARATION, fDescription));
		}
	}

	public OccurrenceLocation[] getOccurrences() {
		performSearch();
		if (fResult.isEmpty())
			return null;

		return (OccurrenceLocation[]) fResult.toArray(new OccurrenceLocation[fResult.size()]);
	}

	public int getSearchKind() {
		return K_EXCEPTION_OCCURRENCE;
	}


	public CompilationUnit getASTRoot() {
		return fASTRoot;
	}

	public String getJobLabel() {
		return SearchMessages.ExceptionOccurrencesFinder_searchfor ;
	}

	public String getElementName() {
		if (fSelectedName != null) {
			return ASTNodes.asString(fSelectedName);
		}
		return null;
	}

	public String getUnformattedPluralLabel() {
		return SearchMessages.ExceptionOccurrencesFinder_label_plural;
	}

	public String getUnformattedSingularLabel() {
		return SearchMessages.ExceptionOccurrencesFinder_label_singular;
	}

	public boolean visit(AnonymousClassDeclaration node) {
		return false;
	}

	public boolean visit(CastExpression node) {
		if ("java.lang.ClassCastException".equals(fException.getQualifiedName())) { //$NON-NLS-1$
			Type type= node.getType();
			fResult.add(new OccurrenceLocation(type.getStartPosition(), type.getLength(), 0, fDescription));
		}
		return super.visit(node);
	}

	public boolean visit(ClassInstanceCreation node) {
		if (matches(node.resolveConstructorBinding())) {
			Type type= node.getType();
			fResult.add(new OccurrenceLocation(type.getStartPosition(), type.getLength(), 0, fDescription));
		}
		return super.visit(node);
	}

	public boolean visit(ConstructorInvocation node) {
		if (matches(node.resolveConstructorBinding())) {
			// mark 'this'
			fResult.add(new OccurrenceLocation(node.getStartPosition(), 4, 0, fDescription));
		}
		return super.visit(node);
	}

	public boolean visit(MethodInvocation node) {
		if (matches(node.resolveMethodBinding())) {
			SimpleName name= node.getName();
			fResult.add(new OccurrenceLocation(name.getStartPosition(), name.getLength(), 0, fDescription));
		}
		return super.visit(node);
	}

	public boolean visit(SuperConstructorInvocation node) {
		if (matches(node.resolveConstructorBinding())) {
			// mark 'super'
			fResult.add(new OccurrenceLocation(node.getStartPosition(), 5, 0, fDescription));
		}
		return super.visit(node);
	}

	public boolean visit(SuperMethodInvocation node) {
		if (matches(node.resolveMethodBinding())) {
			SimpleName name= node.getName();
			fResult.add(new OccurrenceLocation(name.getStartPosition(), name.getLength(), 0, fDescription));
		}
		return super.visit(node);
	}

	public boolean visit(ThrowStatement node) {
		if (matches(node.getExpression().resolveTypeBinding())) {
			// mark 'throw'
			fResult.add(new OccurrenceLocation(node.getStartPosition(), 5, 0, fDescription));
		}
		return super.visit(node);
	}

	public boolean visit(TypeDeclarationStatement node) {
		// don't dive into local type declarations.
		return false;
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

	public IOccurrencesFinder getNewInstance() {
		return new ExceptionOccurrencesFinder();
	}

	public String getID() {
		return ID;
	}

}
