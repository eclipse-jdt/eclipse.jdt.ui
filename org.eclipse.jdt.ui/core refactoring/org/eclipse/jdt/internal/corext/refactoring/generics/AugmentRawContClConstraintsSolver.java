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

package org.eclipse.jdt.internal.corext.refactoring.generics;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.AugmentRawContainerClientsTCModel;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.CollectionElementVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ConstraintVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.DeclaringTypeVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.EquivalenceRepresentative;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ITypeConstraint2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.PlainTypeVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.SimpleTypeConstraint2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.TypeConstraintVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.TypeSet;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.TypeVariable2;



public class AugmentRawContClConstraintsSolver {

	private final static String TYPE_ESTIMATE= "typeEstimate"; //$NON-NLS-1$
	
	private final AugmentRawContainerClientsTCModel fTypeConstraintFactory;
	
	/**
	 * The work-list used by the type constraint solver to hold the set of
	 * nodes in the constraint graph that remain to be (re-)processed. Entries
	 * are <code>ConstraintVariable2</code>s.
	 */
	private LinkedList/*<ConstraintVariable2>*/ fWorkList;
	
	private HashMap/*<ICompilationUnit, List<ConstraintVariable2>>*/ fDeclarationsToUpdate;
	
	public AugmentRawContClConstraintsSolver(AugmentRawContainerClientsTCModel typeConstraintFactory) {
		fTypeConstraintFactory= typeConstraintFactory;
		fWorkList= new LinkedList();
	}
	
	public void solveConstraints() {
		// TODO: solve constraints
		ConstraintVariable2[] allConstraintVariables= fTypeConstraintFactory.getAllConstraintVariables();
		initializeTypeEstimates(allConstraintVariables);
		EquivalenceRepresentative[] equivalenceRepresentatives= fTypeConstraintFactory.getEquivalenceRepresentatives();
		initializeTypeEstimates(equivalenceRepresentatives);
		fWorkList.addAll(Arrays.asList(allConstraintVariables));
		runSolver();
		chooseTypes(allConstraintVariables);
		//chooseTypes(equivalenceRepresentatives);
		// TODO: clear caches?
//		getDeclarationsToUpdate();
	}

	private void initializeTypeEstimates(ConstraintVariable2[] allConstraintVariables) {
		for (int i= 0; i < allConstraintVariables.length; i++) {
			ConstraintVariable2 cv= allConstraintVariables[i];
			//TODO: everything is a TypeConstraintVariable2 now
			if (cv instanceof TypeConstraintVariable2) {
				TypeConstraintVariable2 typeConstraintCv= (TypeConstraintVariable2) cv;
				//TODO: not necessary for types that are not used in a TypeConstraint but only as type in CollectionElementVariable
				setTypeEstimate(cv, TypeSet.create(typeConstraintCv.getTypeBinding()));
			} else if (cv instanceof CollectionElementVariable2) {
				setTypeEstimate(cv, TypeSet.getUniverse());
			}
		}
	}

	private void initializeTypeEstimates(EquivalenceRepresentative[] equivalenceRepresentatives) {
		for (int i= 0; i < equivalenceRepresentatives.length; i++) {
			EquivalenceRepresentative representative= equivalenceRepresentatives[i];
			//TODO: get existing element types iff code was already 1.5
			representative.setTypeEstimate(TypeSet.getUniverse());
		}
	}

	public static void setTypeEstimate(ConstraintVariable2 cv, TypeSet typeSet) {
		cv.setData(TYPE_ESTIMATE, typeSet);
	}

	public static TypeSet getTypeEstimate(ConstraintVariable2 cv) {
		return (TypeSet) cv.getData(TYPE_ESTIMATE);
	}
	
	private void runSolver() {
		while (! fWorkList.isEmpty()) {
			// Get a variable whose type estimate has changed
			ConstraintVariable2 cv= (ConstraintVariable2) fWorkList.removeFirst();
			List/*<ITypeConstraint2>*/ usedIn= fTypeConstraintFactory.getUsedIn(cv);
			processConstraints(usedIn, cv);
		}
	}
	
