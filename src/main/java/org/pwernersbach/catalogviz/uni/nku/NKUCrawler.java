/* NKUCrawler.java
 * Part of CatalogViz Open Source Edition
 * Copyright (c) 2014 Philip Wernersbach <philip.wernersbach@gmail.com>
 * 
 * -----
 * CatalogViz Open Source Edition is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CatalogViz Open Source Edition is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CatalogViz Open Source Edition.  If not, see <http://www.gnu.org/licenses/>.
 * -----
 * 
 * Commercial and proprietary licenses for CatalogViz are also available, contact
 * Philip Wernersbach <philip.wernersbach@gmail.com> for more details.
 */

package org.pwernersbach.catalogviz.uni.nku;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.pwernersbach.catalogviz.db.Course;
import org.pwernersbach.catalogviz.db.CourseManager;
import org.pwernersbach.catalogviz.util.JsoupCrawler;
import org.pwernersbach.catalogviz.util.JsoupCrawlerConsumer;

public class NKUCrawler {
	protected static final String INITIAL_PAGE = "http://nkuonline.nku.edu/smartcatalog/indexpage.htm";
	protected final JsoupCrawler crawler;
	protected final CourseManager manager;

	protected class NKUCrawlerConsumer implements JsoupCrawlerConsumer {
		protected static final int DEPENDENCY_PREREQUISITE = 1;
		protected static final int DEPENDENCY_COREQUISITE = 2;

		protected final Pattern courseTitlePattern = Pattern
				.compile("^([A-Z][A-Z][A-Z][A-Z]?) ([0-9][0-9][0-9]*)([a-zA-Z]*).?");
		protected final Pattern courseCategoryPattern = Pattern
				.compile("^([A-Z][A-Z][A-Z][A-Z]?)$");
		protected final Pattern courseNumberPattern = Pattern
				.compile("^([0-9][0-9][0-9]*)([a-zA-Z]*).?$");
		protected final Pattern courseBothPattern = Pattern
				.compile("^([A-Z][A-Z][A-Z][A-Z]?)([0-9][0-9][0-9]*)([a-zA-Z]*).?$");

		@Override
		public boolean onDocumentDownloaded(String url, Document doc) {
			Matcher matcher = null;
			Element courseTitleElement = null;
			String courseTitle = null;
			String courseCategory = null;
			Integer courseNumber = null;
			String courseSubnumber = null;
			Course course = null;
			int dependencyType = 0;
			String dependencyText = null;
			String[] dependencyTextParts = null;
			Course dependencyCourse = null;

			if (url.equals(INITIAL_PAGE))
				return true;

			System.out.print("PROCESSING: ");
			System.out.println(url);

			courseTitleElement = doc.select(".coursetitle").first();
			if (courseTitleElement == null) {
				manager.rollback();
				return false;
			}

			courseTitle = courseTitleElement.text();
			System.out.print("\t Course Title -> ");
			System.out.println(courseTitle);

			matcher = courseTitlePattern.matcher(courseTitle);

			if (!matcher.find()) {
				manager.rollback();
				return false;
			}

			courseCategory = matcher.group(1);
			courseNumber = new Integer(matcher.group(2));
			courseSubnumber = matcher.group(3);

			System.out.print("\t Course Category -> ");
			System.out.println(courseCategory);

			System.out.print("\t Course Number -> ");
			System.out.println(courseNumber);

			if (courseSubnumber == null)
				courseSubnumber = "";

			if (!courseSubnumber.equals("")) {
				System.out.print("\t Course Subnumber -> ");
				System.out.println(courseSubnumber);
			}

			course = manager.getCourse(courseCategory, courseNumber,
					courseSubnumber);

			for (Element dependencies : doc.select(".prerequisites")) {
				dependencyType = 0;
				dependencyText = dependencies.text();

				if (dependencyText.startsWith("Prerequisites:")) {
					dependencyType = DEPENDENCY_PREREQUISITE;
				} else {
					if (dependencyText.startsWith("Co-requisites:")
							|| dependencyText.startsWith("Prereq OR Co-req:")
							|| dependencyText.startsWith("Corequisites:")) {
						dependencyType = DEPENDENCY_COREQUISITE;
					} else {
						if (!dependencyText.startsWith("Hours:")
								&& !dependencyText.startsWith("Taught:")) {
							System.err.print("INVALID DEPENDENCY TEXT: ");
							System.err.println(dependencyText);

							manager.rollback();
							return false;
						}
					}
				}

				if (dependencyType != 0) {
					courseCategory = null;
					courseNumber = null;
					courseSubnumber = null;
					dependencyTextParts = dependencyText.split(" ");

					for (int i = 0; i < dependencyTextParts.length; i++) {
						matcher = courseCategoryPattern
								.matcher(dependencyTextParts[i]);
						if (matcher.find()) {
							courseCategory = matcher.group(1);
							courseNumber = null;
							courseSubnumber = null;
							i++;
						} else {
							matcher = courseBothPattern
									.matcher(dependencyTextParts[i]);
							if (matcher.find()) {
								courseCategory = matcher.group(1);
								courseNumber = new Integer(matcher.group(2));
								courseSubnumber = matcher.group(3);
							}
						}

						if (courseCategory != null) {
							if (courseNumber == null)
								matcher = courseNumberPattern
										.matcher(dependencyTextParts[i]);

							if ((courseNumber != null) || (matcher.find())) {
								if (courseNumber == null) {
									courseNumber = new Integer(matcher.group(1));
									courseSubnumber = matcher.group(2);
								}

								if (dependencyType == DEPENDENCY_PREREQUISITE)
									System.out.println("\t Prerequisite -> ");
								else
									System.out.println("\t Corequisite -> ");

								System.out.print("\t\t Course Category -> ");
								System.out.println(courseCategory);

								System.out.print("\t\t Course Number -> ");
								System.out.println(courseNumber);

								if (courseSubnumber == null)
									courseSubnumber = "";

								if (!courseSubnumber.equals("")) {
									System.out
											.print("\t\t Course Subnumber -> ");
									System.out.println(courseSubnumber);
								}

								dependencyCourse = manager.getPartialCourse(
										courseCategory, courseNumber,
										courseSubnumber);

								if (dependencyType == DEPENDENCY_PREREQUISITE)
									course.addPrerequisite(dependencyCourse);
								else
									course.addCorequisite(dependencyCourse);

								courseNumber = null;
								courseSubnumber = null;
							}
						}
					}
				}
			}

			course.prepareForVisualization();
			manager.commit();
			return true;
		}

		@Override
		public boolean onDocumentDownloadError(String url, IOException error) {
			System.err.print("DOWNLOAD ERROR: ");
			System.err.println(url);

			error.printStackTrace();

			manager.rollback();
			return false;
		}

	}

	public NKUCrawler(CourseManager aCourseManager) {
		crawler = new JsoupCrawler(
				INITIAL_PAGE,
				"^http://nkuonline.nku.edu/smartcatalog/[a-z][a-z][a-z][a-z]?-.*credits?.htm$",
				new NKUCrawlerConsumer());
		manager = aCourseManager;
	}

	public boolean run() {
		return crawler.run();
	}
}
