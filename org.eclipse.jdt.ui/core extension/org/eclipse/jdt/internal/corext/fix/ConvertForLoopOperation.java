/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.GenericVisitor;
import org.eclipse.jdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

public class ConvertForLoopOperation extends ConvertLoopOperation {
	
	private static final String LENGTH_QUERY= "length"; //$NON-NLS-1$
	private static final String LITERAL_0= "0"; //$NON-NLS-1$
	private static final String LITERAL_1= "1"; //$NON-NLS-1$
	private static final String FOR_LOOP_ELEMENT_IDENTIFIER= "element"; //$NON-NLS-1$
	
	private static final class InvalidBodyError extends Error {
		private static final long serialVersionUID= 1L;
	}
	
	private IVariableBinding fIndexBinding;
	private IVariableBinding fLengthBinding;
	private IBinding fArrayBinding;
	private Expression fArrayAccess;
	private final String fParameterName;
	
	public ConvertForLoopOperation(ForStatement forStatement) {
		this(forStatement, null);
	}
	
	public ConvertForLoopOperation(ForStatement forStatement, String parameterName) {
		super(forStatement);
		fParameterName= parameterName;
	}
	
	public boolean satisfiesPreconditions() {
		ForStatement statement= getForStatement();
		CompilationUnit ast= (CompilationUnit)statement.getRoot();
		
		IJavaElement javaElement= ast.getJavaElement();
		if (javaElement == null)
			return false;
		
		if (!JavaModelUtil.is50OrHigher(javaElement.getJavaProject()))
			return false;
		
		if (!validateInitializers(statement))
			return false;
		
		if (!validateExpression(statement))
			return false;
		
		if (!validateUpdaters(statement))
			return false;
		
		if (!validateBody(statement))
			return false;
		
		return true;
	}
	
	/**
	 * Must be one of:
	 * <ul>
	 * <li>int [result]= 0;</li>
	 * <li>int [result]= 0, [lengthBinding]= [arrayBinding].length;</li>
	 * <li>int , [result]= 0;</li>
	 * </ul>
	 */
	private boolean validateInitializers(ForStatement statement) {
		List initializers= statement.initializers();
		if (initializers.size() != 1)
			return false;
		
		Expression expression= (Expression)initializers.get(0);
		if (!(expression instanceof VariableDeclarationExpression))
			return false;
		
		VariableDeclarationExpression declaration= (VariableDeclarationExpression)expression;
		ITypeBinding declarationBinding= declaration.resolveTypeBinding();
		if (declarationBinding == null)
			return false;
		
		if (!declarationBinding.isPrimitive())
			return false;
		
		if (!PrimitiveType.INT.toString().equals(declarationBinding.getQualifiedName()))
			return false;
		
		List fragments= declaration.fragments();
		if (fragments.size() == 1) {
			IVariableBinding indexBinding= getIndexBindingFromFragment((VariableDeclarationFragment)fragments.get(0));
			if (indexBinding == null)
				return false;
			
			fIndexBinding= indexBinding;
			return true;
		} else if (fragments.size() == 2) {
			IVariableBinding indexBinding= getIndexBindingFromFragment((VariableDeclarationFragment)fragments.get(0));
			if (indexBinding == null) {
				indexBinding= getIndexBindingFromFragment((VariableDeclarationFragment)fragments.get(1));
				if (indexBinding == null)
					return false;
				
				if (!validateLengthFragment((VariableDeclarationFragment)fragments.get(0)))
					return false;
			} else {
				if (!validateLengthFragment((VariableDeclarationFragment)fragments.get(1)))
					return false;
			}
			
			fIndexBinding= indexBinding;
			return true;
		}
		return false;
	}
	
	/**
	 * [lengthBinding]= [arrayBinding].length
	 */
	private boolean validateLengthFragment(VariableDeclarationFragment fragment) {
		Expression initializer= fragment.getInitializer();
		if (initializer == null)
			return false;
		
		if (!validateLengthQuery(initializer))
			return false;
		
		IVariableBinding lengthBinding= (IVariableBinding)fragment.getName().resolveBinding();
		if (lengthBinding == null)
			return false;
		fLengthBinding= lengthBinding;
		
		return true;
	}
	
