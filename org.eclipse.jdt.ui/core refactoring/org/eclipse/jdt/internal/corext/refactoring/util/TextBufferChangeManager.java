/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.util;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IWorkingCopy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.text.ITextBufferChange;
import org.eclipse.jdt.internal.corext.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.internal.corext.refactoring.text.SimpleTextChange;

public class TextBufferChangeManager {
	
	private Map fMap; // ICompilationUnit -> ITextBufferChange
	private ITextBufferChangeCreator fChangeCreator;
	
	public TextBufferChangeManager(ITextBufferChangeCreator changeCreator){
		Assert.isNotNull(changeCreator);
		fMap= new HashMap();
		fChangeCreator= changeCreator;
	}
	
	/**
	 * Returns the <code>ITextBufferChange</code> associated with the given compilation unit.
	 * 
	 * @param cu the compilation unit for which the text buffer change is requested
	 * @return the text buffer change associated with the given compilation unit
	 */
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
	
	/**
	 * Deletes the text [offset, length].
	 * @param name the changes name. The name is mainly used to render the change in the 
	 *  user interface. The name can be <code>null</code> indicating that the change
	 *  doesn't have a name.
	 * @param pos the starting offset of the text to be deleted. The offset must not be
	 *  negative.
	 * @param length the length of the text to be deleted. The length must not be negative.
	 */
	public void addDelete(ICompilationUnit cu, String name, int offset, int length)throws JavaModelException{
		get(cu).addDelete(name, offset, length);
	}
	
	/**
	 * Inserts the given text a the given pos.
	 * @param name the changes name. The name is mainly used to render the change in the 
	 *  user interface. The name can be <code>null</code> indicating that the change
	 *  doesn't have a name.
	 * @param offset the offset where the new text is to be inserted. The offset must not
	 * be negative.
	 * @param text the text to be inserted.
	 */
	public void addInsert(ICompilationUnit cu, String name, int offset, String text)throws JavaModelException{
		get(cu).addInsert(name, offset, text);
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
		get(cu).addReplace(name, offset, length, text);
	}
	
	/**
	 * Adds a simple text change object to this text modifier.
	 *
	 * @param cu compilation unit
	 * @param change the simple text change to add. The change must not be <code>null</code>.
	 */	
	public void addSimpleTextChange(ICompilationUnit cu, SimpleTextChange change) throws JavaModelException{
		get(cu).addSimpleTextChange(change);
	}	
	
	/**
	 * Returns all text buffer changes managed by this instance.
	 * 
	 * @return all text buffer changes managed by this instance
	 */
	public ITextBufferChange[] getAllChanges(){
		return (ITextBufferChange[])fMap.values().toArray(new ITextBufferChange[fMap.values().size()]);
	}
	
	/**
	 * Clears all associations between compilation units and text buffer changes.
	 */
	public void clear() {
		fMap.clear();
	}
}

