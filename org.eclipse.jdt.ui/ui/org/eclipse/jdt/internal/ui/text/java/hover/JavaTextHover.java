package org.eclipse.jdt.internal.ui.text.java.hover;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.ui.text.java.hover.IJavaEditorTextHover;



/**
 * Caution: this implementation is a layer breaker and contains some "shortcuts"
 */
public class JavaTextHover extends AbstractJavaEditorTextHover {
		
	public static final String ID= "org.eclipse.jdt.internal.ui.text.java.hover.JavaTextHover"; //$NON-NLS-1$

	private static class JavaEditorTextHoverDescriptorComparator implements Comparator {
		
		/*
		 * @see Comparator#compare(Object, Object)
		 */
		public int compare(Object object0, Object object1) {

			JavaEditorTextHoverDescriptor element0= (JavaEditorTextHoverDescriptor)object0;
			JavaEditorTextHoverDescriptor element1= (JavaEditorTextHoverDescriptor)object1;	

			String id0=	element0.getId();
			String id1= element1.getId();
			
			if (id0 != null && id0.equals(id1))
				return 0;
			
			if (id0 != null && JavaProblemHover.isJavaProblemHover(id0))
				return -1;

			if (id1 != null && JavaProblemHover.isJavaProblemHover(id1))
				return +1;


			// now compare non-problem hovers
			if (element0.dependsOn(element1))
				return -1;

			if (element1.dependsOn(element0))
				return +1;
			
			return 0;
		}
	}
	
	protected String fCurrentPerspectiveId;
	protected List fTextHoverSpecifications;
	protected List fInstantiatedTextHovers;


	public JavaTextHover() {
		installTextHovers();
	}

	public JavaTextHover(IEditorPart editor) {
		this();
		setEditor(editor);
	}
	
	/**
	 * Installs all text hovers.
	 */
	private void installTextHovers() {
		
		// initialize lists - indicates that the initialization happened
		fTextHoverSpecifications= new ArrayList(2);
		fInstantiatedTextHovers= new ArrayList(2);

		// populate list
		Iterator iter= JavaEditorTextHoverDescriptor.getContributedHovers().iterator();
		while (iter.hasNext()) {
			JavaEditorTextHoverDescriptor descriptor= (JavaEditorTextHoverDescriptor)iter.next();
			// ensure that we don't add ourselves to the list
			if (!ID.equals(descriptor.getId()))
				fTextHoverSpecifications.add(descriptor);
		}
		Collections.sort(fTextHoverSpecifications, new JavaEditorTextHoverDescriptorComparator());
	}	

	private void checkTextHovers() {
		if (fTextHoverSpecifications.size() == 0)
			return;

		for (Iterator iterator= new ArrayList(fTextHoverSpecifications).iterator(); iterator.hasNext(); ) {
			JavaEditorTextHoverDescriptor spec= (JavaEditorTextHoverDescriptor) iterator.next();

			IJavaEditorTextHover hover= spec.createTextHover();
			if (hover != null) {
				hover.setEditor(getEditor());
				addTextHover(hover);
				fTextHoverSpecifications.remove(spec);
			}
		}
	}

	protected void addTextHover(ITextHover hover) {
		if (!fInstantiatedTextHovers.contains(hover))
			fInstantiatedTextHovers.add(hover);
	}

	/*
	 * @see ITextHover#getHoverInfo(ITextViewer, IRegion)
	 */
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {

		checkTextHovers();

		if (fInstantiatedTextHovers == null)
			return null;

		for (Iterator iterator= fInstantiatedTextHovers.iterator(); iterator.hasNext(); ) {
			ITextHover hover= (ITextHover) iterator.next();

			String s= hover.getHoverInfo(textViewer, hoverRegion);
			if (s != null && s.trim().length() > 0)
				return s;
		}

		return null;
	}
}