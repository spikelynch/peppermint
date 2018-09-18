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

//-------------------------------------------------------
// Script Fns
//-------------------------------------------------------

//-------------------------------------------------------
// Start of Script
//-------------------------------------------------------

// add to the main document
entry.each { k, v ->
  if (k == '@type') {
    document['type_s'] = v
  } else if (k == '@id') {
    document['id'] = v
  } else {
    document[k] = v
  }
}
