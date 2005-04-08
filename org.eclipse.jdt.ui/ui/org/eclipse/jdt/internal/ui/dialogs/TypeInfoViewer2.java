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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ILabelProvider;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameRequestor;

import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.corext.util.TypeInfo;
import org.eclipse.jdt.internal.corext.util.TypeInfoFactory;
import org.eclipse.jdt.internal.corext.util.TypeInfoHistory;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.StringMatcher;
import org.eclipse.jdt.internal.ui.util.TypeInfoLabelProvider;

public class TypeInfoViewer2 {
	
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
		private List fResult;
		
		private TypeInfoFactory factory= new TypeInfoFactory();
		private volatile boolean fStop;
		private Set fHistory;
		
		public SearchRequestor() {
			super();
			fResult= new ArrayList(500);
		}
		public TypeInfo[] getResult() {
			return (TypeInfo[])fResult.toArray(new TypeInfo[fResult.size()]);
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
			TypeInfo info= factory.create(packageName, simpleTypeName, enclosingTypeNames, modifiers, path);
			if (!fHistory.contains(info))
				fResult.add(info);
		}
	}
	
	private static class TypeInfoComparator implements Comparator {
	    public int compare(Object left, Object right) {
	     	String leftString= ((TypeInfo)left).getTypeName();
	     	String rightString= ((TypeInfo)right).getTypeName();
	     		     	
	     	if (Strings.isLowerCase(leftString.charAt(0)) &&
	     		!Strings.isLowerCase(rightString.charAt(0)))
	     		return +1;

	     	if (Strings.isLowerCase(rightString.charAt(0)) &&
	     		!Strings.isLowerCase(leftString.charAt(0)))
	     		return -1;
	     	
			int result= leftString.compareToIgnoreCase(rightString);			
			if (result == 0)
				result= leftString.compareTo(rightString);

			return result;
	    }
	}
	
	
	private static class SearchJob extends Job {
		private TypeInfoViewer2 fViewer;
		private TypeInfoFilter fFilter;
		private TypeInfo[] fHistory;
		
		private SearchRequestor fReqestor;
		public SearchJob(TypeInfoViewer2 viewer, TypeInfoFilter filter, TypeInfo[] history, int numberOfVisibleItems) {
			super("Search for all types");
			fViewer= viewer;
			fFilter= filter;
			fHistory= history;
			
			fReqestor= new SearchRequestor();
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
			List toAdd= new ArrayList();
			for (int i= 0; i < fHistory.length; i++) {
				TypeInfo type= fHistory[i];
				if (fFilter.match(type)) {
					fFilteredHistory.add(type);
					toAdd.add(type);
				}
			}
			fViewer.addAll(toAdd);
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
				return canceled(e, false);
			} catch (JavaModelException e) {
				fViewer.failed(e);
				return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.ERROR, "", e);
			}
			System.out.println("Time needed until search has finished: " + (System.currentTimeMillis() - start));
			TypeInfo[] result= fReqestor.getResult();
			Arrays.sort(result, new TypeInfoComparator());
			System.out.println("Time needed until sort has finished: " + (System.currentTimeMillis() - start));
			if (monitor.isCanceled())
				return canceled(null, false);
			
			if (result.length > 0 && fFilteredHistory.size() > 0) {
				fViewer.add(new DashLine());
			}
			int currentIndex= 0;
			while (true) {
				toAdd.clear();
	            int delta = Math.min(50, result.length - currentIndex);
				if (delta == 0)
					break;
				int end= currentIndex + delta;
				for (int i= currentIndex; i < end; i++) {
					toAdd.add(result[i]);
				}
				fViewer.addAll(toAdd);
				currentIndex= end;
				if (monitor.isCanceled())
					return canceled(null, false);
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					return canceled(e, true);
				}
				if (monitor.isCanceled())
					return canceled(null, false);
			}
			fViewer.done();
			return new Status(IStatus.OK, JavaPlugin.getPluginId(), IStatus.OK, "", null);
		}
		private IStatus canceled(Exception e, boolean removePendingItems) {
			fViewer.canceled(removePendingItems);
			return new Status(IStatus.CANCEL, JavaPlugin.getPluginId(), IStatus.CANCEL, "", e);
		}
	}
	
	private IDialogSettings fSettings;
	
	private Table fTable;
	private ILabelProvider fLabelProvider;
	
	private int fNumberOfVisibleItems;
	private int fNextElement;
	private List fItems;
	
	private TypeInfoHistory fHistory;
	private TypeInfo[] fHistoryItems;

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
	
	public TypeInfoViewer2(Composite parent, IDialogSettings settings) {
		fTable= new Table(parent, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.FLAT);
		fLabelProvider= new TypeInfoLabelProvider(TypeInfoLabelProvider.SHOW_POST_QUALIFIED);
		fItems= new ArrayList(500);
		fTable.setHeaderVisible(false);
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
		stop();
		if (text.length() == 0) {
			reset();
		} else {
			SearchJob searchJob= new SearchJob(this, new TypeInfoFilter(text), fHistoryItems, fNumberOfVisibleItems);
			fSearchJob= searchJob;
			fSearchJob.schedule();
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
				addSingleElement(element);
			}
		});
	}
	
	private void addAll(final List elements) {
		fTable.getDisplay().syncExec(new Runnable() {
			public void run() {
				/*
				int top= fTable.getTopIndex();
				boolean redrawOff= false;
				if (top < fNextElement && fNextElement < top + fNumberOfVisibleItems) {
					redrawOff= true;
					fTable.setRedraw(false);
				}
				*/
				for (Iterator iter= elements.iterator(); iter.hasNext();) {
					addSingleElement(iter.next());
				}
				/*
				if (redrawOff) {
					fTable.setRedraw(true);
				}
				*/
			}
		});
	}
	
	private void addSingleElement(final Object element) {
		TableItem item= null;
		if (fItems.size() > fNextElement) {
			item= (TableItem)fItems.get(fNextElement);
		} else {
			item= new TableItem(fTable, SWT.NONE);
			fItems.add(item);
		}
		fillItem(item, element);
		item.setData(element);
		fNextElement++;
		if (fNextElement == 1) {
			fTable.setSelection(0);
		}
	}
	
	private void reset() {
		fNextElement= 0;
		for (int i= 0; i < fHistoryItems.length; i++) {
			add(fHistoryItems[i]);
		}
		fTable.setItemCount(fHistoryItems.length);
		for (int i= fItems.size() - 1; i >= fHistoryItems.length; i--) {
			fItems.remove(i);
		}
	}
	
	private void canceled(boolean removePendingItems) {
		if (!removePendingItems)
			return;
		fTable.getDisplay().syncExec(new Runnable() {
			public void run() {
				finish();
			}
		});
	}
	
	private void done() {
		fTable.getDisplay().syncExec(new Runnable() {
			public void run() {
				finish();
			}
		});
	}
	
	private synchronized void failed(JavaModelException e) {
		fTable.getDisplay().syncExec(new Runnable() {
			public void run() {
				finish();
			}
		});
	}
	
	private void fillItem(TableItem item, Object element) {
		if (element instanceof DashLine) {
			Rectangle bounds= item.getImageBounds(0);
			Rectangle clientArea= fTable.getClientArea();
			item.setText(fDashLine.getText(clientArea.width - bounds.x - bounds.width - 20));
			item.setImage((Image)null);
			item.setGrayed(true);
		} else {
			item.setImage(fLabelProvider.getImage(element));
			item.setText(fLabelProvider.getText(element));
		}
		item.setData(element);
	}

	private void finish() {
		// System.out.println("Finishing. Number of elements " + fNextElement);
		fTable.setItemCount(fNextElement);
		for (int i= fItems.size() - 1; i >= fNextElement; i--) {
			// the item gets disposed via the call setItemCount above.
			fItems.remove(i);
		}
	}
}