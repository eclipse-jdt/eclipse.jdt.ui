package org.eclipse.jdt.internal.ui.viewsupport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Item;

import org.eclipse.core.runtime.IPath;

import org.eclipse.jface.viewers.ILabelProvider;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.ui.util.JavaModelUtil;

/**
 * Helper class for updating error markers.
 * Items are mapped to paths of their underlying resources.
 * Method <code>problemsChanged</code> updates all items that are affected from the changed
 * elements.
 */public class ProblemItemMapper {
	// map from path to item
	private HashMap fPathToItem;

	public ProblemItemMapper() {
		fPathToItem= new HashMap();
	}

	/**
	 * Updates the icons of all mapped elements containing to the changed elements.
	 * Must be called from the UI thread.
	 */
	public void problemsChanged(Collection changedPaths, ILabelProvider lprovider) {
		// iterate over the smaller set/map
		if (changedPaths.size() <= fPathToItem.size()) {
			iterateChanges(changedPaths, lprovider);
		} else {
			iterateItems(changedPaths, lprovider);
		}
	}
	
	private void iterateChanges(Collection changedPaths, ILabelProvider lprovider) {
		Iterator elements= changedPaths.iterator();
		while (elements.hasNext()) {
			IPath curr= (IPath) elements.next();
			Object obj= fPathToItem.get(curr);
			if (obj == null) {
				// not mapped
			} else if (obj instanceof Item) {
				refreshIcon(lprovider, (Item)obj);
			} else { // List of Items
				List list= (List) obj;
				for (int i= 0; i < list.size(); i++) {
					refreshIcon(lprovider, (Item) list.get(i));
				}
			}
		}
	}
	
	private void iterateItems(Collection changedPaths, ILabelProvider lprovider) {
		Iterator keys= fPathToItem.keySet().iterator();
		while (keys.hasNext()) {
			IPath curr= (IPath) keys.next();
			if (changedPaths.contains(curr)) {
				Object obj= fPathToItem.get(curr);
				if (obj instanceof Item) {
					refreshIcon(lprovider, (Item)obj);
				} else { // List of Items
					List list= (List) obj;
					for (int i= 0; i < list.size(); i++) {
						refreshIcon(lprovider, (Item) list.get(i));
					}
				}
			}
		}
	}	
		
	private void refreshIcon(ILabelProvider lprovider, Item item) {
		if (!item.isDisposed()) { // defensive code
			Object data= item.getData();
			Image old= item.getImage();
			Image image= lprovider.getImage(data);
			if (image != null && image != old) {
				item.setImage(image);
			}
		}
	}

	/**
	 * Adds a new item to the map.
	 * @param element Element to map
	 * @param item The item used for the element
	 */
	public void addToMap(Object element, Item item) {
		IPath path= getCorrespondingPath(element);
		if (path != null) {
			Object existingMapping= fPathToItem.get(path);
			if (existingMapping == null) {
				fPathToItem.put(path, item);
			} else if (existingMapping instanceof Item) {
				if (existingMapping != item) {
					ArrayList list= new ArrayList(2);
					list.add(existingMapping);
					list.add(item);
					fPathToItem.put(path, list);
				}
			} else { // List			
				List list= (List)existingMapping;
				if (!list.contains(item)) {
					list.add(item);
				}
				// leave the list for reuse
			}
		}
	}

	/**
	 * Removes an element from the map.
	 */	
	public void removeFromMap(Object element, Item item) {
		IPath path= getCorrespondingPath(element);
		if (path != null) {
			Object existingMapping= fPathToItem.get(path);
			if (existingMapping == null) {
				return;
			} else if (existingMapping instanceof Item) {
				fPathToItem.remove(path);
			} else { // List
				List list= (List) existingMapping;
				list.remove(item);			
			}
		}
	}
	
	/**
	 * Clears the map.
	 */
	public void clearMap() {
		fPathToItem.clear();
	}
	
	/**
	 * Method that decides which elements can have error markers
	 * Returns null if an element can not have error markers.
	 */	
	private static IPath getCorrespondingPath(Object element) {
		if (element instanceof IJavaElement) {
			return getJavaElementPath((IJavaElement)element);
		}
		return null;
	}
	
	/**
	 * Gets the path of the underlying resource without throwing
	 * a JavaModelException if the resource does not exist.
	 */
	private static IPath getJavaElementPath(IJavaElement elem) {
		switch (elem.getElementType()) {
			case IJavaElement.JAVA_MODEL:
				return null;
			case IJavaElement.JAVA_PROJECT:
				return ((IJavaProject)elem).getProject().getFullPath();
			case IJavaElement.PACKAGE_FRAGMENT_ROOT:
				IPackageFragmentRoot root= (IPackageFragmentRoot)elem;
				if (!root.isArchive()) {
					return root.getPath();
				}
				return null;
			case IJavaElement.PACKAGE_FRAGMENT:
				String packName= elem.getElementName();
				IPath rootPath= getCorrespondingPath(elem.getParent());
				if (rootPath != null && packName.length() > 0) {
					rootPath= rootPath.append(packName.replace('.', '/'));
				}
				return rootPath;
			case IJavaElement.CLASS_FILE:
			case IJavaElement.COMPILATION_UNIT:
				IPath packPath= getCorrespondingPath(elem.getParent());
				if (packPath != null) {
					packPath= packPath.append(elem.getElementName());
				}
				return packPath;
			default:
				IOpenable openable= JavaModelUtil.getOpenable(elem);
				if (openable instanceof IJavaElement) {
					return getCorrespondingPath((IJavaElement)openable);
				}
				return null;
		}
	}	



}
