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

package com.koalephant.nlp;

import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.pipeline.TokensRegexNERAnnotator;
import edu.stanford.nlp.util.StringUtils;
import jdk.nashorn.internal.runtime.regexp.joni.Regex;

import java.io.*;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * RegexNER Validator
 */
public class RegexNERValidator {

	/**
	 * Main method
	 *
	 * @param args program arguments, mostly -properties
	 * @throws RuntimeException
	 */
	public static void main(String args[]) throws RuntimeException {

		if (args.length == 0) {
			throw new RuntimeException("No arguments specified");
		}

		Properties properties = StringUtils.argsToProperties(args);

		if (properties.containsKey("inFile")) {
			if (properties.containsKey("outFile")) {
				quoteMappings(properties.getProperty("inFile"), properties.getProperty("outFile"));
			}
			else {
				quoteMappings(properties.getProperty("inFile"));
			}
		}
		else {
			testMappings(properties);
		}
	}

	/**
	 * Test the Mapping by invoking the NERAnnotator
	 *
	 * @param properties the properties object
	 */
	protected static void testMappings(Properties properties) {
		try {
			new TokensRegexNERAnnotator(Annotator.STANFORD_REGEXNER, properties);
		}
		catch (PatternSyntaxException exception) {
			System.err.println(exception.getLocalizedMessage());
			System.exit(1);
		}
	}

	/**
	 * Read an input file, quote the patterns and write to an output file
	 *
	 * @param inFile the file to read in from
	 */
	protected static void quoteMappings(String inFile) {
		quoteMappings(inFile, inFile + ".quoted");
	}

	/**
	 * Read an input file, quote the patterns and write to an output file
	 *
	 * @param inFile the file to read in from
	 * @param outFile the file to write out to
	 */
	protected static void quoteMappings(String inFile, String outFile) {
		BufferedReader bufferedReader = null;
		BufferedWriter bufferedWriter = null;
		try {
			bufferedReader = new BufferedReader(new FileReader(inFile));
			bufferedWriter = new BufferedWriter(new FileWriter(outFile));
			int lineNo = 0;
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				lineNo++;
				String[] tokens = line.split("\t");

				if (tokens.length > 4) {
					System.err.println("Unexpected line longer than 4 tokens: Line " + lineNo);
				}

				String[] patterns = tokens[0].split(" ");
				for (int i = 0; i < patterns.length; i++) {
					patterns[i] = Pattern.quote(patterns[i]);
				}
				tokens[0] = String.join(" ", patterns);

				bufferedWriter.write(String.join("\t", tokens));
				bufferedWriter.newLine();
			}
		}
		catch (FileNotFoundException exception) {
			System.err.println("File not found: " + exception.getLocalizedMessage());
		}
		catch (IOException exception) {
			System.err.println("Error reading file: " + exception.getLocalizedMessage());
		}
		finally {
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				}
				catch (IOException exception) {
					System.err.println("Error while closing file: " + exception.getLocalizedMessage());
				}
			}

			if (bufferedWriter != null) {
				try {
					bufferedWriter.close();
				}
				catch (IOException exception) {
					System.err.println("Error while closing file: " + exception.getLocalizedMessage());
				}
			}
		}
	}
}
