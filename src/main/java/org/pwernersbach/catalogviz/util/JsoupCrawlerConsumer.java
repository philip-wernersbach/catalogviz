/* JsoupCrawlerConsumer.java
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

package org.pwernersbach.catalogviz.util;

import java.io.IOException;

import org.jsoup.nodes.Document;

public interface JsoupCrawlerConsumer {
	public boolean onDocumentDownloaded(String url, Document doc);

	public boolean onDocumentDownloadError(String url, IOException error);
}
