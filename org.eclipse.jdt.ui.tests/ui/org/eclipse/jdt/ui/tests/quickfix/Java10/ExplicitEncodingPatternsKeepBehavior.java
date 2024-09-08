/*******************************************************************************
 * Copyright (c) 2024 Carsten Hammer and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix.Java10;

public enum ExplicitEncodingPatternsKeepBehavior {

		CHARSET("""
package test1;

import java.nio.charset.Charset;

public class E1 {
	@SuppressWarnings("unused")
	void method(String filename) {
		Charset cs1= Charset.forName("UTF-8"); //$NON-NLS-1$
		Charset cs1b= Charset.forName("Utf-8"); //$NON-NLS-1$
		Charset cs2= Charset.forName("UTF-16"); //$NON-NLS-1$
		Charset cs3= Charset.forName("UTF-16BE"); //$NON-NLS-1$
		Charset cs4= Charset.forName("UTF-16LE"); //$NON-NLS-1$
		Charset cs5= Charset.forName("ISO-8859-1"); //$NON-NLS-1$
		Charset cs6= Charset.forName("US-ASCII"); //$NON-NLS-1$
		String result= cs1.toString();
	}
}
				""",
				"""
package test1;

import java.nio.charset.Charset;

public class E1 {
	@SuppressWarnings("unused")
	void method(String filename) {
		Charset cs1= Charset.forName("UTF-8"); //$NON-NLS-1$
		Charset cs1b= Charset.forName("Utf-8"); //$NON-NLS-1$
		Charset cs2= Charset.forName("UTF-16"); //$NON-NLS-1$
		Charset cs3= Charset.forName("UTF-16BE"); //$NON-NLS-1$
		Charset cs4= Charset.forName("UTF-16LE"); //$NON-NLS-1$
		Charset cs5= Charset.forName("ISO-8859-1"); //$NON-NLS-1$
		Charset cs6= Charset.forName("US-ASCII"); //$NON-NLS-1$
		String result= cs1.toString();
	}
}
						"""),
		BYTEARRAYOUTSTREAM("""
				package test1;

				import java.io.ByteArrayOutputStream;
				import java.io.InputStreamReader;
				import java.io.FileInputStream;
				import java.io.FileReader;
				import java.io.Reader;
				import java.io.FileNotFoundException;

				public class E1 {
				    void method(String filename) {
				        ByteArrayOutputStream ba=new ByteArrayOutputStream();
				        String result=ba.toString();
				        ByteArrayOutputStream ba2=new ByteArrayOutputStream();
				        String result2=ba2.toString("UTF-8");
				       }
				    }
				}
				""",

				"""
						package test1;

						import java.io.ByteArrayOutputStream;
						import java.io.InputStreamReader;
						import java.io.FileInputStream;
						import java.io.FileReader;
						import java.io.Reader;
						import java.nio.charset.Charset;
						import java.nio.charset.StandardCharsets;
						import java.io.FileNotFoundException;

						public class E1 {
						    void method(String filename) {
						        ByteArrayOutputStream ba=new ByteArrayOutputStream();
						        String result=ba.toString(Charset.defaultCharset());
						        ByteArrayOutputStream ba2=new ByteArrayOutputStream();
						        String result2=ba2.toString(StandardCharsets.UTF_8);
						       }
						    }
						}
						"""),
		FILEREADER("""
				package test1;

				import java.io.InputStreamReader;
				import java.io.FileInputStream;
				import java.io.FileReader;
				import java.io.Reader;
				import java.io.FileNotFoundException;

				public class E1 {
				    void method(String filename) {
				        try {
				            Reader is=new FileReader(filename);
				            } catch (FileNotFoundException e) {
				            e.printStackTrace();
				            }
				       }
				    }
				}
				""",

				"""
						package test1;

						import java.io.InputStreamReader;
						import java.io.FileInputStream;
						import java.io.FileReader;
						import java.io.Reader;
						import java.nio.charset.Charset;
						import java.io.FileNotFoundException;

						public class E1 {
						    void method(String filename) {
						        try {
						            Reader is=new InputStreamReader(new FileInputStream(filename), Charset.defaultCharset());
						            } catch (FileNotFoundException e) {
						            e.printStackTrace();
						            }
						       }
						    }
						}
						"""),
		FILEWRITER("""
				package test1;

				import java.io.FileWriter;
				import java.io.Writer;
				import java.io.FileNotFoundException;

				public class E1 {
				    void method(String filename) {
				        try {
				            Writer fw=new FileWriter(filename);
				            } catch (FileNotFoundException e) {
				            e.printStackTrace();
				            }
				       }
				    }
				}
				""",

				"""
						package test1;

						import java.io.FileWriter;
						import java.io.OutputStreamWriter;
						import java.io.Writer;
						import java.nio.charset.Charset;
						import java.io.FileNotFoundException;
						import java.io.FileOutputStream;

						public class E1 {
						    void method(String filename) {
						        try {
						            Writer fw=new OutputStreamWriter(new FileOutputStream(filename), Charset.defaultCharset());
						            } catch (FileNotFoundException e) {
						            e.printStackTrace();
						            }
						       }
						    }
						}
						"""),
		INPUTSTREAMREADER(
"""
package test1;

import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

public class E1 {

    void method(String filename) {
        try {
            // Standardkonstruktor ohne Encoding
            InputStreamReader is1 = new InputStreamReader(new FileInputStream("file1.txt")); //$NON-NLS-1$

            // String Literal Encodings, die nach StandardCharsets umgeschrieben werden sollten
            InputStreamReader is2 = new InputStreamReader(new FileInputStream("file2.txt"), "UTF-8"); //$NON-NLS-1$ //$NON-NLS-2$
            InputStreamReader is3 = new InputStreamReader(new FileInputStream("file3.txt"), "ISO-8859-1"); //$NON-NLS-1$ //$NON-NLS-2$
            InputStreamReader is4 = new InputStreamReader(new FileInputStream("file4.txt"), "US-ASCII"); //$NON-NLS-1$ //$NON-NLS-2$

            // String-basiertes Encoding, das in Charset umgeschrieben werden kann, jedoch ohne vordefinierte Konstante
            InputStreamReader is5 = new InputStreamReader(new FileInputStream("file5.txt"), "UTF-16"); //$NON-NLS-1$ //$NON-NLS-2$

            // String-basierte Encodings mit Groß-/Kleinschreibungsvarianten
            InputStreamReader is6 = new InputStreamReader(new FileInputStream("file6.txt"), "utf-8"); //$NON-NLS-1$ //$NON-NLS-2$
            InputStreamReader is7 = new InputStreamReader(new FileInputStream("file7.txt"), "Utf-8"); //$NON-NLS-1$ //$NON-NLS-2$

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace(); // Sollte nach Cleanup entfernt werden
        }
    }

    void methodWithTryCatch(String filename) {
        try {
            // Variante, bei der UnsupportedEncodingException behandelt wird
            InputStreamReader is8 = new InputStreamReader(new FileInputStream("file8.txt"), "UTF-8"); //$NON-NLS-1$ //$NON-NLS-2$
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace(); // Sollte nach Cleanup entfernt werden
        }
    }

    void methodWithoutException(String filename) throws UnsupportedEncodingException, FileNotFoundException {
        // Case ohne Try-Catch-Block, sollte Charset-Konstanten direkt ersetzen
        InputStreamReader is9 = new InputStreamReader(new FileInputStream("file9.txt"), "UTF-8"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    void methodWithVariableEncoding(String filename) throws UnsupportedEncodingException, FileNotFoundException {
        // Case, bei dem das Encoding aus einer Variablen kommt, Cleanup sollte hier keine Änderungen machen
        String encoding = "UTF-8"; //$NON-NLS-1$
        InputStreamReader is10 = new InputStreamReader(new FileInputStream("file10.txt"), encoding); //$NON-NLS-1$
    }

    void methodWithNonStandardEncoding(String filename) {
        try {
            // Case mit nicht vordefiniertem Charset, sollte keine Umwandlung in StandardCharsets erfolgen
            InputStreamReader is11 = new InputStreamReader(new FileInputStream("file11.txt"), "windows-1252"); //$NON-NLS-1$ //$NON-NLS-2$
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    // Methode mit "throws UnsupportedEncodingException" zur Prüfung des Cleanups
    void methodWithThrows(String filename) throws FileNotFoundException, UnsupportedEncodingException {
        InputStreamReader is3 = new InputStreamReader(new FileInputStream(filename), "UTF-8"); //$NON-NLS-1$
    }
}
""",

"""
package test1;

import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class E1 {

    void method(String filename) {
        try {
            // Standardkonstruktor ohne Encoding
            InputStreamReader is1 = new InputStreamReader(new FileInputStream("file1.txt"), Charset.defaultCharset()); //$NON-NLS-1$

            // String Literal Encodings, die nach StandardCharsets umgeschrieben werden sollten
            InputStreamReader is2 = new InputStreamReader(new FileInputStream("file2.txt"), StandardCharsets.UTF_8); //$NON-NLS-1$ //$NON-NLS-2$
            InputStreamReader is3 = new InputStreamReader(new FileInputStream("file3.txt"), StandardCharsets.ISO_8859_1); //$NON-NLS-1$ //$NON-NLS-2$
            InputStreamReader is4 = new InputStreamReader(new FileInputStream("file4.txt"), StandardCharsets.US_ASCII); //$NON-NLS-1$ //$NON-NLS-2$

            // String-basiertes Encoding, das in Charset umgeschrieben werden kann, jedoch ohne vordefinierte Konstante
            InputStreamReader is5 = new InputStreamReader(new FileInputStream("file5.txt"), StandardCharsets.UTF_16); //$NON-NLS-1$ //$NON-NLS-2$

            // String-basierte Encodings mit Groß-/Kleinschreibungsvarianten
            InputStreamReader is6 = new InputStreamReader(new FileInputStream("file6.txt"), StandardCharsets.UTF_8); //$NON-NLS-1$ //$NON-NLS-2$
            InputStreamReader is7 = new InputStreamReader(new FileInputStream("file7.txt"), StandardCharsets.UTF_8); //$NON-NLS-1$ //$NON-NLS-2$

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    void methodWithTryCatch(String filename) {
        try {
            // Variante, bei der UnsupportedEncodingException behandelt wird
            InputStreamReader is8 = new InputStreamReader(new FileInputStream("file8.txt"), StandardCharsets.UTF_8); //$NON-NLS-1$ //$NON-NLS-2$
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    void methodWithoutException(String filename) throws FileNotFoundException {
        // Case ohne Try-Catch-Block, sollte Charset-Konstanten direkt ersetzen
        InputStreamReader is9 = new InputStreamReader(new FileInputStream("file9.txt"), StandardCharsets.UTF_8); //$NON-NLS-1$ //$NON-NLS-2$
    }

    void methodWithVariableEncoding(String filename) throws UnsupportedEncodingException, FileNotFoundException {
        // Case, bei dem das Encoding aus einer Variablen kommt, Cleanup sollte hier keine Änderungen machen
        String encoding = "UTF-8"; //$NON-NLS-1$
        InputStreamReader is10 = new InputStreamReader(new FileInputStream("file10.txt"), encoding); //$NON-NLS-1$
    }

    void methodWithNonStandardEncoding(String filename) {
        try {
            // Case mit nicht vordefiniertem Charset, sollte keine Umwandlung in StandardCharsets erfolgen
            InputStreamReader is11 = new InputStreamReader(new FileInputStream("file11.txt"), "windows-1252"); //$NON-NLS-1$ //$NON-NLS-2$
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    // Methode mit "throws UnsupportedEncodingException" zur Prüfung des Cleanups
    void methodWithThrows(String filename) throws FileNotFoundException {
        InputStreamReader is3 = new InputStreamReader(new FileInputStream(filename), StandardCharsets.UTF_8); //$NON-NLS-1$
    }
}
"""),
		OUTPUTSTREAMWRITER(
"""
package test1;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class E1 {
    private static final String ENCODING_UTF8 = "UTF-8"; // Konstante für Encoding

    void method(String filename) {
        try {
            // Standard-Konstruktor ohne Encoding
            OutputStreamWriter os1 = new OutputStreamWriter(new FileOutputStream(filename));

            // Konstruktor mit String-Encoding (UTF-8) -> muss durch StandardCharsets.UTF_8 ersetzt werden
            OutputStreamWriter os2 = new OutputStreamWriter(new FileOutputStream(filename), "UTF-8");  // "UTF-8" als String-Literal

            // Konstruktor mit String-Encoding (ISO-8859-1) -> muss durch StandardCharsets.ISO_8859_1 ersetzt werden
            OutputStreamWriter os3 = new OutputStreamWriter(new FileOutputStream(filename), "ISO-8859-1"); // "ISO-8859-1" als String-Literal

            // Konstruktor mit String-Encoding (US-ASCII) -> muss durch StandardCharsets.US_ASCII ersetzt werden
            OutputStreamWriter os4 = new OutputStreamWriter(new FileOutputStream(filename), "US-ASCII"); // "US-ASCII" als String-Literal

            // Konstruktor mit String-Encoding (UTF-16) -> muss durch StandardCharsets.UTF_16 ersetzt werden
            OutputStreamWriter os5 = new OutputStreamWriter(new FileOutputStream(filename), "UTF-16");   // "UTF-16" als String-Literal

            // Der Konstruktor mit einer benutzerdefinierten Konstante bleibt unverändert
            OutputStreamWriter os6 = new OutputStreamWriter(new FileOutputStream(filename), ENCODING_UTF8);  // bleibt unverändert

            // Fälle ohne Entsprechung in StandardCharsets (bleiben unverändert)
            OutputStreamWriter os7 = new OutputStreamWriter(new FileOutputStream(filename), "windows-1252"); // bleibt unverändert
            OutputStreamWriter os8 = new OutputStreamWriter(new FileOutputStream(filename), "Shift_JIS");    // bleibt unverändert

            // Hier wird `UnsupportedEncodingException` geworfen (vor dem Cleanup)
            OutputStreamWriter os9 = new OutputStreamWriter(new FileOutputStream(filename), "non-existing-encoding"); // wirft UnsupportedEncodingException

            // Aufruf mit einer ungültigen Zeichenkodierung und catch für UnsupportedEncodingException
            try {
                OutputStreamWriter os10 = new OutputStreamWriter(new FileOutputStream(filename), "non-existing-encoding"); // könnte UnsupportedEncodingException werfen
            } catch (UnsupportedEncodingException e) {
                // Hier wird die UnsupportedEncodingException abgefangen
                e.printStackTrace();
            }

            // Beispiele mit StandardCharsets-Konstanten, die unverändert bleiben
            OutputStreamWriter os11 = new OutputStreamWriter(new FileOutputStream(filename), StandardCharsets.UTF_8); // bleibt unverändert
            OutputStreamWriter os12 = new OutputStreamWriter(new FileOutputStream(filename), StandardCharsets.ISO_8859_1); // bleibt unverändert
            OutputStreamWriter os13 = new OutputStreamWriter(new FileOutputStream(filename), StandardCharsets.US_ASCII); // bleibt unverändert
            OutputStreamWriter os14 = new OutputStreamWriter(new FileOutputStream(filename), StandardCharsets.UTF_16); // bleibt unverändert

            // Beispiel mit Charset.forName und einer Konstanten, die als Parameter übergeben wird (bleibt unverändert)
            OutputStreamWriter os15 = new OutputStreamWriter(new FileOutputStream(filename), StandardCharsets.UTF_16);
        } catch (FileNotFoundException e) {
            // Datei nicht gefunden
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            // Hier wird die UnsupportedEncodingException abgefangen
            e.printStackTrace();
        }
    }

    // Methodendeklaration, die `UnsupportedEncodingException` wirft (und durch den Cleanup angepasst wird)
    void methodWithThrows(String filename) throws UnsupportedEncodingException {
        OutputStreamWriter os = new OutputStreamWriter(new FileOutputStream(filename), "non-existing-encoding"); // wirft UnsupportedEncodingException
    }

    // Neue Methode: methodWithThrowsChange() - nach dem Cleanup wird keine UnsupportedEncodingException mehr geworfen
    void methodWithThrowsChange(String filename) throws FileNotFoundException {
        // Nach dem Cleanup, der String "UTF-8" wird zu einer StandardCharset-Konstanten geändert
        OutputStreamWriter os = new OutputStreamWriter(new FileOutputStream(filename), "UTF-8"); // wirft keine UnsupportedEncodingException mehr
    }

    // Methode mit einem try-catch, um die UnsupportedEncodingException zu behandeln (und durch den Cleanup angepasst wird)
    void methodWithCatch(String filename) {
        try {
            OutputStreamWriter os = new OutputStreamWriter(new FileOutputStream(filename), "non-existing-encoding"); // könnte UnsupportedEncodingException werfen
        } catch (UnsupportedEncodingException e) {
            // Hier wird die UnsupportedEncodingException abgefangen
            e.printStackTrace();
        }
    }

    // Neue Methode: methodWithCatchChange() - nach dem Cleanup wird keine UnsupportedEncodingException mehr abgefangen
    void methodWithCatchChange(String filename) {
        try {
            // Nach dem Cleanup wird "UTF-8" ersetzt
            OutputStreamWriter os = new OutputStreamWriter(new FileOutputStream(filename), "UTF-8"); // keine UnsupportedEncodingException
        } catch (UnsupportedEncodingException e) {
            // Dieser Block wird nicht mehr erreicht, da keine UnsupportedEncodingException mehr geworfen wird
            e.printStackTrace();
        }
    }
}
""",

"""
package test1;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class E1 {
    private static final String ENCODING_UTF8 = "UTF-8"; // Konstante für Encoding

    void method(String filename) {
        try {
            // Standard-Konstruktor ohne Encoding
            OutputStreamWriter os1 = new OutputStreamWriter(new FileOutputStream(filename), Charset.defaultCharset());

            // Konstruktor mit String-Encoding (UTF-8) -> muss durch StandardCharsets.UTF_8 ersetzt werden
            OutputStreamWriter os2 = new OutputStreamWriter(new FileOutputStream(filename), StandardCharsets.UTF_8);  // "UTF-8" als String-Literal

            // Konstruktor mit String-Encoding (ISO-8859-1) -> muss durch StandardCharsets.ISO_8859_1 ersetzt werden
            OutputStreamWriter os3 = new OutputStreamWriter(new FileOutputStream(filename), StandardCharsets.ISO_8859_1); // "ISO-8859-1" als String-Literal

            // Konstruktor mit String-Encoding (US-ASCII) -> muss durch StandardCharsets.US_ASCII ersetzt werden
            OutputStreamWriter os4 = new OutputStreamWriter(new FileOutputStream(filename), StandardCharsets.US_ASCII); // "US-ASCII" als String-Literal

            // Konstruktor mit String-Encoding (UTF-16) -> muss durch StandardCharsets.UTF_16 ersetzt werden
            OutputStreamWriter os5 = new OutputStreamWriter(new FileOutputStream(filename), StandardCharsets.UTF_16);   // "UTF-16" als String-Literal

            // Der Konstruktor mit einer benutzerdefinierten Konstante bleibt unverändert
            OutputStreamWriter os6 = new OutputStreamWriter(new FileOutputStream(filename), ENCODING_UTF8);  // bleibt unverändert

            // Fälle ohne Entsprechung in StandardCharsets (bleiben unverändert)
            OutputStreamWriter os7 = new OutputStreamWriter(new FileOutputStream(filename), "windows-1252"); // bleibt unverändert
            OutputStreamWriter os8 = new OutputStreamWriter(new FileOutputStream(filename), "Shift_JIS");    // bleibt unverändert

            // Hier wird `UnsupportedEncodingException` geworfen (vor dem Cleanup)
            OutputStreamWriter os9 = new OutputStreamWriter(new FileOutputStream(filename), "non-existing-encoding"); // wirft UnsupportedEncodingException

            // Aufruf mit einer ungültigen Zeichenkodierung und catch für UnsupportedEncodingException
            try {
                OutputStreamWriter os10 = new OutputStreamWriter(new FileOutputStream(filename), "non-existing-encoding"); // könnte UnsupportedEncodingException werfen
            } catch (UnsupportedEncodingException e) {
                // Hier wird die UnsupportedEncodingException abgefangen
                e.printStackTrace();
            }

            // Beispiele mit StandardCharsets-Konstanten, die unverändert bleiben
            OutputStreamWriter os11 = new OutputStreamWriter(new FileOutputStream(filename), StandardCharsets.UTF_8); // bleibt unverändert
            OutputStreamWriter os12 = new OutputStreamWriter(new FileOutputStream(filename), StandardCharsets.ISO_8859_1); // bleibt unverändert
            OutputStreamWriter os13 = new OutputStreamWriter(new FileOutputStream(filename), StandardCharsets.US_ASCII); // bleibt unverändert
            OutputStreamWriter os14 = new OutputStreamWriter(new FileOutputStream(filename), StandardCharsets.UTF_16); // bleibt unverändert

            // Beispiel mit Charset.forName und einer Konstanten, die als Parameter übergeben wird (bleibt unverändert)
            OutputStreamWriter os15 = new OutputStreamWriter(new FileOutputStream(filename), StandardCharsets.UTF_16);
        } catch (FileNotFoundException e) {
            // Datei nicht gefunden
            e.printStackTrace();
        }
    }

    // Methodendeklaration, die `UnsupportedEncodingException` wirft (und durch den Cleanup angepasst wird)
    void methodWithThrows(String filename) throws UnsupportedEncodingException {
        OutputStreamWriter os = new OutputStreamWriter(new FileOutputStream(filename), "non-existing-encoding"); // wirft UnsupportedEncodingException
    }

    // Neue Methode: methodWithThrowsChange() - nach dem Cleanup wird keine UnsupportedEncodingException mehr geworfen
    void methodWithThrowsChange(String filename) throws FileNotFoundException {
        // Nach dem Cleanup, der String "UTF-8" wird zu einer StandardCharset-Konstanten geändert
        OutputStreamWriter os = new OutputStreamWriter(new FileOutputStream(filename), StandardCharsets.UTF_8); // wirft keine UnsupportedEncodingException mehr
    }

    // Methode mit einem try-catch, um die UnsupportedEncodingException zu behandeln (und durch den Cleanup angepasst wird)
    void methodWithCatch(String filename) {
        try {
            OutputStreamWriter os = new OutputStreamWriter(new FileOutputStream(filename), "non-existing-encoding"); // könnte UnsupportedEncodingException werfen
        } catch (UnsupportedEncodingException e) {
            // Hier wird die UnsupportedEncodingException abgefangen
            e.printStackTrace();
        }
    }

    // Neue Methode: methodWithCatchChange() - nach dem Cleanup wird keine UnsupportedEncodingException mehr abgefangen
    void methodWithCatchChange(String filename) {
        try {
            // Nach dem Cleanup wird "UTF-8" ersetzt
            OutputStreamWriter os = new OutputStreamWriter(new FileOutputStream(filename), StandardCharsets.UTF_8); // keine UnsupportedEncodingException
        }
    }
}
"""),
		CHANNELSNEWREADER(
"""
package test1;

import java.io.Reader;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.charset.CharsetDecoder;

public class E1 {
    private static final String ENCODING_UTF8 = "UTF-8"; // Konstante für Encoding

    void method(ReadableByteChannel ch, CharsetDecoder decoder) {
        // Fälle für StandardCharsets-Konstanten
        Reader r1 = Channels.newReader(ch, "UTF-8");        // soll StandardCharsets.UTF_8 werden
        Reader r2 = Channels.newReader(ch, "ISO-8859-1");   // soll StandardCharsets.ISO_8859_1 werden
        Reader r3 = Channels.newReader(ch, "US-ASCII");     // soll StandardCharsets.US_ASCII werden
        Reader r4 = Channels.newReader(ch, "UTF-16");       // soll StandardCharsets.UTF_16 werden

        // Aufruf mit einer String-Konstanten (soll unverändert bleiben)
        Reader r5 = Channels.newReader(ch, ENCODING_UTF8);  // bleibt unverändert

        // Fälle ohne Entsprechung in StandardCharsets (sollen unverändert bleiben)
        Reader r6 = Channels.newReader(ch, "windows-1252"); // bleibt unverändert
        Reader r7 = Channels.newReader(ch, "Shift_JIS");    // bleibt unverändert

        // Aufrufe, die bereits `StandardCharsets` verwenden (bleiben unverändert)
        Reader r8 = Channels.newReader(ch, StandardCharsets.UTF_8);
        Reader r9 = Channels.newReader(ch, decoder, 1024);  // mit CharsetDecoder und Buffergröße, bleibt unverändert
    }
}
""",

"""
package test1;

import java.io.Reader;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.charset.CharsetDecoder;

public class E1 {
    private static final String ENCODING_UTF8 = "UTF-8"; // Konstante für Encoding

    void method(ReadableByteChannel ch, CharsetDecoder decoder) {
        // Fälle für StandardCharsets-Konstanten
        Reader r1 = Channels.newReader(ch, StandardCharsets.UTF_8);        // soll StandardCharsets.UTF_8 werden
        Reader r2 = Channels.newReader(ch, StandardCharsets.ISO_8859_1);   // soll StandardCharsets.ISO_8859_1 werden
        Reader r3 = Channels.newReader(ch, StandardCharsets.US_ASCII);     // soll StandardCharsets.US_ASCII werden
        Reader r4 = Channels.newReader(ch, StandardCharsets.UTF_16);       // soll StandardCharsets.UTF_16 werden

        // Aufruf mit einer String-Konstanten (soll unverändert bleiben)
        Reader r5 = Channels.newReader(ch, ENCODING_UTF8);  // bleibt unverändert

        // Fälle ohne Entsprechung in StandardCharsets (sollen unverändert bleiben)
        Reader r6 = Channels.newReader(ch, "windows-1252"); // bleibt unverändert
        Reader r7 = Channels.newReader(ch, "Shift_JIS");    // bleibt unverändert

        // Aufrufe, die bereits `StandardCharsets` verwenden (bleiben unverändert)
        Reader r8 = Channels.newReader(ch, StandardCharsets.UTF_8);
        Reader r9 = Channels.newReader(ch, decoder, 1024);  // mit CharsetDecoder und Buffergröße, bleibt unverändert
    }
}
"""),
		CHANNELSNEWWRITER(
"""
package test1;

import java.io.Writer;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;

public class E1 {
    private static final String ENCODING_UTF8 = "UTF-8"; // Konstante für Encoding

    void method(WritableByteChannel ch, Charset charset) {
        // Fälle für StandardCharsets-Konstanten
        Writer w1 = Channels.newWriter(ch, "UTF-8");        // soll StandardCharsets.UTF_8 werden
        Writer w2 = Channels.newWriter(ch, "ISO-8859-1");   // soll StandardCharsets.ISO_8859_1 werden
        Writer w3 = Channels.newWriter(ch, "US-ASCII");     // soll StandardCharsets.US_ASCII werden
        Writer w4 = Channels.newWriter(ch, "UTF-16");       // soll StandardCharsets.UTF_16 werden

        // Aufruf mit einer String-Konstanten (soll unverändert bleiben)
        Writer w5 = Channels.newWriter(ch, ENCODING_UTF8);  // bleibt unverändert

        // Fälle ohne Entsprechung in StandardCharsets (sollen unverändert bleiben)
        Writer w6 = Channels.newWriter(ch, "windows-1252"); // bleibt unverändert
        Writer w7 = Channels.newWriter(ch, "Shift_JIS");    // bleibt unverändert

        // Aufrufe, die bereits `StandardCharsets` verwenden (bleiben unverändert)
        Writer w8 = Channels.newWriter(ch, StandardCharsets.UTF_8);
        Writer w9 = Channels.newWriter(ch, charset); // unverändert, da `Charset` Instanz verwendet
    }
}
""",

"""
package test1;

import java.io.Writer;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;

public class E1 {
    private static final String ENCODING_UTF8 = "UTF-8"; // Konstante für Encoding

    void method(WritableByteChannel ch, Charset charset) {
        // Fälle für StandardCharsets-Konstanten
        Writer w1 = Channels.newWriter(ch, StandardCharsets.UTF_8);        // soll StandardCharsets.UTF_8 werden
        Writer w2 = Channels.newWriter(ch, StandardCharsets.ISO_8859_1);   // soll StandardCharsets.ISO_8859_1 werden
        Writer w3 = Channels.newWriter(ch, StandardCharsets.US_ASCII);     // soll StandardCharsets.US_ASCII werden
        Writer w4 = Channels.newWriter(ch, StandardCharsets.UTF_16);       // soll StandardCharsets.UTF_16 werden

        // Aufruf mit einer String-Konstanten (soll unverändert bleiben)
        Writer w5 = Channels.newWriter(ch, ENCODING_UTF8);  // bleibt unverändert

        // Fälle ohne Entsprechung in StandardCharsets (sollen unverändert bleiben)
        Writer w6 = Channels.newWriter(ch, "windows-1252"); // bleibt unverändert
        Writer w7 = Channels.newWriter(ch, "Shift_JIS");    // bleibt unverändert

        // Aufrufe, die bereits `StandardCharsets` verwenden (bleiben unverändert)
        Writer w8 = Channels.newWriter(ch, StandardCharsets.UTF_8);
        Writer w9 = Channels.newWriter(ch, charset); // unverändert, da `Charset` Instanz verwendet
    }
}
"""),
		PRINTWRITER("""
				package test1;

				import java.io.PrintWriter;
				import java.io.Writer;
				import java.io.FileNotFoundException;

				public class E1 {
				    void method(String filename) {
				        try {
				            Writer w=new PrintWriter(filename);
				            } catch (FileNotFoundException e) {
				            e.printStackTrace();
				            }
				       }
				    }
				}
				""",

				"""
						package test1;

						import java.io.PrintWriter;
						import java.io.Writer;
						import java.nio.charset.Charset;
						import java.io.BufferedWriter;
						import java.io.FileNotFoundException;
						import java.io.FileOutputStream;
						import java.io.OutputStreamWriter;

						public class E1 {
						    void method(String filename) {
						        try {
						            Writer w=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), Charset.defaultCharset()));
						            } catch (FileNotFoundException e) {
						            e.printStackTrace();
						            }
						       }
						    }
						}
						"""),
		STRINGGETBYTES(
"""
package test1;

import java.nio.charset.StandardCharsets;

public class E1 {

    // Methode 1: Verwendung von StandardCharsets.UTF_8 statt "UTF-8" als String-Literal
    void method(String filename) {
        String s = "asdf"; //$NON-NLS-1$

        // Vorher: getBytes ohne Angabe der Kodierung (verwendet die Plattform-spezifische Standard-Kodierung)
        byte[] bytes = s.getBytes();

        // Nachher: Umstellung auf StandardCharsets.UTF_8
        byte[] bytes2 = s.getBytes(StandardCharsets.UTF_8);

        System.out.println(bytes.length);
        System.out.println(bytes2.length);
    }

    // Methode 2: Behandlung von getBytes mit einer expliziten Kodierung
    void method2(String filename) {
        String s = "asdf"; //$NON-NLS-1$

        // Vorher: getBytes mit expliziter Kodierung (UTF-8 als String-Literal)
        byte[] bytes = s.getBytes("UTF-8");

        // Nachher: Umstellung auf StandardCharsets.UTF_8
        byte[] bytes2 = s.getBytes(StandardCharsets.UTF_8);

        System.out.println(bytes.length);
        System.out.println(bytes2.length);
    }

    // Erweiterter Testfall: Verwendung von verschiedenen Kodierungen
    void methodWithDifferentEncodings(String filename) {
        String s = "asdf";

        // Testen von gängigen Kodierungen
        byte[] bytes1 = s.getBytes("ISO-8859-1");  // ISO-8859-1
        byte[] bytes2 = s.getBytes("US-ASCII");    // US-ASCII
        byte[] bytes3 = s.getBytes(StandardCharsets.UTF_8);  // UTF-8 mit StandardCharsets
        byte[] bytes4 = s.getBytes("UTF-16");      // UTF-16

        System.out.println(bytes1.length); // Ausgabe der Längen
        System.out.println(bytes2.length);
        System.out.println(bytes3.length);
        System.out.println(bytes4.length);
    }

    // Testfall: Verwendung von getBytes mit einer ungültigen Kodierung (sollte im Cleanup behandelt werden)
    void methodWithInvalidEncoding(String filename) {
        String s = "asdf";
        try {
            // Ungültige Kodierung, die zu UnsupportedEncodingException führt
            byte[] bytes = s.getBytes("non-existing-encoding");
        } catch (UnsupportedEncodingException e) {
            // Diese Ausnahme sollte im Cleanup berücksichtigt werden
            e.printStackTrace();
        }
    }

    // Testfall: Verwendung von getBytes mit einer durch Variable angegebenen Kodierung
    void methodWithVariableEncoding(String filename) {
        String s = "asdf";
        String encoding = "UTF-8";  // Kodierung als Variable
        try {
            byte[] bytes = s.getBytes(encoding);  // Kodierung aus der Variablen
            System.out.println(bytes.length);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
""",

"""
package test1;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class E1 {

    // Methode 1: Verwendung von StandardCharsets.UTF_8 statt "UTF-8" als String-Literal
    void method(String filename) {
        String s = "asdf"; //$NON-NLS-1$

        // Vorher: getBytes ohne Angabe der Kodierung (verwendet die Plattform-spezifische Standard-Kodierung)
        byte[] bytes = s.getBytes(Charset.defaultCharset());

        // Nachher: Umstellung auf StandardCharsets.UTF_8
        byte[] bytes2 = s.getBytes(StandardCharsets.UTF_8);

        System.out.println(bytes.length);
        System.out.println(bytes2.length);
    }

    // Methode 2: Behandlung von getBytes mit einer expliziten Kodierung
    void method2(String filename) {
        String s = "asdf"; //$NON-NLS-1$

        // Vorher: getBytes mit expliziter Kodierung (UTF-8 als String-Literal)
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);

        // Nachher: Umstellung auf StandardCharsets.UTF_8
        byte[] bytes2 = s.getBytes(StandardCharsets.UTF_8);

        System.out.println(bytes.length);
        System.out.println(bytes2.length);
    }

    // Erweiterter Testfall: Verwendung von verschiedenen Kodierungen
    void methodWithDifferentEncodings(String filename) {
        String s = "asdf";

        // Testen von gängigen Kodierungen
        byte[] bytes1 = s.getBytes(StandardCharsets.ISO_8859_1);  // ISO-8859-1
        byte[] bytes2 = s.getBytes(StandardCharsets.US_ASCII);    // US-ASCII
        byte[] bytes3 = s.getBytes(StandardCharsets.UTF_8);  // UTF-8 mit StandardCharsets
        byte[] bytes4 = s.getBytes(StandardCharsets.UTF_16);      // UTF-16

        System.out.println(bytes1.length); // Ausgabe der Längen
        System.out.println(bytes2.length);
        System.out.println(bytes3.length);
        System.out.println(bytes4.length);
    }

    // Testfall: Verwendung von getBytes mit einer ungültigen Kodierung (sollte im Cleanup behandelt werden)
    void methodWithInvalidEncoding(String filename) {
        String s = "asdf";
        try {
            // Ungültige Kodierung, die zu UnsupportedEncodingException führt
            byte[] bytes = s.getBytes("non-existing-encoding");
        } catch (UnsupportedEncodingException e) {
            // Diese Ausnahme sollte im Cleanup berücksichtigt werden
            e.printStackTrace();
        }
    }

    // Testfall: Verwendung von getBytes mit einer durch Variable angegebenen Kodierung
    void methodWithVariableEncoding(String filename) {
        String s = "asdf";
        String encoding = "UTF-8";  // Kodierung als Variable
        try {
            byte[] bytes = s.getBytes(encoding);  // Kodierung aus der Variablen
            System.out.println(bytes.length);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
"""),
		STRING(
"""
package test1;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class E1 {

    static void bla(String filename) throws FileNotFoundException, UnsupportedEncodingException {
        byte[] b = {(byte) 59};

        // Fälle mit String Encoding als "UTF-8" (soll durch StandardCharsets.UTF_8 ersetzt werden)
        String s1 = new String(b, "UTF-8"); // "UTF-8" als String-Literal
        String s2 = new String(b, 0, 1, "UTF-8"); // "UTF-8" als String-Literal

        // Fall mit ISO-8859-1 Encoding (soll durch StandardCharsets.ISO_8859_1 ersetzt werden)
        String s3 = new String(b, "ISO-8859-1"); // "ISO-8859-1" als String-Literal
        String s4 = new String(b, 0, 1, "ISO-8859-1"); // "ISO-8859-1" als String-Literal

        // Fall mit US-ASCII Encoding (soll durch StandardCharsets.US_ASCII ersetzt werden)
        String s5 = new String(b, "US-ASCII"); // "US-ASCII" als String-Literal
        String s6 = new String(b, 0, 1, "US-ASCII"); // "US-ASCII" als String-Literal

        // Fall mit UTF-16 Encoding (soll durch StandardCharsets.UTF_16 ersetzt werden)
        String s7 = new String(b, "UTF-16"); // "UTF-16" als String-Literal
        String s8 = new String(b, 0, 1, "UTF-16"); // "UTF-16" als String-Literal

        // Fall mit einer benutzerdefinierten Konstante für Encoding, bleibt unverändert
        String s9 = new String(b, "UTF-8"); // bleibt unverändert
        String s10 = new String(b, 0, 1, "UTF-8"); // bleibt unverändert

        // Fälle ohne Entsprechung in StandardCharsets, bleiben unverändert
        String s11 = new String(b, "windows-1252"); // bleibt unverändert
        String s12 = new String(b, 0, 1, "windows-1252"); // bleibt unverändert
        String s13 = new String(b, "Shift_JIS"); // bleibt unverändert
        String s14 = new String(b, 0, 1, "Shift_JIS"); // bleibt unverändert

        // Fall mit Charset.forName() (wird unverändert bleiben, keine Ersetzung möglich)
        Charset charset = Charset.forName("UTF-16");
        String s15 = new String(b, charset); // bleibt unverändert
        String s16 = new String(b, 0, 1, charset); // bleibt unverändert

        // Fälle, die eine UnsupportedEncodingException werfen (werden im Cleanup angepasst)
        try {
            String s17 = new String(b, "non-existing-encoding"); // könnte UnsupportedEncodingException werfen
        } catch (UnsupportedEncodingException e) {
            // UnsupportedEncodingException wird geworfen und abgefangen
            e.printStackTrace();
        }

        try {
            String s18 = new String(b, 0, 1, "non-existing-encoding"); // könnte UnsupportedEncodingException werfen
        } catch (UnsupportedEncodingException e) {
            // UnsupportedEncodingException wird geworfen und abgefangen
            e.printStackTrace();
        }
    }

    // Methodendeklaration mit throws für UnsupportedEncodingException (wird im Cleanup angepasst)
    static void methodWithThrows(String filename) throws UnsupportedEncodingException {
        byte[] b = {(byte) 59};
        String s1 = new String(b, "non-existing-encoding"); // wirft UnsupportedEncodingException
    }

    // Nach dem Cleanup sollte dies keine UnsupportedEncodingException mehr werfen
    static void methodWithThrowsChange(String filename) throws FileNotFoundException {
        byte[] b = {(byte) 59};
        String s1 = new String(b, "UTF-8"); // wirft keine UnsupportedEncodingException mehr
    }

    // Methodendeklaration mit try-catch für UnsupportedEncodingException (wird im Cleanup angepasst)
    static void methodWithCatch(String filename) {
        byte[] b = {(byte) 59};
        try {
            String s1 = new String(b, "non-existing-encoding"); // könnte UnsupportedEncodingException werfen
        } catch (UnsupportedEncodingException e) {
            // UnsupportedEncodingException wird geworfen und abgefangen
            e.printStackTrace();
        }
    }

    // Nach dem Cleanup wird keine UnsupportedEncodingException mehr abgefangen
    static void methodWithCatchChange(String filename) {
        byte[] b = {(byte) 59};
        try {
            String s1 = new String(b, "UTF-8"); // keine UnsupportedEncodingException
        } catch (UnsupportedEncodingException e) {
            // Dieser Block wird nicht mehr erreicht, da keine UnsupportedEncodingException mehr geworfen wird
            e.printStackTrace();
        }
    }
}
""",

"""
package test1;

import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class E1 {

    static void bla(String filename) throws FileNotFoundException {
        byte[] b = {(byte) 59};

        // Fälle mit String Encoding als "UTF-8" (soll durch StandardCharsets.UTF_8 ersetzt werden)
        String s1 = new String(b, StandardCharsets.UTF_8); // "UTF-8" als String-Literal
        String s2 = new String(b, 0, 1, StandardCharsets.UTF_8); // "UTF-8" als String-Literal

        // Fall mit ISO-8859-1 Encoding (soll durch StandardCharsets.ISO_8859_1 ersetzt werden)
        String s3 = new String(b, StandardCharsets.ISO_8859_1); // "ISO-8859-1" als String-Literal
        String s4 = new String(b, 0, 1, StandardCharsets.ISO_8859_1); // "ISO-8859-1" als String-Literal

        // Fall mit US-ASCII Encoding (soll durch StandardCharsets.US_ASCII ersetzt werden)
        String s5 = new String(b, StandardCharsets.US_ASCII); // "US-ASCII" als String-Literal
        String s6 = new String(b, 0, 1, StandardCharsets.US_ASCII); // "US-ASCII" als String-Literal

        // Fall mit UTF-16 Encoding (soll durch StandardCharsets.UTF_16 ersetzt werden)
        String s7 = new String(b, StandardCharsets.UTF_16); // "UTF-16" als String-Literal
        String s8 = new String(b, 0, 1, StandardCharsets.UTF_16); // "UTF-16" als String-Literal

        // Fall mit einer benutzerdefinierten Konstante für Encoding, bleibt unverändert
        String s9 = new String(b, StandardCharsets.UTF_8); // bleibt unverändert
        String s10 = new String(b, 0, 1, StandardCharsets.UTF_8); // bleibt unverändert

        // Fälle ohne Entsprechung in StandardCharsets, bleiben unverändert
        String s11 = new String(b, "windows-1252"); // bleibt unverändert
        String s12 = new String(b, 0, 1, "windows-1252"); // bleibt unverändert
        String s13 = new String(b, "Shift_JIS"); // bleibt unverändert
        String s14 = new String(b, 0, 1, "Shift_JIS"); // bleibt unverändert

        // Fall mit Charset.forName() (wird unverändert bleiben, keine Ersetzung möglich)
        Charset charset = Charset.forName("UTF-16");
        String s15 = new String(b, charset); // bleibt unverändert
        String s16 = new String(b, 0, 1, charset); // bleibt unverändert

        // Fälle, die eine UnsupportedEncodingException werfen (werden im Cleanup angepasst)
        try {
            String s17 = new String(b, "non-existing-encoding"); // könnte UnsupportedEncodingException werfen
        } catch (UnsupportedEncodingException e) {
            // UnsupportedEncodingException wird geworfen und abgefangen
            e.printStackTrace();
        }

        try {
            String s18 = new String(b, 0, 1, "non-existing-encoding"); // könnte UnsupportedEncodingException werfen
        } catch (UnsupportedEncodingException e) {
            // UnsupportedEncodingException wird geworfen und abgefangen
            e.printStackTrace();
        }
    }

    // Methodendeklaration mit throws für UnsupportedEncodingException (wird im Cleanup angepasst)
    static void methodWithThrows(String filename) throws UnsupportedEncodingException {
        byte[] b = {(byte) 59};
        String s1 = new String(b, "non-existing-encoding"); // wirft UnsupportedEncodingException
    }

    // Nach dem Cleanup sollte dies keine UnsupportedEncodingException mehr werfen
    static void methodWithThrowsChange(String filename) throws FileNotFoundException {
        byte[] b = {(byte) 59};
        String s1 = new String(b, StandardCharsets.UTF_8); // wirft keine UnsupportedEncodingException mehr
    }

    // Methodendeklaration mit try-catch für UnsupportedEncodingException (wird im Cleanup angepasst)
    static void methodWithCatch(String filename) {
        byte[] b = {(byte) 59};
        try {
            String s1 = new String(b, "non-existing-encoding"); // könnte UnsupportedEncodingException werfen
        } catch (UnsupportedEncodingException e) {
            // UnsupportedEncodingException wird geworfen und abgefangen
            e.printStackTrace();
        }
    }

    // Nach dem Cleanup wird keine UnsupportedEncodingException mehr abgefangen
    static void methodWithCatchChange(String filename) {
        byte[] b = {(byte) 59};
        try {
            String s1 = new String(b, StandardCharsets.UTF_8); // keine UnsupportedEncodingException
        }
    }
}
"""),
		PROPERTIESSTORETOXML(
"""
package test1;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class E1 {
    private static final String ENCODING_UTF8 = "UTF-8"; // Benutzerdefinierte Konstante für Encoding
    private String encodingVar = "ISO-8859-1"; // Encoding-Variable

    // Fall 1: UTF-8 als String; Cleanup soll zu StandardCharsets.UTF_8 ändern
    void storeWithTryWithResources() throws IOException {
        Properties p = new Properties();
        try (FileOutputStream os = new FileOutputStream("out.xml")) {
            p.storeToXML(os, "Kommentar", "UTF-8");
        }
    }

    // Fall 2: Benutzerdefiniertes Encoding als Variable; Cleanup soll diesen Fall unverändert lassen
    void storeWithTryWithResourcesAndCustomEncoding() throws IOException {
        Properties p = new Properties();
        try (FileOutputStream os = new FileOutputStream("out.xml")) {
            p.storeToXML(os, "Kommentar", encodingVar);
        }
    }

    // Fall 3: Ungültiges Encoding als String; Cleanup soll auf StandardCharsets.UTF_8 ändern und den catch-Block entfernen
    void storeWithTryWithResourcesAndInvalidEncoding() {
        Properties p = new Properties();
        try (FileOutputStream os = new FileOutputStream("out.xml")) {
            p.storeToXML(os, "Kommentar", "non-existing-encoding");
        } catch (UnsupportedEncodingException e) {
            System.err.println("Unsupported encoding caught!");
        }
    }

    // Fall 4: FileOutputStream außerhalb des try-Blocks und UTF-8 als String; Cleanup soll auf StandardCharsets.UTF_8 ändern und den catch-Block entfernen
    void storeWithoutTryWithResources(String filename) throws IOException {
        Properties p = new Properties();
        FileOutputStream os = new FileOutputStream(filename);
        try {
            p.storeToXML(os, "Kommentar", "UTF-8");
        } catch (UnsupportedEncodingException e) {
            System.err.println("Unexpected UnsupportedEncodingException");
        } finally {
            os.close(); // Bleibt erhalten, um die Ressource korrekt zu schließen
        }
    }

    // Fall 5: Gültiges Encoding ohne Konstante in StandardCharsets (windows-1252); Cleanup soll diesen Fall unverändert lassen
    void storeWithWindows1252Encoding() throws IOException {
        Properties p = new Properties();
        try (FileOutputStream os = new FileOutputStream("out.xml")) {
            p.storeToXML(os, "Kommentar", "windows-1252");
        }
    }

    // Fall 6: Gültiges Encoding ohne Konstante in StandardCharsets (Shift_JIS); Cleanup soll diesen Fall unverändert lassen
    void storeWithShiftJISEncoding() throws IOException {
        Properties p = new Properties();
        try (FileOutputStream os = new FileOutputStream("out.xml")) {
            p.storeToXML(os, "Kommentar", "Shift_JIS");
        }
    }

    // Fall 7: Ungültiges Encoding außerhalb von try-with-resources; Cleanup soll auf StandardCharsets.UTF_8 ändern und den catch-Block entfernen
    void storeWithInvalidEncodingOutsideTry(String filename) throws IOException {
        Properties p = new Properties();
        FileOutputStream os = new FileOutputStream(filename);
        try {
            p.storeToXML(os, "Kommentar", "non-existing-encoding");
        } catch (UnsupportedEncodingException e) {
            System.err.println("Unsupported encoding caught!");
        } finally {
            os.close(); // Bleibt erhalten, um die Ressource korrekt zu schließen
        }
    }

    // Fall 8: StandardCharsets.UTF_8 ohne try-with-resources; Cleanup soll diesen Fall unverändert lassen
    void storeWithoutTryWithResourcesStandardCharsets(String filename) throws IOException {
        Properties p = new Properties();
        FileOutputStream os = new FileOutputStream(filename);
        try {
            p.storeToXML(os, "Kommentar", StandardCharsets.UTF_8);
        } finally {
            os.close(); // Bleibt erhalten, um die Ressource korrekt zu schließen
        }
    }
}
""",

"""
package test1;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class E1 {
    private static final String ENCODING_UTF8 = "UTF-8"; // Benutzerdefinierte Konstante für Encoding
    private String encodingVar = "ISO-8859-1"; // Encoding-Variable

    // Fall 1: UTF-8 als String; Cleanup soll zu StandardCharsets.UTF_8 ändern
    void storeWithTryWithResources() throws IOException {
        Properties p = new Properties();
        try (FileOutputStream os = new FileOutputStream("out.xml")) {
            p.storeToXML(os, "Kommentar", StandardCharsets.UTF_8);
        }
    }

    // Fall 2: Benutzerdefiniertes Encoding als Variable; Cleanup soll diesen Fall unverändert lassen
    void storeWithTryWithResourcesAndCustomEncoding() throws IOException {
        Properties p = new Properties();
        try (FileOutputStream os = new FileOutputStream("out.xml")) {
            p.storeToXML(os, "Kommentar", encodingVar);
        }
    }

    // Fall 3: Ungültiges Encoding als String; Cleanup soll auf StandardCharsets.UTF_8 ändern und den catch-Block entfernen
    void storeWithTryWithResourcesAndInvalidEncoding() {
        Properties p = new Properties();
        try (FileOutputStream os = new FileOutputStream("out.xml")) {
            p.storeToXML(os, "Kommentar", "non-existing-encoding");
        } catch (UnsupportedEncodingException e) {
            System.err.println("Unsupported encoding caught!");
        }
    }

    // Fall 4: FileOutputStream außerhalb des try-Blocks und UTF-8 als String; Cleanup soll auf StandardCharsets.UTF_8 ändern und den catch-Block entfernen
    void storeWithoutTryWithResources(String filename) throws IOException {
        Properties p = new Properties();
        FileOutputStream os = new FileOutputStream(filename);
        try {
            p.storeToXML(os, "Kommentar", StandardCharsets.UTF_8);
        } finally {
            os.close(); // Bleibt erhalten, um die Ressource korrekt zu schließen
        }
    }

    // Fall 5: Gültiges Encoding ohne Konstante in StandardCharsets (windows-1252); Cleanup soll diesen Fall unverändert lassen
    void storeWithWindows1252Encoding() throws IOException {
        Properties p = new Properties();
        try (FileOutputStream os = new FileOutputStream("out.xml")) {
            p.storeToXML(os, "Kommentar", "windows-1252");
        }
    }

    // Fall 6: Gültiges Encoding ohne Konstante in StandardCharsets (Shift_JIS); Cleanup soll diesen Fall unverändert lassen
    void storeWithShiftJISEncoding() throws IOException {
        Properties p = new Properties();
        try (FileOutputStream os = new FileOutputStream("out.xml")) {
            p.storeToXML(os, "Kommentar", "Shift_JIS");
        }
    }

    // Fall 7: Ungültiges Encoding außerhalb von try-with-resources; Cleanup soll auf StandardCharsets.UTF_8 ändern und den catch-Block entfernen
    void storeWithInvalidEncodingOutsideTry(String filename) throws IOException {
        Properties p = new Properties();
        FileOutputStream os = new FileOutputStream(filename);
        try {
            p.storeToXML(os, "Kommentar", "non-existing-encoding");
        } catch (UnsupportedEncodingException e) {
            System.err.println("Unsupported encoding caught!");
        } finally {
            os.close(); // Bleibt erhalten, um die Ressource korrekt zu schließen
        }
    }

    // Fall 8: StandardCharsets.UTF_8 ohne try-with-resources; Cleanup soll diesen Fall unverändert lassen
    void storeWithoutTryWithResourcesStandardCharsets(String filename) throws IOException {
        Properties p = new Properties();
        FileOutputStream os = new FileOutputStream(filename);
        try {
            p.storeToXML(os, "Kommentar", StandardCharsets.UTF_8);
        } finally {
            os.close(); // Bleibt erhalten, um die Ressource korrekt zu schließen
        }
    }
}
"""),
		URLDECODER(
"""
package test1;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class E2 {
    private static final String ENCODING_UTF8 = "UTF-8"; // Konstante für Encoding
    private String encodingVar = "ISO-8859-1"; // Variable für Encoding

    // Methode ohne Encoding-Angabe, bleibt unverändert
    static void decodeDefault() {
        String url = URLDecoder.decode("example");
    }

    // Methode, die "UTF-8" als String verwendet und UnsupportedEncodingException wirft
    static void decodeWithThrows() throws UnsupportedEncodingException {
        String url = URLDecoder.decode("example", "UTF-8"); // sollte in StandardCharsets.UTF_8 geändert werden
    }

    // Methode, die eine ungültige Kodierung verwendet und `UnsupportedEncodingException` wirft
    static void decodeWithInvalidEncodingThrows() throws UnsupportedEncodingException {
        String url = URLDecoder.decode("example", "non-existing-encoding");
    }

    // Methode, die eine benutzerdefinierte Konstante für Encoding verwendet, bleibt unverändert
    void decodeWithCustomConstant() throws UnsupportedEncodingException {
        String url = URLDecoder.decode("example", ENCODING_UTF8);
    }

    // Methode, die Encoding als Variable übergibt, bleibt unverändert
    void decodeWithVariableEncoding() throws UnsupportedEncodingException {
        String url = URLDecoder.decode("example", encodingVar);
    }

    // Methode mit `try-catch`-Block für ungültiges Encoding
    static void decodeWithTryCatch() {
        try {
            String url = URLDecoder.decode("example", "non-existing-encoding"); // könnte UnsupportedEncodingException werfen
        } catch (UnsupportedEncodingException e) {
            System.err.println("Caught UnsupportedEncodingException for invalid encoding!");
        }
    }

    // Beispiel mit StandardCharsets-Konstanten, bleibt unverändert
    static void decodeWithStandardCharset() {
        String url = URLDecoder.decode("example", StandardCharsets.UTF_8);
    }
}
""",
"""
package test1;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class E2 {
    private static final String ENCODING_UTF8 = "UTF-8"; // Konstante für Encoding
    private String encodingVar = "ISO-8859-1"; // Variable für Encoding

    // Methode ohne Encoding-Angabe, bleibt unverändert
    static void decodeDefault() {
        String url = URLDecoder.decode("example", Charset.defaultCharset());
    }

    // Methode, die "UTF-8" als String verwendet und UnsupportedEncodingException wirft
    static void decodeWithThrows() {
        String url = URLDecoder.decode("example", StandardCharsets.UTF_8); // sollte in StandardCharsets.UTF_8 geändert werden
    }

    // Methode, die eine ungültige Kodierung verwendet und `UnsupportedEncodingException` wirft
    static void decodeWithInvalidEncodingThrows() throws UnsupportedEncodingException {
        String url = URLDecoder.decode("example", "non-existing-encoding");
    }

    // Methode, die eine benutzerdefinierte Konstante für Encoding verwendet, bleibt unverändert
    void decodeWithCustomConstant() throws UnsupportedEncodingException {
        String url = URLDecoder.decode("example", ENCODING_UTF8);
    }

    // Methode, die Encoding als Variable übergibt, bleibt unverändert
    void decodeWithVariableEncoding() throws UnsupportedEncodingException {
        String url = URLDecoder.decode("example", encodingVar);
    }

    // Methode mit `try-catch`-Block für ungültiges Encoding
    static void decodeWithTryCatch() {
        try {
            String url = URLDecoder.decode("example", "non-existing-encoding"); // könnte UnsupportedEncodingException werfen
        } catch (UnsupportedEncodingException e) {
            System.err.println("Caught UnsupportedEncodingException for invalid encoding!");
        }
    }

    // Beispiel mit StandardCharsets-Konstanten, bleibt unverändert
    static void decodeWithStandardCharset() {
        String url = URLDecoder.decode("example", StandardCharsets.UTF_8);
    }
}
"""),
		URLENCODER(
"""
package test1;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class E1 {
    private static final String ENCODING_UTF8 = "UTF-8"; // Benutzerdefinierte Kodierungskonstante
    private String encodingVar = "ISO-8859-1"; // Variable für eine Kodierung

    // Methode ohne explizite Kodierung, bleibt unverändert
    static void encodeDefault() {
        String url = URLEncoder.encode("example");
    }

    // Methode, die "UTF-8" als String-Literal verwendet und `throws UnsupportedEncodingException` hat
    static void encodeWithThrows() throws UnsupportedEncodingException {
        String url = URLEncoder.encode("example", "UTF-8"); // sollte in StandardCharsets.UTF_8 geändert werden
    }

    // Methode, die eine ungültige Kodierung verwendet und `UnsupportedEncodingException` wirft
    static void encodeWithInvalidEncodingThrows() throws UnsupportedEncodingException {
        String url = URLEncoder.encode("example", "non-existing-encoding");
    }

    // Methode, die eine benutzerdefinierte Kodierungskonstante verwendet, bleibt unverändert
    void encodeWithCustomConstant() throws UnsupportedEncodingException {
        String url = URLEncoder.encode("example", ENCODING_UTF8);
    }

    // Methode, die eine Kodierungsvariable verwendet, bleibt unverändert
    void encodeWithVariableEncoding() throws UnsupportedEncodingException {
        String url = URLEncoder.encode("example", encodingVar);
    }

    // Methode mit `try-catch`-Block für eine ungültige Kodierung
    static void encodeWithTryCatch() {
        try {
            String url = URLEncoder.encode("example", "non-existing-encoding"); // könnte UnsupportedEncodingException werfen
        } catch (UnsupportedEncodingException e) {
            System.err.println("Caught UnsupportedEncodingException for invalid encoding!");
        }
    }

    // Beispiel mit StandardCharsets-Konstanten, bleibt unverändert
    static void encodeWithStandardCharset() {
        String url = URLEncoder.encode("example", StandardCharsets.UTF_8);
    }
}
""",
"""
package test1;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class E1 {
    private static final String ENCODING_UTF8 = "UTF-8"; // Benutzerdefinierte Kodierungskonstante
    private String encodingVar = "ISO-8859-1"; // Variable für eine Kodierung

    // Methode ohne explizite Kodierung, bleibt unverändert
    static void encodeDefault() {
        String url = URLEncoder.encode("example", Charset.defaultCharset());
    }

    // Methode, die "UTF-8" als String-Literal verwendet und `throws UnsupportedEncodingException` hat
    static void encodeWithThrows() {
        String url = URLEncoder.encode("example", StandardCharsets.UTF_8); // sollte in StandardCharsets.UTF_8 geändert werden
    }

    // Methode, die eine ungültige Kodierung verwendet und `UnsupportedEncodingException` wirft
    static void encodeWithInvalidEncodingThrows() throws UnsupportedEncodingException {
        String url = URLEncoder.encode("example", "non-existing-encoding");
    }

    // Methode, die eine benutzerdefinierte Kodierungskonstante verwendet, bleibt unverändert
    void encodeWithCustomConstant() throws UnsupportedEncodingException {
        String url = URLEncoder.encode("example", ENCODING_UTF8);
    }

    // Methode, die eine Kodierungsvariable verwendet, bleibt unverändert
    void encodeWithVariableEncoding() throws UnsupportedEncodingException {
        String url = URLEncoder.encode("example", encodingVar);
    }

    // Methode mit `try-catch`-Block für eine ungültige Kodierung
    static void encodeWithTryCatch() {
        try {
            String url = URLEncoder.encode("example", "non-existing-encoding"); // könnte UnsupportedEncodingException werfen
        } catch (UnsupportedEncodingException e) {
            System.err.println("Caught UnsupportedEncodingException for invalid encoding!");
        }
    }

    // Beispiel mit StandardCharsets-Konstanten, bleibt unverändert
    static void encodeWithStandardCharset() {
        String url = URLEncoder.encode("example", StandardCharsets.UTF_8);
    }
}
"""),
		SCANNER(
"""
package test1;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class E1 {

    // Methode mit File und explizitem "UTF-8" (wird durch StandardCharsets.UTF_8 ersetzt)
    static void bla3(File file) throws FileNotFoundException {
        // Konstruktor mit String-Encoding, sollte durch StandardCharsets.UTF_8 ersetzt werden
        Scanner s = new Scanner(file, "UTF-8");
    }

    // Methode mit InputStream und explizitem "UTF-8" (wird durch StandardCharsets.UTF_8 ersetzt)
    static void bla4(InputStream is) throws FileNotFoundException {
        Scanner s2 = new Scanner(is, "UTF-8");
    }

    // Methode mit Scanner, aber ohne explizites Encoding, bleibt unverändert
    static void bla5() {
        Scanner s3 = new Scanner("asdf");
    }

    // Methode, die eine benutzerdefinierte Konstante für die Kodierung verwendet (bleibt unverändert)
    private static final String ENCODING_UTF8 = "UTF-8";
    static void bla6(File file) throws FileNotFoundException {
        Scanner s = new Scanner(file, ENCODING_UTF8);
    }

    // Methode mit einer ungültigen Kodierung (muss UnsupportedEncodingException werfen)
    static void bla7(File file) throws FileNotFoundException {
        try {
            Scanner s = new Scanner(file, "non-existing-encoding"); // wirft UnsupportedEncodingException
        } catch (Exception e) {
            e.printStackTrace(); // Catch block für UnsupportedEncodingException
        }
    }

    // Methode mit Scanner und ungültiger Kodierung, die `throws UnsupportedEncodingException` wirft
    static void bla8(InputStream is) throws FileNotFoundException, UnsupportedEncodingException {
        Scanner s = new Scanner(is, "non-existing-encoding"); // könnte UnsupportedEncodingException werfen
    }

    // Methode, die eine ungültige Kodierung und ein try-catch verwendet (für FileNotFoundException)
    static void bla9(File file) {
        try {
            Scanner s = new Scanner(file, "non-existing-encoding"); // könnte UnsupportedEncodingException werfen
        } catch (FileNotFoundException e) {
            // Datei nicht gefunden, hier wird FileNotFoundException behandelt
            e.printStackTrace();
        } catch (Exception e) {
            // UnsupportedEncodingException wird hier abgefangen
            e.printStackTrace();
        }
    }

    // Beispiel mit StandardCharsets-Konstanten, die keine Änderung brauchen
    static void bla10(File file) {
        Scanner s = new Scanner(file, StandardCharsets.UTF_8);
    }

    // Beispiel mit Scanner und InputStream, ohne explizite Kodierung (bleibt unverändert)
    static void bla11(InputStream is) {
        Scanner s = new Scanner(is);
    }

    // Methode mit Scanner und einer benutzerdefinierten Kodierung als Variable (bleibt unverändert)
    private String encodingVar = "ISO-8859-1";
    static void bla12(InputStream is) throws FileNotFoundException {
        Scanner s = new Scanner(is, "ISO-8859-1");
    }
}
""",
"""
package test1;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class E1 {

    // Methode mit File und explizitem "UTF-8" (wird durch StandardCharsets.UTF_8 ersetzt)
    static void bla3(File file) throws FileNotFoundException {
        // Konstruktor mit String-Encoding, sollte durch StandardCharsets.UTF_8 ersetzt werden
        Scanner s = new Scanner(file, StandardCharsets.UTF_8);
    }

    // Methode mit InputStream und explizitem "UTF-8" (wird durch StandardCharsets.UTF_8 ersetzt)
    static void bla4(InputStream is) throws FileNotFoundException {
        Scanner s2 = new Scanner(is, StandardCharsets.UTF_8);
    }

    // Methode mit Scanner, aber ohne explizites Encoding, bleibt unverändert
    static void bla5() {
        Scanner s3 = new Scanner("asdf", Charset.defaultCharset());
    }

    // Methode, die eine benutzerdefinierte Konstante für die Kodierung verwendet (bleibt unverändert)
    private static final String ENCODING_UTF8 = "UTF-8";
    static void bla6(File file) throws FileNotFoundException {
        Scanner s = new Scanner(file, ENCODING_UTF8);
    }

    // Methode mit einer ungültigen Kodierung (muss UnsupportedEncodingException werfen)
    static void bla7(File file) throws FileNotFoundException {
        try {
            Scanner s = new Scanner(file, "non-existing-encoding"); // wirft UnsupportedEncodingException
        } catch (Exception e) {
            e.printStackTrace(); // Catch block für UnsupportedEncodingException
        }
    }

    // Methode mit Scanner und ungültiger Kodierung, die `throws UnsupportedEncodingException` wirft
    static void bla8(InputStream is) throws FileNotFoundException, UnsupportedEncodingException {
        Scanner s = new Scanner(is, "non-existing-encoding"); // könnte UnsupportedEncodingException werfen
    }

    // Methode, die eine ungültige Kodierung und ein try-catch verwendet (für FileNotFoundException)
    static void bla9(File file) {
        try {
            Scanner s = new Scanner(file, "non-existing-encoding"); // könnte UnsupportedEncodingException werfen
        } catch (FileNotFoundException e) {
            // Datei nicht gefunden, hier wird FileNotFoundException behandelt
            e.printStackTrace();
        } catch (Exception e) {
            // UnsupportedEncodingException wird hier abgefangen
            e.printStackTrace();
        }
    }

    // Beispiel mit StandardCharsets-Konstanten, die keine Änderung brauchen
    static void bla10(File file) {
        Scanner s = new Scanner(file, StandardCharsets.UTF_8);
    }

    // Beispiel mit Scanner und InputStream, ohne explizite Kodierung (bleibt unverändert)
    static void bla11(InputStream is) {
        Scanner s = new Scanner(is, Charset.defaultCharset());
    }

    // Methode mit Scanner und einer benutzerdefinierten Kodierung als Variable (bleibt unverändert)
    private String encodingVar = "ISO-8859-1";
    static void bla12(InputStream is) throws FileNotFoundException {
        Scanner s = new Scanner(is, StandardCharsets.ISO_8859_1);
    }
}
"""),
		FORMATTER(
"""
package test1;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Formatter;

public class E1 {

    // Methode mit explizitem UTF-8, sollte durch StandardCharsets.UTF_8 ersetzt werden
    static void bla() throws FileNotFoundException, UnsupportedEncodingException {
        Formatter s = new Formatter(new File("asdf"), "UTF-8"); // 'UTF-8' wird zu StandardCharsets.UTF_8
    }

    // Methode mit try-catch, die eine Kodierung verwendet und Fehler wirft
    static void bli() throws FileNotFoundException {
        try {
            Formatter s = new Formatter(new File("asdf"), "UTF-8"); // 'UTF-8' wird zu StandardCharsets.UTF_8
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            // Der Catch-Block für UnsupportedEncodingException sollte im Cleanup entfernt werden
            e.printStackTrace();
        }
    }

    // Methode mit benutzerdefinierter Konstante für das Encoding
    private static final String ENCODING_UTF8 = "UTF-8";

    static void blc() throws FileNotFoundException, UnsupportedEncodingException {
        Formatter s = new Formatter(new File("asdf"), ENCODING_UTF8); // 'UTF-8' als Konstante
    }

    // Methode mit einer ungültigen Kodierung (z.B. 'non-existing-encoding')
    static void bld() throws FileNotFoundException {
        try {
            Formatter s = new Formatter(new File("asdf"), "non-existing-encoding"); // wirft UnsupportedEncodingException
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace(); // UnsupportedEncodingException wird hier erwartet
        }
    }

    // Methode, die eine ungültige Kodierung und ein try-catch verwendet
    static void ble() throws FileNotFoundException {
        try {
            Formatter s = new Formatter(new File("asdf"), "non-existing-encoding"); // könnte UnsupportedEncodingException werfen
        } catch (FileNotFoundException e) {
            // Datei nicht gefunden, hier wird FileNotFoundException behandelt
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            // UnsupportedEncodingException wird hier behandelt
            e.printStackTrace();
        }
    }

    // Methode mit StandardCharsets.UTF_8
    static void blf() throws FileNotFoundException {
        Formatter s = new Formatter(new File("asdf"), StandardCharsets.UTF_8); // Verwendung von StandardCharsets.UTF_8
    }

    // Beispiel, bei dem das Encoding in einer Variablen gespeichert ist
    private String encodingVar = "UTF-8";

    static void blg() throws FileNotFoundException {
        String encoding = "UTF-8";
        Formatter s = new Formatter(new File("asdf"), encoding); // encoding als Variable
    }
}
""", """
package test1;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.Formatter;

public class E1 {

    // Methode mit explizitem UTF-8, sollte durch StandardCharsets.UTF_8 ersetzt werden
    static void bla() throws FileNotFoundException {
        Formatter s = new Formatter(new File("asdf"), StandardCharsets.UTF_8); // 'UTF-8' wird zu StandardCharsets.UTF_8
    }

    // Methode mit try-catch, die eine Kodierung verwendet und Fehler wirft
    static void bli() throws FileNotFoundException {
        try {
            Formatter s = new Formatter(new File("asdf"), StandardCharsets.UTF_8); // 'UTF-8' wird zu StandardCharsets.UTF_8
        } catch (FileNotFoundException e) {
            // Der Catch-Block für UnsupportedEncodingException sollte im Cleanup entfernt werden
            e.printStackTrace();
        }
    }

    // Methode mit benutzerdefinierter Konstante für das Encoding
    private static final String ENCODING_UTF8 = "UTF-8";

    static void blc() throws FileNotFoundException, UnsupportedEncodingException {
        Formatter s = new Formatter(new File("asdf"), ENCODING_UTF8); // 'UTF-8' als Konstante
    }

    // Methode mit einer ungültigen Kodierung (z.B. 'non-existing-encoding')
    static void bld() throws FileNotFoundException {
        try {
            Formatter s = new Formatter(new File("asdf"), "non-existing-encoding"); // wirft UnsupportedEncodingException
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace(); // UnsupportedEncodingException wird hier erwartet
        }
    }

    // Methode, die eine ungültige Kodierung und ein try-catch verwendet
    static void ble() throws FileNotFoundException {
        try {
            Formatter s = new Formatter(new File("asdf"), "non-existing-encoding"); // könnte UnsupportedEncodingException werfen
        } catch (FileNotFoundException e) {
            // Datei nicht gefunden, hier wird FileNotFoundException behandelt
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            // UnsupportedEncodingException wird hier behandelt
            e.printStackTrace();
        }
    }

    // Methode mit StandardCharsets.UTF_8
    static void blf() throws FileNotFoundException {
        Formatter s = new Formatter(new File("asdf"), StandardCharsets.UTF_8); // Verwendung von StandardCharsets.UTF_8
    }

    // Beispiel, bei dem das Encoding in einer Variablen gespeichert ist
    private String encodingVar = "UTF-8";

    static void blg() throws FileNotFoundException {
        String encoding = "UTF-8";
        Formatter s = new Formatter(new File("asdf"), encoding); // encoding als Variable
    }
}
"""),
		THREE("""
				package test1;

				import java.io.ByteArrayOutputStream;
				import java.io.InputStreamReader;
				import java.io.FileInputStream;
				import java.io.FileReader;
				import java.io.Reader;
				import java.io.FileNotFoundException;

				public class E1 {
				    void method(String filename) {
				        String s="asdf"; //$NON-NLS-1$
				        byte[] bytes= s.getBytes();
				        System.out.println(bytes.length);
				        ByteArrayOutputStream ba=new ByteArrayOutputStream();
				        String result=ba.toString();
				        try {
				            InputStreamReader is=new InputStreamReader(new FileInputStream("")); //$NON-NLS-1$
				            } catch (FileNotFoundException e) {
				            e.printStackTrace();
				            }
				        try {
				            OutputStreamWriter os=new OutputStreamWriter(new FileOutputStream("")); //$NON-NLS-1$
				            } catch (FileNotFoundException e) {
				            e.printStackTrace();
				            }
				        try {
				            Reader is=new FileReader(filename);
				            } catch (FileNotFoundException e) {
				            e.printStackTrace();
				            }
				       }
				    }
				}
				""",

				"""
						package test1;

						import java.io.ByteArrayOutputStream;
						import java.io.InputStreamReader;
						import java.io.FileInputStream;
						import java.io.FileReader;
						import java.io.Reader;
						import java.nio.charset.Charset;
						import java.io.FileNotFoundException;

						public class E1 {
						    void method(String filename) {
						        String s="asdf"; //$NON-NLS-1$
						        byte[] bytes= s.getBytes(Charset.defaultCharset());
						        System.out.println(bytes.length);
						        ByteArrayOutputStream ba=new ByteArrayOutputStream();
						        String result=ba.toString(Charset.defaultCharset());
						        try {
						            InputStreamReader is=new InputStreamReader(new FileInputStream(""), Charset.defaultCharset()); //$NON-NLS-1$
						            } catch (FileNotFoundException e) {
						            e.printStackTrace();
						            }
						        try {
						            OutputStreamWriter os=new OutputStreamWriter(new FileOutputStream(""), Charset.defaultCharset()); //$NON-NLS-1$
						            } catch (FileNotFoundException e) {
						            e.printStackTrace();
						            }
						        try {
						            Reader is=new InputStreamReader(new FileInputStream(filename), Charset.defaultCharset());
						            } catch (FileNotFoundException e) {
						            e.printStackTrace();
						            }
						       }
						    }
						}
						"""),
		ENCODINGASSTRINGPARAMETER(
				"""
						package test1;

						import java.io.ByteArrayOutputStream;
						import java.io.InputStreamReader;
						import java.io.FileInputStream;
						import java.io.FileReader;
						import java.io.Reader;
						import java.io.FileNotFoundException;

						public class E1 {
						    void method(String filename) {
						        String s="asdf"; //$NON-NLS-1$
						        //byte[] bytes= s.getBytes(StandardCharsets.UTF_8);
						        byte[] bytes= s.getBytes("Utf-8");
						        System.out.println(bytes.length);
						        ByteArrayOutputStream ba=new ByteArrayOutputStream();
						        String result=ba.toString();
						        try {
						            InputStreamReader is=new InputStreamReader(new FileInputStream(""), "UTF-8"); //$NON-NLS-1$
						            } catch (FileNotFoundException e) {
						            e.printStackTrace();
						            }
						        try {
						            OutputStreamWriter os=new OutputStreamWriter(new FileOutputStream(""), "UTF-8"); //$NON-NLS-1$
						            } catch (FileNotFoundException e) {
						            e.printStackTrace();
						            }
						        try {
						            Reader is=new FileReader(filename);
						            } catch (FileNotFoundException e) {
						            e.printStackTrace();
						            }
						       }
						    }
						}
						""",

				"""
						package test1;

						import java.io.ByteArrayOutputStream;
						import java.io.InputStreamReader;
						import java.io.FileInputStream;
						import java.io.FileReader;
						import java.io.Reader;
						import java.nio.charset.Charset;
						import java.nio.charset.StandardCharsets;
						import java.io.FileNotFoundException;

						public class E1 {
						    void method(String filename) {
						        String s="asdf"; //$NON-NLS-1$
						        //byte[] bytes= s.getBytes(StandardCharsets.UTF_8);
						        byte[] bytes= s.getBytes(StandardCharsets.UTF_8);
						        System.out.println(bytes.length);
						        ByteArrayOutputStream ba=new ByteArrayOutputStream();
						        String result=ba.toString(Charset.defaultCharset());
						        try {
						            InputStreamReader is=new InputStreamReader(new FileInputStream(""), StandardCharsets.UTF_8); //$NON-NLS-1$
						            } catch (FileNotFoundException e) {
						            e.printStackTrace();
						            }
						        try {
						            OutputStreamWriter os=new OutputStreamWriter(new FileOutputStream(""), StandardCharsets.UTF_8); //$NON-NLS-1$
						            } catch (FileNotFoundException e) {
						            e.printStackTrace();
						            }
						        try {
						            Reader is=new InputStreamReader(new FileInputStream(filename), Charset.defaultCharset());
						            } catch (FileNotFoundException e) {
						            e.printStackTrace();
						            }
						       }
						    }
						}
						""");

		String given;
		String expected;

		ExplicitEncodingPatternsKeepBehavior(String given, String expected) {
			this.given= given;
			this.expected= expected;
		}
	}