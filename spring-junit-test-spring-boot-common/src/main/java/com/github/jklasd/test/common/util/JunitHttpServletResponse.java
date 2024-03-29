package com.github.jklasd.test.common.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedCaseInsensitiveMap;

import com.google.common.net.HttpHeaders;

public class JunitHttpServletResponse implements HttpServletResponse{

	private static final String CHARSET_PREFIX = "charset=";

	private static final String DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";

	private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

	private final Map<String, String> headers = new LinkedCaseInsensitiveMap<>();
	//---------------------------------------------------------------------
	// ServletResponse properties
	//---------------------------------------------------------------------

	private boolean outputStreamAccessAllowed = true;

	private boolean writerAccessAllowed = true;

	@Nullable
	private String characterEncoding = "UTF-8";

	private boolean charset = false;

	private final ByteArrayOutputStream content = new ByteArrayOutputStream(1024);

	private final ServletOutputStream outputStream = new ResponseServletOutputStream(this.content);

	@Nullable
	private PrintWriter writer;

	private long contentLength = 0;

	@Nullable
	private String contentType;

	private int bufferSize = 4096;

	private boolean committed;

	private Locale locale = Locale.getDefault();


	//---------------------------------------------------------------------
	// HttpServletResponse properties
	//---------------------------------------------------------------------

	private final List<Cookie> cookies = new ArrayList<>();

	private int status = HttpServletResponse.SC_OK;

	@Nullable
	private String errorMessage;

	@Nullable
	private String forwardedUrl;

	private final List<String> includedUrls = new ArrayList<>();


	//---------------------------------------------------------------------
	// ServletResponse interface
	//---------------------------------------------------------------------

	/**
	 * Set whether {@link #getOutputStream()} access is allowed.
	 * <p>Default is {@code true}.
	 */
	public void setOutputStreamAccessAllowed(boolean outputStreamAccessAllowed) {
		this.outputStreamAccessAllowed = outputStreamAccessAllowed;
	}

	/**
	 * Return whether {@link #getOutputStream()} access is allowed.
	 */
	public boolean isOutputStreamAccessAllowed() {
		return this.outputStreamAccessAllowed;
	}

	/**
	 * Set whether {@link #getWriter()} access is allowed.
	 * <p>Default is {@code true}.
	 */
	public void setWriterAccessAllowed(boolean writerAccessAllowed) {
		this.writerAccessAllowed = writerAccessAllowed;
	}

	/**
	 * Return whether {@link #getOutputStream()} access is allowed.
	 */
	public boolean isWriterAccessAllowed() {
		return this.writerAccessAllowed;
	}

	/**
	 * Return whether the character encoding has been set.
	 * <p>If {@code false}, {@link #getCharacterEncoding()} will return a default encoding value.
	 */
	public boolean isCharset() {
		return this.charset;
	}

	@Override
	public void setCharacterEncoding(String characterEncoding) {
		this.characterEncoding = characterEncoding;
		this.charset = true;
		updateContentTypePropertyAndHeader();
	}

	private void updateContentTypePropertyAndHeader() {
		if (this.contentType != null) {
			String value = this.contentType;
			if (this.charset && !this.contentType.toLowerCase().contains(CHARSET_PREFIX)) {
				value = value + ';' + CHARSET_PREFIX + this.characterEncoding;
				this.contentType = value;
			}
			doAddHeaderValue(HttpHeaders.CONTENT_TYPE, value, true);
		}
	}

	@Override
	@Nullable
	public String getCharacterEncoding() {
		return this.characterEncoding;
	}

	@Override
	public ServletOutputStream getOutputStream() {
		Assert.state(this.outputStreamAccessAllowed, "OutputStream access not allowed");
		return this.outputStream;
	}

	@Override
	public PrintWriter getWriter() throws UnsupportedEncodingException {
		Assert.state(this.writerAccessAllowed, "Writer access not allowed");
		if (this.writer == null) {
			Writer targetWriter = (this.characterEncoding != null ?
					new OutputStreamWriter(this.content, this.characterEncoding) :
					new OutputStreamWriter(this.content));
			this.writer = new ResponsePrintWriter(targetWriter);
		}
		return this.writer;
	}

	public byte[] getContentAsByteArray() {
		return this.content.toByteArray();
	}

