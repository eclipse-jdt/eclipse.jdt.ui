/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.util;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IWorkingCopy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChange;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.internal.core.refactoring.text.SimpleTextChange;

public class TextBufferChangeManager {
	
	private Map fMap; // ICompilationUnit -> ITextBufferChange
	private ITextBufferChangeCreator fChangeCreator;
	
	public TextBufferChangeManager(ITextBufferChangeCreator changeCreator){
		Assert.isNotNull(changeCreator);
		fMap= new HashMap();
		fChangeCreator= changeCreator;
	}
	
	public ITextBufferChange get(ICompilationUnit cu) throws JavaModelException{
		if (cu.isWorkingCopy()) {
			cu= (ICompilationUnit)cu.getOriginalElement();
		}
		ITextBufferChange result= (ITextBufferChange)fMap.get(cu);
		if (result == null) {
			result= fChangeCreator.create(cu.getElementName(), cu);
			fMap.put(cu, result);
		}
		return result;
	}
	
	public ITextBufferChange[] getAllChanges(){
		return (ITextBufferChange[])fMap.values().toArray(new ITextBufferChange[fMap.values().size()]);
	}
	
	public void clear() {
		fMap.clear();
	}
}

