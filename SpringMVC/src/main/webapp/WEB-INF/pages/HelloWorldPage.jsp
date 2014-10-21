<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<html>
<body>
	<h1>Spring MVC Hello World Example</h1>

	<h2>${msg}</h2>
	
	<p>
	User in Scope: ${user.name}
	</p>
	
	<p>
	<c:out value="This is a simple text"/>
	</p>

</body>
</html>