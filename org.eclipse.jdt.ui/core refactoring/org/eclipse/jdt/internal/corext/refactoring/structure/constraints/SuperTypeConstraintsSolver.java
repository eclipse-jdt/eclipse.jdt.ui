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
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.TType;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.CastVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ConstraintVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.EquivalenceRepresentative;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ITypeConstraint2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ITypeSet;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.SimpleTypeConstraint2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.TypeConstraintVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.TypeSet;

/**
 * Type constraints solver to solve supertype constraint models.
 * 
 * @since 3.1
 */
public final class SuperTypeConstraintsSolver {

	/** The type estimate data */
	private final static String DATA_TYPE_ESTIMATE= "te"; //$NON-NLS-1$

	/** The type constraint model to solve */
	private final SuperTypeConstraintsModel fModel;

	/** the obsolete casts (element type: <code>&ltICompilationUnit, Collection&ltCastVariable2&gt&gt</code>) */
	private Map fObsoleteCasts= null;

	/** The list of constraint variables to be processed */
	private final LinkedList fProcessable= new LinkedList();

	/** The type occurrences (element type: <code>&ltICompilationUnit, Collection&ltConstraintVariable2&gt&gt</code>) */
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
				List casts= (List) fObsoleteCasts.get(unit);
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
	 * Computes the type estimates for the specified variables.
	 * 
	 * @param variables the constraint variables (element type: <code>ConstraintVariable2</code>)
	 */
	private void computeTypeEstimates(final Collection variables) {
		Assert.isNotNull(variables);
		ConstraintVariable2 variable= null;
		for (final Iterator iterator= variables.iterator(); iterator.hasNext();) {
			variable= (ConstraintVariable2) iterator.next();
			if (variable instanceof TypeConstraintVariable2) {
				final TypeConstraintVariable2 constraint= (TypeConstraintVariable2) variable;
				EquivalenceRepresentative representative= constraint.getRepresentative();
				if (representative == null) {
					representative= new EquivalenceRepresentative(constraint);
					representative.setTypeEstimate(TypeSet.create(constraint.getType()));
					constraint.setRepresentative(representative);
				} else {
					ITypeSet estimate= representative.getTypeEstimate();
					if (estimate == null) {
						final TypeConstraintVariable2[] constraints= representative.getElements();
						estimate= TypeSet.getUniverse();
						for (int index= 0; index < constraints.length; index++)
							estimate= estimate.restrictedTo(TypeSet.create(constraints[index].getType()));
						representative.setTypeEstimate(estimate);
					}
				}
			}
		}
	}

	/**
	 * Computes a single type for each of the specified constraint variables.
	 * 
	 * @param variables the constraint variables (element type: <code>ConstraintVariable2</code>)
	 */
	private void computeTypes(final Collection variables) {
		Assert.isNotNull(variables);
		fTypeOccurrences= new HashMap();
		ConstraintVariable2 variable= null;
		for (final Iterator iterator= variables.iterator(); iterator.hasNext();) {
			variable= (ConstraintVariable2) iterator.next();
			final TypeSet set= (TypeSet) variable.getData(DATA_TYPE_ESTIMATE);
			if (set != null)
				variable.setData(DATA_TYPE_ESTIMATE, set.chooseSingleType());
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
	 * @return the type occurrences (element type: <code>&ltICompilationUnit, Collection&ltConstraintVariable2&gt&gt</code>)
	 */
	public final Map getTypeOccurrences() {
		return fTypeOccurrences;
	}

	/**
	 * Processes the given constraints on the constraint variable and propagates it.
	 * 
	 * @param constraints the type constraints to process (element type: <code>ITypeConstraint2</code>)
	 * @param variable the constraint variable
	 */
	private void processConstraints(final Collection constraints, final ConstraintVariable2 variable) {
		Assert.isNotNull(constraints);
		Assert.isNotNull(variable);
		ITypeConstraint2 constraint= null;
		for (final Iterator iterator= constraints.iterator(); iterator.hasNext();) {
			constraint= (ITypeConstraint2) iterator.next();
			if (constraint instanceof SimpleTypeConstraint2) {
				final SimpleTypeConstraint2 simple= (SimpleTypeConstraint2) constraint;
				final ConstraintVariable2 leftVariable= simple.getLeft();
				final ConstraintVariable2 rightVariable= simple.getRight();
				if (leftVariable instanceof TypeConstraintVariable2 && rightVariable instanceof TypeConstraintVariable2) {
					final EquivalenceRepresentative rightEquivalence= ((TypeConstraintVariable2) rightVariable).getRepresentative();
					final ITypeSet rightEstimate= rightEquivalence.getTypeEstimate();
					final EquivalenceRepresentative leftEquivalence= ((TypeConstraintVariable2) leftVariable).getRepresentative();
					final ITypeSet newEstimate= rightEstimate.restrictedTo(leftEquivalence.getTypeEstimate());
					if (rightEstimate != newEstimate) {
						rightEquivalence.setTypeEstimate(newEstimate);
						fProcessable.addAll(Arrays.asList(rightEquivalence.getElements()));
					}
				}
			}
		}
	}

	/**
	 * Solves the constraints of the associated model.
	 */
	public final void solveConstraints() {
		final Collection variables= fModel.getConstraintVariables();
		computeTypeEstimates(variables);
		fProcessable.addAll(variables);
		ConstraintVariable2 variable= null;
		while (!fProcessable.isEmpty()) {
			variable= (ConstraintVariable2) fProcessable.removeFirst();
			processConstraints(SuperTypeConstraintsModel.getUsage(variable), variable);
		}
		computeTypes(variables);
		computeObsoleteCasts(fModel.getCastVariables());
	}
}