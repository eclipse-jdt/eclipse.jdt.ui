/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
 package org.eclipse.jdt.internal.ui.dialogs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ILabelProvider;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameRequestor;

import org.eclipse.jdt.internal.corext.util.TypeInfo;
import org.eclipse.jdt.internal.corext.util.TypeInfoFactory;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.StringMatcher;
import org.eclipse.jdt.internal.ui.util.TypeInfoLabelProvider;

public class TypeInfoViewer {
	
	private static class TypeInfoFilter {

		private String fNamePattern;
		private StringMatcher fMatcher;
		private StringMatcher fQualifierMatcher;
		private boolean fIgnoreCase= true;
		
		private static final char END_SYMBOL= '<';
		private static final char ANY_STRING= '*';
		
		public TypeInfoFilter(String pattern) {
			int qualifierIndex= pattern.lastIndexOf("."); //$NON-NLS-1$
			if (qualifierIndex == -1) {
				fQualifierMatcher= null;
				fNamePattern= createPattern(pattern);
				fMatcher= new StringMatcher(fNamePattern, true, false);
			} else {
				fNamePattern= createPattern(pattern.substring(0, qualifierIndex));
				fQualifierMatcher= new StringMatcher(fNamePattern, fIgnoreCase, false);
				fMatcher= new StringMatcher(pattern.substring(qualifierIndex + 1), fIgnoreCase, false);
			}
		}
		public String getNamePattern() {
			return fNamePattern;
		}
		public int getSearchFlags() {
			int result= 0;
			if (!fIgnoreCase) {
				result= result | SearchPattern.R_CASE_SENSITIVE;
			}
			if (fNamePattern.indexOf("*") != -1) { //$NON-NLS-1$
				result= result | SearchPattern.R_PATTERN_MATCH;
			}
			return result;
		}
		public boolean match(TypeInfo type) {
			if (!fMatcher.match(type.getTypeName()))
				return false;

			if (fQualifierMatcher == null)
				return true;

			return fQualifierMatcher.match(type.getTypeContainerName());
		}
		private String createPattern(final String text) {
			int length= text.length();
			String result= text;
			if (length > 0) {
				if (length > 1) {
					char last= text.charAt(0);
					if (Character.isUpperCase(last)) {
						StringBuffer buffer= new StringBuffer();
						buffer.append(last);
						for (int i= 1; i < length; i++) {
							char c= text.charAt(i);
							if (Character.isUpperCase(last) && Character.isUpperCase(c)) {
								buffer.append("*"); //$NON-NLS-1$
							} 
							buffer.append(c);
							last= c;
						}
						String s= buffer.toString();
						if (!text.equals(s)) {
							fIgnoreCase= false;
							result= s;
						}
					}
				}
				length= result.length();
				switch (result.charAt(length - 1)) {
					case END_SYMBOL:
						result= result.substring(0, length - 1);
						break;
					case ANY_STRING:
						break;
					default:
						result= result + ANY_STRING;
				}
			}
			return result;
		}
	}
	
	/* 
	 * No need to synchronize this class. The synchronization takes place via the 
	 * add/done/cancel/failure methods on the viewer.
	 */
	private static class SearchRequestor extends TypeNameRequestor {
		private TypeInfoFactory factory= new TypeInfoFactory();
		private TypeInfoViewer fViewer;
		private volatile boolean fStop;
		private Set fHistory;
		
		private long fStartTime;
		private List fQueuedResults;
		private boolean fFirst= true;
		public SearchRequestor(TypeInfoViewer viewer) {
			super();
			fViewer= viewer;
			fStartTime= System.currentTimeMillis();
			fQueuedResults= new ArrayList();
		}
		public void cancel() {
			fStop= true;
		}
		public void setHistory(Set history) {
			fHistory= history;
		}
		public void acceptType(int modifiers, char[] packageName, char[] simpleTypeName, char[][] enclosingTypeNames, String path) {
			if (fStop)
				return;
			add(factory.create(packageName, simpleTypeName, enclosingTypeNames, modifiers, path));
		}
		private void add(TypeInfo type) {
			if (fHistory.contains(type))
				return;
			if (fFirst) {
				System.out.println("First result: " + (System.currentTimeMillis() - fStartTime));
				if (fHistory.size() > 0) 
					fViewer.add(new DashLine());
				fFirst= false;
			}
			fViewer.add(type);
		}
	}
	
	
	private static class SearchJob extends Job {
		private TypeInfoViewer fViewer;
		private TypeInfoFilter fFilter;
		private TypeInfo[] fHistory;
		private int fNumberOfVisibleItems;
		
