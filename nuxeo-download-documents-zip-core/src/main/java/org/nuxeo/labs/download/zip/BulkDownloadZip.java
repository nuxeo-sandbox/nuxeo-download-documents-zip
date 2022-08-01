package org.nuxeo.labs.download.zip;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import org.nuxeo.ecm.automation.core.util.PageProviderHelper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.runtime.api.Framework;

import static org.nuxeo.ecm.collections.api.CollectionConstants.COLLECTION_FACET;

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

    @Param(name = "zipMethod", description = "Zip compression method, either deflated or stored (default)", required = false)
    protected String zipMethod = "stored";

    @OperationMethod
    public Blob run(DocumentModelList docs) throws IOException {
        File zipFile = Framework.createTempFile("zip", null);
        long beginning = System.currentTimeMillis();
        try (ZipArchiveOutputStream zipOut = new ZipArchiveOutputStream(zipFile)) {
            switch (zipMethod) {
            case "stored":
                zipOut.setMethod(ZipArchiveOutputStream.STORED);
                break;
            case "deflated":
                zipOut.setMethod(ZipArchiveOutputStream.DEFLATED);
                break;
            default:
                throw new NuxeoException("Unknow ZIP method: " + zipMethod);
            }

            zipOut.setUseZip64(Zip64Mode.Always);
            docs.stream().forEach(doc -> {
                try {
                    if (hasChildren(doc)) {
                        zipFolder(zipOut, doc, "", pageprovider);
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
        Framework.trackFile(zipFile, result);
        return result;
    }

    public void zipFolder(ZipArchiveOutputStream zipOut, DocumentModel root, String path, String pageproviderName)
            throws IOException {
        String rootTitle = (String) root.getPropertyValue("dc:title");
        String currentPath = String.format("%s/%s", path,
                StringUtils.isNotBlank(rootTitle) ? rootTitle : root.getName());

        PageProviderDefinition def = PageProviderHelper.getPageProviderDefinition(pageproviderName);
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("rootId",root.getId());
        PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>)PageProviderHelper.getPageProvider(session, def, namedParams,new Object[]{});

        do {
            List<DocumentModel> children = pp.getCurrentPage();
            for (DocumentModel current : children) {
                if (hasChildren(current)) {
                    zipFolder(zipOut, current, currentPath, pageproviderName);
                } else {
                    zipFile(zipOut, current, currentPath);
                }
            }
            pp.nextPage();
        } while (pp.isNextEntryAvailable());
    }

    public void zipFile(ZipArchiveOutputStream zipOut, DocumentModel current, String path) throws IOException {
        BlobHolder blobHolder = current.getAdapter(BlobHolder.class);
        if (blobHolder == null || blobHolder.getBlob() == null) {
            log.debug("No blob for document {}", current::getPathAsString);
            return;
        }
        Blob blob = blobHolder.getBlob();
        if (!downloadService.checkPermission(current, null, blob, DOWNLOAD_REASON, Collections.emptyMap())) {
            log.debug("Not allowed to download blob for document {}", current::getPathAsString);
            return;
        }
        downloadService.logDownload(null, current, null, blob.getFilename(), DOWNLOAD_REASON, null);

        String entryPath = StringUtils.isNotBlank(path) ? path + "/" + blob.getFilename() : blob.getFilename();
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

    public boolean hasChildren(DocumentModel doc) {
        return doc.isFolder() || doc.hasFacet(COLLECTION_FACET);
    }

}
