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

enforceSolrFieldNames = { k ->
	return k.replaceAll(/[^a-zA-Z\d_]/, '_')
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

addKvAndFacetsToDocument = {k, v, docs, facetDoc, recordTypeConfig, entryTypeFieldName ->
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
    docs.each { doc ->
      doc['type'] = v
      doc['type_label'] = typeLabel;
    }
		solrField = 'type'
  } else if (k == '@id') {
    docs.each { it['id'] = v }
		solrField = 'id'
  } else {
    docs.each { it[solrField] = renameIds(v) }
	}

  def facetConfig = recordTypeConfig.facets[k]
	if (facetConfig) {
		def vals = null
		def val = facetConfig.fieldName && v instanceof Map  && v.containsKey(facetConfig.fieldName) ?  v[facetConfig.fieldName] : v
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
		facetDoc["${solrField}${suffix}"] = vals
	}


}
