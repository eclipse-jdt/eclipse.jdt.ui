/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
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

package org.eclipse.jdt.internal.ui.text.correction.proposals;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.WildcardType;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;
import org.eclipse.jdt.internal.ui.util.ASTHelper;

public class InitializeFinalFieldProposal extends LinkedCorrectionProposal {
	private IProblemLocation fProblem;

	private ASTNode fAstNode;

	private final IVariableBinding fVariableBinding;

	private boolean fInConstructor;


	public InitializeFinalFieldProposal(IProblemLocation problem, ICompilationUnit cu, ASTNode astNode, IVariableBinding variableBinding, int relevance) {
		super(Messages.format(CorrectionMessages.InitializeFieldAtDeclarationCorrectionProposal_description, problem.getProblemArguments()[0]), cu, null, relevance, null);

		fProblem= problem;
		fAstNode= astNode;
		fVariableBinding= variableBinding;
		fInConstructor= false;
		setImage(JavaPluginImages.get(JavaPluginImages.IMG_FIELD_PRIVATE));
	}

	public InitializeFinalFieldProposal(IProblemLocation problem, ICompilationUnit cu, ASTNode astNode, int relevance, boolean inConstructor) {
		super(Messages.format(CorrectionMessages.InitializeFieldInConstructorCorrectionProposal_description, problem.getProblemArguments()[0]), cu, null, relevance, null);

		fProblem= problem;
		fAstNode= astNode;
		fVariableBinding= null;
		fInConstructor= inConstructor;
		setImage(JavaPluginImages.get(JavaPluginImages.IMG_FIELD_PRIVATE));
	}

	@Override
	protected ASTRewrite getRewrite() throws CoreException {
		if (fInConstructor) {
			if (fAstNode != null
					&& fAstNode.getParent() instanceof RecordDeclaration
					&& ASTHelper.isRecordDeclarationNodeSupportedInAST(fAstNode.getAST())) {
				return doInitRecordComponentsInConstructor();
			}
			return doInitFieldInConstructor();
		}
		return doInitField();
	}

	private ASTRewrite doInitField() {
		FieldDeclaration field= getFieldDeclaration(fVariableBinding.getName());
		if (field == null) {
			return null;
		}
		AST ast= fAstNode.getAST();

		ITypeBinding type= fVariableBinding.getType();
		String variableTypeName= type.getName();

		VariableDeclarationFragment newFragment= ast.newVariableDeclarationFragment();
		newFragment.setName(ast.newSimpleName(fVariableBinding.getName()));

		FieldDeclaration declaration= ast.newFieldDeclaration(newFragment);
		declaration.modifiers().addAll(ast.newModifiers(fVariableBinding.getModifiers()));

		Expression expression= null;
		if (type.isPrimitive()) {
			declaration.setType(ast.newPrimitiveType(PrimitiveType.toCode(variableTypeName)));
			expression= ast.newNumberLiteral();
		} else if ("String".equals(variableTypeName)) { //$NON-NLS-1$
			declaration.setType(ast.newSimpleType(ast.newName(variableTypeName)));
			expression= ast.newStringLiteral();
		} else {
			if (type.isParameterizedType()) {
				Type newType= createParameterizedType(ast, type);
				declaration.setType(newType);
			} else {
				SimpleType newSimpleType= ast.newSimpleType(ast.newName(variableTypeName));
				declaration.setType(newSimpleType);
			}

			if (hasDefaultConstructor()) {
				ClassInstanceCreation cic= ast.newClassInstanceCreation();
				cic.setType(ast.newSimpleType(ast.newName(variableTypeName)));
				expression= cic;
			} else {
				expression= ast.newNullLiteral();
			}
		}
		newFragment.setInitializer(expression);

		ASTRewrite rewrite= ASTRewrite.create(ast);
		rewrite.replace(field, declaration, null);
		setEndPosition(rewrite.track(expression)); // set cursor after expression statement
		return rewrite;
	}

