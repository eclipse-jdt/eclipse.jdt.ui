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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.TType;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.CastVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ConstraintVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.IDeclaredConstraintVariable;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ITypeConstraint2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ITypeSet;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.OrOrSubTypeConstraint2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.PlainTypeVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.TypeEquivalenceSet;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.TypeSet;

/**
 * Type constraint solver to solve supertype constraint models.
 * 
 * @since 3.1
 */
public final class SuperTypeConstraintsSolver {

	/** The type estimate data (type: <code>TType</code> */
	public final static String DATA_TYPE_ESTIMATE= "te"; //$NON-NLS-1$

	/** The type constraint model to solve */
	private final SuperTypeConstraintsModel fModel;

	/** The obsolete casts (element type: <code>&ltICompilationUnit, Collection&ltCastVariable2&gt&gt</code>) */
	private Map fObsoleteCasts= null;

	/** The list of constraint variables to be processed */
	private LinkedList fProcessable= null;

	/** The type occurrences (element type: <code>&ltICompilationUnit, Collection&ltIDeclaredConstraintVariable&gt</code>) */
	private Map fTypeOccurrences= null;

	/**
	 * Creates a new super type constraints solver.
	 * 
	 * @param model the model to solve
	 */
	public SuperTypeConstraintsSolver(final SuperTypeConstraintsModel model) {
		Assert.isNotNull(model);

		fModel= model;
	}

	/**
	 * Computes the initial type estimate for the specified constraint variable.
	 * 
	 * @param variable the constraint variable
	 * @return the initial type estimate
	 */
	private ITypeSet computeInitialTypeEstimate(final ConstraintVariable2 variable) {
		Assert.isNotNull(variable);
		final TType type= variable.getType();
		if (variable instanceof PlainTypeVariable2 || !type.equals(fModel.getSubType()))
			return TypeSet.create(type);
		return TypeSet.create(new TType[] { type, fModel.getSuperType()});
	}

	/**
	 * Computes the obsolete casts for the specified cast variables.
	 * 
	 * @param variables the cast variables (element type: <code>CastVariable2</code>)
	 */
	private void computeObsoleteCasts(final Collection variables) {
		Assert.isNotNull(variables);
		fObsoleteCasts= new HashMap();
		CastVariable2 variable= null;
		for (final Iterator iterator= variables.iterator(); iterator.hasNext();) {
			variable= (CastVariable2) iterator.next();
			final TType type= (TType) variable.getExpressionVariable().getData(DATA_TYPE_ESTIMATE);
			if (type != null && type.canAssignTo(variable.getType())) {
				final ICompilationUnit unit= variable.getCompilationUnit();
				Collection casts= (Collection) fObsoleteCasts.get(unit);
				if (casts != null)
					casts.add(variable);
				else {
					casts= new ArrayList(1);
					casts.add(variable);
					fObsoleteCasts.put(unit, casts);
				}
			}
		}
	}

	/**
	 * Computes the initial type estimates for the specified variables.
	 * 
	 * @param variables the constraint variables (element type: <code>ConstraintVariable2</code>)
	 */
	private void computeTypeEstimates(final Collection variables) {
		Assert.isNotNull(variables);
		ConstraintVariable2 variable= null;
		for (final Iterator iterator= variables.iterator(); iterator.hasNext();) {
			variable= (ConstraintVariable2) iterator.next();
			TypeEquivalenceSet set= variable.getTypeEquivalenceSet();
			if (set == null) {
				set= new TypeEquivalenceSet(variable);
				set.setTypeEstimate(computeInitialTypeEstimate(variable));
				variable.setTypeEquivalenceSet(set);
			} else {
				ITypeSet estimate= variable.getTypeEstimate();
				if (estimate == null) {
					final ConstraintVariable2[] contributing= set.getContributingVariables();
					estimate= TypeSet.getTypeUniverse();
					for (int index= 0; index < contributing.length; index++)
						estimate= estimate.restrictedTo(computeInitialTypeEstimate(contributing[index]));
					set.setTypeEstimate(estimate);
				}
			}
		}
	}

	/**
	 * Computes a single type for each of the specified constraint variables.
	 * 
	 * @param variables the constraint variables (element type: <code>ConstraintVariable2</code>)
	 */
	private void computeTypeOccurrences(final Collection variables) {
		Assert.isNotNull(variables);
		fTypeOccurrences= new HashMap();
		ConstraintVariable2 variable= null;
		for (final Iterator iterator= variables.iterator(); iterator.hasNext();) {
			variable= (ConstraintVariable2) iterator.next();
			if (variable instanceof IDeclaredConstraintVariable) {
				final ITypeSet estimate= variable.getTypeEstimate();
				if (estimate != null) {
					variable.setData(DATA_TYPE_ESTIMATE, estimate.chooseSingleType());
					if (variable instanceof IDeclaredConstraintVariable) {
						final IDeclaredConstraintVariable declaration= (IDeclaredConstraintVariable) variable;
						final ICompilationUnit unit= declaration.getCompilationUnit();
						if (unit != null) {
							Collection matches= (Collection) fTypeOccurrences.get(unit);
							if (matches != null)
								matches.add(variable);
							else {
								matches= new ArrayList(1);
								matches.add(variable);
								fTypeOccurrences.put(unit, matches);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Returns the computed obsolete casts.
	 * 
	 * @return the obsolete casts (element type: <code>&ltICompilationUnit, Collection&ltCastVariable2&gt&gt</code>)
	 */
	public final Map getObsoleteCasts() {
		return fObsoleteCasts;
	}

	/**
	 * Returns the computed type occurrences.
	 * 
	 * @return the type occurrences (element type: <code>&ltICompilationUnit, Collection&ltIDeclaredConstraintVariable&gt</code>)
	 */
	public final Map getTypeOccurrences() {
		return fTypeOccurrences;
	}

	/**
	 * Processes the given constraints on the constraint variable and propagates it.
	 * 
	 * @param constraints the type constraints to process (element type: <code>ITypeConstraint2</code>)
	 */
	private void processConstraints(final Collection constraints) {
		Assert.isNotNull(constraints);
		ITypeConstraint2 constraint= null;
		for (final Iterator iterator= constraints.iterator(); iterator.hasNext();) {
			constraint= (ITypeConstraint2) iterator.next();
			final ConstraintVariable2 leftVariable= constraint.getLeft();
			final ConstraintVariable2 rightVariable= constraint.getRight();
			if (constraint instanceof OrOrSubTypeConstraint2) {

				// TODO implement

			} else {
				final TypeEquivalenceSet set= rightVariable.getTypeEquivalenceSet();
				final ITypeSet rightEstimate= rightVariable.getTypeEstimate();
				final ITypeSet newEstimate= rightEstimate.restrictedTo(leftVariable.getTypeEstimate());
				if (rightEstimate != newEstimate) {
					set.setTypeEstimate(newEstimate);
					fProcessable.addAll(Arrays.asList(set.getContributingVariables()));
				}
			}
		}
	}

	/**
	 * Solves the constraints of the associated model.
	 */
	public final void solveConstraints() {
		fProcessable= new LinkedList();
		final Collection variables= fModel.getConstraintVariables();
		computeTypeEstimates(variables);
		fProcessable.addAll(variables);
		ConstraintVariable2 variable= null;
		while (!fProcessable.isEmpty()) {
			variable= (ConstraintVariable2) fProcessable.removeFirst();
			processConstraints(SuperTypeConstraintsModel.getVariableUsage(variable));
		}
		computeTypeOccurrences(variables);
		computeObsoleteCasts(fModel.getCastVariables());
	}
}