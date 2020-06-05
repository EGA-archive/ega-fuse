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

import static okhttp3.mock.Behavior.UNORDERED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.OkHttpClient;
import okhttp3.mock.MockInterceptor;
import uk.ac.ebi.ega.egafuse.config.EgaFuseApplicationConfig;

@TestPropertySource("classpath:application-test.properties")
@ContextConfiguration(classes = EgaFuseApplicationConfig.class)
@RunWith(SpringRunner.class)
public class EgaDatasetServiceTest {

    private EgaDatasetService egaDatasetService;
    private MockInterceptor interceptor;
    private OkHttpClient client;

    @Value("${app.url}")
    private String APP_URL;

    @Mock
    private EgaFileService egaFileService;

    @Mock
    private Token token;

    @Before
    public void before() {
        interceptor = new MockInterceptor(UNORDERED);
        client = new OkHttpClient.Builder().addInterceptor(interceptor).build();
        egaDatasetService = new EgaDatasetService(client, APP_URL, token, egaFileService);
    }

    @Test
    public void testGetDatasets() throws JsonProcessingException {
        List<String> dataset = new ArrayList<String>();
        dataset.add("EGAD00001");
        dataset.add("EGAD00002");
        interceptor.addRule().get(APP_URL + "/metadata/datasets")
                .respond(new ObjectMapper().writeValueAsString(dataset));

        List<EgaDirectory> userDataset = egaDatasetService.getDatasets();
        assertEquals(dataset.get(0), userDataset.get(0).getName());
        assertEquals(dataset.get(1), userDataset.get(1).getName());
    }

    @Test
    public void testGetEmptyDataset() {
        interceptor.addRule().get(APP_URL + "/metadata/datasets").respond(500);

        List<EgaDirectory> userDataset = egaDatasetService.getDatasets();
        assertTrue(userDataset.isEmpty());
    }
}
