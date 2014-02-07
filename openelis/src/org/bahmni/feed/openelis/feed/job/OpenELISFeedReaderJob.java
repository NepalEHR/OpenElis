/*
* The contents of this file are subject to the Mozilla Public License
* Version 1.1 (the "License"); you may not use this file except in
* compliance with the License. You may obtain a copy of the License at
* http://www.mozilla.org/MPL/ 
* 
* Software distributed under the License is distributed on an "AS IS"
* basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
* License for the specific language governing rights and limitations under
* the License.
* 
* The Original Code is OpenELIS code.
* 
* Copyright (C) The Minnesota Department of Health.  All Rights Reserved.
*/

package org.bahmni.feed.openelis.feed.job;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.bahmni.feed.openelis.AtomFeedProperties;
import org.bahmni.feed.openelis.feed.client.AtomFeedClientFactory;
import org.bahmni.webclients.Authenticator;
import org.bahmni.webclients.ClientCookies;
import org.bahmni.webclients.ConnectionDetails;
import org.bahmni.webclients.HttpClient;
import org.ict4h.atomfeed.client.service.AtomFeedClient;
import org.ict4h.atomfeed.client.service.EventWorker;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public abstract class OpenELISFeedReaderJob implements Job {

    protected static Map<Class, AtomFeedClient> atomFeedClients = new HashMap<>();
    private final Logger logger;

    protected OpenELISFeedReaderJob(Logger logger) {
        logger.info("Started");
        this.logger = logger;
    }

    protected abstract EventWorker createWorker(HttpClient authenticatedWebClient, String urlPrefix);
    
    protected abstract Authenticator getAuthenticator(ConnectionDetails connectionDetails);

    protected abstract String getFeedName();

    protected abstract ConnectionDetails getConnectionDetails();

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        try {
            processEvents(jobExecutionContext);
        } catch (Exception e) {
            try {
                if (ExceptionUtils.getStackTrace(e).contains("HTTP response code: 401")) {
                    initializeAtomFeedClient();
                }
            } finally {
                logger.error(e.getMessage(), e);
            }
        }
    }
    
    protected void processEvents(JobExecutionContext jobExecutionContext) {
        if (atomFeedClients.get(this.getClass()) == null)
            initializeAtomFeedClient();
        AtomFeedClient atomFeedClient = atomFeedClients.get(this.getClass());
        atomFeedClient.processEvents();
    }

    protected void processFailedEvents(JobExecutionContext jobExecutionContext) {
        if (atomFeedClients.get(this.getClass()) == null)
            initializeAtomFeedClient();
        AtomFeedClient atomFeedClient = atomFeedClients.get(this.getClass());
        atomFeedClient.processFailedEvents();
    }

    private void initializeAtomFeedClient() {
        AtomFeedClient atomFeedClient = createAtomFeedClient(AtomFeedProperties.getInstance(), new AtomFeedClientFactory());

        if (atomFeedClient != null) {
            atomFeedClients.put(this.getClass(), atomFeedClient);
        }
    }

    private AtomFeedClient createAtomFeedClient(AtomFeedProperties atomFeedProperties, AtomFeedClientFactory atomFeedClientFactory) {
        ConnectionDetails connectionDetails = getConnectionDetails();
        
        String authUri = connectionDetails.getAuthUrl();
        String urlString = getURLPrefix(authUri);

        HttpClient authenticatedWebClient = new HttpClient(connectionDetails, getAuthenticator(connectionDetails));

        ClientCookies cookies = getCookies(authenticatedWebClient, authUri);
        return atomFeedClientFactory.getFeedClient(atomFeedProperties,
                getFeedName(), createWorker(authenticatedWebClient, urlString), cookies);
    }

    private ClientCookies getCookies(HttpClient authenticatedWebClient, String urlString) {
        try {
            return authenticatedWebClient.getCookies(new URI(urlString));
        } catch (URISyntaxException e) {
            throw new RuntimeException("Is not a valid URI - " + urlString);
        }
    }

    //  TODO : should not depend on auth url to determine the prefix
    private static String getURLPrefix(String authenticationURI) {
        URL openMRSAuthURL;
        try {
            openMRSAuthURL = new URL(authenticationURI);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Is not a valid URI - " + authenticationURI);
        }
        return String.format("%s://%s", openMRSAuthURL.getProtocol(), openMRSAuthURL.getAuthority());
    }
}