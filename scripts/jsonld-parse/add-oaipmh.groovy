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

	// oaidcDoc['xml_s'] = """
	// <record xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">
	// <header>
	// <identifier>${mainDoc['id']}</identifier>
	// <datestamp>${nowStamp}</datestamp>
	// <setSpec>${recordTypeConfig['oai-pmh'].set}</setSpec>
	// </header>
	// <metadata>
	// <oai_dc:dc>
	// <dc:identifier>${mainDoc['id']}</dc:identifier>
	// <dc:title>${mainDoc['https___schema_org_name']}</dc:title>
	// <dc:creator>${}</dc:creator>
	// <dc:description>Photograph of Idan-ha Hotel, Soda Springs, Idaho.</dc:description>
	// <dc:source>C.R. Savage</dc:source>
	// <dc:date>ca. 1890</dc:date>
	// <dc:date>1890;</dc:date>
	// <dc:publisher>Brigham Young University</dc:publisher>
	// <dc:description>15.2 x 20.4 cm. (6 x 8 in.)</dc:description>
	// <dc:source>Intellectual Reserve, Inc.</dc:source>
	// <dc:subject>Soda Springs (Idaho);</dc:subject>
	// <dc:subject>Idan-ha Hotel (Soda Springs, Idaho);</dc:subject>
	// <dc:subject>Soda Springs (Idaho); Idan-ha Hotel (Soda Springs, Idaho);</dc:subject>
	// <dc:subject>Idaho, Caribou, Soda Springs;</dc:subject>
	// <dc:subject>Photographs;</dc:subject>
	// <dc:relation>Charles R. Savage Photograph Collection</dc:relation>
	// <dc:rights>http://lib.byu.edu/about/copyright/church_history.php</dc:rights>
	// <dc:rights>Public Domain; Courtesy Church History Collections, The Church of Jesus Christ of Latter-day Saints and Intellectual Reserve, Inc.</dc:rights>
	// <dc:rights>Public</dc:rights>
	// <dc:type>Image</dc:type>
	// <dc:format>Image/j2k</dc:format>
	// <dc:identifier>PH500_fd49_item-7.jpg</dc:identifier>
	// <dc:coverage>42.658680 -111.603077</dc:coverage>
	// <dc:coverage>16 Street;</dc:coverage>
	// <dc:identifier>http://cdm15999.contentdm.oclc.org/cdm/ref/collection/Savage2/id/255</dc:identifier>
	// </oai_dc:dc>
	// </metadata>
	// </record>
	// """
	return oaidcDoc
}

def getFirstElem(arr) {
	return arr instanceof List && arr.size() > 0 ? arr[0] : arr
}

def getRifDoc(mainDoc) {
	def rifDoc = [:]
	rifDoc['record_type_s'] = "oaipmh_record"
	rifDoc['metadataSchema_s'] = 'rif'
	rifDoc['id'] = "oaipmh_record_rif_${mainDoc['id']}"

	def nowStamp = new Date().format("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone("UTC")) 	// now build the DC string...
	def stringw = new StringWriter()
	new MarkupBuilder(stringw).with { mb ->
		record() {
		  header {
				identifier(mainDoc['id'])
				datestamp(nowStamp)
				setSpec(recordTypeConfig['oai-pmh'].set)
			}
			metadata {
				registryObjects(
					'xmlns':"http://ands.org.au/standards/rif-cs/registryObjects",
					'xmlns:xsi':"http://www.w3.org/2001/XMLSchema-instance",
					'xsi:schemaLocation':"http://ands.org.au/standards/rif-cs/registryObjects http://services.ands.org.au/documentation/rifcs/schema/registryObjects.xsd") {
					registryObject(group: getFirstElem(mainDoc['https___schema_org_publisher']['id'])) {
						key(mainDoc['id'])
						originatingSource(mainDoc['id'])
						collection(type: "dataset") {
							name(type: "primary") {
								namePart(mainDoc['https___schema_org_name'])
							}
							description(type:"full", mainDoc['https___schema_org_description'])
							identifier(type: "local", mainDoc['id'])
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
docList << [document: oaidcDoc, core: recordTypeConfig['oai-pmh'].core]
docList << [document: rifDoc, core: recordTypeConfig['oai-pmh'].core]