	/**
	 * Get the content of the response body as a {@code String}, using the charset
	 * specified for the response by the application, either through
	 * {@link HttpServletResponse} methods or through a charset parameter on the
	 * {@code Content-Type}.
	 * @return the content as a {@code String}
	 * @throws UnsupportedEncodingException if the character encoding is not supported
	 * @see #getContentAsString(Charset)
	 */
	public String getContentAsString() throws UnsupportedEncodingException {
		return (this.characterEncoding != null ?
				this.content.toString(this.characterEncoding) : this.content.toString());
	}

	/**
	 * Get the content of the response body as a {@code String}, using the provided
	 * {@code fallbackCharset} if no charset has been explicitly defined and otherwise
	 * using the charset specified for the response by the application, either
	 * through {@link HttpServletResponse} methods or through a charset parameter on the
	 * {@code Content-Type}.
	 * @return the content as a {@code String}
	 * @throws UnsupportedEncodingException if the character encoding is not supported
	 * @since 5.2
	 * @see #getContentAsString()
	 */
	public String getContentAsString(Charset fallbackCharset) throws UnsupportedEncodingException {
		return (isCharset() && this.characterEncoding != null ?
				this.content.toString(this.characterEncoding) :
				this.content.toString(fallbackCharset.name()));
	}

	@Override
	public void setContentLength(int contentLength) {
		this.contentLength = contentLength;
		doAddHeaderValue(HttpHeaders.CONTENT_LENGTH, contentLength, true);
	}

	public int getContentLength() {
		return (int) this.contentLength;
	}

	@Override
	public void setContentLengthLong(long contentLength) {
		this.contentLength = contentLength;
		doAddHeaderValue(HttpHeaders.CONTENT_LENGTH, contentLength, true);
	}

	public long getContentLengthLong() {
		return this.contentLength;
	}

	@Override
	public void setContentType(@Nullable String contentType) {
		this.contentType = contentType;
	}

	@Override
	@Nullable
	public String getContentType() {
		return this.contentType;
	}

	@Override
	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	@Override
	public int getBufferSize() {
		return this.bufferSize;
	}

	@Override
	public void flushBuffer() {
		setCommitted(true);
	}

	@Override
	public void resetBuffer() {
		Assert.state(!isCommitted(), "Cannot reset buffer - response is already committed");
		this.content.reset();
	}

	private void setCommittedIfBufferSizeExceeded() {
		int bufSize = getBufferSize();
		if (bufSize > 0 && this.content.size() > bufSize) {
			setCommitted(true);
		}
	}

	public void setCommitted(boolean committed) {
		this.committed = committed;
	}

	@Override
	public boolean isCommitted() {
		return this.committed;
	}

	@Override
	public void reset() {
		resetBuffer();
		this.characterEncoding = null;
		this.charset = false;
		this.contentLength = 0;
		this.contentType = null;
		this.locale = Locale.getDefault();
		this.cookies.clear();
		this.status = HttpServletResponse.SC_OK;
		this.errorMessage = null;
	}

	@Override
	public void setLocale(Locale locale) {
		this.locale = locale;
		doAddHeaderValue(HttpHeaders.CONTENT_LANGUAGE, locale.toLanguageTag(), true);
	}

	@Override
	public Locale getLocale() {
		return this.locale;
	}


	//---------------------------------------------------------------------
	// HttpServletResponse interface
	//---------------------------------------------------------------------

	@Override
	public void addCookie(Cookie cookie) {
		Assert.notNull(cookie, "Cookie must not be null");
		this.cookies.add(cookie);
		doAddHeaderValue(HttpHeaders.SET_COOKIE, getCookieHeader(cookie), false);
	}

	private String getCookieHeader(Cookie cookie) {
		StringBuilder buf = new StringBuilder();
		return buf.toString();
	}

	public Cookie[] getCookies() {
		return this.cookies.toArray(new Cookie[0]);
	}

	@Nullable
	public Cookie getCookie(String name) {
		Assert.notNull(name, "Cookie name must not be null");
		for (Cookie cookie : this.cookies) {
			if (name.equals(cookie.getName())) {
				return cookie;
			}
		}
		return null;
	}

	/**
	 * The default implementation returns the given URL String as-is.
	 * <p>Can be overridden in subclasses, appending a session id or the like.
	 */
	@Override
	public String encodeURL(String url) {
		return url;
	}

