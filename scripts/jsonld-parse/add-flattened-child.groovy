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

entry.each { k, v ->
	addKvAndFacetsToDocument(data, k, v, [doc], doc, recordTypeConfig, entryTypeFieldName)
}

document['_childDocuments_'] << doc
