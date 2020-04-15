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
package uk.ac.ebi.ega.egafuse.runner;

import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;

import jnr.ffi.Pointer;
import jnr.ffi.types.off_t;
import jnr.ffi.types.size_t;
import ru.serce.jnrfuse.ErrorCodes;
import ru.serce.jnrfuse.FuseFillDir;
import ru.serce.jnrfuse.FuseStubFS;
import ru.serce.jnrfuse.struct.FileStat;
import ru.serce.jnrfuse.struct.FuseFileInfo;
import uk.ac.ebi.ega.egafuse.service.EgaDirectory;
import uk.ac.ebi.ega.egafuse.service.EgaFile;
import uk.ac.ebi.ega.egafuse.service.EgaPath;

public class EgaFuseCommandLineRunner extends FuseStubFS implements CommandLineRunner {

    @Value("${mountPath}")
    private String mountPath;

    private EgaDirectory rootDirectory = new EgaDirectory("", null);

    public EgaFuseCommandLineRunner() {
        rootDirectory.add(new EgaDirectory("Datasets", rootDirectory));
    }

    @Override
    public void run(String... args) throws Exception {
        EgaFuseCommandLineRunner the_fuse = new EgaFuseCommandLineRunner();
        try {
            String[] args_ = new String[] { "-o", "allow_other" };
            the_fuse.mount(Paths.get(mountPath), true, true, args_);
        } finally {
            the_fuse.umount();
        }
    }

    @Override
    public int readdir(String path, Pointer buf, FuseFillDir filter, @off_t long offset, FuseFileInfo fi) {
        EgaPath p = getPath(path);
        if (p == null) {
            return -ErrorCodes.ENOENT();
        }
        if (!(p instanceof EgaDirectory)) {
            return -ErrorCodes.ENOTDIR();
        }
        filter.apply(buf, ".", null, 0);
        filter.apply(buf, "..", null, 0);
        ((EgaDirectory) p).read(buf, filter);
        return 0;
    }

    @Override
    public int getattr(String path, FileStat stat) {
        EgaPath p = getPath(path);
        if (p != null) {
            p.getattr(stat);
            return 0;
        }
        return -ErrorCodes.ENOENT();
    }

    @Override
    public int read(String path, Pointer buf, @size_t long size, @off_t long offset, FuseFileInfo fi) {
        EgaPath p = getPath(path);
        if (p == null) {
            return -ErrorCodes.ENOENT();
        }
        if (!(p instanceof EgaFile)) {
            return -ErrorCodes.EISDIR();
        }
        return ((EgaFile) p).read(buf, size, offset);
    }

    @Override
    public int unlink(String path) {
        EgaPath p = getPath(path);
        if (p == null) {
            return -ErrorCodes.ENOENT();
        }
        p.delete();
        return 0;
    }

    @Override
    public int open(String path, FuseFileInfo fi) {
        EgaPath p = getPath(path);
        if (p == null) {
            return -ErrorCodes.ENOENT();
        }
        if (!(p instanceof EgaFile)) {
            return -ErrorCodes.EISDIR();
        } else {
            ((EgaFile) p).open();
        }
        return 0;
    }

    private EgaPath getPath(String path) {
        return rootDirectory.find(path);
    }
}
