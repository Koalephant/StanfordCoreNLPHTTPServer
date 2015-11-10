/*
	Stanford CoreNLP HTTP Server
	Copyright (C) 2015 Koalephant Co., Ltd

	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.koalephant.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum MediaType {

	TEXT_HTML("text","html"),

	APPLICATION_JSON("application","json"),
	TEXT_JSON("text","json", APPLICATION_JSON),

	TEXT_XML("text","xml"),
	APPLICATION_XML("application","xml", TEXT_XML),
	APPLICATION_XHTML_XML("application", "xhtml+xml"),

	TEXT_JAVASCRIPT("text", "javascript"),
	APPLICATION_JAVASCRIPT("application", "javascript", TEXT_JAVASCRIPT),

	TEXT_CSS("text", "css"),
	TEXT_CSV("text", "csv"),
	TEXT_TSV("text", "tsv"),
	TEXT_PLAIN("text", "plain"),

	IMAGE_GIF("image", "gif"),
	IMAGE_JPG("image", "jpg"),
	IMAGE_JPEG("image", "jpeg", IMAGE_JPG),
	IMAGE_PNG("image", "png");

	/**
	 * Pattern to match type/subtype+suffix
	 */
	private static final Pattern pattern = Pattern.compile("^([^/]+)/([^+;]+)");

	/**
	 * Type
	 */
	private final String type;

	/**
	 * SubType
	 */
	private final String subType;

	/**
	 * The canonical MediaType for this generic type
	 */
	private final MediaType canonical;

	MediaType(String type, String subType) {
		this(type, subType, null);
	}

	MediaType(String type, String subType, MediaType canonical) {
		this.type = type;
		this.subType = subType;
		this.canonical = canonical;
	}

	/**
	 * Get a String representation of this type. e.g. "application/zip"
	 * @return the type/subtype string
	 */
	public String toString() {
		return type + "/" + subType;
	}

	/**
	 * Get the canonical MediaType
	 *
	 * @return the canonical MediaType for this generic type
	 */
	public MediaType getCanonical() {
		return this.canonical != null ? this.canonical : this;
	}

	/**
	 * Check if this is the canonical MediaType for this generic type
	 */
	public boolean isCanonical() {
		return this.canonical == null;
	}

	/**
	 * Check if the given string matches the MediaType
	 * @param typeString the mediatype string to test
	 * @return true/false indicating match
	 */
	public boolean matches(String typeString) throws IllegalArgumentException {
		Matcher parts = pattern.matcher(typeString.toLowerCase());

		if (parts.matches()) {
			String type = parts.group(1);
			String subType = parts.group(2);

			boolean typeMatch = type.equals(this.type) || type.equals("*") || this.type.equals("*");
			boolean subTypeMatch = subType.equals(this.subType) || subType.equals("*") || this.subType.equals("*");

			return typeMatch && subTypeMatch;
		}

		throw new IllegalArgumentException("Cannot parse media type: " + typeString);
	}

	/**
	 * Get a MediaType from a String
	 * @param typeString the mediatype string to lookup
	 * @return the MediaType corresponding to the string provided, or null if none match
	 */
	public static MediaType getFromType(String typeString) {
		return getFromType(typeString, null);
	}

	/**
	 * Get a MediaType from a String
	 * @param typeString the mediatype string to lookup
	 * @param fallback default type to return
	 * @return the MediaType corresponding to the string provided, or the fallback if none match
	 */
	public static MediaType getFromType(String typeString, MediaType fallback) {
		for (MediaType mediaType : MediaType.values()) {
			if(mediaType.matches(typeString)) {
				return mediaType;
			}
		}

		return fallback;
	}
}