	private Type createParameterizedType(AST ast, ITypeBinding typeBinding) {
		if (typeBinding.isParameterizedType() && !typeBinding.isRawType()) {
			Type baseType= ast.newSimpleType(ASTNodeFactory.newName(ast, typeBinding.getErasure().getName()));
			ParameterizedType newType= ast.newParameterizedType(baseType);
			for (int i= 0; i < typeBinding.getTypeArguments().length; i++) {
				ITypeBinding typeArg= typeBinding.getTypeArguments()[i];
				Type argType= createParameterizedType(ast, typeArg); // recursive call
				newType.typeArguments().add(argType);
			}
			return newType;
		} else {
			if (!typeBinding.isTypeVariable()) {
				if (typeBinding.isWildcardType()) {
					String newName= typeBinding.getBound().getName();
					WildcardType newWildcardType= ast.newWildcardType();
					newWildcardType.setBound(ast.newSimpleType(ast.newSimpleName(newName)), typeBinding.isUpperbound());
					return newWildcardType;
				}
				return ast.newSimpleType(ASTNodeFactory.newName(ast, typeBinding.getErasure().getName()));
			}
			return ast.newSimpleType(ast.newSimpleName(typeBinding.getName()));
		}
	}

	private ASTRewrite doInitRecordComponentsInConstructor() {
		String variableName= fProblem.getProblemArguments()[0];
		SingleVariableDeclaration svd= getRecordComponentDeclaration(variableName);
		if (svd == null) {
			return null;
		}
		AST ast= svd.getAST();

		FieldAccess fieldAccess= ast.newFieldAccess();
		fieldAccess.setName(ast.newSimpleName(variableName));
		fieldAccess.setExpression(ast.newThisExpression());

		Assignment assignment= ast.newAssignment();
		assignment.setLeftHandSide(fieldAccess);
		assignment.setOperator(Assignment.Operator.ASSIGN);

		Type fieldType= svd.getType();
		if (fieldType.isPrimitiveType()) {
			assignment.setRightHandSide(ast.newNumberLiteral());
		} else if ("String".equals(fieldType.toString())) { //$NON-NLS-1$
			assignment.setRightHandSide(ast.newStringLiteral());
		} else {
			if (hasDefaultConstructor(fieldType.resolveBinding())) {
				ClassInstanceCreation cic= ast.newClassInstanceCreation();
				cic.setType(ast.newSimpleType(ast.newName(fieldType.toString())));
				assignment.setRightHandSide(cic);
			} else {
				assignment.setRightHandSide(ast.newNullLiteral());
			}
		}
		ExpressionStatement statement= ast.newExpressionStatement(assignment);

		ASTRewrite rewrite= ASTRewrite.create(ast);
		// find all constructors (methods with same name as the type name)
		ConstructorVisitor cv= new ConstructorVisitor(((RecordDeclaration) svd.getParent()).getName().toString());
		fAstNode.getRoot().accept(cv);

		for (MethodDeclaration md : cv.getNodes()) {
			Block body= md.getBody();
			rewrite.getListRewrite(body, Block.STATEMENTS_PROPERTY).insertAt(statement, 0, null);
			setEndPosition(rewrite.track(assignment)); // set cursor after expression statement
		}
		return rewrite;
	}