	/**
	 * Given a list of <code>ITypeConstraint2</code>s that all refer to a
	 * given <code>ConstraintVariable2</code> (whose type bound has presumably
	 * just changed), process each <code>ITypeConstraint</code>, propagating
	 * the type bound across the constraint as needed.
	 * 
	 * @param usedIn the <code>List</code> of <code>ITypeConstraint2</code>s
	 * to process
	 * @param changedCv the constraint variable whose type bound has changed
	 */
	private void processConstraints(List/*<ITypeConstraint2>*/ usedIn, ConstraintVariable2 changedCv) {
		int i= 0;
		for (Iterator iter= usedIn.iterator(); iter.hasNext(); i++) {
			ITypeConstraint2 tc= (ITypeConstraint2) iter.next();
			if (tc instanceof SimpleTypeConstraint2) {
				SimpleTypeConstraint2 stc= (SimpleTypeConstraint2) tc;
				maintainSimpleConstraint(changedCv, stc);
				//TODO: prune tcs which cannot cause further changes
				// Maybe these should be pruned after a special first loop over all ConstraintVariables,
				// Since this can only happen once for every CV in the work list.
//				if (isConstantConstraint(stc))
//					fTypeConstraintFactory.removeUsedIn(stc, changedCv);
			} else {
				//TODO
			}
		}
	}
	
	private void maintainSimpleConstraint(ConstraintVariable2 changedCv, SimpleTypeConstraint2 stc) {
		ConstraintVariable2 left= stc.getLeft();
		ConstraintVariable2 right= stc.getRight();
		
		Assert.isTrue(stc.getOperator().isSubtypeOperator()); // left <= right
		
		//TODO: this is only case:
		if (left instanceof TypeConstraintVariable2 && right instanceof CollectionElementVariable2) {
			EquivalenceRepresentative rightRep= ((CollectionElementVariable2) right).getRepresentative();
			TypeSet rightEstimate= rightRep.getTypeEstimate();
			TypeSet leftEstimate= getTypeEstimate(left);
			TypeSet newRightEstimate= rightEstimate.restrictedTo(leftEstimate);
			if (rightEstimate != newRightEstimate) {
				rightRep.setTypeEstimate(newRightEstimate);
				fWorkList.addAll(Arrays.asList(rightRep.getElements()));
			}
		}
	}

	private boolean isConstantConstraint(SimpleTypeConstraint2 stc) {
		return isConstantTypeEntity(stc.getLeft()) || isConstantTypeEntity(stc.getRight());
	}

	private static boolean isConstantTypeEntity(ConstraintVariable2 v) {
		return v instanceof PlainTypeVariable2 || v instanceof DeclaringTypeVariable2 || v instanceof TypeVariable2;
	}

	private void chooseTypes(ConstraintVariable2[] allConstraintVariables) {
		fDeclarationsToUpdate= new HashMap();
		for (int i= 0; i < allConstraintVariables.length; i++) {
			ConstraintVariable2 cv= allConstraintVariables[i];
			if (cv instanceof CollectionElementVariable2) {
				CollectionElementVariable2 elementCv= (CollectionElementVariable2) cv;
				//TODO: should calculate only once per EquivalenceRepresentative
				ITypeBinding typeBinding= elementCv.getRepresentative().getTypeEstimate().chooseSingleType();
				setChosenType(elementCv, typeBinding);
				ICompilationUnit cu= elementCv.getCompilationUnit();
				ArrayList cvs= (ArrayList) fDeclarationsToUpdate.get(cu);
				if (cvs != null) {
					cvs.add(cv);
				} else {
					cvs= new ArrayList(1);
					cvs.add(cv);
					fDeclarationsToUpdate.put(cu, cvs);
				}
			} else {
				setTypeEstimate(cv, null);
			}
		}
	}

	public HashMap getDeclarationsToUpdate() {
		return fDeclarationsToUpdate;
	}

	public static ITypeBinding getChosenType(ConstraintVariable2 cv) {
		if (cv instanceof CollectionElementVariable2)
			return ((CollectionElementVariable2) cv).getRepresentative().getTypeEstimate().chooseSingleType();
		return (ITypeBinding) cv.getData(TYPE_ESTIMATE);
	}

	private static void setChosenType(ConstraintVariable2 cv, ITypeBinding type) {
		cv.setData(TYPE_ESTIMATE, type);
	}
}
