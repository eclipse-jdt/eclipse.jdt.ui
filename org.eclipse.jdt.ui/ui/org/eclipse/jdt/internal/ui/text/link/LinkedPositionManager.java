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

package org.eclipse.jdt.internal.ui.text.link;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * This class manages linked positions in a document. Positions are linked
 * by type names. If positions have the same type name, they are considered
 * as <em>linked</em>.
 * 
 * The manager remains active on a document until any of the following actions
 * occurs:
 * 
 * <ul>
 *   <li>A document change is performed which would invalidate any of the
 *       above constraints.</li>
 * 
 *   <li>The method <code>uninstall()</code> is called.</li>
 * 
 *   <li>Another instance of <code>LinkedPositionManager</code> tries to
 *       gain control of the same document.
 * </ul>
 */
public class LinkedPositionManager implements IDocumentListener, IPositionUpdater, IAutoEditStrategy {

	// This class still exists to properly handle code assist. 
	// This is due to the fact that it cannot be distinguished betweeen document changes which are
	// issued by code assist and document changes which origin from another text viewer.
	// There is a conflict in interest since in the latter case the linked mode should be left, but in the former case
	// the linked mode should remain.
	// To support content assist, document changes have to be propagated to connected positions
	// by registering replace commands using IDocumentExtension.
	// if it wasn't for the support of content assist, the documentChanged() method could be reduced to 
	// a simple call to leave(true)  
	private class Replace implements IDocumentExtension.IReplace {
		
		private Position fReplacePosition;
		private int fReplaceDeltaOffset;
		private int fReplaceLength;
		private String fReplaceText;
		
		public Replace(Position position, int deltaOffset, int length, String text) {
			fReplacePosition= position;
			fReplaceDeltaOffset= deltaOffset;
			fReplaceLength= length;
			fReplaceText= text;
		}
				
		public void perform(IDocument document, IDocumentListener owner) {
			document.removeDocumentListener(owner);
			try {
				document.replace(fReplacePosition.getOffset() + fReplaceDeltaOffset, fReplaceLength, fReplaceText);
			} catch (BadLocationException e) {
				JavaPlugin.log(e);
				// TBD
			}
			document.addDocumentListener(owner);
		}
	}
	
	private static class PositionComparator implements Comparator {
		/*
		 * @see Comparator#compare(Object, Object)
		 */
		public int compare(Object object0, Object object1) {
			Position position0= (Position) object0;
			Position position1= (Position) object1;
			
			return position0.getOffset() - position1.getOffset();
		}
	}

	private static final String LINKED_POSITION_PREFIX= "LinkedPositionManager.linked.position"; //$NON-NLS-1$
	private static final Comparator fgPositionComparator= new PositionComparator();
	private static final Map fgActiveManagers= new HashMap();
	private static int fgCounter= 0;
		
	private IDocument fDocument;
	private ILinkedPositionListener fListener;
	private String fPositionCategoryName;
	private boolean fMustLeave;
	/**	
	 * Flag that records the state of this manager. As there are many different entities that may
	 * call leave or exit, these cannot always be sure whether the linked position infrastructure is
	 * still active. This is especially true for multithreaded situations. 
	 */
	private boolean fIsActive= false;


	/**
	 * Creates a <code>LinkedPositionManager</code> for a <code>IDocument</code>.
	 * 
	 * @param document the document to use with linked positions.
	 * @param canCoexist <code>true</code> if this manager can coexist with an already existing one
	 */
	public LinkedPositionManager(IDocument document, boolean canCoexist) {
		Assert.isNotNull(document);
		fDocument= document;
		fPositionCategoryName= LINKED_POSITION_PREFIX + (fgCounter++);
		install(canCoexist);
	}
	
	/**
	 * Creates a <code>LinkedPositionManager</code> for a <code>IDocument</code>.
	 * 
	 * @param document the document to use with linked positions.
	 */
	public LinkedPositionManager(IDocument document) {
		this(document, false);
	}
	
	/**
	 * Sets a listener to notify changes of current linked position.
	 */
	public void setLinkedPositionListener(ILinkedPositionListener listener) {
		fListener= listener;	
	}
	
	/**
	 * Adds a linked position to the manager with the type being the content of
	 * the document at the specified range.
	 * There are the following constraints for linked positions:
	 * 
	 * <ul>
	 *   <li>Any two positions have spacing of at least one character.
	 *       This implies that two positions must not overlap.</li>
	 *
	 *   <li>The string at any position must not contain line delimiters.</li>
	 * </ul>
	 * 
	 * @param offset the offset of the position.
	 * @param length the length of the position.
	 */
	public void addPosition(int offset, int length) throws BadLocationException {
		String type= fDocument.get(offset, length);
		addPosition(offset, length, type);
	}
	
