/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.reorg;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChange;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.internal.core.refactoring.text.SimpleTextChange;

class TextBufferChangeManager {
	
	private Map fMap; //ICompilationUnit -> ITextBufferChange
	private ITextBufferChangeCreator fChangeCreator;
	
	TextBufferChangeManager(ITextBufferChangeCreator changeCreator){
		Assert.isNotNull(changeCreator);
		fMap= new HashMap();
		fChangeCreator= changeCreator;
	}
	
	private ITextBufferChange getTextBufferChange(ICompilationUnit cu) throws JavaModelException{
		if (! fMap.containsKey(cu))
			fMap.put(cu, fChangeCreator.create(cu.getElementName(), cu));
		return (ITextBufferChange)fMap.get(cu);
	}
	
	void addReplace(ICompilationUnit cu, String name, int offset, int length, String text) throws JavaModelException{
		getTextBufferChange(cu).addReplace(name, offset, length, text);
	}
		
	void addSimpleTextChange(ICompilationUnit cu, SimpleTextChange change) throws JavaModelException{
		getTextBufferChange(cu).addSimpleTextChange(change);
	}
	
	ITextBufferChange[] getAllChanges(){
		return (ITextBufferChange[])fMap.values().toArray(new ITextBufferChange[fMap.values().size()]);
	}
}

