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

//-------------------------------------------------------
// Script Fns
//-------------------------------------------------------
def enforceSolrFieldNames(k) {
	return k.replaceAll(/[^a-zA-Z\d_]/, '_')
}
//-------------------------------------------------------
// Start of Script
//-------------------------------------------------------
def doc = [:]

entry.each { k, v ->
  if (k == '@type') {
    doc['type'] = v
  } else if (k == '@id') {
    doc['id'] = v
  } else {
    doc[enforceSolrFieldNames(k)] = v
  }
}

document['_childDocuments_'] << doc
