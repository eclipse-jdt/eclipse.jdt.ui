/*******************************************************************************
 * Copyright (c) 2013 Yatta Solutions GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Lukas Hanke <hanke@yatta.de> - Bug 241696 [quick fix] quickfix to iterate over a collection - https://bugs.eclipse.org/bugs/show_bug.cgi?id=241696
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction.proposals;

import java.util.Collection;
import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.text.link.LinkedPosition;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;
import org.eclipse.jdt.internal.ui.text.correction.IProposalRelevance;

/**
 * Generates a proposal for quick assist, to loop over a variable or method result which represents
 * an {@link Iterable} or an array.
 */
public class GenerateForLoopAssistProposal extends LinkedCorrectionProposal {

	public static final int GENERATE_FOREACH= 0;

	public static final int GENERATE_ITERATOR_FOR= 1;

	public static final int GENERATE_ITERATE_ARRAY= 2;

	private ASTNode fCurrentNode;

	private Expression fCurrentExpression;

	private Expression fSubExpression;

	private int fLoopTypeToGenerate= -1;

	/**
	 * Creates an instance of a {@link GenerateForLoopAssistProposal}.
	 * 
	 * @param cu the current {@link ICompilationUnit}
	 * @param currentNode the {@link ASTNode} instance representing the statement on which the
	 *            assist was called
	 * @param currentExpression the {@link Expression} contained in the currentNode
	 * @param loopTypeToGenerate the type of the loop to generate, possible values are
	 *            {@link GenerateForLoopAssistProposal#GENERATE_FOREACH},
	 *            {@link GenerateForLoopAssistProposal#GENERATE_ITERATOR_FOR} or
	 *            {@link GenerateForLoopAssistProposal#GENERATE_ITERATE_ARRAY}
	 */
	public GenerateForLoopAssistProposal(ICompilationUnit cu, ASTNode currentNode, Expression currentExpression, int loopTypeToGenerate) {
		super("", cu, null, IProposalRelevance.GENERATE_FOR_LOOP, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE)); //$NON-NLS-1$
		fCurrentNode= currentNode;
		fCurrentExpression= currentExpression;
		fLoopTypeToGenerate= loopTypeToGenerate;

