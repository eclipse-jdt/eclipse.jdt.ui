/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.structure.constraints;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Type;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.CompilationUnitRange;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.TypeEnvironment;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.CastVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ConstraintOperator2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ConstraintVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ITypeConstraint2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ParameterTypeVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.SimpleTypeConstraint2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.TypeConstraintVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.TypeVariable2;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;

/**
 * Type constraints model to hold all type constraints to replace type occurrences by a given supertype.
 * 
 * @since 3.1
 */
public final class SuperTypeConstraintsModel {

	/** The usage data */
	private static final String DATA_USAGE= "us"; //$NON-NLS-1$

	/** The type environment to use */
	private static TypeEnvironment fEnvironment= new TypeEnvironment();

	/**
	 * Returns the usage of the specified constraint variable.
	 * 
	 * @param variable the constraint variable
	 * @return the usage of the constraint variable (element type: <code>ITypeConstraint2</code>)
	 */
	public static Collection getVariableUsage(final ConstraintVariable2 variable) {
		Assert.isNotNull(variable);
		final Object data= variable.getData(DATA_USAGE);
		if (data == null)
			return Collections.EMPTY_LIST;
		else if (data instanceof Collection)
			return Collections.unmodifiableCollection((Collection) data);
		else
			return Collections.singletonList(data);
	}

	/**
	 * Is the type represented by the specified binding a constrained type?
	 * 
	 * @param binding the binding to check
	 * @return <code>true</code> if it is constrained, <code>false</code> otherwise
	 */
	private static boolean isConstrainedType(final ITypeBinding binding) {
		Assert.isNotNull(binding);
		return !binding.isPrimitive();
	}

	/**
	 * Sets the usage of the specified constraint variable.
	 * 
	 * @param variable the constraint variable
	 * @param constraint the type constraint
	 */
	public static void setVariableUsage(final ConstraintVariable2 variable, final ITypeConstraint2 constraint) {
		Assert.isNotNull(variable);
		Assert.isNotNull(constraint);
		final Object data= variable.getData(DATA_USAGE);
		if (data == null)
			variable.setData(DATA_USAGE, constraint);
		else if (data instanceof Collection)
			((Collection) data).add(constraint);
		else {
			final Collection usage= new ArrayList(2);
			usage.add(data);
			usage.add(constraint);
			variable.setData(DATA_USAGE, usage);
		}
	}

	/** The cast variables (element type: <code>CastVariable2</code>) */
	private final Collection fCastVariables= new ArrayList();

	/** The set of constraint variables (element type: <code>ConstraintVariable2</code>) */
	private final Set fConstraintVariables= new HashSet();

	/** The set of type constraints (element type: <code>ITypeConstraint2</code>) */
	private final Set fTypeConstraints= new HashSet();

	/**
	 * Creates a cast variable.
	 * 
	 * @param expression the cast expression
	 * @param variable the associated constraint variable
	 * @return the created cast variable
	 */
	public final CastVariable2 createCastVariable(final CastExpression expression, final TypeConstraintVariable2 variable) {
		Assert.isNotNull(expression);
		Assert.isNotNull(variable);
		final ITypeBinding binding= expression.resolveTypeBinding();
		if (isConstrainedType(binding)) {
			final CastVariable2 result= new CastVariable2(fEnvironment.create(binding), new CompilationUnitRange(RefactoringASTParser.getCompilationUnit(expression), expression), variable);
			fCastVariables.add(result);
			return result;
		}
		return null;
	}

	/**
	 * Creates a new method parameter variable.
	 * 
	 * @param method the method
	 * @param index the index of the parameter
	 * @return the created method parameter variable
	 */
	public final ParameterTypeVariable2 createMethodParameterVariable(final IMethodBinding method, final int index) {
		Assert.isNotNull(method);
		final ITypeBinding binding= method.getParameterTypes()[index];
		if (isConstrainedType(binding)) {
			final ParameterTypeVariable2 variable= new ParameterTypeVariable2(fEnvironment.create(binding), index, method);
			fConstraintVariables.add(variable);
			return variable;
		}
		return null;
	}

	/**
	 * Creates a new simple type constraint.
	 * 
	 * @param left the left constraint variable
	 * @param right the right constraint variable
	 * @param operator the constraint operator
	 */
	protected final void createSimpleTypeConstraint(final ConstraintVariable2 left, final ConstraintVariable2 right, final ConstraintOperator2 operator) {
		Assert.isNotNull(left);
		Assert.isNotNull(right);
		Assert.isNotNull(operator);
		final ITypeConstraint2 constraint= new SimpleTypeConstraint2(left, right, operator);
		if (!fTypeConstraints.contains(constraint)) {
			fTypeConstraints.add(constraint);
			setVariableUsage(left, constraint);
			setVariableUsage(right, constraint);
		}
	}

	/**
	 * Creates a subtype constraint.
	 * 
	 * @param left the left constraint variable
	 * @param right the right constraint variable
	 */
	public final void createSubtypeConstraint(final ConstraintVariable2 left, final ConstraintVariable2 right) {
		createSimpleTypeConstraint(left, right, ConstraintOperator2.createSubTypeOperator());
	}

	/**
	 * Creates a type variable.
	 * 
	 * @param type the type
	 * @return the created type variable
	 */
	public final TypeVariable2 createTypeVariable(final Type type) {
		Assert.isNotNull(type);
		final ITypeBinding binding= type.resolveBinding();
		if (isConstrainedType(binding)) {
			final TypeVariable2 variable= new TypeVariable2(fEnvironment.create(binding), new CompilationUnitRange(RefactoringASTParser.getCompilationUnit(type), type));
			fConstraintVariables.add(variable);
			return variable;
		}
		return null;
	}

	/**
	 * Returns the cast variables of this model.
	 * 
	 * @return the cast variables (element type: <code>CastVariable2</code>)
	 */
	public final Collection getCastVariables() {
		return Collections.unmodifiableCollection(fCastVariables);
	}

	/**
	 * Returns the constraint variables of this model.
	 * 
	 * @return the constraint variables (element type: <code>ConstraintVariable2</code>)
	 */
	public final Collection getConstraintVariables() {
		return Collections.unmodifiableCollection(fConstraintVariables);
	}
}