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

// checking for init runs
try {
	if (initRun) {
		println "Flattened Child script, init okay."
		return
	}
} catch (e) {
	// swallowing
}

def doc = [:]

entry.each { k, v ->
  if (k == '@type') {
    doc['type_s'] = v
  } else if (k == '@id') {
    doc['id'] = v
  } else {
    doc[k] = v
  }
}

document['_childDocuments_'] << doc