	/**
	 * Adds a linked position of the specified position type to the manager. 
	 * There are the following constraints for linked positions:
	 * 
	 * <ul>
	 *   <li>Any two positions have spacing of at least one character.
	 *       This implies that two positions must not overlap.</li>
	 *
	 *   <li>The string at any position must not contain line delimiters.</li>
	 * </ul>
	 * 
	 * @param offset the offset of the position.
	 * @param length the length of the position.
	 * @param type the position type name - any positions with the same type are linked.
	 */
	public void addPosition(int offset, int length, String type) throws BadLocationException {
		Position[] positions= getPositions(fDocument);

		if (positions != null) {
			for (int i = 0; i < positions.length; i++)
				if (collides(positions[i], offset, length))
					throw new BadLocationException(LinkedPositionMessages.getString(("LinkedPositionManager.error.position.collision"))); //$NON-NLS-1$
		}
		
		String content= fDocument.get(offset, length);		

		if (containsLineDelimiters(content))
			throw new BadLocationException(LinkedPositionMessages.getString(("LinkedPositionManager.error.contains.line.delimiters"))); //$NON-NLS-1$

		try {
			fDocument.addPosition(fPositionCategoryName, new TypedPosition(offset, length, type));
		} catch (BadPositionCategoryException e) {
			JavaPlugin.log(e);
			Assert.isTrue(false);
		}
	}
	
	/**
	 * Adds a linked position to the manager. The current document content at the specified range is
	 * taken as the position type.
	 * <p> 
	 * There are the following constraints for linked positions:
	 * 
	 * <ul>
	 *   <li>Any two positions have spacing of at least one character.
	 *       This implies that two positions must not overlap.</li>
	 *
	 *   <li>The string at any position must not contain line delimiters.</li>
	 * </ul>
	 * 
	 * It is usually best to set the first item in <code>additionalChoices</code> to be equal with
	 * the text inserted at the current position.
	 * </p>
	 * 
	 * @param offset the offset of the position.
	 * @param length the length of the position.
	 * @param additionalChoices a number of additional choices to be displayed when selecting 
	 * a position of this <code>type</code>.
	 */
	public void addPosition(int offset, int length, ICompletionProposal[] additionalChoices) throws BadLocationException {
		String type= fDocument.get(offset, length);
		addPosition(offset, length, type, additionalChoices);
	}
	/**
	 * Adds a linked position of the specified position type to the manager. 
	 * There are the following constraints for linked positions:
	 * 
	 * <ul>
	 *   <li>Any two positions have spacing of at least one character.
	 *       This implies that two positions must not overlap.</li>
	 *
	 *   <li>The string at any position must not contain line delimiters.</li>
	 * </ul>
	 * 
	 * It is usually best to set the first item in <code>additionalChoices</code> to be equal with
	 * the text inserted at the current position.
	 * 
	 * @param offset the offset of the position.
	 * @param length the length of the position.
	 * @param type the position type name - any positions with the same type are linked.
	 * @param additionalChoices a number of additional choices to be displayed when selecting 
	 * a position of this <code>type</code>.
	 */
	public void addPosition(int offset, int length, String type, ICompletionProposal[] additionalChoices) throws BadLocationException {
		Position[] positions= getPositions(fDocument);

		if (positions != null) {
			for (int i = 0; i < positions.length; i++)
				if (collides(positions[i], offset, length))
					throw new BadLocationException(LinkedPositionMessages.getString(("LinkedPositionManager.error.position.collision"))); //$NON-NLS-1$
		}
		
		String content= fDocument.get(offset, length);		

		if (containsLineDelimiters(content))
			throw new BadLocationException(LinkedPositionMessages.getString(("LinkedPositionManager.error.contains.line.delimiters"))); //$NON-NLS-1$

		try {
			fDocument.addPosition(fPositionCategoryName, new ProposalPosition(offset, length, type, additionalChoices));
		} catch (BadPositionCategoryException e) {
			JavaPlugin.log(e);
			Assert.isTrue(false);
		}
	}
	
	/**
	 * Tests if a manager is already active for a document.
	 */
	public static boolean hasActiveManager(IDocument document) {
		return fgActiveManagers.get(document) != null;
	}
	
