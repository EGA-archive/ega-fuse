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

import com.google.api.client.auth.oauth2.PasswordTokenRequest;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.BasicAuthentication;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;

public class Token {
    private HttpTransport HTTP_TRANSPORT;
    private JsonFactory JSON_FACTORY;
    private String username;
    private String password;
    private String egaUserId;
    private String egaUserSecret;
    private String egaUserGrant;
    private String aaiUrl;

    
    public Token(HttpTransport hTTP_TRANSPORT, JsonFactory jSON_FACTORY, String username, String password,
            String egaUserId, String egaUserSecret, String egaUserGrant, String aaiUrl) {
        HTTP_TRANSPORT = hTTP_TRANSPORT;
        JSON_FACTORY = jSON_FACTORY;
        this.username = username;
        this.password = password;
        this.egaUserId = egaUserId;
        this.egaUserSecret = egaUserSecret;
        this.egaUserGrant = egaUserGrant;
        this.aaiUrl = aaiUrl;
    }

    public String getBearerToken() throws Exception {
        TokenResponse response = new PasswordTokenRequest(HTTP_TRANSPORT, JSON_FACTORY,
                new GenericUrl(aaiUrl.concat("/token")), username, password).setGrantType(egaUserGrant)
                        .setClientAuthentication(new BasicAuthentication(egaUserId, egaUserSecret)).execute();

        return response.getAccessToken();
    }

}
