# Description
A plugin that provides enhanced bulk download capabilities for the Nuxeo Platform Web UI:
- ability to choose the stored compression method for maximum speed
- ability to use a custom page provider to fetch folder and collection content
- unlike the platform default feature, no multiple zip archiving level (a subfolder is not stored as a zip within the zip)
- unlike the platform default feature, download is managed by the browser as soon as the file is ready server side

# How to build
```bash
git clone https://github.com/nuxeo-sandbox/nuxeo-download-documents-zip
cd nuxeo-download-documents-zip
mvn clean install
```

# Plugin Features
## An automation operation
The plugin contains a new Automation operation to bulk download a set of file, folder or collection

```bash
curl 'http://localhost:8080/nuxeo/api/v1/automation/Labs.BulkDownloadZip' \
  -H 'Content-Type: application/json' \
  -H 'properties: *' \
  --data-raw '{"params":{},"context":{},"input":"docs:[<DOC_UUIDS>]"}' \
  --compressed
```

Parameters:

| Name         | Description                                    | Type   | Required | Default value           |
|:-------------|:-----------------------------------------------|:-------|:---------|:------------------------|
| pageprovider | a page provider name to fetch folder content   | string | false    | zip_folder_get_children |
| zipMethod    | the zip compression method, stored or deflated | string | false    | stored                  |
| filename     | The filename of the resulting zip file         | string | false    |                         |

As with any potentially long operation, it is recommended to use the [Automation Async adapter|https://jira.nuxeo.com/browse/NXP-26172]

## Web UI contribs
The package contains Web UI slot contributions to replace the default bulk download action with the one provided in this plugin.

### Select All Support
This plugin provides partial support for Select All (the default action doesn't). The download all action will be available if the number of results is less than the page provider page size. This is the only situation where Web UI can provide the complete list of document UUIDs to the server. Otherwise the select all feature relies on the server bulk action framework but there is no action implemented for bulk download. 

# Support

**These features are not part of the Nuxeo Production platform.**

These solutions are provided for inspiration and we encourage customers to use them as code samples and learning resources.

This is a moving project (no API maintenance, no deprecation process, etc.) If any of these solutions are found to be useful for the Nuxeo Platform in general, they will be integrated directly into platform, not maintained here.

# Nuxeo Marketplace
This plugin is published on the [marketplace](https://connect.nuxeo.com/nuxeo/site/marketplace/package/nuxeo-download-documents-zip)

# License

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)

# About Nuxeo

Nuxeo Platform is an open source Content Services platform, written in Java. Data can be stored in both SQL & NoSQL databases.

The development of the Nuxeo Platform is mostly done by Nuxeo employees with an open development model.

The source code, documentation, roadmap, issue tracker, testing, benchmarks are all public.

Typically, Nuxeo users build different types of information management solutions for [document management](https://www.nuxeo.com/solutions/document-management/), [case management](https://www.nuxeo.com/solutions/case-management/), and [digital asset management](https://www.nuxeo.com/solutions/dam-digital-asset-management/), use cases. It uses schema-flexible metadata & content models that allows content to be repurposed to fulfill future use cases.

More information is available at [www.nuxeo.com](https://www.nuxeo.com).
