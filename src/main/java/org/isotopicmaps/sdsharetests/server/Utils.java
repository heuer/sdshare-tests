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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

import org.isotopicmaps.sdsharetests.IConstants;

import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import nu.xom.XPathContext;

/**
 * 
 * 
 * @author Lars Heuer (heuer[at]semagia.com) <a href="http://www.semagia.com/">Semagia</a>
 * @version $Rev:$ - $Date:$
 */
final class Utils implements IConstants {

    private static final XPathContext _XPATH_CTX;

    static {
        _XPATH_CTX = new XPathContext("atom", NS_ATOM);
        _XPATH_CTX.addNamespace("sd", NS_SDSHARE);
    }

    private Utils() {
        // noop.
    }

    public static URI getServerAddress() {
        final String addr = System.getProperty(SERVER_ADDRESS_PROPERTY);
        if (addr == null) {
            throw new IllegalStateException("The system property '" + SERVER_ADDRESS_PROPERTY + "' is not set");
        }
        return URI.create(addr);
    }

    public static XPathContext getDefaultXPathContext() {
        return _XPATH_CTX;
    }

    public static Document makeDocument(final InputStream in, final URI base) throws Exception {
        return makeDocument(in, base.toASCIIString());
    }

    public static Document makeDocument(final InputStream in, final String base) throws Exception {
        return new Builder().build(in, base);
    }

    /**
     * 
     *
     * @param uri
     * @return
     * @throws MalformedURLException
     * @throws IOException
     */
    public static HttpURLConnection connect(final URI uri) throws IOException {
        return (HttpURLConnection) uri.toURL().openConnection();
    }

    /**
     * 
     *
     * @param uri
     * @param acceptHeader
     * @return
     * @throws MalformedURLException
     * @throws IOException
     */
    public static HttpURLConnection connect(final URI uri, final String acceptHeader) throws IOException {
        final HttpURLConnection conn = connect(uri);
        conn.setRequestProperty("Accept", acceptHeader);
        return conn;
    }

    /**
     * Returns the overview feed.
     *
     * @return The overview feed as DOM.
     * @throws Exception In case of an error.
     */
    public static Document fetchOverviewFeed() throws Exception {
        final URI uri = getServerAddress();
        final HttpURLConnection conn = connect(uri, MEDIA_TYPE_ATOM_XML);
        return makeDocument(conn.getInputStream(), uri);
    }

    /**
     * Returns a feed.
     *
     * @return The overview feed as DOM.
     * @throws Exception In case of an error.
     */
    public static Document fetchFeed(final URI uri) throws Exception {
        final HttpURLConnection conn = connect(uri, MEDIA_TYPE_ATOM_XML);
        return makeDocument(conn.getInputStream(), uri);
    }

    public static Collection<URI> fetchCollectionFeedURIs() throws Exception {
        final Document overviewFeed = fetchOverviewFeed();
        return linksToURIs(overviewFeed.getBaseURI(), overviewFeed.query("atom:feed/atom:entry/atom:link[@rel='" + REL_COLLECTION_FEED + "'][not(@type) or @type='application/atom+xml']", getDefaultXPathContext()));
    }

    public static Collection<URI> fetchFragmentsFeedURIs() throws Exception {
        final Collection<URI> result = new ArrayList<URI>();
        for (URI uri: Utils.fetchCollectionFeedURIs()) {
            Document doc = fetchFeed(uri);
            Nodes nodes = doc.query("atom:feed/atom:entry/atom:link[@rel='" + REL_FRAGMENTS_FEED + "'][not(@type) or @type='application/atom+xml']", getDefaultXPathContext());
            result.addAll(Utils.linksToURIs(doc.getBaseURI(), nodes));
        }
        return result;
    }

    public static Collection<URI> fetchSnapshotsFeedURIs() throws Exception {
        final Collection<URI> result = new ArrayList<URI>();
        for (URI uri: Utils.fetchCollectionFeedURIs()) {
            Document doc = fetchFeed(uri);
            Nodes nodes = doc.query("atom:feed/atom:entry/atom:link[@rel='" + REL_SNAPSHOTS_FEED + "'][not(@type) or @type='application/atom+xml']", getDefaultXPathContext());
            result.addAll(Utils.linksToURIs(doc.getBaseURI(), nodes));
        }
        return result;
    }

    public static Collection<URI> linksToURIs(final String base, final Nodes links) {
        final Collection<URI> result = new ArrayList<URI>();
        final URI baseURI = URI.create(base);
        for (int i=0; i<links.size(); i++) {
            Element link = (Element) links.get(i);
            Attribute attr = link.getAttribute("href");
            result.add(baseURI.resolve(attr.getValue()));
        }
        return result;
    }

}