		switch (loopTypeToGenerate) {
			case GenerateForLoopAssistProposal.GENERATE_FOREACH:
				setDisplayName(CorrectionMessages.QuickAssistProcessor_generate_enhanced_for_loop);
				break;
			case GenerateForLoopAssistProposal.GENERATE_ITERATOR_FOR:
				setDisplayName(CorrectionMessages.QuickAssistProcessor_generate_iterator_for_loop);
				break;
			case GenerateForLoopAssistProposal.GENERATE_ITERATE_ARRAY:
				setDisplayName(CorrectionMessages.QuickAssistProcessor_generate_iterate_array_for_loop);
				break;
			default:
				break;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposal#getRewrite()
	 */
	@Override
	protected ASTRewrite getRewrite() throws CoreException {

		AST ast= fCurrentNode.getAST();

		// generate the subexpression which represents the expression to iterate over
		if (fCurrentExpression instanceof Assignment) {
			this.fSubExpression= ((Assignment) fCurrentExpression).getLeftHandSide();
		} else {
			this.fSubExpression= fCurrentExpression;
		}

		switch (fLoopTypeToGenerate) {
			case GenerateForLoopAssistProposal.GENERATE_FOREACH:
				return generateForEachRewrite(ast);
			case GenerateForLoopAssistProposal.GENERATE_ITERATOR_FOR:
				return generateIteratorBasedForRewrite(ast);
			case GenerateForLoopAssistProposal.GENERATE_ITERATE_ARRAY:
				return generateIndexBasedForRewrite(ast);
			default:
				return null;
		}
	}

	/**
	 * Helper to generate a <code>foreach</code> loop to iterate over an {@link Iterable}.
	 * 
	 * @param ast the {@link AST} instance to rewrite the loop to
	 * @return the complete {@link ASTRewrite} object
	 */
	private ASTRewrite generateForEachRewrite(AST ast) {

		EnhancedForStatement loopStatement= ast.newEnhancedForStatement();

		ASTRewrite rewrite= ASTRewrite.create(ast);
		String loopOverTypename= extractElementTypeName(ast);
		if (loopOverTypename == null) {
			loopOverTypename= Object.class.getSimpleName();
		}
		// generate name proposals and add them to the variable declaration
		SimpleName forDeclarationName= resolveLinkedVariableNameWithProposals(ast, rewrite, loopOverTypename, true);

		SingleVariableDeclaration forLoopInitializer= ast.newSingleVariableDeclaration();
		forLoopInitializer.setType(ast.newSimpleType(ast.newSimpleName(loopOverTypename)));
		forLoopInitializer.setName(forDeclarationName);

		loopStatement.setParameter(forLoopInitializer);
		loopStatement.setExpression((Expression) ASTNode.copySubtree(ast, fSubExpression));

		Block forLoopBody= ast.newBlock();
		forLoopBody.statements().add(createBlankLineStatementWithCursorPosition(rewrite));

		loopStatement.setBody(forLoopBody);

		rewrite.replace(fCurrentNode, loopStatement, null);

		return rewrite;
	}

	/**
	 * Helper to generate an iterator based <code>for</code> loop to iterate over an
	 * {@link Iterable}.
	 * 
	 * @param ast the {@link AST} instance to rewrite the loop to
	 * @return the complete {@link ASTRewrite} object
	 */
	private ASTRewrite generateIteratorBasedForRewrite(AST ast) {
		ASTRewrite rewrite= ASTRewrite.create(ast);
		ForStatement loopStatement= ast.newForStatement();

		String loopOverTypename= extractElementTypeName(ast);
		if (loopOverTypename == null && JavaModelUtil.is50OrHigher(getCompilationUnit().getJavaProject())) {
			loopOverTypename= Object.class.getSimpleName();
		}

		SimpleName loopVariableName= resolveLinkedVariableNameWithProposals(ast, rewrite, "iterator", true); //$NON-NLS-1$
		loopStatement.initializers().add(getIteratorBasedForInitializer(ast, rewrite, loopOverTypename, loopVariableName));

		MethodInvocation loopExpression= ast.newMethodInvocation();
		loopExpression.setName(ast.newSimpleName("hasNext")); //$NON-NLS-1$
		SimpleName expressionName= ast.newSimpleName(loopVariableName.getIdentifier());
		addLinkedPosition(rewrite.track(expressionName), false, expressionName.getIdentifier());
		loopExpression.setExpression(expressionName);

		loopStatement.setExpression(loopExpression);

		Block forLoopBody= ast.newBlock();
		Assignment assignResolvedVariable= getIteratorBasedForBodyAssignment(ast, rewrite, loopOverTypename, loopVariableName);
		forLoopBody.statements().add(ast.newExpressionStatement(assignResolvedVariable));
		forLoopBody.statements().add(createBlankLineStatementWithCursorPosition(rewrite));

		loopStatement.setBody(forLoopBody);

		ImportRewrite fixImports= createImportRewrite((CompilationUnit) fCurrentExpression.getRoot());
		fixImports.addImport("java.util.Iterator"); //$NON-NLS-1$

		rewrite.replace(fCurrentNode, loopStatement, null);

		return rewrite;
	}

	/**
	 * Generates the initializer for an iterator based <code>for</code> loop, which declares and
	 * initializes the variable to loop over.
	 * 
	 * @param ast the current {@link AST}
	 * @param rewrite the instance of {@link ASTRewrite}
	 * @param loopOverTypename the type of the loop variable represented as {@link String}
	 * @param loopVariableName the proposed name of the loop variable
	 * @return a {@link VariableDeclarationExpression} to use as initializer
	 */
	private VariableDeclarationExpression getIteratorBasedForInitializer(AST ast, ASTRewrite rewrite, String loopOverTypename, SimpleName loopVariableName) {
		// initializing fragment
		VariableDeclarationFragment varDeclarationFragment= ast.newVariableDeclarationFragment();
		varDeclarationFragment.setName(loopVariableName);
		MethodInvocation iteratorExpression= ast.newMethodInvocation();
		iteratorExpression.setName(ast.newSimpleName("iterator")); //$NON-NLS-1$
		iteratorExpression.setExpression((Expression) ASTNode.copySubtree(ast, fSubExpression));
		varDeclarationFragment.setInitializer(iteratorExpression);

		// declaration
		VariableDeclarationExpression varDeclarationExpression= ast.newVariableDeclarationExpression(varDeclarationFragment);
		SimpleType type= ast.newSimpleType(ast.newSimpleName("Iterator")); //$NON-NLS-1$
		if (loopOverTypename != null) {
			ParameterizedType parameterizedType= ast.newParameterizedType(type);
			parameterizedType.typeArguments().add(ast.newSimpleType(ast.newSimpleName(loopOverTypename)));
			varDeclarationExpression.setType(parameterizedType);
		} else {
			varDeclarationExpression.setType(type);
		}

		return varDeclarationExpression;
	}

	/**
	 * Generates the Assignment in an iterator based for, used in the first statement of an iterator
	 * based <code>for</code> loop body, to retrieve the next element of the {@link Iterable}
	 * instance.
	 * 
	 * @param ast the current {@link AST}
	 * @param rewrite the current instance of {@link ASTRewrite}
	 * @param loopOverTypename the type of the loop variable in string representation
	 * @param loopVariableName the name of the loop variable
	 * @return an {@link Assignment}, which retrieves the next element of the {@link Iterable} using
	 *         the active {@link Iterator}
	 */
	private Assignment getIteratorBasedForBodyAssignment(AST ast, ASTRewrite rewrite, String loopOverTypename, SimpleName loopVariableName) {
		Assignment assignResolvedVariable= ast.newAssignment();

		// in case no generics were given we get instances of Object.class using iterator.next()
		String elementTypename= (loopOverTypename == null ? Object.class.getSimpleName() : loopOverTypename);
		// left hand side
		SimpleName resolvedVariableName= resolveLinkedVariableNameWithProposals(ast, rewrite, elementTypename, false);
		VariableDeclarationFragment resolvedVariableDeclarationFragment= ast.newVariableDeclarationFragment();
		resolvedVariableDeclarationFragment.setName(resolvedVariableName);
		VariableDeclarationExpression resolvedVariableDeclaration= ast.newVariableDeclarationExpression(resolvedVariableDeclarationFragment);
		resolvedVariableDeclaration.setType(ast.newSimpleType(ast.newSimpleName(elementTypename)));
		assignResolvedVariable.setLeftHandSide(resolvedVariableDeclaration);

		// right hand side
		MethodInvocation invokeIteratorNextExpression= ast.newMethodInvocation();
		invokeIteratorNextExpression.setName(ast.newSimpleName("next")); //$NON-NLS-1$
		SimpleName currentElementName= ast.newSimpleName(loopVariableName.getIdentifier());
		addLinkedPosition(rewrite.track(currentElementName), false, currentElementName.getIdentifier());
		invokeIteratorNextExpression.setExpression(currentElementName);
		assignResolvedVariable.setRightHandSide(invokeIteratorNextExpression);

		assignResolvedVariable.setOperator(Assignment.Operator.ASSIGN);

		return assignResolvedVariable;
	}

	/**
	 * Helper to generate an index based <code>for</code> loop to iterate over an array.
	 * 
	 * @param ast the current {@link AST} instance to generate the {@link ASTRewrite} for
	 * @return an applicable {@link ASTRewrite} instance
	 */
	private ASTRewrite generateIndexBasedForRewrite(AST ast) {
		ASTRewrite rewrite= ASTRewrite.create(ast);

		ForStatement loopStatement= ast.newForStatement();
		SimpleName loopVariableName= resolveLinkedVariableNameWithProposals(ast, rewrite, "i", true); //$NON-NLS-1$
		loopStatement.initializers().add(getIndexBasedForInitializer(ast, loopVariableName));
		loopStatement.setExpression(getLinkedInfixExpression(ast, rewrite, loopVariableName.getIdentifier()));
		loopStatement.updaters().add(getLinkedIncrementExpression(ast, rewrite, loopVariableName.getIdentifier()));

		Block forLoopBody= ast.newBlock();
		forLoopBody.statements().add(createBlankLineStatementWithCursorPosition(rewrite));
		loopStatement.setBody(forLoopBody);
		rewrite.replace(fCurrentNode, loopStatement, null);

		return rewrite;
	}

	/**
	 * Creates an {@link InfixExpression} which is linked to the group of the variableToIncrement.
	 * 
	 * @param ast the current {@link AST} instance
	 * @param rewrite the current {@link ASTRewrite} instance
	 * @param variableToIncrement the name of the variable to generate the {@link InfixExpression}
	 *            for
	 * @return a filled, new {@link InfixExpression} instance
	 */
	private InfixExpression getLinkedInfixExpression(AST ast, ASTRewrite rewrite, String variableToIncrement) {
		InfixExpression loopExpression= ast.newInfixExpression();
		SimpleName name= ast.newSimpleName(variableToIncrement);
		addLinkedPosition(rewrite.track(name), false, name.getIdentifier());
		loopExpression.setLeftOperand(name);
		loopExpression.setOperator(InfixExpression.Operator.LESS);

		FieldAccess getArrayLengthExpression= ast.newFieldAccess();
		getArrayLengthExpression.setExpression((Expression) ASTNode.copySubtree(ast, fSubExpression));
		getArrayLengthExpression.setName(ast.newSimpleName("length")); //$NON-NLS-1$

		loopExpression.setRightOperand(getArrayLengthExpression);
		return loopExpression;
	}

	/**
	 * Creates a {@link PostfixExpression} used to increment the loop variable of a <code>for</code>
	 * loop to iterate over an array.
	 * 
	 * @param ast the current {@link AST} instance
	 * @param rewrite the current {@link ASTRewrite} instance
	 * @param variableToIncrement the name of the variable to increment
	 * @return a filled {@link PostfixExpression} realizing an incrementation of the specified
	 *         variable
	 */
	private Expression getLinkedIncrementExpression(AST ast, ASTRewrite rewrite, String variableToIncrement) {
		PostfixExpression incrementLoopVariable= ast.newPostfixExpression();
		SimpleName name= ast.newSimpleName(variableToIncrement);
		addLinkedPosition(rewrite.track(name), false, name.getIdentifier());
		incrementLoopVariable.setOperand(name);
		incrementLoopVariable.setOperator(PostfixExpression.Operator.INCREMENT);
		return incrementLoopVariable;
	}

	/**
	 * Generates an {@link VariableDeclarationExpression}, which initializes the loop variable to
	 * iterate over an array.
	 * 
	 * @param ast the current {@link AST} instance
	 * @param loopVariableName the name of the variable which should be initialized
	 * @return a filled {@link VariableDeclarationExpression}, declaring a int variable, which is
	 *         initializes with 0
	 */
	private VariableDeclarationExpression getIndexBasedForInitializer(AST ast, SimpleName loopVariableName) {
		// initializing fragment
		VariableDeclarationFragment firstDeclarationFragment= ast.newVariableDeclarationFragment();
		firstDeclarationFragment.setName(loopVariableName);
		NumberLiteral startIndex= ast.newNumberLiteral();
		firstDeclarationFragment.setInitializer(startIndex);

		// declaration
		VariableDeclarationExpression variableDeclaration= ast.newVariableDeclarationExpression(firstDeclarationFragment);
		PrimitiveType variableType= ast.newPrimitiveType(PrimitiveType.INT);
		variableDeclaration.setType(variableType);

		return variableDeclaration;
	}

	/**
	 * Resolves name proposals by the given basename and adds a {@link LinkedPosition} to the
	 * returned {@link SimpleName} expression.
	 * 
	 * @param ast the current {@link AST}
	 * @param rewrite the current instance of an {@link ASTRewrite}
	 * @param basename the base string to use for proposal calculation
	 * @param firstLinkedProposal true if the generated name is the first {@link LinkedPosition} to
	 *            edit in the current {@link CompilationUnit}, false otherwise
	 * @return the linked {@link SimpleName} instance based on the name proposals
	 */
	private SimpleName resolveLinkedVariableNameWithProposals(AST ast, ASTRewrite rewrite, String basename, boolean firstLinkedProposal) {
		String[] nameProposals= getVariableNameProposals(basename);
		SimpleName forDeclarationName= (nameProposals.length > 0 ? ast.newSimpleName(nameProposals[0]) : ast.newSimpleName(basename));
		for (int i= 0; i < nameProposals.length; i++) {
			addLinkedPositionProposal(forDeclarationName.getIdentifier(), nameProposals[i], null);
		}

		// mark declaration name as editable
		addLinkedPosition(rewrite.track(forDeclarationName), firstLinkedProposal, forDeclarationName.getIdentifier());
		return forDeclarationName;
	}

	/**
	 * Generates an empty statement, which is shown as blank line and is set as end position for the
	 * cursor.
	 * 
	 * @param rewrite the current {@link ASTRewrite} instance
	 * @return an empty statement, shown as blank line
	 */
	private Statement createBlankLineStatementWithCursorPosition(ASTRewrite rewrite) {
		Statement blankLineStatement= (Statement) rewrite.createStringPlaceholder("", ASTNode.EMPTY_STATEMENT); //$NON-NLS-1$
		setEndPosition(rewrite.track(blankLineStatement));
		return blankLineStatement;
	}

	/**
	 * Retrieves variable name proposals for the loop variable.
	 * 
	 * @param basename the basename of the proposals
	 * @return an array of proposal strings
	 */
	private String[] getVariableNameProposals(String basename) {
		ASTNode surroundingBlock= fCurrentNode;
		while ((surroundingBlock= surroundingBlock.getParent()) != null) {
			if (surroundingBlock instanceof Block) {
				break;
			}
		}
		Collection<String> localUsedNames= new ScopeAnalyzer((CompilationUnit) fCurrentExpression.getRoot()).getUsedVariableNames(surroundingBlock.getStartPosition(), surroundingBlock.getLength());
		String[] names= StubUtility.getLocalNameSuggestions(getCompilationUnit().getJavaProject(), basename, 0, localUsedNames.toArray(new String[localUsedNames.size()]));
		return names;
	}

	/**
	 * Extracts the type parameter of the variable contained in fSubExpression or the elements type
	 * to iterate over an array using <code>foreach</code>.
	 * 
	 * @param ast the current {@link AST} instance
	 * @return the string representation of the type's unqualified name
	 */
	private String extractElementTypeName(AST ast) {
		ITypeBinding binding= fSubExpression.resolveTypeBinding();
		if (binding.isArray()) {
			return Bindings.normalizeForDeclarationUse(binding.getElementType(), ast).getName();
		}
		ITypeBinding[] typeArguments= Bindings.findTypeInHierarchy(binding, "java.lang.Iterable").getTypeArguments(); //$NON-NLS-1$
		if (typeArguments != null && typeArguments.length > 0) {
			return Bindings.normalizeForDeclarationUse(typeArguments[0], ast).getName();
		}

		return null;
	}

}
