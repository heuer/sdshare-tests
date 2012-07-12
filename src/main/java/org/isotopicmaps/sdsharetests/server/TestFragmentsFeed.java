/*
 * Copyright 2010 Lars Heuer (heuer[at]semagia.com). All rights reserved.
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
package org.isotopicmaps.sdsharetests.server;

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
import static org.junit.Assert.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.isotopicmaps.sdsharetests.MediaType;

/**
 * Tests against the 
 * <a href="http://www.egovpt.org/fg/CWA_Part_1b#head-c41ad0664f1b6fb60343d7369e78ad90ea9b1bc3">fragments feed</a>.
 * 
 * @author Lars Heuer (heuer[at]semagia.com) <a href="http://www.semagia.com/">Semagia</a>
 * @version $Rev:$ - $Date:$
 */
@RunWith(Parameterized.class)
public class TestFragmentsFeed extends AbstractServerTestCase {

    private static final Logger LOG = LoggerFactory.getLogger(TestFragmentsFeed.class);

    private final URI _uri;

    public TestFragmentsFeed(final URI uri) {
        _uri = uri;
    }

    @Parameters
    public static Collection<Object> makeTestCases() {
        final Collection<Object> result = new ArrayList<Object>();
        try {
            for (URI uri: Utils.fetchFragmentsFeedURIs()) {
                result.add(new Object[] { uri });
            }
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return result;
    }

    @Test
    public void testFragmentFeed() throws Exception {
        check(_uri);
    }

    // we use this submethod so we can recurse along 'next' links
    private void check(URI uri) throws Exception {
        validate(_uri);
        final Document feed = super.fetchAtomFeedAsDOM(_uri);
        final Nodes entries = query(feed, "atom:feed/atom:entry[sd:resource]");
        if (entries.size() == 0) {
            LOG.warn("No fragment entries found in " + feed.getBaseURI());
            return;
        }
        Nodes links = query(feed, "atom:feed/atom:entry[sd:resource]/atom:link[@rel='" + REL_ALTERNATE + "']");
        int alternates = links.size();
        if (alternates == 0) {
            fail(feed.getBaseURI() + " provides no 'alternate' links to fragments");
        }
        links = query(feed, "atom:feed/atom:entry[sd:resource]/atom:link[@rel='" + REL_FRAGMENT + "']");
        if (links.size() == 0) {
            fail(feed.getBaseURI() + " provides no links to fragments");
        }
        assertEquals(feed.getBaseURI() + " doesn't have the same number of 'alternate' and 'fragment' links",
                    alternates, links.size());

        for (int i=0; i<links.size(); i++) {
            Element link = (Element) links.get(i);
            Attribute attr = link.getAttribute("href");
            assertNotNull("No href attribute available", attr);
            final URI href = URI.create(feed.getBaseURI()).resolve(attr.getValue());
            attr = link.getAttribute("type");
            // TODO: Assume that the media type is required,
            // see <http://projects.topicmapslab.de/issues/3691>
            assertNotNull("Expected a type attribute", attr);
            assertFalse("Expected a non-empty type attribute", attr.getValue().isEmpty());
            super.testURIRetrieval(href, attr.getValue());
            super.testWithUnknownMediaType(href);
        }

        Nodes next = query(feed, "atom:feed/atom:link[rel='next']");
        assertFalse("Expected zero or one 'next' links in " + feed.getBaseURI(),
                    next.size() > 1);
        if (next.size() == 0)
          return;
        Attribute attr = ((Element) next.get(0)).getAttribute("href");
        assertNotNull("No href attribute on 'next' link", attr);

        attr = ((Element) next.get(0)).getAttribute("type");
        MediaType type = MediaType.valueOf(attr.getValue());
        assertTrue("Expected media type on 'next' link to be compatible with " +
                   MEDIA_TYPE_ATOM_XML,
                   MediaType.ATOM_XML.isCompatible(type));
        
        URI href = URI.create(feed.getBaseURI()).resolve(attr.getValue());
        check(href);
    }
}
