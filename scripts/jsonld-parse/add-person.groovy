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

//-------------------------------------------------------
// Script Fns
//-------------------------------------------------------
def enforceSolrFieldNames(k) {
	return k.replaceAll(/[^a-zA-Z\d_]/, '_')
}

def trim(vals) {
	if (vals instanceof List && vals.size() > 0) {
		return vals.collect { it instanceof String ?  it.trim() : it }
	}
	return vals instanceof String ? vals.trim() : vals
}
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
	def solrField = enforceSolrFieldNames(k)
  if (k == '@type') {
    doc['type'] = v
    newDoc['type'] = v
		solrField = 'type'
  } else if (k == '@id') {
    doc['id'] = v
    newDoc['id'] = v
		solrField = 'id'
  } else {
    newDoc[solrField] = v
    doc[solrField] = v
  }

	def facetConfig = personConfig.facets[k]
	if (facetConfig) {
		def vals = null
		def val = facetConfig.fieldName ? v[facetConfig.fieldName] : v
		if (facetConfig.tokenize) {
			vals = val ? val.tokenize(facetConfig.tokenize.delim) : val
		} else {
			vals = val
		}
		if (facetConfig.trim) {
			vals =  trim(vals)
		}
		if (facetConfig.escape_value == "solr_field") {
			if (vals instanceof List) {
				vals = vals.collect { enforceSolrFieldNames(it) }
			} else {
				vals = enforceSolrFieldNames(vals)
			}
		}
		def suffix = "facet"
		if (facetConfig.field_suffix) {
			suffix = facetConfig.field_suffix
		}
		if (facetConfig.skip_entry_type_suffix) {
			suffix = "_______${suffix}"
		} else {
			suffix = "_______${entryTypeFieldName}_______${suffix}"
		}
		newDoc["${solrField}${suffix}"] = vals
	}
}
docList << [document: newDoc, core: personConfig.core]
document['_childDocuments_'] << doc
