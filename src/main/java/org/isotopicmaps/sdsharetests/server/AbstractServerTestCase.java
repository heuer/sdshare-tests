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

import junit.framework.TestCase;

import org.isotopicmaps.sdsharetests.IConstants;
import org.isotopicmaps.sdsharetests.MediaType;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.XPathContext;

/**
 * Abstract test case which provides some useful utility methods.
 * 
 * @author Lars Heuer (heuer[at]semagia.com) <a href="http://www.semagia.com/">Semagia</a>
 * @version $Rev:$ - $Date:$
 */
public class AbstractServerTestCase extends TestCase {

    public static final String SERVER_ADDRESS_PROPERTY = "org.isotopicmaps.sdshare.serveraddress";

    private static final String _UNKNOWN_MEDIA_TYPE = "application/x-hello-iam+unknown";

    protected static final XPathContext _XPATH_CTX;

    static {
        _XPATH_CTX = new XPathContext("atom", IConstants.NS_ATOM);
        _XPATH_CTX.addNamespace("sd", IConstants.NS_SDSHARE);
    }

    protected URI _serverAddress;

    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        final String baseURI = System.getProperty(SERVER_ADDRESS_PROPERTY);
        if (baseURI == null) {
            fail("The server address was not found. Undefined system property '" + SERVER_ADDRESS_PROPERTY + "'");
        }
        _serverAddress = URI.create(baseURI);
    }

    /**
     * Returns the address of the server.
     *
     * @return The server address, never {@code null}.
     */
    protected final URI getServerAddress() {
        return _serverAddress;
    }

    /**
     * 
     *
     * @param uri
     * @return
     * @throws MalformedURLException
     * @throws IOException
     */
    protected HttpURLConnection connect(final URI uri) throws IOException {
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
    protected HttpURLConnection connect(final URI uri, final String acceptHeader) throws IOException {
        final HttpURLConnection conn = connect(uri);
        conn.setRequestProperty("Accept", acceptHeader);
        return conn;
    }

    /**
     * Helper method to fetch an Atom feed.
     * 
     * The feed is not validated.
     *
     * @param uri
     * @return
     * @throws IOException
     */
    protected InputStream fetchAtomFeed(final URI uri) throws IOException {
        final HttpURLConnection conn = connect(uri, IConstants.MEDIA_TYPE_ATOM_XML);
        assertEquals(HttpURLConnection.HTTP_OK, conn.getResponseCode());
        assertTrue(MediaType.ATOM_XML.isCompatible(MediaType.valueOf(conn.getContentType())));
        return conn.getInputStream();
    }

    private Document _makeDocument(final InputStream in, final URI base) throws Exception {
        return _makeDocument(in, base.toASCIIString());
    }

    private Document _makeDocument(final InputStream in, final String base) throws Exception {
        return new Builder().build(in, base);
    }

    /**
     * Executes an XPath query against the provided {@code node} using the default XPathContext.
     *
     * @node The context node.
     * @query The XPath expression.
     */
    protected Nodes query(final Node node, final String query) {
        return node.query(query, _XPATH_CTX);
    }

    /**
     * Tries to open a connection to the provided URI (which must be a valid URL) with
     * an unknown media type and expects a 
     *
     * @param uri The URI to test
     */
    protected void testWithUnknownMediaType(final URI uri) throws Exception {
        final HttpURLConnection conn = connect(uri, _UNKNOWN_MEDIA_TYPE);
        assertEquals("Expected a Not Acceptable response for " + uri, HttpURLConnection.HTTP_NOT_ACCEPTABLE, conn.getResponseCode());
    }

    /**
     * Does some basic tests (i.e. if the atom:id is provided etc.) to validate
     * an Atom 1.0 feed.
     *
     * @doc The feed document
     */
    protected void testAtomFeedValidity(final Document doc) {
        Nodes nodes = query(doc, "atom:feed");
        assertEquals("Expected exactly one atom:feed element", 1, nodes.size());
        final Node feed = nodes.get(0);
        validateEntryCommons(feed);
        // If the feed has no author, all entries must have an author
        nodes = query(feed, "atom:author");
        boolean feedAuthor = false;
        if (nodes.size() > 0) {
            feedAuthor = true;
            for (int i=0; i<nodes.size(); i++) {
                validateAuthor(nodes.get(i));
            }
        }
        // Check each atom:entry for validity
        final Nodes entries = query(feed, "atom:entry");
        for (int i=0; i<entries.size(); i++) {
            Node entry = entries.get(i);
            validateEntryCommons(entry);
            final Nodes authors = query(entry, "atom:author");
            if (!feedAuthor) {
                // atom:feed has no author, all atom:entry children MUST have an author.
                assertTrue("atom:feed has no atom:author, each atom:entry must have 1..n atom:author children", 
                            authors.size() > 0);
            }
            for (int j=0; j<authors.size(); j++) {
                validateAuthor(authors.get(i));
            }
        }
    }

    protected void validateEntryCommons(final Node node) {
        Nodes nodes = query(node, "atom:id");
        assertEquals("No atom:id found", 1, nodes.size());
        final Node id = nodes.get(0);
        assertFalse("atom:id must be an IRI", id.getValue().isEmpty());
        nodes = query(node, "atom:title");
        assertEquals("No atom:title found", 1, nodes.size());
        final Node title = nodes.get(0);
        assertFalse("atom:title should not be empty", title.getValue().isEmpty());
        nodes = query(node, "atom:updated");
        assertEquals("No atom:updated found", 1, nodes.size());
        final Node updated = nodes.get(0);
        assertFalse("atom:updated must not be empty", updated.getValue().isEmpty());
    }

    /**
     * Validates atom:author.
     *
     * @author The author to validate.
     */
    protected final void validateAuthor(final Node author) {
        assertNotNull(author);
        // atom:name is normative
        Nodes nodes = query(author, "atom:name");
        assertEquals(1, nodes.size());
        assertFalse("atom:name should not be empty", nodes.get(0).getValue().isEmpty());
        // atom:email is optional
        nodes = query(author, "atom:email");
        if (nodes.size() != 0) {
            assertEquals("atom:author must have 0..1 atom:email children", 1, nodes.size());
            assertFalse("atom:email must be empty", nodes.get(0).getValue().isEmpty());
        }
        // atom:uri is optional
        nodes = query(author, "atom:uri");
        if (nodes.size() != 0) {
            assertEquals("atom:uri must have 0..1 atom:uri children", 1, nodes.size());
            assertFalse("atom:uri must be an IRI", nodes.get(0).getValue().isEmpty());
        }
    }

    /**
     * Returns the overview feed.
     *
     * The feed is validated.
     *
     * @return The overview feed as DOM.
     * @throws Exception In case of an error.
     */
    protected Document fetchOverviewFeed() throws Exception {
        return fetchAtomFeedAsDOM(getServerAddress());
    }

    /**
     * Returns the feed under the specified URI.
     *
     * The feed is validated.
     *
     * @param uri The URI to retrieve the feed from.
     * @return The feed as DOM.
     * @throws Exception In case of an error.
     */
    protected Document fetchAtomFeedAsDOM(final URI uri) throws Exception {
        final Document doc = _makeDocument(fetchAtomFeed(uri), uri);
        testAtomFeedValidity(doc);
        return doc;
    }

    /**
     * Checks the returned media type and if the collection feed exists.
     *
     * @param uri The IRI to check.
     * @param mediaType The expected media type of the IRI.
     * @throws IOException In case of an error.
     */
    protected void testURIRetrieval(final URI uri, final String mediaType) throws IOException {
        final HttpURLConnection conn = connect(uri, mediaType);
        final MediaType requestMediaType = MediaType.valueOf(mediaType);
        final MediaType responseMediaType = MediaType.valueOf(conn.getContentType());
        assertEquals(HttpURLConnection.HTTP_OK, conn.getResponseCode());
        assertTrue(requestMediaType.isCompatible(responseMediaType));
    }

}
