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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;

import org.sdshare.sdsharetests.IConstants;
import org.sdshare.sdsharetests.MediaType;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import static org.junit.Assert.*;

import nu.xom.Document;
import nu.xom.Node;
import nu.xom.Nodes;

import com.thaiopensource.validate.ValidationDriver;

/**
 * Abstract test case which provides some useful utility methods.
 */
abstract class AbstractServerTestCase implements IConstants{

    private static final String _UNKNOWN_MEDIA_TYPE = "application/x-hello-iam+unknown";

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
        final HttpURLConnection conn = Utils.connect(uri, MEDIA_TYPE_ATOM_XML);
        assertEquals("Expected a status code 200 for " + uri.toString(), HttpURLConnection.HTTP_OK, conn.getResponseCode());
        assertTrue("Expected media type application/atom+xml for " + uri.toString(), MediaType.ATOM_XML.isCompatible(MediaType.valueOf(conn.getContentType())));
        return conn.getInputStream();
    }

    /**
     * Executes an XPath query against the provided {@code node} using the default XPathContext.
     *
     * @node The context node.
     * @query The XPath expression.
     */
    protected static Nodes query(final Node node, final String query) {
        return node.query(query, Utils.getDefaultXPathContext());
    }

    /**
     * Tries to open a connection to the provided URI (which must be a valid URL) with
     * an unknown media type and expects a 
     *
     * @param uri The URI to test
     */
    protected void testWithUnknownMediaType(final URI uri) throws Exception {
        //TODO: Think about this
//        final HttpURLConnection conn = Utils.connect(uri, _UNKNOWN_MEDIA_TYPE);
//        assertEquals("Expected a Not Acceptable response for " + uri, HttpURLConnection.HTTP_NOT_ACCEPTABLE, conn.getResponseCode());
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
        return fetchAtomFeedAsDOM(Utils.getServerAddress());
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
        final Document doc = Utils.makeDocument(fetchAtomFeed(uri), uri);
        validate(uri);
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
        final HttpURLConnection conn = Utils.connect(uri, mediaType);
        final MediaType requestMediaType = MediaType.valueOf(mediaType);
        final MediaType responseMediaType = MediaType.valueOf(conn.getContentType());
        assertEquals("Expected a status code 200 for " + uri.toString(), HttpURLConnection.HTTP_OK, conn.getResponseCode());
        assertTrue("Expected a compatible media type to " + mediaType + ", got " + responseMediaType.toString(), requestMediaType.isCompatible(responseMediaType));
    }

    /**
     * Validates the document at the given URI against the Atom schema.
     */
    protected void validate(URI uri) throws IOException, SAXException {
        ValidationDriver driver = new ValidationDriver();

        // first, locate and load the schema
        ClassLoader cloader = Thread.currentThread().getContextClassLoader();
        InputStream istream = cloader.getResourceAsStream("atom.rng");
        assertNotNull("Couldn't load Atom schema", istream);
        driver.loadSchema(new InputSource(istream));

        // second, go go go!
        assertTrue("Document " + uri + " failed to validate; see stdout",
                   driver.validate(new InputSource(uri.toString())));
    }
}
