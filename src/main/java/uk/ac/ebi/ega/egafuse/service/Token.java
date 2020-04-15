/*
 *
 * Copyright 2020 EMBL - European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package uk.ac.ebi.ega.egafuse.service;

import org.springframework.beans.factory.annotation.Value;

import com.google.api.client.auth.oauth2.PasswordTokenRequest;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.BasicAuthentication;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

public class Token {

    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final JsonFactory JSON_FACTORY = new JacksonFactory();

    @Value("${username}")
    private String username;
    @Value("${password}")
    private String password;

    @Value("${ega.userId}")
    private String egaUserId;
    @Value("${ega.userSecret}")
    private String egaUserSecret;
    @Value("${ega.userGrant}")
    private String egaUserGrant;

    @Value("${aai.server.url}")
    private String aaiUrl;

    public String getBearerToken() throws Exception {
        TokenResponse response = new PasswordTokenRequest(HTTP_TRANSPORT, JSON_FACTORY,
                new GenericUrl(aaiUrl.concat("/token")), username, password).setGrantType(egaUserGrant)
                        .setClientAuthentication(new BasicAuthentication(egaUserId, egaUserSecret)).execute();

        return response.getAccessToken();
    }

}
