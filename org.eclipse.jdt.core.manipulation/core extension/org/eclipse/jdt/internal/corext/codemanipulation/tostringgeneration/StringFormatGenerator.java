/*******************************************************************************
 * Copyright (c) 2008, 2024 Mateusz Matela and others.
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
 *     Mateusz Matela <mateusz.matela@gmail.com> - [toString] toString wizard generates wrong code - https://bugs.eclipse.org/bugs/show_bug.cgi?id=270462
 *     Red Hat Inc. - moved to jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.codemanipulation.tostringgeneration;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.StringLiteral;


/**
 * <p>
 * Implementation of <code>AbstractToStringGenerator</code> that creates <code>toString()</code>
 * method using <code>String.format()</code>. This style ignores <i>skip null values</i> option.
 * <p>
 * Generated methods look like this:
 *
 * <pre>
 * public String toString() {
 * 	return String.format(&quot;FooClass( field1=%s, field2=%s )&quot;, field1, field2);
 * }
 * </pre>
 *
 * @since 3.5
 */
public class StringFormatGenerator extends AbstractToStringGenerator {
	private List<Expression> arguments;

	private StringBuffer buffer;

	@Override
	protected void initialize() {
		super.initialize();
		arguments= new ArrayList<>();
		buffer= new StringBuffer();
	}

	@Override
	protected void complete() throws CoreException {
		super.complete();
		ReturnStatement rStatement= fAst.newReturnStatement();
		String formatClass= "java.lang.String"; //$NON-NLS-1$
		MethodInvocation formatInvocation= createMethodInvocation(addImport(formatClass), "format", null); //$NON-NLS-1$
		StringLiteral literal= fAst.newStringLiteral();
		literal.setLiteralValue(buffer.toString());
		formatInvocation.arguments().add(literal);
		formatInvocation.arguments().addAll(arguments);
		rStatement.setExpression(formatInvocation);
		toStringMethod.getBody().statements().add(rStatement);
	}

	@Override
	protected Object processElement(String templateElement, Object member) {
		if (ToStringTemplateParser.MEMBER_VALUE_VARIABLE.equals(templateElement)) {
			return createMemberAccessExpression(member, false, false);
		}
		return super.processElement(templateElement, member);
	}

	@Override
	protected void addElement(Object element) {
		if (element instanceof String) {
			buffer.append((String)element);
		}
		if (element instanceof Expression) {
			arguments.add((Expression) element);
			buffer.append("%s"); //$NON-NLS-1$
		}
	}

}
