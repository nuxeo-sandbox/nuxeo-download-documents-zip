package org.nuxeo.labs.download.zip;

import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(AutomationFeature.class)
@RepositoryConfig(init = CustomRepoInit.class, cleanup = Granularity.METHOD)
@Deploy("nuxeo-download-documents-zip-core")
public class TestBulkDownloadZip {

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService automationService;

    @Test
    public void testZipFolder() throws OperationException {
        DocumentModel folder = session.getDocument(new PathRef("/Test"));
        OperationContext ctx = new OperationContext(session);
        Map<String, Object> params = new HashMap<>();
        ctx.setInput(new DocumentModelListImpl(Collections.singletonList(folder)));
        Blob zip  = (Blob) automationService.run(ctx, BulkDownloadZip.ID, params);
        assertNotNull(zip);
    }

    @Test
    public void testZipStoredFiles() throws OperationException {
        DocumentModelList files = session.getDocuments(new DocumentRef[]{new PathRef("/Test/File1"),new PathRef("/Test/File2")});
        OperationContext ctx = new OperationContext(session);
        Map<String, Object> params = new HashMap<>();
        ctx.setInput(files);
        Blob zip  = (Blob) automationService.run(ctx, BulkDownloadZip.ID, params);
        assertNotNull(zip);
    }

    @Test
    public void testZipDeflatedFiles() throws OperationException {
        DocumentModelList files = session.getDocuments(new DocumentRef[]{new PathRef("/Test/File1"),new PathRef("/Test/File2")});
        OperationContext ctx = new OperationContext(session);
        Map<String, Object> params = new HashMap<>();
        params.put("zipMethod","deflated");
        ctx.setInput(files);
        Blob zip  = (Blob) automationService.run(ctx, BulkDownloadZip.ID, params);
        assertNotNull(zip);
    }

    @Test(expected = NuxeoException.class)
    public void testZipUnknownMethod() throws OperationException {
        DocumentModelList files = session.getDocuments(new DocumentRef[]{new PathRef("/Test/File1")});
        OperationContext ctx = new OperationContext(session);
        Map<String, Object> params = new HashMap<>();
        params.put("zipMethod","banana");
        ctx.setInput(files);
        Blob zip  = (Blob) automationService.run(ctx, BulkDownloadZip.ID, params);
        assertNotNull(zip);
    }

}
