/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.link;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.jface.util.Assert;

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
public class LinkedPositionManager implements IDocumentListener, IPositionUpdater {
	
	private static class PositionComparator implements Comparator {
		/*
		 * @see Comparator#compare(Object, Object)
		 */
		public int compare(Object object0, Object object1) {
			Position position0= (Position) object0;
			Position position1= (Position) object1;
			
			if (position0.getOffset() < position1.getOffset())
				return -1;
			else if (position0.getOffset() > position1.getOffset())
				return +1;
			else
				return 0;
		}
	}
	
	private class Replace implements IDocumentExtension.IReplace, Runnable {
		
		private Position[] fReplacePositions;
		private int fReplaceDeltaOffset;
		private int fReplaceLength;
		private String fReplaceText;
		
		// XXX StyledText workaround
		private IDocument fDocument;
		private IDocumentListener fOwner;
		
		public Replace(Position[] positions, int deltaOffset, int length, String text, IDocument document, IDocumentListener owner) {
//		public Replace(Position[] positions, int deltaOffset, int length, String text) {
			fDocument= document;
			fOwner= owner;
			
			fReplacePositions= positions;
			fReplaceDeltaOffset= deltaOffset;
			fReplaceLength= length;
			fReplaceText= text;
		}
		
		// XXX workaround
		public void run() {
			try {
				perform(fDocument, fOwner);	
			} catch (BadLocationException e) {
				JavaPlugin.log(e);
				Assert.isTrue(false);
			}
		}
		
		public void perform(IDocument document, IDocumentListener owner) throws BadLocationException {
			document.removeDocumentListener(owner);
			
			for (int i= 0; i != fReplacePositions.length; i++) {
/*
				Position position= fReplacePositions[i];
				int offset= position.getOffset() + fReplaceDeltaOffset;
				int length= fReplaceLength;

				// robustness
				Position[] positions= getPositions(fDocument);
				Position foundPosition= findCurrentPosition(positions, offset);				
				if (!position.equals(foundPosition))	{
					document.addDocumentListener(owner);
					leave(true);
					return;
				}
*/				
				document.replace(fReplacePositions[i].getOffset() + fReplaceDeltaOffset,
					fReplaceLength, fReplaceText);
			}

			document.addDocumentListener(owner);
		}		
	}

	private static final String LINKED_POSITION= "LinkedPositionManager.linked.position";	
	private static final Comparator fgPositionComparator= new PositionComparator();
	private static final Map fgActiveManagers= new HashMap();
		
	private IDocument fDocument;
	
	private LinkedPositionListener fListener;

	/**
	 * Creates a <code>LinkedPositionManager</code> for a <code>IDocument</code>.
	 * 
	 * @param document the document to use with linked positions.
	 */
	public LinkedPositionManager(IDocument document) {
		Assert.isNotNull(document);
		
		fDocument= document;		
		install();
	}

	/**
	 * Sets a listener to notify changes of current linked position.
	 */
	public void setLinkedPositionListener(LinkedPositionListener listener) {
		fListener= listener;	
	}
	
