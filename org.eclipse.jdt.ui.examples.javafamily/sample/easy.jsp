<%@ page contentType="text/html" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<html>
  <head>
    <title>JSP is Easy</title>
  </head>
  <body bgcolor="white">
  <jsp:useBean id="clock" class="Date2" />
 
  <%! int globalCounter= 0; %> 
 
    <h1>JSP is as easy as ...</h1>

    <%-- Calculate the sum of 1 + 2 + 3 dynamically --%>
    1 + 2 + 3 = <c:out value="${1 + 2 + 3}" />
    
    <% int i= 4;
       i= i+1; %>
       
	<% if (clock.getHours() < 12) { %>
		Good morning!
	<% } else if (clock.getHours() < 17) { %>
		Good day!
	<% } else { %>
		Good evening!
	<% } %>
 
  </body>
</html>
