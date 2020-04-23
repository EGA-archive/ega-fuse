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

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import jnr.ffi.Pointer;
import ru.serce.jnrfuse.FuseFillDir;
import uk.ac.ebi.ega.egafuse.config.EgaFuseApplicationConfig;

@TestPropertySource("classpath:application-test.properties")
@ContextConfiguration(classes = EgaFuseApplicationConfig.class)
@RunWith(SpringRunner.class)
public class EgaDirectoryTest {

    private EgaDirectory egaParentdirectory;
    
    @Mock
    private EgaDatasetService egaDatasetService;
    
    @Mock
    private EgaFileService egaFileService;
    
    @Mock
    Pointer pointer;
    
    @Mock
    private FuseFillDir fuseFillDir;
    
    @Before
    public void before() {
        egaParentdirectory = new EgaDirectory("dataset");
    }
    
    @Test
    public void testFindPath() {
        EgaDirectory directory = new EgaDirectory("dataset1", egaDatasetService, egaFileService);
        egaParentdirectory.add(directory);
        EgaPath path = egaParentdirectory.find(directory.getName());
        assertEquals(path.getName(), directory.getName());
    }
    
    @Test
    public void testReadDatasets() {
        EgaDirectory directory = new EgaDirectory("dataset1", egaDatasetService, egaFileService);
        egaParentdirectory.add(directory);        
        List<EgaDirectory> egaDirectorys = new ArrayList<>();
        egaDirectorys.add(directory);
        Mockito.when(egaDatasetService.getDatasets()).thenReturn(egaDirectorys);
        egaParentdirectory.read(pointer, fuseFillDir);
        
        List<EgaPath> contents  = egaParentdirectory.contents;
        assertEquals(egaDirectorys.get(0).getName(), contents.get(0).getName());
    }
    
    @Test
    public void testReadFiles() {
        EgaDirectory directory = new EgaDirectory("dataset1", egaDatasetService, egaFileService);
        EgaFile egaFile = new EgaFile("files1", directory);
        List<EgaFile> egaFiles = new ArrayList<>();
        egaFiles.add(egaFile);        
        Mockito.when(egaFileService.getFiles(directory)).thenReturn(egaFiles);
        directory.read(pointer, fuseFillDir);

        List<EgaPath> contents  = directory.contents;
        assertEquals(egaFile.getName(), contents.get(0).getName());
    }
    
    @Test
    public void testDeleteChild() {
        EgaDirectory directory = new EgaDirectory("dataset1", egaDatasetService, egaFileService);
        EgaFile egaFile = new EgaFile("files1", directory);
        List<EgaFile> egaFiles = new ArrayList<>();
        egaFiles.add(egaFile);        

        directory.deleteChild(egaFile);
        List<EgaPath> contents  = directory.contents;
        assertTrue(contents.isEmpty());
    }
}
