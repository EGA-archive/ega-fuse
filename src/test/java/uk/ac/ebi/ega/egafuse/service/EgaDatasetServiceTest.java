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
import uk.ac.ebi.ega.egafuse.model.File;

@TestPropertySource("classpath:application-test.properties")
@ContextConfiguration(classes = EgaFuseApplicationConfig.class)
@RunWith(SpringRunner.class)
public class EgaDatasetServiceTest {

    private IEgaDatasetService egaDatasetService;
    private MockInterceptor interceptor;
    private OkHttpClient client;

    @Value("${app.url}")
    private String APP_URL;

    @Mock
    private IEgaFileService egaFileService;

    @Mock
    private Token token;

    @Before
    public void before() {
        interceptor = new MockInterceptor(UNORDERED);
        client = new OkHttpClient.Builder().addInterceptor(interceptor).build();
        egaDatasetService = new EgaDatasetService(client, APP_URL, token, egaFileService);
    }

    @Test
    public void getDatasets_WhenGivenDataset_ThenReturnsDatasets() throws JsonProcessingException {
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
    public void getDatasets_WhenGivenException_ThenReturnsNoDataset() {
        interceptor.addRule().get(APP_URL + "/metadata/datasets").respond(500);

        List<EgaDirectory> userDataset = egaDatasetService.getDatasets();
        assertTrue(userDataset.isEmpty());
    }

    @Test
    public void buildFileDirectoryFromFilePath_WhenGivenListAndParentDataset_ThenReturnsTreeStructure() {
        EgaDirectory egaParentdirectory = new EgaDirectory("directory", egaDatasetService, egaFileService);

        File file1 = new File();
        file1.setFileId("EGAF01");
        file1.setFileName("EGAF01.cip");
        file1.setFilePath("A/B/C/");
        EgaFile egaFile = new EgaFile("EGAF01", file1, null);
        List<EgaFile> egaFiles = new ArrayList<>();
        egaFiles.add(egaFile);

        egaDatasetService.buildSubDirectoryFromFilePath(egaFiles, egaParentdirectory);

        EgaDirectory firstDirectory = (EgaDirectory) egaParentdirectory.contents.get(0);
        EgaDirectory secondDirectory = (EgaDirectory) firstDirectory.contents.get(0);
        EgaDirectory thirdDirectory = (EgaDirectory) secondDirectory.contents.get(0);
        EgaFile file = (EgaFile) thirdDirectory.contents.get(0);

        assertEquals(file1.getFilePath().split("/")[0], firstDirectory.getName());
        assertEquals(file1.getFilePath().split("/")[1], secondDirectory.getName());
        assertEquals(file1.getFilePath().split("/")[2], thirdDirectory.getName());
        assertEquals(egaFile.getName(), file.getName());
    }

    @Test
    public void buildFileDirectoryFromFilePath_WhenGivenListHaving2FilesAndParentDataset_ThenReturnsTreeStructure() {
        EgaDirectory egaParentdirectory = new EgaDirectory("directory", egaDatasetService, egaFileService);

        File file1 = new File();
        file1.setFileId("EGAF01");
        file1.setFileName("EGAF01.cip");
        file1.setFilePath("A/B/C");
        EgaFile egaFile1 = new EgaFile("EGAF01", file1, null);

        File file2 = new File();
        file2.setFileId("EGAF02");
        file2.setFileName("EGAF02.cip");
        file2.setFilePath("A/B/D");
        EgaFile egaFile2 = new EgaFile("EGAF02", file2, null);

        List<EgaFile> egaFiles = new ArrayList<>();
        egaFiles.add(egaFile1);
        egaFiles.add(egaFile2);

        egaDatasetService.buildSubDirectoryFromFilePath(egaFiles, egaParentdirectory);

        EgaDirectory firstDirectory = (EgaDirectory) egaParentdirectory.contents.get(0);
        EgaDirectory secondDirectory = (EgaDirectory) firstDirectory.contents.get(0);
        EgaDirectory thirdDirectoryFirstFile = (EgaDirectory) secondDirectory.contents.get(0);
        EgaDirectory thirdDirectorySecondFile = (EgaDirectory) secondDirectory.contents.get(1);
        EgaFile fileFirst = (EgaFile) thirdDirectoryFirstFile.contents.get(0);
        EgaFile fileSecond = (EgaFile) thirdDirectorySecondFile.contents.get(0);

        assertEquals(file1.getFilePath().split("/")[0], firstDirectory.getName());
        assertEquals(file1.getFilePath().split("/")[1], secondDirectory.getName());
        assertEquals(file1.getFilePath().split("/")[2], thirdDirectoryFirstFile.getName());
        assertEquals(file2.getFilePath().split("/")[2], thirdDirectorySecondFile.getName());
        assertEquals(egaFile1.getName(), fileFirst.getName());
        assertEquals(egaFile2.getName(), fileSecond.getName());
    }

}
