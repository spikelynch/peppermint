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
		println "Add Person, init okay."
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
def newDoc = [:]
def personConfig = config['recordType']['person']
newDoc['record_type_s'] = personConfig['recordTypeName']
newDoc["record_format_s"] = personConfig['format']
newDoc['_childDocuments_'] = []
def entryTypeFieldName = enforceSolrFieldNames(entryType)
entry.each { k, v ->
	addKvAndFacetsToDocument(data, k, v, [doc, newDoc], newDoc, personConfig, entryTypeFieldName)
}
// docList << [document: newDoc, core: personConfig.core]
document['_childDocuments_'] << doc