	/**
	 * The default implementation delegates to {@link #encodeURL},
	 * returning the given URL String as-is.
	 * <p>Can be overridden in subclasses, appending a session id or the like
	 * in a redirect-specific fashion. For general URL encoding rules,
	 * override the common {@link #encodeURL} method instead, applying
	 * to redirect URLs as well as to general URLs.
	 */
	@Override
	public String encodeRedirectURL(String url) {
		return encodeURL(url);
	}

	@Override
	@Deprecated
	public String encodeUrl(String url) {
		return encodeURL(url);
	}

	@Override
	@Deprecated
	public String encodeRedirectUrl(String url) {
		return encodeRedirectURL(url);
	}

	@Override
	public void sendError(int status, String errorMessage) throws IOException {
		Assert.state(!isCommitted(), "Cannot set error status - response is already committed");
		this.status = status;
		this.errorMessage = errorMessage;
		setCommitted(true);
	}

	@Override
	public void sendError(int status) throws IOException {
		Assert.state(!isCommitted(), "Cannot set error status - response is already committed");
		this.status = status;
		setCommitted(true);
	}

	@Override
	public void sendRedirect(String url) throws IOException {
		Assert.state(!isCommitted(), "Cannot send redirect - response is already committed");
		Assert.notNull(url, "Redirect URL must not be null");
		setHeader(HttpHeaders.LOCATION, url);
		setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
		setCommitted(true);
	}

	@Nullable
	public String getRedirectedUrl() {
		return getHeader(HttpHeaders.LOCATION);
	}

	@Override
	public void setDateHeader(String name, long value) {
		setHeaderValue(name, formatDate(value));
	}

	@Override
	public void addDateHeader(String name, long value) {
		addHeaderValue(name, formatDate(value));
	}

	public long getDateHeader(String name) {
		String headerValue = getHeader(name);
		if (headerValue == null) {
			return -1;
		}
		try {
			return newDateFormat().parse(getHeader(name)).getTime();
		}
		catch (ParseException ex) {
			throw new IllegalArgumentException(
					"Value for header '" + name + "' is not a valid Date: " + headerValue);
		}
	}

	private String formatDate(long date) {
		return newDateFormat().format(new Date(date));
	}

