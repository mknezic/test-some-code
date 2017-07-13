<!DOCTYPE html>
<head>
  <title>Sonar violations report</title>
  <style type="text/css">
    body
    {
      margin: 0px;
      padding: 15px;
    }

    body, td, th
    {
      font-family: "Lucida Grande", "Lucida Sans Unicode", Helvetica, Arial, Tahoma, sans-serif;
      font-size: 10pt;
    }

    table
    {
      border-collapse: collapse;
      width: 100%
    }

    th
    {
      text-align: left;
    }

    h1
    {
      margin-top: 0px;
    }

    li
    {
      line-height: 15pt;
    }

    table.source-incut
    {
      background-color: #EEE;
      border: 1px solid #DDD;
    }

    table.source-incut td
    {
      white-space: pre;
      padding: 3px;
      font-family: "Lucida Console", "Courier New";
      font-size: 9pt;
    }

    .priority-blocker
    {
      color: #A22;
    }

    .priority-critical
    {
      color: #A42;
    }

    .priority-major
    {
      color: #A62;
    }

    .priority-minor
    {
      color: #4A2;
    }

    .priority-info
    {
      color: #2A2;
    }

    .line-number
    {
      color: #777;
      width: 20px;
    }

    .res-name
    {
      color: #363;
    }

    .source
    {
      color: #336;
    }

    .error
    {
      background-color: #FCC;
    }
  </style>
</head>
<body>

<h1>Sonar violations report</h1>

<%

	if (violationsCount > 0) { %>
		<p>Found ${violationsCount} new violations in ${filesCount} files.</p>
		<% userMappedViolations.each { authors, mappedViolations -> %>
			<h2>Violations by ${authors}</h2>
			<ol>
			<% mappedViolations.each { mappedViolation -> %>
				<li>
					<p>[<b><span class="priority-${mappedViolation.violation.priority.toLowerCase()}">${mappedViolation.violation.priority}</span></b>] <b>${mappedViolation.violation.rule}</b>. ${mappedViolation.violation.message}<br />
					in resource <span class="res-name">${mappedViolation.resKey}</span> at line ${mappedViolation.violation.lineNum}
					(<a href="${webSonarRoot}/drilldown/measures/${mappedViolation.resKey}?metric=new_violations#L${mappedViolation.violation.lineNum}">view in Sonar</a>):</p>
					<% if (mappedViolation.source) { %>
					<table class="source-incut">
						<% mappedViolation.source.each { lineNum, line -> %>
							<tr<% if (lineNum == mappedViolation.violation.lineNum) { %> class="error"<% } %>>
								<td class="line-number">${lineNum}</td><td class="source">${line.replaceAll('\t', '  ')}</td>
							</tr>
						<% } %>
					</table>
					<% } %>
				</li>
			<% } %>
			</ol>
		<% } %>
	<% } else { %>
		<p>No new violations! Woot!!!</p>
	<% } %>

%>

</body>
