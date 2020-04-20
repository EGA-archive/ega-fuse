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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jnr.ffi.Pointer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import ru.serce.jnrfuse.FuseFillDir;
import ru.serce.jnrfuse.struct.FileStat;
import uk.ac.ebi.ega.egafuse.exception.ClientProtocolException;
import uk.ac.ebi.ega.egafuse.model.File;

public class EgaDirectory extends EgaPath {

    private static final Logger LOGGER = LoggerFactory.getLogger(EgaDirectory.class);

    protected List<EgaPath> contents = new ArrayList<>();
    private OkHttpClient okHttpClient;
    private String apiURL;
    private Token token;
    private ObjectMapper mapper;

    public EgaDirectory(String name, EgaDirectory parent, OkHttpClient okHttpClient, String apiURL, Token token) {
        super(name, parent);
        this.apiURL = apiURL;
        this.okHttpClient = okHttpClient;
        this.token = token;
        this.mapper = new ObjectMapper();
    }

    public synchronized void add(EgaPath p) {
        contents.add(p);
        p.setParent(this);
    }

    public synchronized void deleteChild(EgaPath child) {
        contents.remove(child);
    }

    @Override
    public EgaPath find(String path) {
        if (super.find(path) != null) {
            return super.find(path);
        }
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        synchronized (this) {
            if (!path.contains("/")) {
                for (EgaPath p : contents) {
                    if (p.getName().equals(path)) {
                        return p;
                    }
                }
                return null;
            }
            String nextName = path.substring(0, path.indexOf("/"));
            String rest = path.substring(path.indexOf("/"));
            for (EgaPath p : contents) {
                if (p.getName().equals(nextName)) {
                    return p.find(rest);
                }
            }
        }
        return null;
    }

    @Override
    public void getattr(FileStat stat) {
        stat.st_mode.set(FileStat.S_IFDIR | 0777);
        stat.st_uid.set(0);
        stat.st_gid.set(0);
    }

    public synchronized void read(Pointer buf, FuseFillDir filler) {
        if (contents == null || contents.size() == 0) {
            if (getName().equalsIgnoreCase("datasets")) {
                getDatasets();
            } else {
                getFiles();
            }
        }

        for (EgaPath p : contents) {
            filler.apply(buf, p.getName(), null, 0);
        }
    }

    private void getDatasets() {
        try {
            Request datasetRequest = new Request.Builder().url(apiURL + "/metadata/datasets")
                    .addHeader("Authorization", "Bearer " + token.getBearerToken()).build();

            try (Response response = okHttpClient.newCall(datasetRequest).execute()) {
                buildResponseGetDataset(response);
            } catch (IOException e) {
                throw new IOException("Unable to execute request. Can be retried.", e);
            } catch (ClientProtocolException e) {
                throw e;
            }
        } catch (Exception e) {
            LOGGER.error("Error in get dataset - {}", e.getMessage());
        }
    }

    private void buildResponseGetDataset(final Response response) throws IOException, ClientProtocolException {
        final int status = response.code();
        switch (status) {
        case 200:
            List<String> datasets = mapper.readValue(response.body().string(), new TypeReference<List<String>>() {
            });
            for (String dataset : datasets) {
                EgaDirectory egaDirectory = new EgaDirectory(dataset, this, okHttpClient, apiURL, token);
                contents.add(egaDirectory);
            }
            break;
        default:
            LOGGER.error("status: {}", status);
            throw new ClientProtocolException(response.body().string());
        }
    }

    private void getFiles() {
        String datasetId = this.getName();
        if (datasetId.endsWith("/")) {
            datasetId = datasetId.substring(0, datasetId.length() - 1);
        }

        try {
            Request fileRequest = new Request.Builder()
                    .url(apiURL.concat("/metadata/datasets/").concat(datasetId).concat("/files"))
                    .addHeader("Authorization", "Bearer " + token.getBearerToken()).build();

            try (Response response = okHttpClient.newCall(fileRequest).execute()) {
                buildResponseGetFiles(response);
            } catch (IOException e) {
                throw new IOException("Unable to execute request. Can be retried.", e);
            } catch (ClientProtocolException e) {
                throw e;
            }
        } catch (Exception e) {
            LOGGER.error("Error in get dataset - {}", e.getMessage());
        }
    }

    private void buildResponseGetFiles(final Response response) throws IOException, ClientProtocolException {
        final int status = response.code();
        switch (status) {
        case 200:
            List<File> files = mapper.readValue(response.body().string(), new TypeReference<List<File>>() {
            });
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
                
                EgaFile newFile = new EgaFile(filename.substring(0, filename.length() - 4), type, this, file);
                contents.add(newFile);
            }
            break;
        default:
            LOGGER.error("status: {}", status);
            throw new ClientProtocolException(response.body().string());
        }
    }
}
