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

The intent is that participants describe aspects of a search process, which can span across several participants (the search scope 
determines how many participants are involved). Participants are responsible for remember their index file locations, but not their
indexes per se which aren't in the API directly. Each participant would have its own set of indexes. If JSP participant wants to record
Java references into its index, then it would use services of a Java indexer, but targeting its own JSP index (implicitly).

What isn't addressed yet:
- no way to perform an index search only. Search will always go through match locator. (AND/OR combiners are only retaining
path information which is all required by match locators).
This is problematic to provide an efficient equivalent to search all type names.

