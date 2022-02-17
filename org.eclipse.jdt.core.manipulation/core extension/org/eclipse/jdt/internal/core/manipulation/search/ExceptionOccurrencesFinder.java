/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core.manipulation.search;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.UnionType;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.util.Messages;

public class ExceptionOccurrencesFinder extends ASTVisitor implements IOccurrencesFinder {

	public static final String ID= "ExceptionOccurrencesFinder"; //$NON-NLS-1$

	public static final String IS_EXCEPTION= "isException"; //$NON-NLS-1$

	private CompilationUnit fASTRoot;
	private ASTNode fSelectedNode;

	private ITypeBinding fException;
	private ASTNode fStart;
	private TryStatement fTryStatement;
	private List<OccurrenceLocation> fResult;
	private String fDescription;
	private List<ITypeBinding> fCaughtExceptions;

	public ExceptionOccurrencesFinder() {
		fResult= new ArrayList<>();
	}

	@Override
	public String initialize(CompilationUnit root, int offset, int length) {
		return initialize(root, NodeFinder.perform(root, offset, length));
	}

	@Override
	public String initialize(CompilationUnit root, ASTNode node) {
		fASTRoot= root;
		if (node == null)
			return SearchMessages.ExceptionOccurrencesFinder_no_exception;

		// The ExceptionOccurrencesFinder selects the whole type, no matter what part of it was selected. MethodExitsFinder behaves similar.

		if (node instanceof Name) {
			node= ASTNodes.getTopMostName((Name) node);
		}
		if (node.getLocationInParent() == TagElement.FRAGMENTS_PROPERTY) {
			MethodDeclaration method= ASTResolving.findParentMethodDeclaration(node);
			if (method == null)
				return SearchMessages.ExceptionOccurrencesFinder_no_exception;
			// in Javadoc tag:
			TagElement tagElement= (TagElement) node.getParent();
			String tagName= tagElement.getTagName();
			if (node instanceof Name
					&& node == tagElement.fragments().get(0)
					&& (TagElement.TAG_EXCEPTION.equals(tagName) || TagElement.TAG_THROWS.equals(tagName))) {
				fSelectedNode= node;
				fException= ((Name) node).resolveTypeBinding();
				fStart= method;
			}

		} else {
			Type type= ASTNodes.getTopMostType(node);
			if (type == null) {
				return SearchMessages.ExceptionOccurrencesFinder_no_exception;
			}

			// in method's "throws" list:
			if (type.getLocationInParent() == MethodDeclaration.THROWN_EXCEPTION_TYPES_PROPERTY) {
				MethodDeclaration method= ASTResolving.findParentMethodDeclaration(node);
				if (method == null)
					return SearchMessages.ExceptionOccurrencesFinder_no_exception;
				fSelectedNode= type;
				fException= type.resolveBinding();
				fStart= method;
			}

			// in catch clause:
			Type topType= type;
			if (type.getLocationInParent() == UnionType.TYPES_PROPERTY) {
				topType= (Type) type.getParent();
			}
			if (topType.getLocationInParent() == SingleVariableDeclaration.TYPE_PROPERTY
					&& topType.getParent().getLocationInParent() == CatchClause.EXCEPTION_PROPERTY) {
				fSelectedNode= type;
				fException= type.resolveBinding();
				fTryStatement= (TryStatement) topType.getParent().getParent().getParent();
				fStart= fTryStatement.getBody();
			}
		}
		if (fException == null || fStart == null)
			return SearchMessages.ExceptionOccurrencesFinder_no_exception;
		fDescription= Messages.format(SearchMessages.ExceptionOccurrencesFinder_occurrence_description, BasicElementLabels.getJavaElementName(fException.getName()));
		return null;
	}

	private void performSearch() {
		fCaughtExceptions= new ArrayList<>();
		fStart.accept(this);
		if (fTryStatement != null) {
			handleResourceDeclarations(fTryStatement);
		}
		if (fSelectedNode != null) {
			fResult.add(new OccurrenceLocation(fSelectedNode.getStartPosition(), fSelectedNode.getLength(), F_EXCEPTION_DECLARATION, fDescription));
		}
	}

