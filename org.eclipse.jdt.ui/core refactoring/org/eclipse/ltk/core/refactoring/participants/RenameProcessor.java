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
package org.eclipse.ltk.core.refactoring.participants;

import org.eclipse.core.runtime.CoreException;

/**
 * A rename processor is a special refactoring processor to support 
 * participating in rename refactorings. A rename processor is responsible
 * for actual renaming the element to be refactored. Additionally the
 * processor can update reference which are part of the same domain as the 
 * element to be renamed. For example a processor to rename a Java field 
 * can also update all references to that field found in Java files.
 * <p>
 * A rename processor is also responsible to load participants that want
 * to participate in a rename refactoring.
 * </p>
 * 
 * @since 3.0
 */
public abstract class RenameProcessor extends RefactoringProcessor {

	private int fStyle;
	private SharableParticipants fSharedParticipants= new SharableParticipants();
	
	private static final RefactoringParticipant[] EMPTY_PARTICIPANT_ARRAY= new RefactoringParticipant[0];
	
	protected RenameProcessor() {
		fStyle= RefactoringStyles.NEEDS_PREVIEW;	
	}
	
	protected RenameProcessor(int style) {
		fStyle= style;	
	}

	public int getStyle() {
		return fStyle;
	}
	
	/**
	 * Forwards the current rename arguments to the passed participant.
	 *  
	 * @param participant the participant to set the arguments to
	 * 
	 * @throws CoreException if the arguments can't be set
	 */
	public void setArgumentsTo(RenameParticipant participant) throws CoreException {
		participant.setArguments(getArguments());
	}
	
	/**
	 * Returns the participants that participate in the rename of the element. The
	 * method is called after {@link #checkInitialConditions} has been called on the 
	 * processor itself. 
	 * 
	 * The arguments are set to the participants by the processor via the call 
	 * {@link RenameParticipant#setArguments(RenameArguments)}. They are set 
	 * before {@link #checkFinalConditions}is called on the participants. 
	 * 
	 * @return an array of rename participants
	 * 
	 * @throws CoreException if creating or loading of the participants failed
	 */
	public abstract RenameParticipant[] getElementParticipants() throws CoreException;
	
	/**
	 * Returns an array of secondary participants. There are two different kinds of 
	 * secondary participants that should be added via this hook method:
	 * <ul>
	 *   <li>participants listening to changes of derived elements. For example if
	 *       a Java field gets renamed corresponding setter and getters methods are 
	 *       renamed as well. The setter and getter methods are considered as
	 *       derived elements and the corresponding participants should be added
	 *       via this hook.</li>
	 *   <li>participants listening to changes of a domain model different than the
	 *       one that gets manipulated, but changed as a "side effect" of the
	 *       refactoring. For example, renaming a package moves all its files to a
	 *       different folder. If the package contains a HTML file then the rename
	 *       package processor is supposed to load all move HTML file participants 
	 *       via this hook.</li>
	 * </ul>
	 * <p>
	 * Implementors are responsible to initialize the created participants with the
	 * right arguments. The method is called after {@link #checkFinalConditions} has
	 * been called on the processor itself.
	 * </p>
	 * <p>
	 * This default implementation returns an empty array.
	 * </p>
	 * 
	 * @return an array of secondary participants
	 * 
	 * @throws CoreException if creating or loading of the participants failed
	 */
	public RefactoringParticipant[] getSecondaryParticipants() throws CoreException {
		return EMPTY_PARTICIPANT_ARRAY;
	}
	
	/**
	 * Returns the shared participants. ????
	 * 
	 * @return
	 */
	protected SharableParticipants getSharedParticipants() {
		return fSharedParticipants;
	}
	
	/**
	 * Returns the arguments of the rename.
	 * 
	 * @return the rename arguments
	 */
	protected RenameArguments getArguments() {
		return new RenameArguments(getNewElementName(), getUpdateReferences());
	}
	
	/**
	 * Returns the new name of the element to be renamed. The 
	 * method must not return <code>null</code>.
	 * 
	 * @return the new element name.
	 */
	protected abstract String getNewElementName();
	
	/**
	 * Returns whether reference updating is requested or not.
	 * 
	 * @return whether reference updating is requested or not
	 */
	protected abstract boolean getUpdateReferences();
}
