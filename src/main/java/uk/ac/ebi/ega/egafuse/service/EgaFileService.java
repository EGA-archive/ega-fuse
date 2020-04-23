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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import uk.ac.ebi.ega.egafuse.exception.ClientProtocolException;
import uk.ac.ebi.ega.egafuse.model.File;

public class EgaFileService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EgaFileService.class);
    private OkHttpClient okHttpClient;
    private String apiURL;
    private Token token;
    private ObjectMapper mapper;

    public EgaFileService(OkHttpClient okHttpClient, String apiURL, Token token) {
        this.okHttpClient = okHttpClient;
        this.apiURL = apiURL;
        this.token = token;
        this.mapper = new ObjectMapper();
    }

    public List<EgaFile> getFiles(EgaDirectory egaDirectory) {
        String datasetId = egaDirectory.getName();
        if (datasetId.endsWith("/")) {
            datasetId = datasetId.substring(0, datasetId.length() - 1);
        }

        try {
            Request fileRequest = new Request.Builder()
                    .url(apiURL.concat("/metadata/datasets/").concat(datasetId).concat("/files"))
                    .addHeader("Authorization", "Bearer " + token.getBearerToken()).build();

            try (Response response = okHttpClient.newCall(fileRequest).execute()) {
                return buildResponseGetFiles(response, egaDirectory);
            } catch (IOException e) {
                throw new IOException("Unable to execute request. Can be retried.", e);
            } catch (ClientProtocolException e) {
                throw e;
            }
        } catch (Exception e) {
            LOGGER.error("Error in get dataset - {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    private List<EgaFile> buildResponseGetFiles(final Response response, EgaDirectory egaDirectory)
            throws IOException, ClientProtocolException {
        final int status = response.code();
        switch (status) {
        case 200:
            List<File> files = mapper.readValue(response.body().string(), new TypeReference<List<File>>() {
            });
            List<EgaFile> egaFiles = new ArrayList<>();
            for (File file : files) {
                String filename = file.getDisplayFileName();
                if (filename.contains("/")) {
                    filename = filename.substring(filename.lastIndexOf("/") + 1);
                }

                String type = "SOURCE";
                if (filename.toLowerCase().endsWith(".gpg")) {
                    type = "GPG";
                } else if (filename.toLowerCase().endsWith(".cip")) {
                    type = "CIP";
                }

                egaFiles.add(new EgaFile(filename.substring(0, filename.length() - 4), type, egaDirectory, file));
            }
            return egaFiles;
        default:
            LOGGER.error("status: {}", status);
            throw new ClientProtocolException(response.body().string());
        }
    }
}
