/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.text;

import java.util.ArrayList;import java.util.Arrays;import java.util.Iterator;import java.util.List;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.jdt.core.ICompilationUnit;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.core.refactoring.Assert;import org.eclipse.jdt.internal.core.refactoring.base.Change;import org.eclipse.jdt.internal.core.refactoring.base.ChangeContext;import org.eclipse.jdt.internal.core.refactoring.base.IChange;import org.eclipse.jdt.internal.core.refactoring.base.ITextChange;import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.RefactoringCoreMessages;

/**
 * A simple text change that operates on an <code>ITextBuffer</code>. A <code>SimpleTextChange
 * </code> must be added to a high level text buffer change. It is not possible to execute
 * a <code>SimpleTextChange</code> standalone.
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
 */
public abstract class SimpleTextChange extends Change implements ITextChange {
	
	private int fOffset;
	private AbstractTextBufferChange fChange;

	/**
	 * Creates a simple text change. The change's offset is set to zero.
	 */
	public SimpleTextChange() {
		this(0);
	}
	
	/**
	 * Creates a simple text change.
	 *
	 * @param offset the starting offset of this change. The offset must not be negative. 
	 * @see getOffset
	 */
	public SimpleTextChange(int offset) {
		setOffset(offset);
	}
	
	/**
	 * Performs the simple text change. The method must return a text change
	 * that can be used to undo this change. The result value <em>must not</em> 
	 * be <code>null</code>.
	 *
	 * @param the text buffer this change operates on. Must not be <code>null</code>.
	 * @return the undo change.
	 */
	protected abstract SimpleTextChange perform(ITextBuffer textBuffer) throws JavaModelException;
	
	/**
	 * This method can be overidden to adjust this change (e.g. change its offset) or to
	 * compute new <code>SimpleTextChange</code> objects the enclosing text buffer change
	 * is supposed to consider. After this method is called the <code>SimpleTextChange</code>
	 * objects are sorted according to their offset and are appield to the text buffer from 
	 * the end to the beginning.
	 * <p>
	 * This method is called after <code>aboutToPerform</code> has been called but before the
	 * perform method is called on any of the <code>SimpleTextChange</code> objects managed by
	 * the enclosing text buffer change. </p>
	 *
	 * @param buffer the text buffer this change operates on. Must not be <code>null</code>.
	 * @return additional simple text changes to be considered by the text buffer change. The
	 *  method can return <code>null</code>.
	 */
	protected SimpleTextChange[] adjust(ITextBuffer buffer) throws JavaModelException {
		return null;
	}
	
	/**
	 * Returns the starting offset of this change.
	 *
	 * @param the starting offset of this text change.
	 * @see setOffset(int)
	 */	
	public int getOffset() {
		return fOffset;
	}
		
	/**
	 * Sets the starting offset of the change into the text buffer. This value is used
	 * to sort the changes in descending order. This ensure that the position of
	 * the change aren't effected by any previous change applied to the text buffer.
	 *
	 * @param the starting offset of this text change. The offset must not be negative.
	 */	
	protected void setOffset(int offset) {
		fOffset= offset;
		Assert.isTrue(fOffset >= 0, RefactoringCoreMessages.getString("SimpleTextChange.assert.offset_negative")); //$NON-NLS-1$
	}
	 
	/**
	 * Sets the surrounding <code>AbstractTextBufferChange</code>.
	 *
	 * @param the enclosing text buffer change. The given value must not be <code>null</code>.
	 */
	void setChange(AbstractTextBufferChange change) {
		Assert.isNotNull(change);
		fChange= change;
	}

	/* (Non-Javadoc)
	 * Method declared in IChange.
	 */
	public IJavaElement getCorrespondingJavaElement() {
		IJavaElement element= fChange.getCorrespondingJavaElement();
		if (element instanceof ICompilationUnit) {
			ICompilationUnit unit= (ICompilationUnit)element;
			try {
				return unit.getElementAt(getOffset());
			} catch (JavaModelException e) {
				// do nothing
			}	
		}
		return null;
	}
	
	/* (Non-Javadoc)
	 * Method declared in ITextChange.
	 */
	public String getCurrentContent() throws JavaModelException {
		return fChange.getCurrentContent();
	}
	
	/* (Non-Javadoc)
	 * Method declared in ITextChange.
	 */
	public String getPreview() throws JavaModelException {
		if (!isActive())
			return getCurrentContent();				
			
		ITextBuffer buffer= fChange.createTextBuffer(fChange.getCurrentContent());
		SimpleTextChange[] adds= adjust(buffer);
		if (adds == null) {
			perform(buffer);
		} else {
			List changes= new ArrayList(Arrays.asList(adds));
			changes.add(this);
			fChange.sortChanges(changes);
			for (Iterator iter= changes.iterator(); iter.hasNext(); ) {
				SimpleTextChange change= (SimpleTextChange)iter.next();
				change.perform(buffer);
			}
		}
		return buffer.getContent();
	}
	
	/* (Non-Javadoc)
	 * Method declared in IChange.
	 */
	public RefactoringStatus aboutToPerform(ChangeContext context, IProgressMonitor pm) {
		Assert.isTrue(false,
			RefactoringCoreMessages.getFormattedString("SimpleTextChange.assert.only_from",  //$NON-NLS-1$
				new String[] {"SimpleTextChange", "AbstractTextBufferChange"})); //$NON-NLS-1$ //$NON-NLS-2$
		return null;
	}
	
	/* (Non-Javadoc)
	 * Method declared in IChange.
	 */
	public void perform(ChangeContext context, IProgressMonitor pm) throws JavaModelException {
		Assert.isTrue(false,
			RefactoringCoreMessages.getFormattedString("SimpleTextChange.assert.only_from",  //$NON-NLS-1$
				new String[] {"SimpleTextChange", "AbstractTextBufferChange"})); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
		
	/* (Non-Javadoc)
	 * Method declared in IChange.
	 */
	public void performed() {
		Assert.isTrue(false,
			RefactoringCoreMessages.getFormattedString("SimpleTextChange.assert.only_from",  //$NON-NLS-1$
				new String[] {"SimpleTextChange", "AbstractTextBufferChange"})); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/* (Non-Javadoc)
	 * Method declared in IChange.
	 */
	public IChange getUndoChange() {
		Assert.isTrue(false,
			RefactoringCoreMessages.getFormattedString("SimpleTextChange.assert.only_from",  //$NON-NLS-1$
				new String[] {"SimpleTextChange", "AbstractTextBufferChange"})); //$NON-NLS-1$ //$NON-NLS-2$
		return null;
	}	
}