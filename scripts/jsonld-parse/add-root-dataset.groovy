/**
 * This script expects the following on the binding:
 *
 *  document - the parent document
 *  docList - the list of maps that will be saved (see commit-to-solr.groovy)
 *  entry - the map containing the JSON entry
 *
 */
@Grapes([
	@Grab(group='org.apache.solr', module='solr-solrj', version='7.4.0')
])

import org.apache.solr.common.*;
import groovy.json.*;

//-------------------------------------------------------
// Init, executed once to grab dependencies
//-------------------------------------------------------
try {
	if (initRun) {
		println "Dataset Root Node processor script, init okay."
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

// add to the main document
logger.info("Processing root node....")
// logger.info(JsonOutput.toJson(entry))
def entryTypeFieldName = enforceSolrFieldNames("https://schema.org/Dataset")
entry.each { k, v ->
	addKvAndFacetsToDocument(data, k, v, [document], document, recordTypeConfig, entryTypeFieldName)
}
