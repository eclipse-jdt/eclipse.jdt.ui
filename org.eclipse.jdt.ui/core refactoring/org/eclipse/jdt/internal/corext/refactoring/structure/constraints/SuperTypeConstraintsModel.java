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

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Type;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.CompilationUnitRange;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.TypeEnvironment;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.CastVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ConstraintOperator2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ConstraintVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.EquivalenceRepresentative;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.IndependentTypeVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ParameterTypeVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.PlainTypeVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ReturnTypeVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.TypeConstraint2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.TypeVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.VariableVariable2;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;

/**
 * Type constraints model to hold all type constraints to replace type occurrences by a given supertype.
 * 
 * @since 3.1
 */
public final class SuperTypeConstraintsModel {

	/** Customized implementation of a hash set */
	private static class HashedSet extends AbstractSet implements Set {

		/** The backing hash map */
		private final Map fImplementation= new HashMap();

		/*
		 * @see java.util.AbstractCollection#add(java.lang.Object)
		 */
		public final boolean add(final Object object) {
			return fImplementation.put(object, object) == null;
		}

		/**
		 * Attempts to add the specified object to this set.
		 * 
		 * @param object the object to add
		 * @return An already existing object considered equal to the specified one, or the newly added object
		 */
		public final Object addExisting(final Object object) {
			final Object result= fImplementation.put(object, object);
			if (result != null)
				return result;
			return object;
		}

		/*
		 * @see java.util.AbstractCollection#clear()
		 */
		public final void clear() {
			fImplementation.clear();
		}

		/*
		 * @see java.util.AbstractCollection#contains(java.lang.Object)
		 */
		public final boolean contains(final Object object) {
			return fImplementation.containsKey(object);
		}

		/*
		 * @see java.util.AbstractCollection#isEmpty()
		 */
		public final boolean isEmpty() {
			return fImplementation.isEmpty();
		}

		/*
		 * @see java.util.AbstractCollection#iterator()
		 */
		public final Iterator iterator() {
			return fImplementation.keySet().iterator();
		}

		/*
		 * @see java.util.AbstractCollection#remove(java.lang.Object)
		 */
		public final boolean remove(final Object object) {
			return fImplementation.remove(object) == object;
		}

		/*
		 * @see java.util.AbstractCollection#size()
		 */
		public final int size() {
			return fImplementation.size();
		}
	}

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
	public static boolean isConstrainedType(final ITypeBinding binding) {
		Assert.isNotNull(binding);
		return !binding.isPrimitive();
	}

