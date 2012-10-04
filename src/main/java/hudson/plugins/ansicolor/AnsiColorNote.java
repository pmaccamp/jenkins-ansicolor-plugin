/*
 * The MIT License
 * 
 * Copyright (c) 2011 Daniel Doubrovkine
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.ansicolor;

import hudson.Extension;
import hudson.MarkupText;
import hudson.MarkupText.SubText;
import hudson.console.ConsoleAnnotationDescriptor;
import hudson.console.ConsoleAnnotator;
import hudson.console.ConsoleNote;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;

public class AnsiColorNote extends ConsoleNote {

	private static final long serialVersionUID = 1L;
	private static final Logger LOG = Logger.getLogger(AnsiColorNote.class
			.getName());
	private static final Pattern URL = Pattern.compile("&quot;file://.*&quot;|"
			+ // File wrapped by quotes that have been html escaped
			"\'file://.*\'|" + // File wrapped by single quotes
			"\"file://.*\"|" + // File wrapped by double quotes
			"file://[^\\s<>]+[^\\s<>,\\.:\"'()\\[\\]=]"); // File without quotes
															// containing no
															// whitespace(\\s)
															// or <>
															// ending in any
															// character except
															// the following -
															// whitespace <> , \
															// . : " ' () [ ] =
	private String data;

	private final AnsiColorMap colorMap;

	public AnsiColorNote(String data, final AnsiColorMap colorMap) {
		this.data = data;
		this.colorMap = colorMap;
	}

	/**
	 * Return this color note's color map.
	 * 
	 * @return AnsiColorMap
	 */
	public AnsiColorMap getColorMap() {
		return this.colorMap != null ? this.colorMap : AnsiColorMap.Default;
	}

	/**
	 * Annotate output that contains ANSI codes and hide raw text.
	 */
	@Override
	public ConsoleAnnotator annotate(Object context, MarkupText text,
			int charPos) {
		try {
			// Removing html encoding until I can figure out a better way to
			// handle urls.
			// String colorizedData =
			// colorize(StringEscapeUtils.escapeHtml(this.data),
			// this.getColorMap());
			String colorizedData = colorize(this.data, this.getColorMap());
			if (!colorizedData.contentEquals(this.data)) {
				text.addMarkup(charPos, stringFileUrlAnnotate(colorizedData));
				text.addMarkup(charPos, charPos + text.getText().length(),
						"<span style=\"display: none;\">", "</span>");
			} else
				fileUrlAnnotate(text);
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Failed to add markup to \"" + text + "\"",
					e);
		}
		return null;
	}

	/**
	 * Process a string, convert ANSI markup to HTML.
	 * 
	 * @param data
	 *            string that may contain ANSI escape characters
	 * @return HTML string
	 * @throws IOException
	 */
	public static String colorize(String data, final AnsiColorMap colorMap)
			throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		AnsiColorizer colorizer = new AnsiColorizer(out,
				Charset.defaultCharset(), colorMap);
		byte[] bytes = data.getBytes();
		colorizer.eol(bytes, bytes.length);
		return out.toString();
	}

	private String stringFileUrlAnnotate(String text) {
		// Search for quoted url or url without spaces
		Matcher matcher = URL.matcher(text);
		while (matcher.find()) {
			// generate the hyperlink for the file removing any wrapping
			// characters
			String file = matcher.group().replaceAll("\"|'|&quot;", "");
			text = text.substring(0, matcher.start()) + "<a href=\'" + file
					+ "\'>" + file + "</a>"
					+ text.substring(matcher.end(), text.length());

		}
		return text;
	}

	private void fileUrlAnnotate(MarkupText text) {
		// Search for quoted url or url without spaces
		for (SubText t : text.findTokens(URL)) {
			// generate the hyperlink for the file removing any wrapping
			// characters
			t.href(t.getText().replaceAll("\"|'|&quot;", ""));
		}
	}

	public static String encodeTo(String html, final AnsiColorMap colorMap) {
		try {
			return new AnsiColorNote(html, colorMap).encode();
		} catch (IOException e) {
			LOG.log(Level.WARNING,
					"Failed to serialize " + AnsiColorNote.class, e);
			return "";
		}
	}

	@Extension
	public static final class DescriptorImpl extends
			ConsoleAnnotationDescriptor {
		public String getDisplayName() {
			return "ANSI Color";
		}
	}
}