/*******************************************************************************
 * Copyright (c) 2003 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.participants.properties;


public class PropertyInterface {
	
	static final PropertyInterface LAZY= new PropertyInterface() {
		public int eval(Class type, Object o, String name, String value) {
			return 0;
		}
	};
	
	static final PropertyInterface[] LAZY_ARRAY= new PropertyInterface[0];
	
	private IPropertyEvaluator fEvalutor;
	
	private PropertyInterface fExtends= LAZY;
	private PropertyInterface[] fImplements;
	
	public PropertyInterface() {
		fEvalutor= null;
	}
	
	public PropertyInterface(IPropertyEvaluator evaluator) {
		fEvalutor= evaluator;
	}
	
	public int eval(Class type, Object o, String name, String value) {
		// evaluator must return true, false and unhandled !!.
		
		int eval= fEvalutor != null ? fEvalutor.eval(o, name, value) : IPropertyEvaluator.UNKNOWN;
		if (eval == IPropertyEvaluator.FALSE || eval == IPropertyEvaluator.TRUE)
			return eval;
			
		Class superClass= type.getSuperclass();
		if (superClass != null && fExtends == LAZY) {
			fExtends= PropertyInterfaceManager.getInterface(superClass);
		} else {
			fExtends= null;
		}
		if (fExtends != null && fExtends.eval(type.getSuperclass(), o, name, value) == 0)
			return 0;
			
		Class[] interfaces= type.getInterfaces();
		if (interfaces != null && fImplements == LAZY_ARRAY) {
			fImplements= new PropertyInterface[interfaces.length];
			for (int i= 0; i < interfaces.length; i++) {
				fImplements[i]= LAZY;
			} 
		} else {
			fImplements= null;
		}
		if (fImplements != null) {
			for (int i= 0; i < fImplements.length; i++) {
				PropertyInterface pi= fImplements[i];
				if (pi == LAZY) {
					fImplements[i]= PropertyInterfaceManager.getInterface(interfaces[i]);
				}
				if (pi.eval(interfaces[i], o, name, value) == 0)
					return 0;
			}
		}
		return 0;
	}
}