	/**
	 * Sets the usage of the specified constraint variable.
	 * 
	 * @param variable the constraint variable
	 * @param constraint the type constraint
	 */
	public static void setVariableUsage(final ConstraintVariable2 variable, final TypeConstraint2 constraint) {
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
	private final HashedSet fConstraintVariables= new HashedSet();

	/** The set of type constraints (element type: <code>ITypeConstraint2</code>) */
	private final Set fTypeConstraints= new HashSet();

	/**
	 * Creates a cast variable.
	 * 
	 * @param expression the cast expression
	 * @param variable the associated constraint variable
	 * @return the created cast variable
	 */
	public final ConstraintVariable2 createCastVariable(final CastExpression expression, final ConstraintVariable2 variable) {
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
	 * Creates an equality constraint.
	 * 
	 * @param left the left typeconstraint variable
	 * @param right the right typeconstraint variable
	 */
	public final void createEqualsConstraint(final ConstraintVariable2 left, final ConstraintVariable2 right) {
		if (left != null && right != null) {
			final EquivalenceRepresentative first= left.getRepresentative();
			final EquivalenceRepresentative second= right.getRepresentative();
			if (first == null) {
				if (second == null) {
					final EquivalenceRepresentative representative= new EquivalenceRepresentative(left, right);
					left.setRepresentative(representative);
					right.setRepresentative(representative);
				} else {
					second.add(left);
					left.setRepresentative(second);
				}
			} else {
				if (second == null) {
					first.add(right);
					right.setRepresentative(first);
				} else if (first == second)
					return;
				else {
					final ConstraintVariable2[] elements= second.getElements();
					first.addAll(elements);
					for (int index= 0; index < elements.length; index++)
						elements[index].setRepresentative(first);
				}
			}
		}
	}

	/**
	 * Creates an independant type variable.
	 * <p>
	 * An independant type variable stands for an arbitrary type.
	 * </p>
	 * 
	 * @param type the type binding
	 * @return the created independant type variable
	 */
	public final ConstraintVariable2 createIndependantTypeVariable(final ITypeBinding type) {
		Assert.isNotNull(type);
		if (isConstrainedType(type))
			return (ConstraintVariable2) fConstraintVariables.addExisting(new IndependentTypeVariable2(fEnvironment.create(type)));
		return null;
	}

	/**
	 * Creates a new method parameter variable.
	 * 
	 * @param method the method binding
	 * @param index the index of the parameter
	 * @return the created method parameter variable
	 */
	public final ConstraintVariable2 createMethodParameterVariable(final IMethodBinding method, final int index) {
		Assert.isNotNull(method);
		final ITypeBinding binding= method.getParameterTypes()[index];
		if (isConstrainedType(binding))
			return (ConstraintVariable2) fConstraintVariables.addExisting(new ParameterTypeVariable2(fEnvironment.create(binding), index, method));
		return null;
	}

	/**
	 * Creates a plain type variable.
	 * <p>
	 * A plain type variable stands for an immutable type.
	 * </p>
	 * 
	 * @param type the type binding
	 * @return the created plain type variable
	 */
	public final ConstraintVariable2 createPlainTypeVariable(final ITypeBinding type) {
		Assert.isNotNull(type);
		if (isConstrainedType(type))
			return (ConstraintVariable2) fConstraintVariables.addExisting(new PlainTypeVariable2(fEnvironment.create(type)));
		return null;
	}

	/**
	 * Creates a new return type variable.
	 * 
	 * @param method the method binding
	 * @return the created return type variable
	 */
	public final ConstraintVariable2 createReturnTypeVariable(final IMethodBinding method) {
		Assert.isNotNull(method);
		if (!method.isConstructor() && !method.isDefaultConstructor()) {
			final ITypeBinding binding= method.getReturnType();
			if (binding != null && isConstrainedType(binding))
				return (ConstraintVariable2) fConstraintVariables.addExisting(new ReturnTypeVariable2(fEnvironment.create(binding), method));
		}
		return null;
	}

	/**
	 * Creates a subtype constraint.
	 * 
	 * @param descendant the descendant type constraint variable
	 * @param ancestor the ancestor type constraint variable
	 */
	public final void createSubtypeConstraint(final ConstraintVariable2 descendant, final ConstraintVariable2 ancestor) {
		Assert.isNotNull(descendant);
		Assert.isNotNull(ancestor);
		final TypeConstraint2 constraint= new TypeConstraint2(descendant, ancestor, ConstraintOperator2.createSubTypeOperator());
		if (!fTypeConstraints.contains(constraint)) {
			fTypeConstraints.add(constraint);
			setVariableUsage(descendant, constraint);
			setVariableUsage(ancestor, constraint);
		}
	}

	/**
	 * Creates a type variable.
	 * 
	 * @param type the type
	 * @return the created type variable
	 */
	public final ConstraintVariable2 createTypeVariable(final Type type) {
		Assert.isNotNull(type);
		final ITypeBinding binding= type.resolveBinding();
		if (isConstrainedType(binding))
			return (ConstraintVariable2) fConstraintVariables.addExisting(new TypeVariable2(fEnvironment.create(binding), new CompilationUnitRange(RefactoringASTParser.getCompilationUnit(type), type)));
		return null;
	}

	/**
	 * Creates a variable variable.
	 * 
	 * @param variable the variable
	 * @return the created variable variable
	 */
	public final ConstraintVariable2 createVariableVariable(final IVariableBinding variable) {
		Assert.isNotNull(variable);
		final ITypeBinding binding= variable.getType();
		if (isConstrainedType(binding))
			return (ConstraintVariable2) fConstraintVariables.addExisting(new VariableVariable2(fEnvironment.create(binding), variable));
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