	private DateFormat newDateFormat() {
		SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.US);
		dateFormat.setTimeZone(GMT);
		return dateFormat;
	}

	@Override
	public void setHeader(String name, String value) {
		setHeaderValue(name, value);
	}

	@Override
	public void addHeader(String name, String value) {
		addHeaderValue(name, value);
	}

	@Override
	public void setIntHeader(String name, int value) {
		setHeaderValue(name, value);
	}

	@Override
	public void addIntHeader(String name, int value) {
		addHeaderValue(name, value);
	}

	private void setHeaderValue(String name, Object value) {
		boolean replaceHeader = true;
		if (setSpecialHeader(name, value, replaceHeader)) {
			return;
		}
		doAddHeaderValue(name, value, replaceHeader);
	}

	private void addHeaderValue(String name, Object value) {
		boolean replaceHeader = false;
		if (setSpecialHeader(name, value, replaceHeader)) {
			return;
		}
		doAddHeaderValue(name, value, replaceHeader);
	}

	private boolean setSpecialHeader(String name, Object value, boolean replaceHeader) {
		if (HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(name)) {
			setContentType(value.toString());
			return true;
		}
		else if (HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(name)) {
			setContentLength(value instanceof Number ? ((Number) value).intValue() :
					Integer.parseInt(value.toString()));
			return true;
		}
		else if (HttpHeaders.CONTENT_LANGUAGE.equalsIgnoreCase(name)) {
//			String contentLanguages = value.toString();
//			HttpHeaders headers = new HttpHeaders();
//			headers.add(HttpHeaders.CONTENT_LANGUAGE, contentLanguages);
//			Locale language = headers.getContentLanguage();
//			setLocale(language != null ? language : Locale.getDefault());
//			// Since setLocale() sets the Content-Language header to the given
//			// single Locale, we have to explicitly set the Content-Language header
//			// to the user-provided value.
//			doAddHeaderValue(HttpHeaders.CONTENT_LANGUAGE, contentLanguages, true);
//			return true;
		}
		else if (HttpHeaders.SET_COOKIE.equalsIgnoreCase(name)) {
			return true;
		}
		else {
			return false;
		}
		return replaceHeader;
	}

	private void doAddHeaderValue(String name, Object value, boolean replace) {
		Assert.notNull(value, "Header value must not be null");
	}

	/**
	 * Set the {@code Set-Cookie} header to the supplied {@link Cookie},
	 * overwriting any previous cookies.
	 * @param cookie the {@code Cookie} to set
	 * @since 5.1.10
	 * @see #addCookie(Cookie)
	 */
	private void setCookie(Cookie cookie) {
		Assert.notNull(cookie, "Cookie must not be null");
		this.cookies.clear();
		this.cookies.add(cookie);
		doAddHeaderValue(HttpHeaders.SET_COOKIE, getCookieHeader(cookie), true);
	}

	@Override
	public void setStatus(int status) {
		if (!this.isCommitted()) {
			this.status = status;
		}
	}

	@Override
	@Deprecated
	public void setStatus(int status, String errorMessage) {
		if (!this.isCommitted()) {
			this.status = status;
			this.errorMessage = errorMessage;
		}
	}

	@Override
	public int getStatus() {
		return this.status;
	}

	@Nullable
	public String getErrorMessage() {
		return this.errorMessage;
	}


	//---------------------------------------------------------------------
	// Methods for MockRequestDispatcher
	//---------------------------------------------------------------------

	public void setForwardedUrl(@Nullable String forwardedUrl) {
		this.forwardedUrl = forwardedUrl;
	}

	@Nullable
	public String getForwardedUrl() {
		return this.forwardedUrl;
	}

	public void setIncludedUrl(@Nullable String includedUrl) {
		this.includedUrls.clear();
		if (includedUrl != null) {
			this.includedUrls.add(includedUrl);
		}
	}

	@Nullable
	public String getIncludedUrl() {
		int count = this.includedUrls.size();
		Assert.state(count <= 1,
				() -> "More than 1 URL included - check getIncludedUrls instead: " + this.includedUrls);
		return (count == 1 ? this.includedUrls.get(0) : null);
	}

	public void addIncludedUrl(String includedUrl) {
		Assert.notNull(includedUrl, "Included URL must not be null");
		this.includedUrls.add(includedUrl);
	}

	public List<String> getIncludedUrls() {
		return this.includedUrls;
	}


	/**
	 * Inner class that adapts the ServletOutputStream to mark the
	 * response as committed once the buffer size is exceeded.
	 */
	private class ResponseServletOutputStream extends ServletOutputStream {

		private final OutputStream targetStream;


		/**
		 * Create a DelegatingServletOutputStream for the given target stream.
		 * @param targetStream the target stream (never {@code null})
		 */
		public ResponseServletOutputStream(OutputStream targetStream) {
			Assert.notNull(targetStream, "Target OutputStream must not be null");
			this.targetStream = targetStream;
		}

		/**
		 * Return the underlying target stream (never {@code null}).
		 */
		public final OutputStream getTargetStream() {
			return this.targetStream;
		}


		@Override
		public void write(int b) throws IOException {
			this.targetStream.write(b);
		}

		@Override
		public void flush() throws IOException {
			super.flush();
			this.targetStream.flush();
		}

		@Override
		public void close() throws IOException {
			super.close();
			this.targetStream.close();
		}

		@Override
		public boolean isReady() {
			return true;
		}

		@Override
		public void setWriteListener(WriteListener writeListener) {
			throw new UnsupportedOperationException();
		}
	}


	/**
	 * Inner class that adapts the PrintWriter to mark the
	 * response as committed once the buffer size is exceeded.
	 */
	private class ResponsePrintWriter extends PrintWriter {

		public ResponsePrintWriter(Writer out) {
			super(out, true);
		}

		@Override
		public void write(char[] buf, int off, int len) {
			super.write(buf, off, len);
			super.flush();
			setCommittedIfBufferSizeExceeded();
		}

		@Override
		public void write(String s, int off, int len) {
			super.write(s, off, len);
			super.flush();
			setCommittedIfBufferSizeExceeded();
		}

		@Override
		public void write(int c) {
			super.write(c);
			super.flush();
			setCommittedIfBufferSizeExceeded();
		}

		@Override
		public void flush() {
			super.flush();
			setCommitted(true);
		}

		@Override
		public void close() {
			super.flush();
			super.close();
			setCommitted(true);
		}
	}


	@Override
	public boolean containsHeader(String name) {
		return false;
	}

	@Override
	public String getHeader(String name) {
		return headers.get(name);
	}

	@Override
	public Collection<String> getHeaders(String name) {
		return getHeaders(name);
	}

	@Override
	public Collection<String> getHeaderNames() {
		return null;
	}

}