	private void install(boolean canCoexist) {
		
		if (fIsActive)
			JavaPlugin.log(new Status(IStatus.WARNING, JavaPlugin.getPluginId(), IStatus.OK, "LinkedPositionManager is already active: "+fPositionCategoryName, new IllegalStateException())); //$NON-NLS-1$
		else {
			fIsActive= true;
			//JavaPlugin.log(new Status(IStatus.INFO, JavaPlugin.getPluginId(), IStatus.OK, "LinkedPositionManager activated: "+fPositionCategoryName, new Exception())); //$NON-NLS-1$
		}
		
		if (!canCoexist) {
			LinkedPositionManager manager= (LinkedPositionManager) fgActiveManagers.get(fDocument);
			if (manager != null)
				manager.leave(true);	
		}
		
		fgActiveManagers.put(fDocument, this);
		fDocument.addPositionCategory(fPositionCategoryName);
		fDocument.addPositionUpdater(this);		
		fDocument.addDocumentListener(this);
		
		fMustLeave= false;
	}	
	
	/**
	 * Leaves the linked mode. If unsuccessful, the linked positions
	 * are restored to the values at the time they were added.
	 */
	public void uninstall(boolean success) {	
		
		if (!fIsActive)
			// we migth also just return
			JavaPlugin.log(new Status(IStatus.WARNING, JavaPlugin.getPluginId(), IStatus.OK, "LinkedPositionManager activated: "+fPositionCategoryName, new IllegalStateException())); //$NON-NLS-1$
		else {
			fDocument.removeDocumentListener(this);
			
			try {
				Position[] positions= getPositions(fDocument);	
				if ((!success) && (positions != null)) {
					// restore
					for (int i= 0; i != positions.length; i++) {
						TypedPosition position= (TypedPosition) positions[i];				
						fDocument.replace(position.getOffset(), position.getLength(), position.getType());
					}
				}		
				
				fDocument.removePositionCategory(fPositionCategoryName);
				
				fIsActive= false;
				// JavaPlugin.log(new Status(IStatus.INFO, JavaPlugin.getPluginId(), IStatus.OK, "LinkedPositionManager deactivated: "+fPositionCategoryName, new Exception())); //$NON-NLS-1$
				
			} catch (BadLocationException e) {
				JavaPlugin.log(e);
				Assert.isTrue(false);
				
			} catch (BadPositionCategoryException e) {
				JavaPlugin.log(e);
				Assert.isTrue(false);
				
			} finally {
				fDocument.removePositionUpdater(this);		
				fgActiveManagers.remove(fDocument);		
			}
		}
		
	}

	/**
	 * Returns the position at the given offset, <code>null</code> if there is no position.
	 * @since 2.1
	 */
	public Position getPosition(int offset) {
		Position[] positions= getPositions(fDocument);		
		if (positions == null)
			return null;

		for (int i= positions.length - 1; i >= 0; i--) {
			Position position= positions[i];
			if (offset >= position.getOffset() && offset <= position.getOffset() + position.getLength())
				return positions[i];
		}
		
		return null;
	}

	/**
	 * Returns the first linked position.
	 * 
	 * @return returns <code>null</code> if no linked position exist.
	 */
	public Position getFirstPosition() {
		return getNextPosition(-1);
	}
	
	public Position getLastPosition() {
		Position[] positions= getPositions(fDocument);
		for (int i= positions.length - 1; i >= 0; i--) {			
			String type= ((TypedPosition) positions[i]).getType();
			int j;
			for (j = 0; j != i; j++)
				if (((TypedPosition) positions[j]).getType().equals(type))
					break;

			if (j == i)
				return positions[i];				
		}

		return null;
	}

	/**
	 * Returns the next linked position with an offset greater than <code>offset</code>.
	 * If another position with the same type and offset lower than <code>offset</code>
	 * exists, the position is skipped.
	 * 
	 * @return returns <code>null</code> if no linked position exist.
	 */
	public Position getNextPosition(int offset) {
		Position[] positions= getPositions(fDocument);
		return findNextPosition(positions, offset);
	}

	private static Position findNextPosition(Position[] positions, int offset) {
		// skip already visited types
		for (int i= 0; i != positions.length; i++) {			
			if (positions[i].getOffset() > offset) {
				String type= ((TypedPosition) positions[i]).getType();
				int j;
				for (j = 0; j != i; j++)
					if (((TypedPosition) positions[j]).getType().equals(type))
						break;

				if (j == i)
					return positions[i];				
			}
		}

		return null;
	}
	
