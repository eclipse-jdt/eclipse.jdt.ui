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

import java.util.HashMap;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;

import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ConstraintVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.TypeHandle;

public class ContainerMethods {

	private final AugmentRawContainerClientsTCFactory fTCFactory;
	private final HashMap/*<String, SpecialMethod>*/ fContainerMethods;
	
	public ContainerMethods(AugmentRawContainerClientsTCFactory factory) {
		fTCFactory= factory;
		fContainerMethods= new HashMap();
		initialize();
	}

	public SpecialMethod getSpecialMethodFor(IMethodBinding methodBinding) {
		String key= getKey(methodBinding);
		SpecialMethod specialMethod= (SpecialMethod) fContainerMethods.get(key);
		//TODO: can be multiple...
		if (isTargetOf(specialMethod, methodBinding))
			return specialMethod;
		else
			return null;
	}

	private boolean isTargetOf(SpecialMethod specialMethod, IMethodBinding methodBinding) {
		//TODO: check parameter types (resolve type variables)
		TypeHandle declaringType= fTCFactory.getTypeHandleFactory().getTypeHandle(methodBinding.getDeclaringClass());
		return declaringType.canAssignTo(specialMethod.fTypeHandle);
	}

	private void initialize() {
		addSpecialMethod(new SpecialMethod(fTCFactory.fCollection, "add", new TypeHandle[] {fTCFactory.fObject}) {
			public void generateConstraintsFor(MethodInvocation invocation, AugmentRawContClConstraintCreator constraintCreator) {
				AugmentRawContainerClientsTCFactory tcFactory= constraintCreator.getTCFactory();
				
				Expression receiver= invocation.getExpression();
				//TODO: expression can be null when visiting a non-special method in a subclass of a container type.
				ConstraintVariable2 expressionCv= constraintCreator.getConstraintVariable(receiver);
				Expression arg0= (Expression) invocation.arguments().get(0);
				ConstraintVariable2 arg0Cv= constraintCreator.getConstraintVariable(arg0);
				ConstraintVariable2 elementCv= tcFactory.makeElementVariable(expressionCv);
				// [arg0] <= Elem(receiver)
				tcFactory.createSubtypeConstraint(arg0Cv, elementCv);
			}
		});
		
//		new MethodEntry("java.util.Collection", "add", new String[]{"java.lang.Object"}) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//			public ITypeConstraint[] generateConstraintsFor(MethodInvocation inv, GenericizeConstraintCreator gcc) {
//				if (DEBUG) System.out.println("Encountered a call to Collection.add" + formatArgs(inv.arguments())); //$NON-NLS-1$
//				Expression	rcvr= inv.getExpression();
//				Expression	arg0= (Expression) inv.arguments().get(0);
//
//				return gcc.getConstraintFactory().createSubtypeConstraint(
//						// [arg0] <= Elem(rcvr)
//						gcc.getConstraintVariableFactory().makeExpressionOrTypeVariable(arg0, gcc.getContext()),
//						gcc.getGenericizeVariableFactory().createElementVariable(rcvr, gcc.getContext(), gcc.getConstraintFactory()));
//			}
//		};
	}
	
	private void addSpecialMethod(SpecialMethod specialMethod) {
		String key= specialMethod.fName + '.' + specialMethod.fParameterTypes.length;
		fContainerMethods.put(key, specialMethod);
	}

	private String getKey(IMethodBinding methodBinding) {
		return methodBinding.getName() + '.' + methodBinding.getParameterTypes().length;
	}

}
