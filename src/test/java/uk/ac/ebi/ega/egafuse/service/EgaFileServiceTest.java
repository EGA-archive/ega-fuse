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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;

import jnr.ffi.Pointer;
import okhttp3.OkHttpClient;
import okhttp3.mock.MockInterceptor;
import uk.ac.ebi.ega.egafuse.config.EgaFuseApplicationConfig;
import uk.ac.ebi.ega.egafuse.model.CacheKey;
import uk.ac.ebi.ega.egafuse.model.File;

@TestPropertySource("classpath:application-test.properties")
@ContextConfiguration(classes = EgaFuseApplicationConfig.class)
@RunWith(SpringRunner.class)
public class EgaFileServiceTest {
    private EgaFileService egaFileService;
    private MockInterceptor interceptor;
    private OkHttpClient client;
    private ObjectMapper objectMapper;

    @Value("${api.chunksize}")
    private long CHUNK_SIZE;

    @Value("${cache.prefetch}")
    private int PREFETCH;

    @Value("${app.url}")
    private String APP_URL;

    @Mock
    private AsyncLoadingCache<CacheKey, byte[]> cache;

    @Mock
    private Token token;

    @Mock
    private Pointer pointer;

    @Before
    public void before() {
        objectMapper = new ObjectMapper();
        interceptor = new MockInterceptor(UNORDERED);
        client = new OkHttpClient.Builder().addInterceptor(interceptor).build();
        egaFileService = new EgaFileService(client, APP_URL, CHUNK_SIZE, PREFETCH, token, cache);
    }

    @Test
    public void testGetFiles() throws JsonProcessingException {
        EgaDirectory userDirectory = new EgaDirectory("EGAD00001", null, null);
        List<File> files = new ArrayList<>();
        File file = new File();
        file.setFileId("EGAF00001");
        file.setDisplayFileName("test.cip");
        file.setFileSize(100l);
        files.add(file);
        interceptor.addRule()
                .get(APP_URL.concat("/metadata/datasets/").concat(userDirectory.getName()).concat("/files"))
                .respond(objectMapper.writeValueAsString(files));

        List<EgaFile> userFile = egaFileService.getFiles(userDirectory);
        File responseFile = userFile.get(0).getFile();
        assertEquals(file.getFileId(), responseFile.getFileId());
        assertEquals(file.getFileSize() - 16, responseFile.getFileSize());
        assertEquals(file.getDisplayFileName().substring(0, file.getDisplayFileName().length() - 4),
                userFile.get(0).getName());

    }

    @Test
    public void testGetFiles_Cip_And_Gpg() throws JsonProcessingException {
        EgaDirectory userDirecory = new EgaDirectory("EGAD00001", null, null);
        List<File> files = new ArrayList<>();
        File file1 = new File();
        file1.setDisplayFileName("/test1.cip");
        File file2 = new File();
        file2.setDisplayFileName("/test2.gpg");
        files.add(file1);
        files.add(file2);

        interceptor.addRule().get(APP_URL.concat("/metadata/datasets/").concat(userDirecory.getName()).concat("/files"))
                .respond(objectMapper.writeValueAsString(files));

        List<EgaFile> userFile = egaFileService.getFiles(userDirecory);
        assertEquals(1, userFile.size());
    }

    @Test
    public void testGetFilesEmpty() {
        EgaDirectory userDirecory = new EgaDirectory("EGAD00001", null, null);
        interceptor.addRule().get(APP_URL.concat("/metadata/datasets/").concat(userDirecory.getName()).concat("/files"))
                .respond(500);

        List<EgaFile> userFile = egaFileService.getFiles(userDirecory);
        assertTrue(userFile.isEmpty());
    }

    @Test
    public void testFillBufferCurrentChunk() throws JsonProcessingException, InterruptedException, ExecutionException {
        CompletableFuture<byte[]> future = mock(CompletableFuture.class);
        when(cache.get(any())).thenReturn(future);
        when(future.get()).thenReturn(new byte[] {});
        long bytesToRead = 10l;
        int chunksize = egaFileService.fillBufferCurrentChunk(pointer, "fileId", 100l, bytesToRead, 0l);
        assertEquals(bytesToRead, chunksize);
    }

    @Test
    public void testFillBufferCurrentChunk_null()
            throws JsonProcessingException, InterruptedException, ExecutionException {
        CompletableFuture<byte[]> future = mock(CompletableFuture.class);
        when(cache.get(any())).thenReturn(future);
        when(future.get()).thenReturn(null);
        int chunksize = egaFileService.fillBufferCurrentChunk(pointer, "fileId", 100l, 10l, 0l);
        assertEquals(-1, chunksize);
    }

    @Test
    public void testFillBufferCurrentChunk_exception()
            throws JsonProcessingException, InterruptedException, ExecutionException {
        CompletableFuture<byte[]> future = mock(CompletableFuture.class);
        when(cache.get(any())).thenReturn(future);
        when(future.get()).thenThrow(InterruptedException.class);
        int chunksize = egaFileService.fillBufferCurrentChunk(pointer, "fileId", 100l, 10l, 0l);
        assertEquals(-1, chunksize);
    }
}
