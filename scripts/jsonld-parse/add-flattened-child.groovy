/**
 * This script expects the following on the binding:
 *
 *  document - the parent document
 *  docList - the list of maps that will be saved (see commit-to-solr.groovy)
 *  entry - the map containing the JSON entry
 *
 */
import groovy.json.*;

//-------------------------------------------------------
// Init, executed once to grab dependencies
//-------------------------------------------------------
try {
	if (initRun) {
		println "Flattened Child script, init okay."
		return
	}
} catch (e) {
	// swallowing
}

evaluate(new File('scripts/jsonld-parse/utils.groovy'))

//-------------------------------------------------------
// Script Fns
//-------------------------------------------------------

//-------------------------------------------------------
// Start of Script
//-------------------------------------------------------
def doc = [:]
def entryTypeFieldName = enforceSolrFieldNames(entryType)
def rootEntryDoc = [:]

entry.each { k, v ->
	addKvAndFacetsToDocument(data, k, v, [doc, rootEntryDoc], rootEntryDoc, recordTypeConfig, entryTypeFieldName)
}
// check if the documents have IDs, otherwise don't add
if (!doc['id'] || !rootEntryDoc['id']) {
	logger.info("Document has no ID, ignoring:")
	logger.info(JsonOutput.toJson(doc))
	return
}
doc['child_id'] = doc['id']
doc['id'] = "${document['id']}_${doc['id']}"
document['_childDocuments_'] << doc
docList << [document: rootEntryDoc, core: recordTypeConfig.core]