	/**
	 * Must be one of:
	 * <ul>
	 * <li>[result]= 0</li>
	 * </ul>
	 */
	private IVariableBinding getIndexBindingFromFragment(VariableDeclarationFragment fragment) {
		Expression initializer= fragment.getInitializer();
		if (!(initializer instanceof NumberLiteral))
			return null;
		
		NumberLiteral number= (NumberLiteral)initializer;
		if (!LITERAL_0.equals(number.getToken()))
			return null;
		
		return (IVariableBinding)fragment.getName().resolveBinding();
	}
	
	/**
	 * Must be one of:
	 * <ul>
	 * <li>[indexBinding] < [result].length;</li>
	 * <li>[result].length > [indexBinding];</li>
	 * <li>[indexBinding] < [lengthBinding];</li>
	 * <li>[lengthBinding] > [indexBinding];</li>
	 * </ul>
	 */
	private boolean validateExpression(ForStatement statement) {
		Expression expression= statement.getExpression();
		if (!(expression instanceof InfixExpression))
			return false;
		
		InfixExpression infix= (InfixExpression)expression;
		
		Expression left= infix.getLeftOperand();
		Expression right= infix.getRightOperand();
		if (left instanceof SimpleName && right instanceof SimpleName) {
			IVariableBinding lengthBinding= fLengthBinding;
			if (lengthBinding == null)
				return false;
			
			IBinding leftBinding= ((SimpleName)left).resolveBinding();
			IBinding righBinding= ((SimpleName)right).resolveBinding();
			
			if (fIndexBinding.equals(leftBinding)) {
				return lengthBinding.equals(righBinding);
			} else if (fIndexBinding.equals(righBinding)) {
				return lengthBinding.equals(leftBinding);
			}
			
			return false;
		} else if (left instanceof SimpleName) {
			if (!fIndexBinding.equals(((SimpleName)left).resolveBinding()))
				return false;
			
			if (!Operator.LESS.equals(infix.getOperator()))
				return false;
			
			return validateLengthQuery(right);
		} else if (right instanceof SimpleName) {
			if (!fIndexBinding.equals(((SimpleName)right).resolveBinding()))
				return false;
			
			if (!Operator.GREATER.equals(infix.getOperator()))
				return false;
			
			return validateLengthQuery(left);
		}
		
		return false;
	}
	
	/**
	 * Must be one of:
	 * <ul>
	 * <li>[result].length</li>
	 * </ul>
	 */
	private boolean validateLengthQuery(Expression lengthQuery) {
		if (lengthQuery instanceof QualifiedName) {
			QualifiedName qualifiedName= (QualifiedName)lengthQuery;
			SimpleName name= qualifiedName.getName();
			if (!LENGTH_QUERY.equals(name.getIdentifier()))
				return false;
			
			Name arrayAccess= qualifiedName.getQualifier();
			ITypeBinding accessType= arrayAccess.resolveTypeBinding();
			if (accessType == null)
				return false;
			
			if (!accessType.isArray())
				return false;
			
			IBinding arrayBinding= arrayAccess.resolveBinding();
			if (arrayBinding == null)
				return false;
			
			fArrayBinding= arrayBinding;
			fArrayAccess= arrayAccess;
			return true;
		} else if (lengthQuery instanceof FieldAccess) {
			FieldAccess fieldAccess= (FieldAccess)lengthQuery;
			SimpleName name= fieldAccess.getName();
			if (!LENGTH_QUERY.equals(name.getIdentifier()))
				return false;
			
			Expression arrayAccess= fieldAccess.getExpression();
			ITypeBinding accessType= arrayAccess.resolveTypeBinding();
			if (accessType == null)
				return false;
			
			if (!accessType.isArray())
				return false;
			
			IBinding arrayBinding= getBinding(arrayAccess);
			if (arrayBinding == null)
				return false;
			
			fArrayBinding= arrayBinding;
			fArrayAccess= arrayAccess;
			return true;
		}
		
		return false;
	}
	
