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

public class TextBufferChangeManager {
	
	private Map fMap; //ICompilationUnit -> ITextBufferChange
	private ITextBufferChangeCreator fChangeCreator;
	
	public TextBufferChangeManager(ITextBufferChangeCreator changeCreator){
		Assert.isNotNull(changeCreator);
		fMap= new HashMap();
		fChangeCreator= changeCreator;
	}
	
	private ITextBufferChange getTextBufferChange(ICompilationUnit cu) throws JavaModelException{
		if (! fMap.containsKey(cu))
			fMap.put(cu, fChangeCreator.create(cu.getElementName(), cu));
		return (ITextBufferChange)fMap.get(cu);
	}
	
	/**
	 * Replaces the text [offset, length] with the given text.
	 * @param cu compilation unit
	 * @param name the changes name. The name is mainly used to render the change in the 
	 *  user interface. The name can be <code>null</code> indicating that the change
	 *  doesn't have a name.
	 * @param offset the starting offset of the text to be replaced. The offset must not
	 *  be negative.
	 * @param length the length of the text to be replaced. The length must not be negative.
	 * @param text the new text.
	 */
	public void addReplace(ICompilationUnit cu, String name, int offset, int length, String text) throws JavaModelException{
		getTextBufferChange(cu).addReplace(name, offset, length, text);
	}
	
	/**
	 * Adds a simple text change object to this text modifier.
	 *
	 * @param cu compilation unit
	 * @param change the simple text change to add. The change must not be <code>null</code>.
	 */	
	public void addSimpleTextChange(ICompilationUnit cu, SimpleTextChange change) throws JavaModelException{
		getTextBufferChange(cu).addSimpleTextChange(change);
	}
	
	public ITextBufferChange[] getAllChanges(){
		return (ITextBufferChange[])fMap.values().toArray(new ITextBufferChange[fMap.values().size()]);
	}
}

