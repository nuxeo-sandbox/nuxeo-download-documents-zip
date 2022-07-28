/*
 * (C) Copyright 2022 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Michael Vachette
 */

package org.nuxeo.labs.download.zip;

import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.annotations.RepositoryInit;
import org.nuxeo.runtime.test.runner.Features;

import java.io.File;
import java.io.Serializable;

@Features({ AutomationFeature.class })
public class CustomRepoInit implements RepositoryInit {

    @Override
    public void populate(CoreSession session) {

        DocumentModel folder = session.createDocumentModel(session.getRootDocument().getPathAsString(),"Test","Folder");
        folder = session.createDocument(folder);

        Blob blob1 = new FileBlob(new File(getClass().getResource("/files/Bear.svg").getPath()));
        DocumentModel file1 = session.createDocumentModel(folder.getPathAsString(),"File1","File");
        file1.setPropertyValue("file:content", (Serializable) blob1);
        session.createDocument(file1);

        Blob blob2 = new FileBlob(new File(getClass().getResource("/files/sample.psb").getPath()));
        DocumentModel file2 = session.createDocumentModel(folder.getPathAsString(),"File2","File");
        file2.setPropertyValue("file:content", (Serializable) blob2);
        session.createDocument(file2);

        DocumentModel emptyDoc = session.createDocumentModel(folder.getPathAsString(),"empty","File");
        session.createDocument(emptyDoc);

        DocumentModel subfolder = session.createDocumentModel(folder.getPathAsString(),"sub","Folder");
        subfolder = session.createDocument(subfolder);

        Blob blob3 = new FileBlob(new File(getClass().getResource("/files/Brief.docx").getPath()));
        DocumentModel file3 = session.createDocumentModel(subfolder.getPathAsString(),"File3","File");
        file3.setPropertyValue("file:content", (Serializable) blob3);
        session.createDocument(file3);
    }

}
