package org.eclipse.jdt.internal.ui.text;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import org.eclipse.swt.widgets.Listener;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.util.Assert;



/**
 */
public class JavaReconciler implements IReconciler {

	
	/**
	 * Background thread for the periodic reconciling activity.
	 */
	class BackgroundThread extends Thread {
		
		/** Has the reconciler been canceled */
		private boolean fCanceled= false;
		/** Has the reconciler been reset */
		private boolean fReset= false;
		/** Has a change been applied */
		private boolean fIsDirty= false;
		/** Is a reconciling strategy active */
		private boolean fIsActive= false;
		
		/**
		 * Creates a new background thread. The thread 
		 * runs with minimal priority.
		 *
		 * @param name the thread's name
		 */
		public BackgroundThread(String name) {
			super(name);
			setPriority(Thread.MIN_PRIORITY);
			setDaemon(true);
		}
		
		/**
		 * Returns whether a reconciling strategy is active right now.
		 *
		 * @return <code>true</code> if a activity is active
		 */
		public boolean isActive() {
			return fIsActive;
		}
		
		/**
		 * Cancels the background thread.
		 */
		public void cancel() {
			fCanceled= true;
			synchronized (fSyncPoint) {
				fSyncPoint.notifyAll();
			}
		}
		
		/**
		 * Reset the background thread as the text viewer has been changed,
		 */
		public void reset() {
			
			if (fDelay > 0) {
				
				synchronized (this) {
					fIsDirty= true;
					fReset= true;
				}
				
			} else {
				
				synchronized(this) {
					fIsDirty= true;
				}
				
				synchronized (fSyncPoint) {
					fSyncPoint.notifyAll();
				}
			}
		}
		
		/**
		 * The periodic activity. Wait until there is something in the
		 * queue managing the changes applied to the text viewer. Remove the
		 * first change from the queue and process it.
		 */
		public void run() {
			while (!fCanceled) {
				
				synchronized (fSyncPoint) {
					try {
						fSyncPoint.wait(fDelay);
					} catch (InterruptedException x) {
					}
				}
					
				if (fCanceled)
					break;
					
				if (!fIsDirty)
					continue;
					
				if (fReset) {
					synchronized (this) {
						fReset= false;
					}
					continue;
				}
									
				fIsActive= true;
				
				process();
				synchronized (this) {
					fIsDirty= false;
				}
				
				fIsActive= false;
			}
		}
	};
	
	/**
	 * Internal document listener and text input listener.
	 */
	class Listener implements IDocumentListener, ITextInputListener {
		
		/*
		 * @see IDocumentListener#documentAboutToBeChanged
		 */
		public void documentAboutToBeChanged(DocumentEvent e) {
		}
		
		/*
		 * @see IDocumentListener#documentChanged
		 */
		public void documentChanged(DocumentEvent e) {
			fThread.reset();
		}
		
		/*
		 * @see ITextInputListener#inputDocumentAboutToBeChanged
		 */
		public void inputDocumentAboutToBeChanged(IDocument oldInput, IDocument newInput) {
			
			if (oldInput == fDocument) {
				
				if (fDocument != null)
					fDocument.removeDocumentListener(this);
				
				fDocument= null;
			}
		}
		
		/*
		 * @see ITextInputListener#inputDocumentChanged
		 */
		public void inputDocumentChanged(IDocument oldInput, IDocument newInput) {
			
			if (newInput == null)
				return;
				
			fDocument= newInput;
			if (fStrategy != null)
				fStrategy.setDocument(fDocument);
				
			fDocument.addDocumentListener(this);
						
			fThread.reset();
		}
	};
	
	/** Synchronization object */
	private Object fSyncPoint;
	/** The background thread */
	private BackgroundThread fThread;
	/** Internal document and text input listener */
	private Listener fListener;
	/** The background thread delay */
	private int fDelay= 500;
	/** The reconciling strategy */
	private IReconcilingStrategy fStrategy;

	/** The text viewer's document */
	private IDocument fDocument;
	/** The text viewer */
	private ITextViewer fViewer;
	
	
	/**
	 * Creates a new reconciler with the following configuration: it is
	 * an incremental reconciler which kicks in every 500 ms. There are no
	 * predefined reconciling strategies.
	 */ 
	public JavaReconciler() {
		super();
	}
		
	/**
	 * Tells the reconciler how long it should collect text changes before
	 * it activates the appropriate reconciling strategies.
	 *
	 * @param delay the duration in milli seconds of a change collection period.
	 */
	public void setDelay(int delay) {
		fDelay= delay;
	}
	
	/**
	 * Registers a given reconciling strategy for a particular content type.
	 * If there is already a strategy registered for this type, the new strategy 
	 * is registered instead of the old one.
	 *
	 * @param strategy the reconciling strategy to register, or <code>null</code> to remove an existing one
	 * @param contentType the content type under which to register
	 */
	public void setReconcilingStrategy(IReconcilingStrategy strategy) {
		fStrategy= strategy;
	}
	
	/*
	 * @see IReconciler#install
	 */
	public void install(ITextViewer textViewer) {
		
		Assert.isNotNull(textViewer);
		
		fViewer= textViewer;
		
		fListener= new Listener();
		fViewer.addTextInputListener(fListener);
		
		fSyncPoint= new Object();
		fThread= new BackgroundThread(getClass().getName());
		fThread.start();
	}
	
	/*
	 * @see IReconciler#uninstall
	 */
	public void uninstall() {
		if (fListener != null) {
			fViewer.removeTextInputListener(fListener);
			fListener= null;
			fThread.cancel();
			fThread= null;
		}
	}
	
	/*
	 * @see IReconciler#getReconcilingStrategy
	 */
	public IReconcilingStrategy getReconcilingStrategy(String contentType) {
		return fStrategy;
	}
		
	/**
	 * Processes a dirty region. If the dirty region is <code>null</code> the whole
	 * document is consider being dirty. The dirty region is partitioned by the
	 * document and each partition is handed over to a reconciling strategy registered
	 * for the partition's content type.
	 *
	 * @param dirtyRegion the dirty region to be processed
	 */
	private void process() {
		if (fStrategy != null) {
			IRegion region= new Region(0, fDocument.getLength());
			fStrategy.reconcile(region);
		}
	}
}