	private void handleResourceDeclarations(TryStatement tryStatement) {
		List<Expression> resources= tryStatement.resources();
		for (Expression expression : resources) {
			expression.accept(this);
		}

		//check if the exception is thrown as a result of resource#close()
		boolean exitMarked= false;
		for (Expression variable : resources) {
			ITypeBinding typeBinding= null;
			if (variable instanceof VariableDeclarationExpression) {
				typeBinding= ((VariableDeclarationExpression) variable).getType().resolveBinding();
			} else if (variable instanceof Name) {
				typeBinding= ((Name) variable).resolveTypeBinding();
			}
			if (typeBinding != null) {
				IMethodBinding methodBinding= Bindings.findMethodInHierarchy(typeBinding, "close", new ITypeBinding[0]); //$NON-NLS-1$
				if (methodBinding != null) {
					for (ITypeBinding exceptionType : methodBinding.getExceptionTypes()) {
						if (matches(exceptionType)) {
							// a close() throws the caught exception
							// mark name of resource
							if (variable instanceof VariableDeclarationExpression) {
								VariableDeclarationExpression varDeclExpr= (VariableDeclarationExpression) variable;
								for (VariableDeclarationFragment fragment : (List<VariableDeclarationFragment>) varDeclExpr.fragments()) {
									SimpleName name= fragment.getName();
									fResult.add(new OccurrenceLocation(name.getStartPosition(), name.getLength(), 0, fDescription));
								}
							} else if (variable instanceof Name) {
								Name name= (Name) variable;
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

	@Override
	public OccurrenceLocation[] getOccurrences() {
		performSearch();
		if (fResult.isEmpty())
			return null;

		return fResult.toArray(new OccurrenceLocation[fResult.size()]);
	}

	@Override
	public int getSearchKind() {
		return K_EXCEPTION_OCCURRENCE;
	}


	@Override
	public CompilationUnit getASTRoot() {
		return fASTRoot;
	}

	@Override
	public String getJobLabel() {
		return SearchMessages.ExceptionOccurrencesFinder_searchfor ;
	}

	@Override
	public String getElementName() {
		if (fSelectedNode != null) {
			return ASTNodes.asString(fSelectedNode);
		}
		return null;
	}

	@Override
	public String getUnformattedPluralLabel() {
		return SearchMessages.ExceptionOccurrencesFinder_label_plural;
	}

	@Override
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
	public boolean visit(LambdaExpression node) {
		return false;
	}

	@Override
	public boolean visit(MethodDeclaration node) {
		Javadoc javadoc= node.getJavadoc();
		if (javadoc != null) {
			List<TagElement> tags= javadoc.tags();
			for (TagElement tag : tags) {
				String tagName= tag.getTagName();
				if (TagElement.TAG_EXCEPTION.equals(tagName) || TagElement.TAG_THROWS.equals(tagName)) {
					ASTNode name= (ASTNode) tag.fragments().get(0);
					if (name instanceof Name) {
						if (name != fSelectedNode && Bindings.equals(fException, ((Name) name).resolveBinding())) {
							fResult.add(new OccurrenceLocation(name.getStartPosition(), name.getLength(), 0, fDescription));
						}
					}
				}
			}
		}
		List<Type> thrownExceptionTypes= node.thrownExceptionTypes();
		for (Type type : thrownExceptionTypes) {
			if (type != fSelectedNode && Bindings.equals(fException, type.resolveBinding())) {
				fResult.add(new OccurrenceLocation(type.getStartPosition(), type.getLength(), 0, fDescription));
			}
		}
		Block body= node.getBody();
		if (body != null) {
			node.getBody().accept(this);
		}
		return false;
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
		for (CatchClause catchClause : catchClauses) {
			Type type= catchClause.getException().getType();
			if (type instanceof UnionType) {
				List<Type> types= ((UnionType) type).types();
				for (Type type2 : types) {
					addCaughtException(type2);
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
		for (CatchClause catchClause : catchClauses) {
			catchClause.accept(this);
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
		for (ITypeBinding exception : binding.getExceptionTypes()) {
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
		for (ITypeBinding catchException : fCaughtExceptions) {
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

	@Override
	public String getID() {
		return ID;
	}

}
