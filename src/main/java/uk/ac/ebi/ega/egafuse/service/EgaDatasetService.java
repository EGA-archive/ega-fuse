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

public class EgaDatasetService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EgaDatasetService.class);
    private EgaFileService egaFileService;
    private OkHttpClient okHttpClient;
    private String apiURL;
    private Token token;
    private ObjectMapper mapper;

    public EgaDatasetService(OkHttpClient okHttpClient, String apiURL, Token token, EgaFileService egaFileService) {
        this.okHttpClient = okHttpClient;
        this.apiURL = apiURL;
        this.token = token;
        this.egaFileService = egaFileService;
        this.mapper = new ObjectMapper();
    }

    public List<EgaDirectory> getDatasets() {
        try {
            Request datasetRequest = new Request.Builder().url(apiURL + "/metadata/datasets")
                    .addHeader("Authorization", "Bearer " + token.getBearerToken()).build();

            try (Response response = okHttpClient.newCall(datasetRequest).execute()) {
                return buildResponseGetDataset(response);
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

    private List<EgaDirectory> buildResponseGetDataset(final Response response)
            throws IOException, ClientProtocolException {
        final int status = response.code();
        switch (status) {
        case 200:
            List<String> datasets = mapper.readValue(response.body().string(), new TypeReference<List<String>>() {
            });
            List<EgaDirectory> egaDirectorys = new ArrayList<>();
            for (String dataset : datasets) {
                EgaDirectory egaDirectory = new EgaDirectory(dataset, this, egaFileService);
                egaDirectorys.add(egaDirectory);
            }
            return egaDirectorys;
        default:
            LOGGER.error("status: {}", status);
            throw new ClientProtocolException(response.body().string());
        }
    }
}