	/**
	 * Must be one of:
	 * <ul>
	 * <li>[indexBinding]++</li>
	 * <li>[indexBinding]+= 1</li>
	 * <li>[indexBinding]= [indexBinding] + 1</li>
	 * <li>[indexBinding]= 1 + [indexBinding]</li>
	 * <ul>
	 */
	private boolean validateUpdaters(ForStatement statement) {
		List updaters= statement.updaters();
		if (updaters.size() != 1)
			return false;
		
		Expression updater= (Expression)updaters.get(0);
		if (updater instanceof PostfixExpression) {
			PostfixExpression postfix= (PostfixExpression)updater;
			
			if (!PostfixExpression.Operator.INCREMENT.equals(postfix.getOperator()))
				return false;
			
			IBinding binding= getBinding(postfix.getOperand());
			if (!fIndexBinding.equals(binding))
				return false;
			
			return true;
		} else if (updater instanceof Assignment) {
			Assignment assignment= (Assignment)updater;
			Expression left= assignment.getLeftHandSide();
			IBinding binding= getBinding(left);
			if (!fIndexBinding.equals(binding))
				return false;
			
			if (Assignment.Operator.PLUS_ASSIGN.equals(assignment.getOperator())) {
				return isOneLiteral(assignment.getRightHandSide());
			} else if (Assignment.Operator.ASSIGN.equals(assignment.getOperator())) {
				Expression right= assignment.getRightHandSide();
				if (!(right instanceof InfixExpression))
					return false;
				
				InfixExpression infixExpression= (InfixExpression)right;
				Expression leftOperand= infixExpression.getLeftOperand();
				IBinding leftBinding= getBinding(leftOperand);
				Expression rightOperand= infixExpression.getRightOperand();
				IBinding rightBinding= getBinding(rightOperand);
				
				if (fIndexBinding.equals(leftBinding)) {
					return isOneLiteral(rightOperand);
				} else if (fIndexBinding.equals(rightBinding)) {
					return isOneLiteral(leftOperand);
				}
			}
		}
		return false;
	}
	
	private boolean isOneLiteral(Expression expression) {
		if (!(expression instanceof NumberLiteral))
			return false;
		
		NumberLiteral literal= (NumberLiteral)expression;
		return LITERAL_1.equals(literal.getToken());
	}
	
	/**
	 * returns false iff
	 * <ul>
	 * <li><code>indexBinding</code> is used for anything else then accessing
	 * an element of <code>arrayBinding</code></li>
	 * <li><code>arrayBinding</code> is assigned</li>
	 * <li>an element of <code>arrayBinding</code> is assigned</li>
	 * <li><code>lengthBinding</code> is referenced</li>
	 * </ul>
	 * within <code>body</code>
	 */
	private boolean validateBody(ForStatement statement) {
		Statement body= statement.getBody();
		try {
			body.accept(new GenericVisitor() {
				/**
				 * {@inheritDoc}
				 */
				protected boolean visitNode(ASTNode node) {
					if (node instanceof Name) {
						Name name= (Name)node;
						IBinding nameBinding= name.resolveBinding();
						if (nameBinding == null)
							throw new InvalidBodyError();
						
						if (nameBinding.equals(fIndexBinding)) {
							if (node.getLocationInParent() != ArrayAccess.INDEX_PROPERTY)
								throw new InvalidBodyError();
							
							ArrayAccess arrayAccess= (ArrayAccess)node.getParent();
							Expression array= arrayAccess.getArray();
							
							IBinding binding= getBinding(array);
							if (binding == null)
								throw new InvalidBodyError();
							
							if (!fArrayBinding.equals(binding))
								throw new InvalidBodyError();
							
						} else if (nameBinding.equals(fArrayBinding)) {
							ASTNode current= node;
							while (current != null && !(current instanceof Statement)) {
								if (current.getLocationInParent() == Assignment.LEFT_HAND_SIDE_PROPERTY)
									throw new InvalidBodyError();
								
								if (current instanceof PrefixExpression)
									throw new InvalidBodyError();
								
								if (current instanceof PostfixExpression)
									throw new InvalidBodyError();
								
								current= current.getParent();
							}
						} else if (nameBinding.equals(fLengthBinding)) {
							throw new InvalidBodyError();
						}
					}
					
					return true;
				}
				
			});
		} catch (InvalidBodyError e) {
			return false;
		}
		
		return true;
	}
	
