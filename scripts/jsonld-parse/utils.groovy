import groovy.json.*

renameIds = { v ->
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

trim = { vals ->
	if (vals instanceof List && vals.size() > 0) {
		return vals.collect { it instanceof String ?  it.trim() : it }
	}
	return vals instanceof String ? vals.trim() : vals
}



//enforceSolrFieldNames = { k ->
//	return k.replaceAll(/[^a-zA-Z\d_]/, '_')
//}

// using this to just trim the schema prefix and then
// normalise any others

enforceSolrFieldNames = { k ->
  k1 = k.replaceAll('https?://schema.org/', '');
  return k1.replaceAll(/[^a-zA-Z\d_]/, '_')
}


ensureValidId = { val ->
  return val.replaceAll(/\s/, '_')
}

addKvToDocument = { solrField, k, v, document ->
  if (k == '@type') {
    document['type'] = v
		solrField = 'type'
  } else if (k == '@id') {
    document['id'] = v
		solrField = 'id'
  } else {
		document[solrField] = renameIds(v)
	}
}

getGraphEntry = { data, id ->
  return data['@graph'].find { entry ->
    entry['@id'] == id
  }
}

addKvAndFacetsToDocument = {data, k, v, docs, facetDoc, recordTypeConfig, entryTypeFieldName ->
  def slurper = new JsonSlurper()
  def solrField = enforceSolrFieldNames(k)
  if (k == '@type') {
    def typeVal = v
    if (v instanceof java.util.ArrayList) {
      // select the last one...
      typeVal = v[v.size()-1]
    }
    // select the type label as the last one...
    def vparts = typeVal.split('/')
    def typeLabel  = vparts[vparts.length - 1]
    // stripping the schema domain off the types
    docs.each { doc ->
      doc['type'] = typeLabel
      doc['type_label'] = typeLabel;
    }
		solrField = 'type'
  } else if (k == '@id') {
    docs.each { doc ->
      doc['id'] = v
    }
		solrField = 'id'
  } else {
    if (v instanceof Map || v instanceof List) {
      def expanded = null
      if (v instanceof Map && v['@id']) {
        expanded = getGraphEntry(data, v['@id'])
        docs.each { doc ->
          if (expanded) {
            doc[solrField] = v << expanded
          } else {
            doc[solrField] = renameIds(v)
          }
        }
      } else {
        v.each {vEntry ->
          if (vEntry instanceof Map && vEntry['@id']) {
            expanded = getGraphEntry(data, vEntry['@id'])
            if (expanded && expanded instanceof Map) {
              vEntry << expanded
            } else if (expanded instanceof String) {
              vEntry << slurper.parseText(expanded)
            }
          }
        }
        docs.each { doc ->
          doc[solrField] = renameIds(v)
        }
      }
    } else {
      docs.each { doc ->
        doc[solrField] = renameIds(v)
      }
    }
	}


  def facetConfig = recordTypeConfig.facets[solrField]
  logger.info("Looking for facetConfig for ${solrField}");
	if (facetConfig) {
    logger.info("facetConfig found");
		def vals = null
    def val;
    if( facetConfig.relation ) {
      val = v['relation_' + facetConfig.relation]
    } else {
		  val =  facetConfig.fieldName && v instanceof Map && v.containsKey(facetConfig.fieldName) ?  v[facetConfig.fieldName] : v;
    }
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
			suffix = "_${suffix}"
		} else {
			suffix = "_${entryTypeFieldName}_${suffix}"
		}
		facetDoc["${solrField}${suffix}"] = vals
	} else {
    logger.info("facetConfig not found for ${solrField}");
  }


}