	private ASTRewrite doInitFieldInConstructor() {
		String variableName= fProblem.getProblemArguments()[0];
		FieldDeclaration field= getFieldDeclaration(variableName);
		if (field == null) {
			return null;
		}
		AST ast= field.getAST();

		FieldAccess fieldAccess= ast.newFieldAccess();
		fieldAccess.setName(ast.newSimpleName(variableName));
		fieldAccess.setExpression(ast.newThisExpression());

		Assignment assignment= ast.newAssignment();
		assignment.setLeftHandSide(fieldAccess);
		assignment.setOperator(Assignment.Operator.ASSIGN);

		Type fieldType= field.getType();
		if (fieldType.isPrimitiveType()) {
			assignment.setRightHandSide(ast.newNumberLiteral());
		} else if ("String".equals(fieldType.toString())) { //$NON-NLS-1$
			assignment.setRightHandSide(ast.newStringLiteral());
		} else {
			if (hasDefaultConstructor(fieldType.resolveBinding())) {
				ClassInstanceCreation cic= ast.newClassInstanceCreation();
				cic.setType(ast.newSimpleType(ast.newName(fieldType.toString())));
				assignment.setRightHandSide(cic);
			} else {
				assignment.setRightHandSide(ast.newNullLiteral());
			}
		}
		ExpressionStatement statement= ast.newExpressionStatement(assignment);

		ASTRewrite rewrite= ASTRewrite.create(ast);
		// find all constructors (methods with same name as the type name)
		ConstructorVisitor cv= new ConstructorVisitor(((TypeDeclaration) field.getParent()).getName().toString());
		fAstNode.getRoot().accept(cv);

		for (MethodDeclaration md : cv.getNodes()) {
			Block body= md.getBody();
			// check if we call this(), then we can't add initialization here
			if (!hasThisCall(body)) {
				rewrite.getListRewrite(body, Block.STATEMENTS_PROPERTY).insertAt(statement, 0, null);
				setEndPosition(rewrite.track(assignment)); // set cursor after expression statement
			}
		}
		return rewrite;
	}

	private boolean hasThisCall(Block body) {
		for (Object object : body.statements()) {
			if (((ASTNode) object).getNodeType() == ASTNode.CONSTRUCTOR_INVOCATION) {
				return true;
			}
		}
		return false;
	}

	private boolean hasDefaultConstructor(ITypeBinding type) {
		for (IMethodBinding mb : type.getDeclaredMethods()) {
			String key = mb.getKey();
			if (key.contains(";.()V")) { //$NON-NLS-1$
				return true;
			}
		}
		return false;
	}

	private boolean hasDefaultConstructor() {
		return hasDefaultConstructor(fVariableBinding.getType());
	}

	private FieldDeclaration getFieldDeclaration(String name) {
		FieldVisitor fieldVisitor= new FieldVisitor();
		fAstNode.getRoot().accept(fieldVisitor);

		for (FieldDeclaration f : fieldVisitor.getNodes()) {
			for (Object object : f.fragments()) {
				VariableDeclarationFragment fragment= (VariableDeclarationFragment) object;
				if (fragment.getName().getIdentifier().equals(name)) {
					return f;
				}
			}
		}
		return null;
	}

	private SingleVariableDeclaration getRecordComponentDeclaration(String name) {
		RecordComponentVisitor recordComponentVisitor= new RecordComponentVisitor();
		fAstNode.getRoot().accept(recordComponentVisitor);

		for (SingleVariableDeclaration f : recordComponentVisitor.getNodes()) {
			if (f.getName().getIdentifier().equals(name)) {
				return f;
			}
		}
		return null;
	}

	private static final class FieldVisitor extends ASTVisitor {
		private final List<FieldDeclaration> fNodes= new ArrayList<>();

		public FieldVisitor() {
		}

		@Override
		public boolean visit(FieldDeclaration node) {
			fNodes.add(node);
			return true;
		}

		public List<FieldDeclaration> getNodes() {
			return fNodes;
		}
	}

	private static final class ConstructorVisitor extends ASTVisitor {
		private final List<MethodDeclaration> fNodes= new ArrayList<>();

		private String fTypeName;

		public ConstructorVisitor(String typeName) {
			fTypeName= typeName;
		}

		@Override
		public boolean visit(MethodDeclaration node) {
			if (node.getName().toString().equals(fTypeName)) {
				fNodes.add(node);
			}
			return true;
		}

		public List<MethodDeclaration> getNodes() {
			return fNodes;
		}
	}

	private static final class RecordComponentVisitor extends ASTVisitor {
		private final List<SingleVariableDeclaration> fNodes= new ArrayList<>();

		public RecordComponentVisitor() {
		}

		@Override
		public boolean visit(RecordDeclaration node) {
			List<SingleVariableDeclaration> decl= node.recordComponents();
			if (decl != null)
				fNodes.addAll(decl);
			return true;
		}

		public List<SingleVariableDeclaration> getNodes() {
			return fNodes;
		}
	}
}
