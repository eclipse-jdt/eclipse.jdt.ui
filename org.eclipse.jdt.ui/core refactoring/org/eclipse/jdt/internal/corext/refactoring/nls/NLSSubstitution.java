/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.nls;

import org.eclipse.jdt.internal.corext.Assert;

public class NLSSubstitution {
	
	public static final int TRANSLATE= 0;
	public static final int NEVER_TRANSLATE= 1;
	public static final int SKIP= 2;

	public static final int DEFAULT= TRANSLATE;
	public static final int STATE_COUNT= 3;
	
	public String key;
	public NLSElement value;
	public int task;
	public boolean putToPropertyFile= true;
	
	public NLSSubstitution(NLSSubstitution el){
		this.key= el.key;
		this.value= el.value;
		this.task= el.task;
	}
	
	public NLSSubstitution(String key, NLSElement element, int task) {
		this.key= key;
		this.value= element;
		this.task= task;
		Assert.isTrue(task == TRANSLATE || task == NEVER_TRANSLATE || task == SKIP);
	}
	
	public static int countItems(NLSSubstitution[] elems, int task){
		Assert.isTrue(task == NLSSubstitution.TRANSLATE 
				   || task == NLSSubstitution.NEVER_TRANSLATE 
				   || task == NLSSubstitution.SKIP);
		int result= 0;
		for (int i= 0; i < elems.length; i++){
			if (elems[i].task == task)
				result++;
		}	
		return result;   
	}
  
	public String getKeyWithPrefix(String prefix) {
		return prefix + key;
	}
}