		private SearchRequestor fReqestor;
		public SearchJob(TypeInfoViewer viewer, TypeInfoFilter filter, TypeInfo[] history, int numberOfVisibleItems) {
			super("Search for all types");
			fViewer= viewer;
			fFilter= filter;
			fHistory= history;
			fNumberOfVisibleItems= numberOfVisibleItems;
			
			fReqestor= new SearchRequestor(fViewer);
			setSystem(true);
		}
		public void stop() {
			fReqestor.cancel();
			cancel();
		}
		protected IStatus run(IProgressMonitor monitor) {
			if (monitor.isCanceled()) {
				return new Status(IStatus.CANCEL, JavaPlugin.getPluginId(), IStatus.CANCEL, "", null);
			}
			fViewer.clear();
			Set fFilteredHistory= new HashSet();
			for (int i= 0; i < fHistory.length; i++) {
				TypeInfo type= fHistory[i];
				if (fFilter.match(type)) {
					fFilteredHistory.add(type);
					fViewer.add(type);
				}
			}
			fReqestor.setHistory(fFilteredHistory);
			long start= System.currentTimeMillis();
			SearchEngine engine= new SearchEngine();
			try {
				engine.searchAllTypeNames(
					null, 
					fFilter.getNamePattern().toCharArray(), 
					fFilter.getSearchFlags(), 
					IJavaSearchConstants.TYPE, 
					SearchEngine.createWorkspaceScope(), 
					fReqestor, 
					IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, 
					monitor);
			} catch (OperationCanceledException e) {
				fViewer.canceled();
				return new Status(IStatus.CANCEL, JavaPlugin.getPluginId(), IStatus.CANCEL, "", e);
			} catch (JavaModelException e) {
				fViewer.failed(e);
				return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.ERROR, "", e);
			}
			System.out.println("Time needed: " + (System.currentTimeMillis() - start));
			fViewer.done();
			return new Status(IStatus.OK, JavaPlugin.getPluginId(), IStatus.OK, "", null);
		}
	}
	
	private static class SearchJobStarter implements Runnable {
		private TypeInfoViewer fOwner;
		private SearchJob fSearchJob;
		private volatile boolean fCanceled;
		public SearchJobStarter(TypeInfoViewer owner, SearchJob job) {
			super();
			fOwner= owner;
			fSearchJob= job;
		}
		public synchronized void cancel() {
			fCanceled= true;
		}
		public synchronized void run() {
			if (fCanceled)
				return;
			fSearchJob.schedule();
			fOwner.clearSearchJobStarter();
			fOwner.setSearchJob(fSearchJob);
		}
	}
	
	
	private IDialogSettings fSettings;
	
	private Table fTable;
	private ILabelProvider fLabelProvider;
	
	private int fNumberOfVisibleItems;
	private int fNextElement;
	private List fElements;
	private List fItems;
	
	private TypeInfoHistory fHistory;
	private TypeInfo[] fHistoryItems;

	private SearchJobStarter fSearchJobStarter;
	private SearchJob fSearchJob;
	
	private static final char MDASH= '—';
	private static class DashLine {
		public int fCharWidth;
		public String getText(int width) {
			StringBuffer buffer= new StringBuffer();
			for (int i= 0; i < width / fCharWidth; i++) {
				buffer.append(MDASH);
			}
			return buffer.toString();
		}
		public void setCharWidth(int width) {
			fCharWidth= width;
		}
	}
	private DashLine fDashLine= new DashLine();
	
	public TypeInfoViewer(Composite parent, IDialogSettings settings) {
		fTable= new Table(parent, SWT.VIRTUAL | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.FLAT);
		fLabelProvider= new TypeInfoLabelProvider(TypeInfoLabelProvider.SHOW_POST_QUALIFIED);
		fElements= new ArrayList(500);
		fItems= new ArrayList(500);
		fTable.setHeaderVisible(false);
		fTable.addListener(SWT.SetData, new Listener() {
			public void handleEvent(Event event) {
				handleElementRequest(event);
			}
		});
		fTable.addControlListener(new ControlListener() {
			public void controlResized(ControlEvent event) {
				int itemHeight= fTable.getItemHeight();
				Rectangle clientArea= fTable.getClientArea();
				fNumberOfVisibleItems= (clientArea.height / itemHeight) + 1;
			}
			public void controlMoved(ControlEvent e) {
			}
		});
		GC gc= null;
		try {
			gc= new GC(fTable);
			fDashLine.setCharWidth(gc.getCharWidth(MDASH));
		} finally {
			gc.dispose();
		}
		fSettings= settings;
		fHistory= new TypeInfoHistory(fSettings);
		fHistoryItems= fHistory.getTypeInfos();
		reset();
	}
	
	public Table getTable() {
		return fTable;
	}
	
	public void stop() {
		synchronized (this) {
			if (fSearchJobStarter != null) {
				fSearchJobStarter.cancel();
				fSearchJobStarter= null;
			}
			if (fSearchJob != null) {
				fSearchJob.stop();
				fSearchJob= null;
			}
		}
	}
	
	public TypeInfo[] getSelection() {
		TableItem[] items= fTable.getSelection();
		TypeInfo[] result= new TypeInfo[items.length];
		for (int i= 0; i < items.length; i++) {
			result[i]= (TypeInfo)items[i].getData();
			fHistory.accessed(result[i]);
		}
		fHistory.save(fSettings);
		return result;
	}
	
	public void setSearchPattern(String text) {
		boolean first= true;
		stop();
		if (text.length() == 0) {
			reset();
		} else {
			SearchJob searchJob= new SearchJob(this, new TypeInfoFilter(text), fHistoryItems, fNumberOfVisibleItems);
			if (first) {
				fSearchJob= searchJob;
				fSearchJob.schedule();
			} else {
				fSearchJob= searchJob;
				fSearchJob.schedule();
				// fSearchJobStarter= new SearchJobStarter(this, searchJob);
				// fTable.getDisplay().timerExec(100, fSearchJobStarter);
			}
		}
	}
	
	private void clear() {
		fTable.getDisplay().syncExec(new Runnable() {
			public void run() {
				fNextElement= 0;
			}
		});
	}
	
	private void add(final Object element) {
		fTable.getDisplay().syncExec(new Runnable() {
			public void run() {
				if (fElements.size() > fNextElement) {
					fElements.set(fNextElement, element);
					TableItem item= (TableItem)fItems.get(fNextElement);
					if (item != null) {
						item.setData(null);
					}
					fTable.clear(fNextElement);
				} else {
					fElements.add(element);
					fItems.add(null);
					fTable.setItemCount(fElements.size());
				}
				fNextElement++;
				if (fNextElement == 1) {
					fTable.setSelection(0);
				}
			}
		});
	}
	
	private void add(final Object[] elements) {
		fTable.getDisplay().syncExec(new Runnable() {
			public void run() {
				// fElements.add(element);
				int elementSize= fElements.size();
				if (fItems.size() < elementSize) {
					fItems.add(null);
					fTable.setItemCount(elementSize);
				} else {
					fTable.clear(elementSize - 1);
				}
				if (elementSize == 1) {
					fTable.setSelection(0);
				}
			}
		});
	}
	
	private void reset() {
		int numberOfItems= fTable.getItemCount();
		fElements.clear();
		fItems.clear();
		for (int i= 0; i < fHistoryItems.length; i++) {
			fElements.add(fHistoryItems[i]);
			fItems.add(null);
			if (i < numberOfItems)
				fTable.clear(i);
		}
		fTable.setItemCount(fHistoryItems.length);
		if (fHistoryItems.length > 0) {
			fTable.setSelection(0);
		}
		fTable.redraw();
	}
	
	private void canceled() {
		fTable.getDisplay().syncExec(new Runnable() {
			public void run() {
			}
		});
	}
	
	/*
	 * No need to synchronize since we do a sync exec right away anyway.
	 */
	private void done() {
		fTable.getDisplay().syncExec(new Runnable() {
			public void run() {
				fTable.setItemCount(fNextElement);
				// bug if count drops to zero table doesn't redraw under windows.
				if (fNextElement == 0) {
					fTable.redraw();
				}
				for (int i= fElements.size() - 1; i >= fNextElement; i--) {
					fElements.remove(i);
				}
				for (int i= fItems.size() - 1; i >= fNextElement; i--) {
					// the item gets disposed via the call setItemCount above.
					fItems.remove(i);
				}
			}
		});
	}
	
	private synchronized void failed(JavaModelException e) {
		fTable.getDisplay().syncExec(new Runnable() {
			public void run() {
			}
		});
	}
	
	private void handleElementRequest(Event event) {
		TableItem item = (TableItem) event.item;
		final int index = fTable.indexOf(item);
		Object element= fElements.get(index);
		if (element instanceof DashLine) {
			Rectangle bounds= item.getImageBounds(0);
			Rectangle clientArea= fTable.getClientArea();
			item.setText(fDashLine.getText(clientArea.width - bounds.x - bounds.width - 20));
			item.setGrayed(true);
		} else {
			item.setImage(fLabelProvider.getImage(element));
			item.setText(fLabelProvider.getText(element));
		}
		item.setData(element);
		fItems.set(index, item);
	}
	
	private synchronized void clearSearchJobStarter() {
		fSearchJobStarter= null;
	}
	
	private synchronized void setSearchJob(SearchJob job) {
		fSearchJob= job;
	}
}