	/**
	 * Returns the position with the greatest offset smaller than <code>offset</code>.
	 *
	 * @return returns <code>null</code> if no linked position exist.
	 */
	public Position getPreviousPosition(int offset) {
		Position[] positions= getPositions(fDocument);
		if (positions == null)
			return null;

		TypedPosition currentPosition= (TypedPosition) findCurrentPosition(positions, offset);
		String currentType= currentPosition == null ? null : currentPosition.getType();

		Position lastPosition= null;
		Position position= getFirstPosition();

		while (position != null && position.getOffset() < offset) {
			if (!((TypedPosition) position).getType().equals(currentType))
				lastPosition= position;
			position= findNextPosition(positions, position.getOffset());
		}
		
		return lastPosition;
	}

	private Position[] getPositions(IDocument document) {

		if (!fIsActive)
			// we migth also just return an empty array
			JavaPlugin.log(new Status(IStatus.WARNING, JavaPlugin.getPluginId(), IStatus.OK, "LinkedPositionManager is not active: "+fPositionCategoryName, new IllegalStateException())); //$NON-NLS-1$
		
		try {
			Position[] positions= document.getPositions(fPositionCategoryName);
			Arrays.sort(positions, fgPositionComparator);
			return positions;

		} catch (BadPositionCategoryException e) {
			JavaPlugin.log(e);
			Assert.isTrue(false);
		}
		
		return null;
	}	

	public static boolean includes(Position position, int offset, int length) {
		return
			(offset >= position.getOffset()) &&
			(offset + length <= position.getOffset() + position.getLength());
	}

	public static boolean excludes(Position position, int offset, int length) {
		return
			(offset + length <= position.getOffset()) ||
			(position.getOffset() + position.getLength() <= offset);
	}

	/*
	 * Collides if spacing if positions intersect each other or are adjacent.
	 */
	private static boolean collides(Position position, int offset, int length) {
		return
			(offset <= position.getOffset() + position.getLength()) &&
			(position.getOffset() <= offset + length);	
	}
	
	private void leave(boolean success) {
		try {
			uninstall(success);
	
			if (fListener != null)
				fListener.exit((success ? LinkedPositionUI.COMMIT : 0) | LinkedPositionUI.UPDATE_CARET);
		} finally {
			fMustLeave= false;
		}		
	}
	
	private void abort() {
		uninstall(true); // don't revert anything
		
		if (fListener != null)
			fListener.exit(LinkedPositionUI.COMMIT); // don't let the UI restore anything
		
		// don't set fMustLeave, as we will get re-registered by a document event
	}

	/*
	 * @see IDocumentListener#documentAboutToBeChanged(DocumentEvent)
	 */
	public void documentAboutToBeChanged(DocumentEvent event) {

		if (fMustLeave) {
			event.getDocument().removeDocumentListener(this);
			return;
		}

		IDocument document= event.getDocument();

		Position[] positions= getPositions(document);
		Position position= findCurrentPosition(positions, event.getOffset());

		// modification outside editable position
		if (position == null) {
			// check for destruction of constraints (spacing of at least 1)
			if ((event.getText() == null || event.getText().length() == 0) &&
				(findCurrentPosition(positions, event.getOffset()) != null) && // will never become true, see condition above
				(findCurrentPosition(positions, event.getOffset() + event.getLength()) != null))
			{
				leave(true);
			}				

		// modification intersects editable position
		} else {
			// modificaction inside editable position
			if (includes(position, event.getOffset(), event.getLength())) {
				if (containsLineDelimiters(event.getText()))
					leave(true);

			// modificaction exceeds editable position
			} else {
				leave(true);
			}
		}
	}

	/*
	 * @see IDocumentListener#documentChanged(DocumentEvent)
	 */
	public void documentChanged(DocumentEvent event) {
		
		// have to handle code assist, so can't just leave the linked mode 
		// leave(true);
		
		IDocument document= event.getDocument();

		Position[] positions= getPositions(document);
		TypedPosition currentPosition= (TypedPosition) findCurrentPosition(positions, event.getOffset());

		// ignore document changes (assume it won't invalidate constraints)
		if (currentPosition == null)
			return;
		
		int deltaOffset= event.getOffset() - currentPosition.getOffset();		

		if (fListener != null) {
			int length= event.getText() == null ? 0 : event.getText().length();
			fListener.setCurrentPosition(currentPosition, deltaOffset + length);		
		}

		for (int i= 0; i != positions.length; i++) {
			TypedPosition p= (TypedPosition) positions[i];			
			
			if (p.getType().equals(currentPosition.getType()) && !p.equals(currentPosition)) {
				Replace replace= new Replace(p, deltaOffset, event.getLength(), event.getText());
				((IDocumentExtension) document).registerPostNotificationReplace(this, replace);
			}
		}
	}
	
