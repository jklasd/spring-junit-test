package com.github.jklasd.test.core.common.methodann.mock.h2;

import java.io.IOException;
import java.io.LineNumberReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.CannotReadScriptException;
import org.springframework.jdbc.datasource.init.ScriptException;
import org.springframework.jdbc.datasource.init.ScriptStatementFailedException;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.jdbc.datasource.init.UncategorizedScriptException;
import org.springframework.util.StringUtils;

public class ScriptUtilsExt extends ScriptUtils {
	
	private static final Log logger = LogFactory.getLog(ScriptUtils.class);
	
	private static String readScript(EncodedResource resource, String commentPrefix, String separator)
			throws IOException {

		LineNumberReader lnr = new LineNumberReader(resource.getReader());
		try {
			return readScript(lnr, commentPrefix, separator);
		}
		finally {
			lnr.close();
		}
	}
	public static void executeSqlScript(Connection connection, EncodedResource resource) throws ScriptException {
		executeSqlScript(connection, resource, false, false, DEFAULT_COMMENT_PREFIX, DEFAULT_STATEMENT_SEPARATOR,
				DEFAULT_BLOCK_COMMENT_START_DELIMITER, DEFAULT_BLOCK_COMMENT_END_DELIMITER);
	}
	
	public static void executeSqlScript(Connection connection, EncodedResource resource, boolean continueOnError,
			boolean ignoreFailedDrops, String commentPrefix, String separator, String blockCommentStartDelimiter,
			String blockCommentEndDelimiter) throws ScriptException {

		try {
			if (logger.isInfoEnabled()) {
				logger.info("Executing SQL script from " + resource);
			}
			long startTime = System.currentTimeMillis();

			String script;
			try {
				script = readScript(resource, commentPrefix, separator);
			}
			catch (IOException ex) {
				throw new CannotReadScriptException(resource, ex);
			}

			if (separator == null) {
				separator = DEFAULT_STATEMENT_SEPARATOR;
			}
			if (!EOF_STATEMENT_SEPARATOR.equals(separator) && !containsSqlScriptDelimiters(script, separator)) {
				separator = FALLBACK_STATEMENT_SEPARATOR;
			}

			List<String> statements = new LinkedList<String>();
			splitSqlScript(resource, script, separator, commentPrefix, blockCommentStartDelimiter,
					blockCommentEndDelimiter, statements);

			int stmtNumber = 0;
			Statement stmt = connection.createStatement();
			try {
				for (String statement : statements) {
					stmtNumber++;
					try {
						stmt.execute(statement);
						int rowsAffected = stmt.getUpdateCount();
						if (logger.isDebugEnabled()) {
							logger.debug(rowsAffected + " returned as update count for SQL: " + statement);
							SQLWarning warningToLog = stmt.getWarnings();
							while (warningToLog != null) {
								logger.debug("SQLWarning ignored: SQL state '" + warningToLog.getSQLState() +
										"', error code '" + warningToLog.getErrorCode() +
										"', message [" + warningToLog.getMessage() + "]");
								warningToLog = warningToLog.getNextWarning();
							}
						}
					}
					catch (SQLException ex) {
						boolean dropStatement = StringUtils.startsWithIgnoreCase(statement.trim(), "drop");
						if (continueOnError || (dropStatement && ignoreFailedDrops)) {
							if (logger.isDebugEnabled()) {
								logger.debug(ScriptStatementFailedException.buildErrorMessage(statement, stmtNumber, resource), ex);
							}
						}
						else {
							throw new ScriptStatementFailedException(statement, stmtNumber, resource, ex);
						}
					}
				}
			}
			finally {
				try {
					stmt.close();
				}
				catch (Throwable ex) {
					logger.debug("Could not close JDBC Statement", ex);
				}
			}

			long elapsedTime = System.currentTimeMillis() - startTime;
			if (logger.isInfoEnabled()) {
				logger.info("Executed SQL script from " + resource + " in " + elapsedTime + " ms.");
			}
		}
		catch (Exception ex) {
			if (ex instanceof ScriptException) {
				throw (ScriptException) ex;
			}
			throw new UncategorizedScriptException(
				"Failed to execute database script from resource [" + resource + "]", ex);
		}
	}
	
	public static String readScript(LineNumberReader lineNumberReader, String commentPrefix, String separator)
			throws IOException {

		String currentStatement = lineNumberReader.readLine();
		StringBuilder scriptBuilder = new StringBuilder();
		while (currentStatement != null) {
			if (commentPrefix != null && !currentStatement.startsWith(commentPrefix)) {
				if (scriptBuilder.length() > 0) {
					scriptBuilder.append('\n');
				}
				scriptBuilder.append(currentStatement);
			}
			currentStatement = lineNumberReader.readLine();
		}
		appendSeparatorToScriptIfNecessary(scriptBuilder, separator);
		
		return TransformMysqlToH2.transformCreateTable(scriptBuilder.toString());
	}
	
	private static void appendSeparatorToScriptIfNecessary(StringBuilder scriptBuilder, String separator) {
		if (separator == null) {
			return;
		}
		String trimmed = separator.trim();
		if (trimmed.length() == separator.length()) {
			return;
		}
		// separator ends in whitespace, so we might want to see if the script is trying
		// to end the same way
		if (scriptBuilder.lastIndexOf(trimmed) == scriptBuilder.length() - trimmed.length()) {
			scriptBuilder.append(separator.substring(trimmed.length()));
		}
	}
}
