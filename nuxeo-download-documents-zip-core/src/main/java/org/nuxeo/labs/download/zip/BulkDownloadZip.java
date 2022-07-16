package org.nuxeo.labs.download.zip;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.Zip64Mode;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.runtime.api.Framework;

/**
 *
 */
@Operation(id = BulkDownloadZip.ID, category = "Labs", label = "Labs ZIP Bulk Download", description = "Describe here what your operation does.")
public class BulkDownloadZip {

    public static final String ID = "Labs.BulkDownloadZip";

    protected static final String DOWNLOAD_REASON = "download";

    private static final Logger log = LogManager.getLogger(BulkDownloadZip.class);

    @Context
    protected CoreSession session;

    @Context
    protected DownloadService downloadService;

    @Param(name = "filename", required = false)
    protected String fileName;

    @Param(name = "pageprovider", required = false)
    protected String pageprovider = "zip_folder_get_children";

    @OperationMethod
    public Blob run(DocumentModelList docs) throws IOException {
        File zipFile = Framework.createTempFile("zip", null);
        long beginning = System.currentTimeMillis();
        try (ZipArchiveOutputStream zipOut = new ZipArchiveOutputStream(zipFile)) {
            zipOut.setMethod(ZipArchiveOutputStream.STORED);
            zipOut.setUseZip64(Zip64Mode.Always);
            docs.stream().forEach(doc -> {
                try {
                    if (doc.isFolder()) {
                        zipFolder(zipOut, doc, pageprovider);
                    } else {
                        zipFile(zipOut, doc, null);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        long end = System.currentTimeMillis();
        log.info("Compressed documents to zip in: " + (end - beginning));

        String filename = StringUtils.isNotBlank(this.fileName) ? this.fileName
                : String.format("BlobListZip-%s-%s", UUID.randomUUID(), session.getPrincipal().getName());

        FileBlob result = new FileBlob(zipFile, "application/zip", null, filename, null);
        Framework.trackFile(zipFile,result);
        return result;
    }

    public void zipFolder(ZipArchiveOutputStream zipOut, DocumentModel root, String pageproviderName)
            throws IOException {
        PageProviderService pageProviderService = Framework.getService(PageProviderService.class);
        Map<String, Serializable> props = new HashMap<>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY, (Serializable) root.getCoreSession());
        @SuppressWarnings("unchecked")
        PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) pageProviderService.getPageProvider(
                pageproviderName, null, null, null, null, props, new Object[] { root.getId() });
        do {
            List<DocumentModel> children = pp.getCurrentPage();
            for (DocumentModel current : children) {
                if (current.isFolder()) {
                    continue;
                }
                zipFile(zipOut, current, null);
            }
            pp.nextPage();
        } while (pp.isNextEntryAvailable());
    }

    public void zipFile(ZipArchiveOutputStream zipOut, DocumentModel current, String path) throws IOException {
        Blob blob = current.getAdapter(BlobHolder.class).getBlob();
        if (blob != null) {
            if (!downloadService.checkPermission(current, null, blob, DOWNLOAD_REASON, Collections.emptyMap())) {
                log.debug("Not allowed to download blob for document {}", current::getPathAsString);
                return;
            }
            downloadService.logDownload(null, current, null, blob.getFilename(), DOWNLOAD_REASON, null);

            String entryPath = path != null && path.length() > 0 ? path + "/" + blob.getFilename() : blob.getFilename();
            if (entryPath.startsWith("/")) {
                entryPath = entryPath.substring(1);
            }
            try (InputStream in = blob.getStream()) {
                ArchiveEntry entry = new ZipArchiveEntry(entryPath);
                zipOut.putArchiveEntry(entry);
                IOUtils.copy(in, zipOut);
                zipOut.closeArchiveEntry();
            }
        }
    }

}
