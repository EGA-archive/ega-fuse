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

import static uk.ac.ebi.ega.egafuse.config.EgaFuseApplicationConfig.NUM_PAGES;
import static uk.ac.ebi.ega.egafuse.config.EgaFuseApplicationConfig.PAGE_SIZE;

import java.io.IOException;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jnr.ffi.Pointer;
import ru.serce.jnrfuse.struct.FileStat;
import uk.ac.ebi.ega.egafuse.model.File;

public class EgaFile extends EgaPath {
    private static final Logger LOGGER = LoggerFactory.getLogger(EgaFile.class);
    private File file;
    private EgaFileService egaFileService;
    private HashSet<Integer> keys = new HashSet<>();

    public EgaFile(String name, EgaDirectory parent) {
        super(name, parent);
    }

    public EgaFile(String name, File file, EgaFileService egaFileService) {
        super(name);
        this.file = file;
        this.egaFileService = egaFileService;
    }

    @Override
    public void getattr(FileStat stat) {
        stat.st_mode.set(FileStat.S_IFREG | 0444);
        stat.st_size.set(file.getFileSize());
    }

    public int read(Pointer buffer, long size, long offset) {
        long fsize = file.getFileSize();
        int bytesToRead = (int) Math.min(fsize - offset, size);

        if (offset >= fsize || bytesToRead <= 0)
            return -1;

        int cachePage = (int) (offset / PAGE_SIZE);
        loadNextPage(fsize, cachePage);
        keys.add(cachePage);
        byte[] page = this.get(cachePage);

        int retry = 0;
        while (page == null && retry <= 5) {
            page = this.get(cachePage);
            ++retry;
        }

        if (page == null) {
            LOGGER.error("Service seems to be down");
            return 0;
        } else {
            int page_offset = (int) (offset - cachePage * PAGE_SIZE);
            buffer.put(0L, page, page_offset, bytesToRead);
            return bytesToRead;
        }
    }

    private void loadNextPage(long fsize, int currentPage) {
        int maxPage = (int) (fsize / PAGE_SIZE);
        int nextPage = currentPage + 1;
        int nextEndPage = nextPage + NUM_PAGES - 1;

        if (nextPage <= maxPage) {
            nextEndPage = nextEndPage > maxPage ? maxPage : nextEndPage;
            for (int loopPage = nextPage; loopPage <= nextEndPage; loopPage++) {
                final int pageNumber = loopPage;
                if (!keys.contains(pageNumber)) {
                    keys.add(pageNumber);
                    new Thread(() -> {
                        this.get(pageNumber);
                    }).start();
                }
            }
        }
    }

    private byte[] get(int page_number) {
        try {
            return egaFileService.downloadFiles(file.getFileId(), page_number, file.getFileSize());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int open() {
        return 0;
    }
}
