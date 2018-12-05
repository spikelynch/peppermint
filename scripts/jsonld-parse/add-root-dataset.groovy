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
def renameIds(v) {
  if (v instanceof Map) {
    def remapped = [:]
    v.each { k, val ->
      if (k == '@id') {
        remapped['id'] = val
      } else {
        remapped[k] = renameIds(val)
      }
    }
    return remapped
  } else {
    return v
  }
}

def trim(vals) {
	if (vals instanceof List && vals.size() > 0) {
		return vals.collect { it instanceof String ?  it.trim() : it }
	}
	return vals instanceof String ? vals.trim() : vals
}

def enforceSolrFieldNames(k) {
	return k.replaceAll(/[^a-zA-Z\d_]/, '_')
}

//-------------------------------------------------------
// Start of Script
//-------------------------------------------------------

// add to the main document
logger.info("Processing root node....")
// logger.info(JsonOutput.toJson(entry))
def entryTypeFieldName = enforceSolrFieldNames("https://schema.org/Dataset")
entry.each { k, v ->
	def solrField = enforceSolrFieldNames(k)
  if (k == '@type') {
    document['type'] = v
		solrField = 'type'
  } else if (k == '@id') {
    document['id'] = v
		solrField = 'id'
  } else {
		document[solrField] = renameIds(v)
	}

	def facetConfig = recordTypeConfig.facets[k]
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
		document["${solrField}${suffix}"] = vals
	}
}
