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
package org.eclipse.jdt.internal.ui.preferences.formatter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import org.eclipse.jdt.ui.PreferenceConstants;

/**
 * 
 */
public class CommentsTabPage extends ModifyDialogTabPage {
	
	
	private final class Controller implements Observer {
		
		private final Collection fMasters;
		private final Collection fSlaves;
		
		public Controller(Collection masters, Collection slaves) {
			fMasters= masters;
			fSlaves= slaves;
			for (final Iterator iter= fMasters.iterator(); iter.hasNext();) {
			    ((CheckboxPreference)iter.next()).addObserver(this);
			}
			update(null, null);
		}

		public void update(Observable o, Object arg) {
		    boolean enabled= true; 

		    for (final Iterator iter= fMasters.iterator(); iter.hasNext();) {
		        enabled &= ((CheckboxPreference)iter.next()).getChecked();
		    }

			for (final Iterator iter = fSlaves.iterator(); iter.hasNext();) {
			    Object obj= iter.next();
			    if (obj instanceof CheckboxPreference) {
			        ((CheckboxPreference)obj).setEnabled(enabled);
			    } else if (obj instanceof Group) {
			        ((Group)obj).setEnabled(enabled);
			    }
			}
		}
	}
	
	
	final int numColumns= 4;
	

	final String preview=
		"/**\n" +
		" * An example for comment formatting. This example is meant to illustrate the various possiblilities offered by <i>Eclipse</i> in order to format comments.\n" +
		" */\n" +
		" interface Example {" +
		" /**\n" +
		" *\n" +
		" * These possibilities include:\n" +
		" * <ul><li>Formatting of header comments.</li><li>Formatting of Javadoc tags</li></ul>\n" +
		" */\n" +
		" int bar();" +
		" /**\n" +
		" * The following is some sample code which illustrates source formatting within javadoc comments:\n" +
		" * <pre>public class Example {final int a= 1;final boolean b= true;}</pre>\n" + 
		" * @param a The first parameter. For an optimum result, this should be an odd number\n" +
		" * between 0 and 100.\n" +
		" * @param b The second parameter.\n" +
		" * @result The result of the foo operation, usually within 0 and 1000.\n" +
		" */" +
		" int foo(int a, int b);" +
		"}";
	
	
	public CommentsTabPage(Map workingValues) {
		super(workingValues);
		fJavaPreview.setPreviewText(preview);
	}

	
	protected Composite doCreatePreferences(Composite parent) {
		
		final Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(createGridLayout(numColumns, false));
		
		// global group
		final Group globalGroup= createGroup(numColumns, composite, "Comment Formatting");
		final CheckboxPreference global= createPref(globalGroup, "Enable comment formatting", PreferenceConstants.FORMATTER_COMMENT_FORMAT);

		// format group
		final Group formatGroup= createGroup(numColumns, composite, "Format");
		final CheckboxPreference header= createPref(formatGroup, "Format &header comment", PreferenceConstants.FORMATTER_COMMENT_FORMATHEADER);
		final CheckboxPreference html= createPref(formatGroup, "Format HTML ta&gs", PreferenceConstants.FORMATTER_COMMENT_FORMATHTML);
		final CheckboxPreference code= createPref(formatGroup, "Format &Java code snippets", PreferenceConstants.FORMATTER_COMMENT_FORMATSOURCE);

		// blank lines group
		final Group blankLinesGroup= createGroup(numColumns, composite, "Blank lines");
		final CheckboxPreference blankComments= createPref(blankLinesGroup, "Clear blank lines in comments", PreferenceConstants.FORMATTER_COMMENT_CLEARBLANKLINES);
		final CheckboxPreference blankJavadoc= createPref(blankLinesGroup, "Blank line before Javadoc tags", PreferenceConstants.FORMATTER_COMMENT_SEPARATEROOTTAGS);

		// indentation group
		final Group indentationGroup= createGroup(numColumns, composite, "Indentation");
		final CheckboxPreference indentJavadoc= createPref(indentationGroup, "Indent Javado&c tags", PreferenceConstants.FORMATTER_COMMENT_INDENTROOTTAGS);
		final CheckboxPreference indentDesc= createPref(indentationGroup, "Indent de&scription after @param", PreferenceConstants.FORMATTER_COMMENT_INDENTPARAMETERDESCRIPTION);

		// new lines group
		final Group newLinesGroup= createGroup(numColumns, composite, "New lines");
		final CheckboxPreference nlParam= createPref(newLinesGroup, "New line &after @param tags", PreferenceConstants.FORMATTER_COMMENT_NEWLINEFORPARAMETER);

		Collection masters, slaves;

		masters= new ArrayList();
		masters.add(global);
		
		slaves= new ArrayList();
		slaves.add(formatGroup);
		slaves.add(blankLinesGroup);
		slaves.add(indentationGroup);
		slaves.add(newLinesGroup);
		slaves.add(header);
		slaves.add(html);
		slaves.add(code);
		slaves.add(blankComments);
		slaves.add(blankJavadoc);
		slaves.add(indentJavadoc);
		slaves.add(nlParam);
		
		new Controller(masters, slaves);
		
		masters= new ArrayList();
		masters.add(global);
		masters.add(indentJavadoc);
		
		slaves= new ArrayList();
		slaves.add(indentDesc);
		
		new Controller(masters, slaves);
		
		return composite;
	}
	
	private CheckboxPreference createPref(Composite composite, String text, String key) {
	    return createCheckboxPref(composite, numColumns, text, key, falseTrue);
	}
}
