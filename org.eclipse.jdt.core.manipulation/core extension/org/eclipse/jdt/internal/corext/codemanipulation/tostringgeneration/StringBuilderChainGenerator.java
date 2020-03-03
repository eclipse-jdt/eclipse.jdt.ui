/*******************************************************************************
 * Copyright (c) 2008, 2019 Mateusz Matela and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Mateusz Matela <mateusz.matela@gmail.com> - [code manipulation] [dcr] toString() builder wizard - https://bugs.eclipse.org/bugs/show_bug.cgi?id=26070
 *     Red Hat Inc. - moved to jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.codemanipulation.tostringgeneration;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;

/**
 * <p>
 * Implementation of <code>AbstractToStringGenerator</code> that creates <code>toString()</code>
 * method using <code>StringBuilder</code> (or <code>StringBuffer</code> for old versions of JDK)
 * with chained calls to the <code>append</code> method.
 * </p>
 * <p>
 * Generated methods look like this:
 *
 * <pre>
 * public String toString() {
 * 	StringBuilder builder= new StringBuilder();
 * 	builder.append(&quot;FooClass( field1=&quot;).append(field1).append(&quot;, field2=&quot;).append(field2).append(&quot; )&quot;);
 * 	return builder.toString();
 * }
 * </pre>
 *
 * </p>
 *
 * @since 3.5
 */
public class StringBuilderChainGenerator extends StringBuilderGenerator {
	protected Block temporaryBlock= null;

	protected Expression temporaryExpression= null;

	protected void appendExpression(Expression expression) {
		MethodInvocation appendInvocation= fAst.newMethodInvocation();
		if (temporaryExpression != null)
			appendInvocation.setExpression(temporaryExpression);
		else
			appendInvocation.setExpression(fAst.newSimpleName(fBuilderVariableName));
		appendInvocation.setName(fAst.newSimpleName(APPEND_METHOD_NAME));
		appendInvocation.arguments().add(expression);
		temporaryExpression= appendInvocation;
	}

	protected void flushBuffer() {
		if (fBuffer.length() > 0) {
			if (temporaryBlock == null)
				temporaryBlock= toStringMethod.getBody();
			StringLiteral literal= fAst.newStringLiteral();
			literal.setLiteralValue(fBuffer.toString());
			appendExpression(literal);
			fBuffer.setLength(0);
		}
	}

	protected void flushTemporaryExpression() {
		flushBuffer();
		if (temporaryBlock != null && temporaryExpression != null) {
			temporaryBlock.statements().add(fAst.newExpressionStatement(temporaryExpression));
			temporaryExpression= null;
		}
	}

	@Override
	protected void addElement(Object element, Block block) {
		if (block != temporaryBlock) {
			flushTemporaryExpression();
			temporaryBlock= block;
		}
		if (element instanceof String)
			fBuffer.append((String)element);
		if (element instanceof Expression) {
			flushBuffer();
			appendExpression((Expression)element);
		}
	}

	@Override
	protected void addMemberCheckNull(Object member, boolean addSeparator) {
		IfStatement ifStatement= fAst.newIfStatement();
		ifStatement.setExpression(createInfixExpression(createMemberAccessExpression(member, true, true), Operator.NOT_EQUALS, fAst.newNullLiteral()));
		Block thenBlock= fAst.newBlock();
		flushTemporaryExpression();
		for (String s : getContext().getTemplateParser().getBody()) {
			addElement(processElement(s, member), thenBlock);
		}
		if (addSeparator)
			addElement(getContext().getTemplateParser().getSeparator(), thenBlock);
		flushTemporaryExpression();

		if (thenBlock.statements().size() == 1 && !getContext().isForceBlocks()) {
			ifStatement.setThenStatement((Statement)ASTNode.copySubtree(fAst, (ASTNode)thenBlock.statements().get(0)));
		} else {
			ifStatement.setThenStatement(thenBlock);
		}
		toStringMethod.getBody().statements().add(ifStatement);
	}

	@Override
	protected void complete() throws CoreException {
		flushTemporaryExpression();
		super.complete();
	}

}
