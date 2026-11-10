This folder holds the one external dependency of the project.

  mysql-connector-j-9.4.0.jar   - the official MySQL JDBC driver

It is not committed to git. If it is ever missing, download it again from
Maven Central:

  https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/9.4.0/mysql-connector-j-9.4.0.jar

and check that it is the genuine file before using it:

  SHA-1  8f6c66269048fbd2316b7b45d75898ebee44986f

The version matches MySQL Server 9.4. The driver is only needed on the
classpath; build.cmd and run.cmd already put it there.