	private static IBinding getBinding(Expression expression) {
		if (expression instanceof FieldAccess) {
			return ((FieldAccess)expression).resolveFieldBinding();
		} else if (expression instanceof Name) {
			return ((Name)expression).resolveBinding();
		}
		
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.fix.LinkedFix.ILinkedFixRewriteOperation#rewriteAST(org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite, java.util.List, java.util.List)
	 */
	public void rewriteAST(CompilationUnitRewrite cuRewrite, List textEditGroups, LinkedProposalModel positionGroups) throws CoreException {
		TextEditGroup group= createTextEditGroup(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description);
		textEditGroups.add(group);
		ASTRewrite rewrite= cuRewrite.getASTRewrite();
		
		Statement statement= convert(cuRewrite, group, positionGroups);
		rewrite.replace(getForStatement(), statement, group);
	}
	
	protected Statement convert(CompilationUnitRewrite cuRewrite, TextEditGroup group, LinkedProposalModel positionGroups) throws CoreException {
		ASTRewrite rewrite= cuRewrite.getASTRewrite();
		ImportRewrite importRewrite= cuRewrite.getImportRewrite();
		
		ForStatement forStatement= getForStatement();
		
		VariableDeclarationFragment fragement= findArrayElementDeclaration(forStatement.getBody(), fIndexBinding, fArrayBinding);
		
		String[] proposals= getProposalsForElement(fArrayAccess.resolveTypeBinding(), cuRewrite.getCu().getJavaProject());
		
		String parameterName;
		if (fragement == null) {
			if (fParameterName != null) {
				parameterName= fParameterName;
			} else {
				parameterName= proposals[0];
			}
		} else {
			parameterName= fragement.getName().getIdentifier();
		}
		LinkedProposalPositionGroup pg= positionGroups.getPositionGroup(parameterName, true);
		
		AST ast= forStatement.getAST();
		EnhancedForStatement result= ast.newEnhancedForStatement();
		
		SingleVariableDeclaration parameterDeclaration= createParameterDeclaration(parameterName, fragement, fArrayAccess, forStatement, importRewrite, rewrite, group, pg);
		result.setParameter(parameterDeclaration);
		
		result.setExpression((Expression)rewrite.createCopyTarget(fArrayAccess));
		
		convertBody(forStatement.getBody(), fIndexBinding, fArrayBinding, parameterName, rewrite, group, pg);
		result.setBody(getBody(cuRewrite, group, positionGroups));
		
		for (int i= 0; i < proposals.length; i++) {
			pg.addProposal(proposals[i], null, 10);
		}
		
		positionGroups.setEndPosition(rewrite.track(result));
		
		return result;
	}
	
	/**
	 * Finds the first fragment in the body looking like:
	 * <code>variable= arrayBinding[indexBinding]</code> or null if no such
	 * fragment
	 */
	private VariableDeclarationFragment findArrayElementDeclaration(Statement body, final IBinding indexBinding, final IBinding arrayBinding) {
		final VariableDeclarationFragment[] result= new VariableDeclarationFragment[] {null};
		body.accept(new GenericVisitor() {
			public boolean visit(ArrayAccess node) {
				IBinding binding= getBinding(node.getArray());
				if (arrayBinding.equals(binding)) {
					IBinding index= getBinding(node.getIndex());
					if (indexBinding.equals(index)) {
						if (node.getLocationInParent() == VariableDeclarationFragment.INITIALIZER_PROPERTY) {
							result[0]= (VariableDeclarationFragment)node.getParent();
							return false;
						}
					}
				}
				return result[0] == null;
			}
		});
		
		return result[0];
	}
	
	private void convertBody(Statement body, final IBinding indexBinding, final IBinding arrayBinding, final String parameterName, final ASTRewrite rewrite, final TextEditGroup editGroup, final LinkedProposalPositionGroup pg) {
		final AST ast= body.getAST();
		
		final HashSet assignedBindings= new HashSet();
		
		body.accept(new GenericVisitor() {
			public boolean visit(ArrayAccess node) {
				IBinding binding= getBinding(node.getArray());
				if (arrayBinding.equals(binding)) {
					IBinding index= getBinding(node.getIndex());
					if (indexBinding.equals(index)) {
						replaceAccess(node);
					}
				}
				
				return super.visit(node);
			}
			
			public boolean visit(SimpleName node) {
				if (assignedBindings.contains(node.resolveBinding())) {
					replaceAccess(node);
				}
				return super.visit(node);
			}
			
			private void replaceAccess(ASTNode node) {
				if (node.getLocationInParent() == VariableDeclarationFragment.INITIALIZER_PROPERTY) {
					VariableDeclarationFragment fragment= (VariableDeclarationFragment)node.getParent();
					IBinding targetBinding= fragment.getName().resolveBinding();
					if (targetBinding != null) {
						assignedBindings.add(targetBinding);
						
						VariableDeclarationStatement statement= (VariableDeclarationStatement)fragment.getParent();
						
						if (statement.fragments().size() == 1) {
							rewrite.remove(statement, editGroup);
						} else {
							ListRewrite listRewrite= rewrite.getListRewrite(statement, VariableDeclarationStatement.FRAGMENTS_PROPERTY);
							listRewrite.remove(fragment, editGroup);
						}
						
					} else {
						SimpleName name= ast.newSimpleName(parameterName);
						rewrite.replace(node, name, editGroup);
						pg.addPosition(rewrite.track(name), true);
					}
				} else {
					SimpleName name= ast.newSimpleName(parameterName);
					rewrite.replace(node, name, editGroup);
					pg.addPosition(rewrite.track(name), true);
				}
			}
		});
	}
	
	private SingleVariableDeclaration createParameterDeclaration(String parameterName, VariableDeclarationFragment fragement, Expression arrayAccess, ForStatement statement, ImportRewrite importRewrite, ASTRewrite rewrite, TextEditGroup group, LinkedProposalPositionGroup pg) {
		CompilationUnit compilationUnit= (CompilationUnit)arrayAccess.getRoot();
		AST ast= compilationUnit.getAST();
		
		SingleVariableDeclaration result= ast.newSingleVariableDeclaration();
		
		SimpleName name= ast.newSimpleName(parameterName);
		pg.addPosition(rewrite.track(name), true);
		result.setName(name);
		
		ITypeBinding arrayTypeBinding= arrayAccess.resolveTypeBinding();
		Type type= importType(arrayTypeBinding.getElementType(), statement, importRewrite, compilationUnit);
		if (arrayTypeBinding.getDimensions() != 1) {
			type= ast.newArrayType(type, arrayTypeBinding.getDimensions() - 1);
		}
		result.setType(type);
		
		if (fragement != null) {
			VariableDeclarationStatement declaration= (VariableDeclarationStatement)fragement.getParent();
			ModifierRewrite.create(rewrite, result).copyAllModifiers(declaration, group);
		}
		
		return result;
	}
	
	private String[] getProposalsForElement(ITypeBinding arrayTypeBinding, IJavaProject project) {
		String[] variableNames= getUsedVariableNames();
		String[] elementSuggestions= StubUtility.getLocalNameSuggestions(project, FOR_LOOP_ELEMENT_IDENTIFIER, 0, variableNames);
		
		String type= arrayTypeBinding.getElementType().getName();
		String[] typeSuggestions= StubUtility.getLocalNameSuggestions(project, type, arrayTypeBinding.getDimensions() - 1, variableNames);
		
		String[] result= new String[elementSuggestions.length + typeSuggestions.length];
		System.arraycopy(elementSuggestions, 0, result, 0, elementSuggestions.length);
		System.arraycopy(typeSuggestions, 0, result, elementSuggestions.length, typeSuggestions.length);
		return result;
	}
	
	private String[] getUsedVariableNames() {
		final List results= new ArrayList();
		
		ForStatement forStatement= getForStatement();
		CompilationUnit root= (CompilationUnit)forStatement.getRoot();
		
		Collection variableNames= new ScopeAnalyzer(root).getUsedVariableNames(forStatement.getStartPosition(), forStatement.getLength());
		results.addAll(variableNames);
		
		forStatement.accept(new GenericVisitor() {
			public boolean visit(SingleVariableDeclaration node) {
				results.add(node.getName().getIdentifier());
				return super.visit(node);
			}
			
			public boolean visit(VariableDeclarationFragment fragment) {
				results.add(fragment.getName().getIdentifier());
				return super.visit(fragment);
			}
		});
		
		return (String[])results.toArray(new String[results.size()]);
	}
	
}
