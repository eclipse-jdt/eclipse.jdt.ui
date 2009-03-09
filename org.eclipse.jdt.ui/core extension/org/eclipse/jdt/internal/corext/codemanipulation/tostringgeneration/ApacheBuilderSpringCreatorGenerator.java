/*******************************************************************************
 * Copyright (c) 2008 Mateusz Matela and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mateusz Matela <mateusz.matela@gmail.com> - [code manipulation] [dcr] toString() builder wizard - https://bugs.eclipse.org/bugs/show_bug.cgi?id=26070
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.codemanipulation.tostringgeneration;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.NamingConventions;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationMessages;


/**
 * <p>
 * Implementation of <code>AbstractToStringGenerator</code> that creates <code>toString()</code>
 * method using external library. The library must deliver a stringBuilder with
 * <code>append(name, value)</code> method, for example Apache ToStringBuilder or Spring Framework
 * ToStringCreator.
 * </p>
 * <p>
 * Generated methods look like this:
 * 
 * <pre>
 * public String toString() {
 * 	StringBuilder builder= new StringBuilder();
 * 	builder.append(&quot;FooClass( field1=&quot;);
 * 	builder.append(field1);
 * 	builder.append(&quot;, field2=&quot;);
 * 	builder.append(field2);
 * 	builder.append(&quot; )&quot;);
 * 	return builder.toString();
 * }
 * </pre>
 * 
 * </p>
 * 
 * @since 3.5
 */
public class ApacheBuilderSpringCreatorGenerator extends AbstractToStringGenerator {
	private String builderVariableName;

	private String importText;

	private boolean chained;

	public ApacheBuilderSpringCreatorGenerator(String importText, String builderName, boolean chained) {
		builderVariableName= builderName;
		this.importText= importText;
		this.chained= chained;
	}
	
	protected void initialize() {
		super.initialize();
		this.builderVariableName= createNameSuggestion(builderVariableName, NamingConventions.VK_PARAMETER);
	}

	public RefactoringStatus checkConditions() {
		RefactoringStatus status= super.checkConditions();
		if (fContext.isCustomArray() || fContext.isLimitItems())
			status.addWarning(CodeGenerationMessages.GenerateToStringOperation_warning_no_arrays_collections_with_this_style);
		return status;
	}


	protected void addElement(Object element) {
	}

	public MethodDeclaration generateToStringMethod() throws CoreException {
		initialize();

		//ToStringBuilder builder= new ToStringBuilder(this);
		VariableDeclarationFragment fragment= fAst.newVariableDeclarationFragment();
		fragment.setName(fAst.newSimpleName(builderVariableName));
		ClassInstanceCreation classInstance= fAst.newClassInstanceCreation();
		String typeName= addImport(importText);
		classInstance.setType(fAst.newSimpleType(fAst.newSimpleName(typeName)));
		classInstance.arguments().add(fAst.newThisExpression());
		fragment.setInitializer(classInstance);
		VariableDeclarationStatement vStatement= fAst.newVariableDeclarationStatement(fragment);
		vStatement.setType(fAst.newSimpleType(fAst.newName(typeName)));
		toStringMethod.getBody().statements().add(vStatement);

		Expression expression= null;

		for (int i= 0; i < getContext().getSelectedMembers().length; i++) {
			//builder.append("member", member);
			StringLiteral literal= fAst.newStringLiteral();
			literal.setLiteralValue(getMemberName(getContext().getSelectedMembers()[i], ToStringTemplateParser.MEMBER_NAME_PARENTHESIS_VARIABLE));
			MethodInvocation appendInvocation= fAst.newMethodInvocation();
			appendInvocation.setName(fAst.newSimpleName("append")); //$NON-NLS-1$
			appendInvocation.arguments().add(literal);
			appendInvocation.arguments().add(createMemberAccessExpression(getContext().getSelectedMembers()[i], false, getContext().isSkipNulls()));

			if (getContext().isSkipNulls() && !getMemberType(getContext().getSelectedMembers()[i]).isPrimitive()) {
				if (expression != null) {
					toStringMethod.getBody().statements().add(fAst.newExpressionStatement(expression));
					expression= null;
				}
				appendInvocation.setExpression(fAst.newSimpleName(builderVariableName));
				IfStatement ifStatement= fAst.newIfStatement();
				ifStatement.setExpression(createInfixExpression(createMemberAccessExpression(getContext().getSelectedMembers()[i], true, true), Operator.NOT_EQUALS, fAst.newNullLiteral()));
				ifStatement.setThenStatement(createOneStatementBlock(appendInvocation));
				toStringMethod.getBody().statements().add(ifStatement);
			} else {
				if (expression != null) {
					appendInvocation.setExpression(expression);
				} else {
					appendInvocation.setExpression(fAst.newSimpleName(builderVariableName));
				}
				if (chained) {
					expression= appendInvocation;
				} else {
					toStringMethod.getBody().statements().add(fAst.newExpressionStatement(appendInvocation));
				}
			}
		}

		if (expression != null) {
			toStringMethod.getBody().statements().add(fAst.newExpressionStatement(expression));
		}
		// return builder.toString();
		ReturnStatement rStatement= fAst.newReturnStatement();
		rStatement.setExpression(createMethodInvocation(builderVariableName, "toString", null)); //$NON-NLS-1$
		toStringMethod.getBody().statements().add(rStatement);

		complete();

		return toStringMethod;
	}

}
