This source folder contains early thoughts about an API for generic search plug-in org.eclipse.search.core.

It is not functional yet.

The Java contribution would be implemented this API, and be available as an API on its own, so that it could be further 
reused by clients belonging to the Java family.

The API should in particular allow clients to:
- index their source by converting it into Java equivalent, and feeding it to the Java indexer
- index their source by parsing it themselves, but record Java index entries
- locate matches in their source by converting it into Java equivalent, and feeding it to the Java match locator
- locate matches in their source by matching themselves, and return Java matches

A match is supposed to be quite generic, and Java participant will provide a set of Java specific matches (think method decl
for instance).

A search query is quite generic, the Java participant will provide specific queries (think of search for method reference for instance).
Two combiners are existing: AND and OR query. These cannot be extended, however clients may contribute their own 
specific atomic queries, which may or may not be recognized by other tools (depending on the query implementation).


