/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.UnionType;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.viewsupport.BasicElementLabels;

public class ExceptionOccurrencesFinder extends ASTVisitor implements IOccurrencesFinder {

	public static final String ID= "ExceptionOccurrencesFinder"; //$NON-NLS-1$

	public static final String IS_EXCEPTION= "isException"; //$NON-NLS-1$

	private CompilationUnit fASTRoot;
	private Name fSelectedName;

	private ITypeBinding fException;
	private ASTNode fStart;
	private TryStatement fTryStatement;
	private List<OccurrenceLocation> fResult;
	private String fDescription;
	private List<ITypeBinding> fCaughtExceptions;

	public ExceptionOccurrencesFinder() {
		fResult= new ArrayList<OccurrenceLocation>();
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
			if (parent instanceof UnionType) {
				parent= parent.getParent();
			}
			if (parent instanceof SingleVariableDeclaration && parent.getParent() instanceof CatchClause) {
				CatchClause catchClause= (CatchClause)parent.getParent();
				fTryStatement= (TryStatement)catchClause.getParent();
				if (fTryStatement != null) {
					fException= fSelectedName.resolveTypeBinding();
					fStart= fTryStatement.getBody();
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
		for (Iterator<Name> iter = method.thrownExceptions().iterator(); iter.hasNext();) {
			Name thrown = iter.next();
			if (exception.subtreeMatch(matcher, thrown))
				return true;
		}
		return false;
	}

	private void performSearch() {
		fCaughtExceptions= new ArrayList<ITypeBinding>();
		fStart.accept(this);
		if (fTryStatement != null) {
			handleResourceDeclarations(fTryStatement);
		}
		if (fSelectedName != null) {
			fResult.add(new OccurrenceLocation(fSelectedName.getStartPosition(), fSelectedName.getLength(), F_EXCEPTION_DECLARATION, fDescription));
		}
	}

	private void handleResourceDeclarations(TryStatement tryStatement) {
		if (tryStatement.getAST().apiLevel() >= AST.JLS4) {
			List<VariableDeclarationExpression> resources= tryStatement.resources();
			for (Iterator<VariableDeclarationExpression> iterator= resources.iterator(); iterator.hasNext();) {
				iterator.next().accept(this);
			}

			//check if the exception is thrown as a result of resource#close()
			boolean exitMarked= false;
			for (VariableDeclarationExpression variable : resources) {
				Type type= variable.getType();
				IMethodBinding methodBinding= Bindings.findMethodInHierarchy(type.resolveBinding(), "close", new ITypeBinding[0]); //$NON-NLS-1$
				if (methodBinding != null) {
					ITypeBinding[] exceptionTypes= methodBinding.getExceptionTypes();
					for (int j= 0; j < exceptionTypes.length; j++) {
						if (matches(exceptionTypes[j])) { // a close() throws the caught exception
							// mark name of resource
							for (VariableDeclarationFragment fragment : (List<VariableDeclarationFragment>) variable.fragments()) {
								SimpleName name= fragment.getName();
								fResult.add(new OccurrenceLocation(name.getStartPosition(), name.getLength(), 0, fDescription));
							}
							if (!exitMarked) {
								// mark exit position
								exitMarked= true;
								Block body= tryStatement.getBody();
								int offset= body.getStartPosition() + body.getLength() - 1; // closing bracket of try block
								fResult.add(new OccurrenceLocation(offset, 1, 0, Messages.format(SearchMessages.ExceptionOccurrencesFinder_occurrence_implicit_close_description,
										BasicElementLabels.getJavaElementName(fException.getName()))));
							}
						}
					}
				}
			}
		}
	}

	public OccurrenceLocation[] getOccurrences() {
		performSearch();
		if (fResult.isEmpty())
			return null;

		return fResult.toArray(new OccurrenceLocation[fResult.size()]);
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

	@Override
	public boolean visit(AnonymousClassDeclaration node) {
		return false;
	}

	@Override
	public boolean visit(CastExpression node) {
		if ("java.lang.ClassCastException".equals(fException.getQualifiedName())) { //$NON-NLS-1$
			Type type= node.getType();
			fResult.add(new OccurrenceLocation(type.getStartPosition(), type.getLength(), 0, fDescription));
		}
		return super.visit(node);
	}

	@Override
	public boolean visit(ClassInstanceCreation node) {
		if (matches(node.resolveConstructorBinding())) {
			Type type= node.getType();
			fResult.add(new OccurrenceLocation(type.getStartPosition(), type.getLength(), 0, fDescription));
		}
		return super.visit(node);
	}

	@Override
	public boolean visit(ConstructorInvocation node) {
		if (matches(node.resolveConstructorBinding())) {
			// mark 'this'
			fResult.add(new OccurrenceLocation(node.getStartPosition(), 4, 0, fDescription));
		}
		return super.visit(node);
	}

	@Override
	public boolean visit(MethodInvocation node) {
		if (matches(node.resolveMethodBinding())) {
			SimpleName name= node.getName();
			fResult.add(new OccurrenceLocation(name.getStartPosition(), name.getLength(), 0, fDescription));
		}
		return super.visit(node);
	}

	@Override
	public boolean visit(SuperConstructorInvocation node) {
		if (matches(node.resolveConstructorBinding())) {
			// mark 'super'
			fResult.add(new OccurrenceLocation(node.getStartPosition(), 5, 0, fDescription));
		}
		return super.visit(node);
	}

	@Override
	public boolean visit(SuperMethodInvocation node) {
		if (matches(node.resolveMethodBinding())) {
			SimpleName name= node.getName();
			fResult.add(new OccurrenceLocation(name.getStartPosition(), name.getLength(), 0, fDescription));
		}
		return super.visit(node);
	}

	@Override
	public boolean visit(ThrowStatement node) {
		if (matches(node.getExpression().resolveTypeBinding())) {
			// mark 'throw'
			fResult.add(new OccurrenceLocation(node.getStartPosition(), 5, 0, fDescription));
		}
		return super.visit(node);
	}

	@Override
	public boolean visit(TryStatement node) {
		int currentSize= fCaughtExceptions.size();
		List<CatchClause> catchClauses= node.catchClauses();
		for (Iterator<CatchClause> iter= catchClauses.iterator(); iter.hasNext();) {
			Type type= iter.next().getException().getType();
			if (type instanceof UnionType) {
				List<Type> types= ((UnionType) type).types();
				for (Iterator<Type> iterator= types.iterator(); iterator.hasNext();) {
					addCaughtException(iterator.next());
				}
			} else {
				addCaughtException(type);
			}
		}

		node.getBody().accept(this);

		handleResourceDeclarations(node);

		int toRemove= fCaughtExceptions.size() - currentSize;
		for (int i= toRemove; i > 0; i--) {
			fCaughtExceptions.remove(currentSize);
		}

		// visit catch and finally
		for (Iterator<CatchClause> iter= catchClauses.iterator(); iter.hasNext();) {
			iter.next().accept(this);
		}
		if (node.getFinally() != null)
			node.getFinally().accept(this);

		// return false. We have visited the body by ourselves.
		return false;
	}

	private void addCaughtException(Type type) {
		ITypeBinding typeBinding= type.resolveBinding();
		if (typeBinding != null) {
			fCaughtExceptions.add(typeBinding);
		}
	}

	@Override
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
		if (isCaught(exception))
			return false;
		while (exception != null) {
			if (Bindings.equals(fException, exception))
				return true;
			exception= exception.getSuperclass();
		}
		return false;
	}

	private boolean isCaught(ITypeBinding binding) {
		for (Iterator<ITypeBinding> iter= fCaughtExceptions.iterator(); iter.hasNext();) {
			ITypeBinding catchException= iter.next();
			if (catches(catchException, binding))
				return true;
		}
		return false;
	}

	private boolean catches(ITypeBinding catchTypeBinding, ITypeBinding throwTypeBinding) {
		while (throwTypeBinding != null) {
			if (throwTypeBinding == catchTypeBinding)
				return true;
			throwTypeBinding= throwTypeBinding.getSuperclass();
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
