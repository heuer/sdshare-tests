/*
 * Copyright 2010 - 2012 SDShare.org. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sdshare.sdsharetests.server;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.sdshare.sdsharetests.IConstants;

import static org.junit.Assert.*;

/**
 * Tests against the 
 * <a href="http://www.egovpt.org/fg/CWA_Part_1b#head-cfc066c9ca58d6eb6880b6c4d357b62e2b7a7403">collection feed</a>.
 * 
 * @author Lars Heuer (heuer[at]semagia.com) <a href="http://www.semagia.com/">Semagia</a>
 * @version $Rev:$ - $Date:$
 */
@RunWith(Parameterized.class)
public class TestCollectionFeed extends AbstractServerTestCase {

    private final URI _uri;

    public TestCollectionFeed(final URI uri) {
        _uri = uri;
    }

    @Parameters
    public static Collection<Object> makeTestCases() {
        final Collection<Object> result = new ArrayList<Object>();
        try {
            for (URI uri: Utils.fetchCollectionFeedURIs()) {
                result.add(new Object[] {uri});
            }
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return result;
    }

    /**
     * Checks the collection feed for links to snapshot/fragment feeds.
     *
     * @throws Exception In case of an error.
     */
    @Test
    public void testCollectionFeed() throws Exception {
        validate(_uri);
      
        final Document feed = super.fetchAtomFeedAsDOM(_uri);
        // Fetch all links which point to a collection.
        final Nodes fragmentFeedLinks = query(feed, "atom:feed/atom:entry/atom:link[@rel='" + REL_FRAGMENTS_FEED + "'][not(@type) or @type='application/atom+xml']");
        assertEquals("Exected one fragment feed link in " + feed.getBaseURI(), 1, fragmentFeedLinks.size());
        final Nodes snapshotFeedLinks = query(feed, "atom:feed/atom:entry/atom:link[@rel='" + REL_SNAPSHOTS_FEED + "'][not(@type) or @type='application/atom+xml']");
        assertEquals("Expected one snapshot feed link in " + feed.getBaseURI(), 1, snapshotFeedLinks.size());
        final Nodes links = query(feed, "atom:feed/atom:entry/atom:link[@rel='" + REL_FRAGMENTS_FEED + "' or @rel='" + REL_SNAPSHOTS_FEED + "']");
        for (int i=0; i<links.size(); i++) {
            Element link = (Element) links.get(i);
            Attribute attr = link.getAttribute("href");
            assertNotNull("No href attribute available", attr);
            final URI href = URI.create(feed.getBaseURI()).resolve(attr.getValue());
            attr = link.getAttribute("type");
            // Assume Atom iff the "type" attribute is not provided
            final String mediaType = attr != null ? attr.getValue() : IConstants.MEDIA_TYPE_ATOM_XML;
            super.testURIRetrieval(href, mediaType);
            super.testWithUnknownMediaType(href);
        }
    }

}