	/*
	 * @see IPositionUpdater#update(DocumentEvent)
	 */
	public void update(DocumentEvent event) {
		
		int eventOffset= event.getOffset();
		int eventOldLength= event.getLength();
		int eventNewLength= event.getText() == null ? 0 : event.getText().length();
		int deltaLength= eventNewLength - eventOldLength;

		Position[] positions= getPositions(event.getDocument());
		
		
		for (int i= 0; i != positions.length; i++) {
			
			Position position= positions[i];
			
			if (position.isDeleted())
				continue;
			
			int offset= position.getOffset();
			int length= position.getLength();
			int end= offset + length;
			
			if (offset > eventOffset + eventOldLength) // position comes way after change - shift
				position.setOffset(offset + deltaLength);
			else if (end < eventOffset) // position comes way before change - leave alone
				;
			else if (offset <= eventOffset && end >= eventOffset + eventOldLength) { 
				// event completely internal to the position - adjust length
				position.setLength(length + deltaLength);
			} else if (offset < eventOffset) {
				// event extends over end of position - adjust length
				int newEnd= eventOffset + eventNewLength;
				position.setLength(newEnd - offset);
			} else if (end > eventOffset + eventOldLength) { 
				// event extends from before position into it - adjust offset and length
				// offset becomes end of event, length ajusted acordingly
				// we want to recycle the overlapping part
				int newOffset = eventOffset + eventNewLength;
				position.setOffset(newOffset);
				position.setLength(length + deltaLength);
			} else {
				// event consumes the position - delete it
				position.delete();
				JavaPlugin.log(new Status(IStatus.INFO, JavaPlugin.getPluginId(), IStatus.OK, "linked position deleted -> must leave: "+fPositionCategoryName, null)); //$NON-NLS-1$
				fMustLeave= true;
			}
		}
		
		if (fMustLeave)
			abort();
	}

	private static Position findCurrentPosition(Position[] positions, int offset) {
		for (int i= 0; i != positions.length; i++)
			if (includes(positions[i], offset, 0))
				return positions[i];
		
		return null;			
	}

	private boolean containsLineDelimiters(String string) {
		
		if (string == null)
			return false;
		
		String[] delimiters= fDocument.getLegalLineDelimiters();

		for (int i= 0; i != delimiters.length; i++)
			if (string.indexOf(delimiters[i]) != -1)
				return true;

		return false;
	}
	
	/**
	 * Test if ok to modify through UI.
	 */
	public boolean anyPositionIncludes(int offset, int length) {
		Position[] positions= getPositions(fDocument);

		Position position= findCurrentPosition(positions, offset);
		if (position == null)
			return false;
		
		return includes(position, offset, length);
	}
	
	/**
	 * Returns the position that includes the given range.
	 * @param offset
	 * @param length
	 * @return position that includes the given range
	 */
	public Position getEmbracingPosition(int offset, int length) {
		Position[] positions= getPositions(fDocument);

		Position position= findCurrentPosition(positions, offset);
		if (position != null && includes(position, offset, length))
			return position;
			
		return null;
	}
	
	/*
	 * @see org.eclipse.jface.text.IAutoIndentStrategy#customizeDocumentCommand(org.eclipse.jface.text.IDocument, org.eclipse.jface.text.DocumentCommand)
	 */
	public void customizeDocumentCommand(IDocument document, DocumentCommand command) {

		if (fMustLeave) {
			leave(true);
			return;
		}

		// don't interfere with preceding auto edit strategies
		if (command.getCommandCount() != 1) {
			leave(true);
			return;
		}

		Position[] positions= getPositions(document);
		TypedPosition currentPosition= (TypedPosition) findCurrentPosition(positions, command.offset);

		// handle edits outside of a position
		if (currentPosition == null) {
			leave(true);
			return;
		}

		if (! command.doit)
			return;

		command.doit= false;
		command.owner= this;
		command.caretOffset= command.offset + command.length;

		int deltaOffset= command.offset - currentPosition.getOffset();		

		if (fListener != null)
			fListener.setCurrentPosition(currentPosition, deltaOffset + command.text.length());
		
		for (int i= 0; i != positions.length; i++) {
			TypedPosition position= (TypedPosition) positions[i];			
			
			try {
				if (position.getType().equals(currentPosition.getType()) && !position.equals(currentPosition))
					command.addCommand(position.getOffset() + deltaOffset, command.length, command.text, true, this);
			} catch (BadLocationException e) {
				JavaPlugin.log(e);
			}
		}
	}

}
