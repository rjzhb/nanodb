@echo off

set NANODB_SERVER_JAR=target\nanodb-server-0.1-SNAPSHOT.jar

rem Make sure the NanoDB server JAR file has been built
if not exist %NANODB_SERVER_JAR% (
    echo Can't find NanoDB server JAR file.  Run 'mvn package' to build the project.
    exit /b 1
)

set JAVA_OPTS=-Dlog4j.configurationFile=log4j2.properties

rem Server properties can be specified as system-property arguments.  Examples:
rem  - To change the default page-size to use, add -Dnanodb.pagesize=2048
rem  - To enable transaction processing, add -Dnanodb.enableTransactions=on
rem set JAVA_OPTS=%JAVA_OPTS% -Dnanodb.enableTransactions=on

rem To enable connection to a running server via the IntelliJ IDEA debugger,
rem uncomment this line:
rem set JAVA_OPTS=%JAVA_OPTS% -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5009

java %JAVA_OPTS% %* -jar %NANODB_SERVER_JAR%
