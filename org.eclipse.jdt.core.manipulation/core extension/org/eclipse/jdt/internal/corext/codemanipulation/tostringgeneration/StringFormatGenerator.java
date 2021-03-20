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
 *     Mateusz Matela <mateusz.matela@gmail.com> - [toString] toString wizard generates wrong code - https://bugs.eclipse.org/bugs/show_bug.cgi?id=270462
 *     Red Hat Inc. - moved to jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.codemanipulation.tostringgeneration;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
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
 * </p>
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
		String formatClass;
		if (getContext().is50orHigher())
			formatClass= "java.lang.String"; //$NON-NLS-1$
		else
			formatClass= "java.text.MessageFormat"; //$NON-NLS-1$
		MethodInvocation formatInvocation= createMethodInvocation(addImport(formatClass), "format", null); //$NON-NLS-1$
		StringLiteral literal= fAst.newStringLiteral();
		literal.setLiteralValue(buffer.toString());
		formatInvocation.arguments().add(literal);
		if (getContext().is50orHigher()) {
			formatInvocation.arguments().addAll(arguments);
		} else {
			ArrayCreation arrayCreation= fAst.newArrayCreation();
			arrayCreation.setType(fAst.newArrayType(fAst.newSimpleType(addImport("java.lang.Object")))); //$NON-NLS-1$
			ArrayInitializer initializer= fAst.newArrayInitializer();
			arrayCreation.setInitializer(initializer);
			initializer.expressions().addAll(arguments);
			formatInvocation.arguments().add(arrayCreation);
		}
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
			if (getContext().is50orHigher()) {
				buffer.append("%s"); //$NON-NLS-1$
			} else {
				buffer.append("{" + (arguments.size() - 1) + "}"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	@Override
	protected Expression createMemberAccessExpression(Object member, boolean ignoreArraysCollections, boolean ignoreNulls) {
		ITypeBinding type= getMemberType(member);
		if (!getContext().is50orHigher() && type.isPrimitive()) {
			String nonPrimitiveType= null;
			String typeName= type.getName();
			if ("byte".equals(typeName))nonPrimitiveType= "java.lang.Byte"; //$NON-NLS-1$ //$NON-NLS-2$
			if ("short".equals(typeName))nonPrimitiveType= "java.lang.Short"; //$NON-NLS-1$ //$NON-NLS-2$
			if ("char".equals(typeName))nonPrimitiveType= "java.lang.Character"; //$NON-NLS-1$ //$NON-NLS-2$
			if ("int".equals(typeName))nonPrimitiveType= "java.lang.Integer"; //$NON-NLS-1$ //$NON-NLS-2$
			if ("long".equals(typeName))nonPrimitiveType= "java.lang.Long"; //$NON-NLS-1$ //$NON-NLS-2$
			if ("float".equals(typeName))nonPrimitiveType= "java.lang.Float"; //$NON-NLS-1$ //$NON-NLS-2$
			if ("double".equals(typeName))nonPrimitiveType= "java.lang.Double"; //$NON-NLS-1$ //$NON-NLS-2$
			if ("boolean".equals(typeName))nonPrimitiveType= "java.lang.Boolean"; //$NON-NLS-1$ //$NON-NLS-2$
			ClassInstanceCreation classInstance= fAst.newClassInstanceCreation();
			classInstance.setType(fAst.newSimpleType(addImport(nonPrimitiveType)));
			classInstance.arguments().add(super.createMemberAccessExpression(member, true, true));
			return classInstance;
		}
		return super.createMemberAccessExpression(member, ignoreArraysCollections, ignoreNulls);
	}

}