	/**
	 * Adds a linked position to the manager.
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
		Position[] positions= getPositions(fDocument);

		if (positions != null) {
			for (int i = 0; i < positions.length; i++)
				if (collides(positions[i], offset, length))
					throw new BadLocationException(LinkedPositionMessages.getString(("LinkedPositionManager.position.collision")));
		}
		
		String type= fDocument.get(offset, length);		

		if (containsLineDelimiters(type))
			throw new BadLocationException(LinkedPositionMessages.getString(("LinkedPositionManager.contains.line.delimiters")));

		try {
			fDocument.addPosition(LINKED_POSITION, new TypedPosition(offset, length, type));
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

	private void install() {
		LinkedPositionManager manager= (LinkedPositionManager) fgActiveManagers.get(fDocument);
		if (manager != null)
			manager.leave(true);		

		fgActiveManagers.put(fDocument, this);
		
		fDocument.addPositionCategory(LINKED_POSITION);
		fDocument.addPositionUpdater(this);		
		fDocument.addDocumentListener(this);
	}	
	
	/**
	 * Leaves the linked mode. If unsuccessful, the linked positions
	 * are restored to the values at the time they were added.
	 */
	public void uninstall(boolean success) {			
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
			
			fDocument.removePositionCategory(LINKED_POSITION);

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

	/**
	 * Returns the first linked position.
	 * 
	 * @return returns <code>null</code> if no linked position exist.
	 */
	public Position getFirstPosition() {
		return getNextPosition(-1);
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
		if (positions == null)
			return null;
	
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

		for (int i= positions.length - 1; i >= 0; i--)
			if (positions[i].getOffset() < offset)
				return positions[i];
		
		return null;		
	}

	private static Position[] getPositions(IDocument document) {
		try {
			Position[] positions= document.getPositions(LINKED_POSITION);
			Arrays.sort(positions, fgPositionComparator);
			return positions;

		} catch (BadPositionCategoryException e) {
			JavaPlugin.log(e);
			Assert.isTrue(false);
		}
		
		return null;
	}	

	private static boolean exceeds(Position position, int offset, int length) {
		return
			(offset < position.getOffset()) ||
			(offset + length > position.getOffset() + position.getLength());
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
		uninstall(success);

		if (fListener != null)
			fListener.exit(success);		
	}

	/*
	 * @see IDocumentListener#documentAboutToBeChanged(DocumentEvent)
	 */
	public void documentAboutToBeChanged(DocumentEvent event) {
		IDocument document= event.getDocument();

		// check if document change includes line delimiters
		if (containsLineDelimiters(event.getText())) {
			leave(true);
			return;
		}
			
		Position[] positions= getPositions(document);
		if (positions == null) {
			leave(true);
			return;
		}
				
		// find a valid position
		for (int i= 0; i != positions.length; i++)
			if (!exceeds(positions[i], event.getOffset(), event.getLength()))
				return;
		
		leave(true);
	}

	/*
	 * @see IDocumentListener#documentChanged(DocumentEvent)
	 */
	public void documentChanged(DocumentEvent event) {
		IDocument document= event.getDocument();

		Position[] positions= getPositions(document);
		TypedPosition currentPosition= (TypedPosition) findCurrentPosition(positions, event.getOffset());
		
		int deltaOffset= event.getOffset() - currentPosition.getOffset();

		Vector vector= new Vector();
		for (int i= 0; i != positions.length; i++) {
			TypedPosition position= (TypedPosition) positions[i];
			
			if (position.getType().equals(currentPosition.getType()) &&
				!position.equals(currentPosition))
			{
				vector.add(position);
			}
		}

		if (fListener != null) {
			Position[] replacePositions= (Position[]) vector.toArray(new Position[vector.size()]);		

			Replace replace= new Replace(replacePositions, deltaOffset, event.getLength(), event.getText(), document, this);
//			Replace replace= new Replace(replacePositions, deltaOffset, event.getLength(), event.getText());

			IDocumentExtension extension= (IDocumentExtension) fDocument;
			extension.registerPostNotificationReplace(this, replace);
//			fListener.setReplace(replace);

			fListener.setCurrentPosition(currentPosition, deltaOffset + event.getText().length());
		}
	}
	
	/*
	 * @see IPositionUpdater#update(DocumentEvent)
	 */
	public void update(DocumentEvent event) {
		Position[] positions= getPositions(event.getDocument());
		TypedPosition currentPosition= (TypedPosition) findCurrentPosition(positions, event.getOffset());

		// XXX occurs when using async exec
		if (currentPosition == null) {
			leave(true);
			return;
		}
		
		int deltaLength= event.getText().length() - event.getLength();
		int length= currentPosition.getLength();

		for (int i= 0; i != positions.length; i++) {
			TypedPosition position= (TypedPosition) positions[i];
			int offset= position.getOffset();
			
			if (position.equals(currentPosition)) {
				position.setLength(length + deltaLength);					
			} else if (offset > currentPosition.getOffset()) {
				position.setOffset(offset + deltaLength);
			}
		}		
	}

	private static Position findCurrentPosition(Position[] positions, int offset) {
		for (int i= 0; i != positions.length; i++)
			if (!exceeds(positions[i], offset, 0))
				return positions[i];
		
		return null;			
	}

	private boolean containsLineDelimiters(String string) {
		String[] delimiters= fDocument.getLegalLineDelimiters();

		for (int i= 0; i != delimiters.length; i++)
			if (string.indexOf(delimiters[i]) != -1)
				return true;

		return false;
	}
}
