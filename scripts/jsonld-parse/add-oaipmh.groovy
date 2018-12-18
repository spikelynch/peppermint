/**
 * This script expects the following on the binding:
 *
 *  document - the parent document
 *  docList - the list of maps that will be saved (see commit-to-solr.groovy)
 *  entry - the map containing the JSON entry
 *
 */
import groovy.xml.*
//-------------------------------------------------------
// Init, executed once to grab dependencies
//-------------------------------------------------------
try {
	if (initRun) {
		println "OAIMPH script, init okay."
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

def getDcDoc(mainDoc) {
	def oaidcDoc = [:]
	oaidcDoc['record_type_s'] = "oaipmh_record"
	oaidcDoc['metadataSchema_s'] = 'oai_dc'
	oaidcDoc['id'] = "oaipmh_record_dc_${mainDoc['id']}"

	def nowStamp = new Date().format("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone("UTC"))

	// now build the DC string...
	def stringw = new StringWriter()
	new MarkupBuilder(stringw).with { mb ->
		record('xmlns:oai_dc':"http://www.openarchives.org/OAI/2.0/oai_dc/",
					 'xmlns:dc':"http://purl.org/dc/elements/1.1/",
					 'xmlns:xsi':"http://www.w3.org/2001/XMLSchema-instance",
					 'xsi:schemaLocation':"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd") {
		  header {
				identifier(mainDoc['id'])
				datestamp(nowStamp)
				setSpec(recordTypeConfig['oai-pmh'].set)
			}
			metadata {
				'oai_dc:dc' {
					'dc:identifier'(mainDoc['id'])
					'dc:title'(mainDoc['https___schema_org_name'])
					'dc:description'(mainDoc['https___schema_org_description'])
				}
			}
	  }
	}
	oaidcDoc['xml_s'] = stringw.toString()
	return oaidcDoc
}

def getFirstElem(arr) {
	return arr && arr instanceof List && arr.size() > 0 ? arr[0] : arr
}

def getRifDoc(mainDoc) {
	def rifDoc = [:]
	rifDoc['record_type_s'] = "oaipmh_record"
	rifDoc['metadataSchema_s'] = 'rif'
	rifDoc['id'] = "oaipmh_record_rif_${mainDoc['id']}"

	def nowStamp = new Date().format("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone("UTC")) 	// now build the DC string...
	def stringw = new StringWriter()
	def mainDocId = mainDoc['id']
	def publisherProperty = 'https___schema_org_publisher'
	def publisherId = getFirstElem(mainDoc[publisherProperty])['id']
	new MarkupBuilder(stringw).with { mb ->
		record() {
		  header {
				identifier(mainDocId)
				datestamp(nowStamp)
				setSpec(recordTypeConfig['oai-pmh'].set)
			}
			metadata {
				registryObjects(
					'xmlns':"http://ands.org.au/standards/rif-cs/registryObjects",
					'xmlns:xsi':"http://www.w3.org/2001/XMLSchema-instance",
					'xsi:schemaLocation':"http://ands.org.au/standards/rif-cs/registryObjects http://services.ands.org.au/documentation/rifcs/schema/registryObjects.xsd") {
					registryObject(group: publisherId) {
						key(mainDocId)
						originatingSource(mainDocId)
						collection(type: "dataset") {
							name(type: "primary") {
								namePart(mainDoc['https___schema_org_name'])
							}
							description(type:"full", mainDoc['https___schema_org_description'])
							identifier(type: "local", mainDocId)
							location {
								address {
									electronic(type: "url") {
										value(getFirstElem(mainDoc['https___schema_org_distribution']))
									}
								}
							}
						}
					}
				}
			}
	  }
	}
	rifDoc['xml_s'] = stringw.toString()
	return rifDoc
}

def getOaipmhDoc(docList, core) {
	def metadoc = null
	docList.each { d ->
		if (d.document != null && d.document.id == "oaipmh_meta") {
			metadoc = d
		}
	}
	if (!metadoc) {
		metadoc = [id:"oaipmh_meta"]
		docList << [document: metadoc, core: core]
	}
	return metadoc
}

def updateOaipmhLatest(doc) {
	if (doc) {
		doc['latest_dt'] = new Date()
	}
}
//-------------------------------------------------------
// Start of Script
//-------------------------------------------------------

// add to the main document
logger.info("Creating OAIPMH document(s)...")
// logger.info(JsonOutput.toJson(entry))
//
def recordTypeConfig = config['recordType'][recordType]

// use the mapped document to simplify traversing the map
def oaidcDoc = getDcDoc(document)
def rifDoc = getRifDoc(document)
def oaipmhMetaDoc = getOaipmhDoc(docList, recordTypeConfig['oai-pmh'].core)
updateOaipmhLatest(oaipmhMetaDoc)
docList << [document: oaidcDoc, core: recordTypeConfig['oai-pmh'].core]
docList << [document: rifDoc, core: recordTypeConfig['oai-pmh'].core]
