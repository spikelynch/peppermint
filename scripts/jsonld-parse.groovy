/**
 * JSON-LD Parser for Solr
 *
 * Works with commit-to-solr.groovy.
 */
@Grapes([
	@Grab(group='org.apache.solr', module='solr-solrj', version='7.4.0'),
	@Grab(group='com.github.jsonld-java', module='jsonld-java', version='0.12.0')
])
import java.util.*;
import java.io.*;
import groovy.json.*;
import org.apache.solr.common.*;
import com.github.jsonldjava.core.*;
import com.github.jsonldjava.utils.*;


// checking for init runs
try {
	if (initRun) {
		println "JSON LD Parser, init okay."
		return
	}
} catch (e) {
	// swallowing
}
def docList = []
def document = [:]
// put the document list
manager.getBindings().put('docList', docList)
manager.getBindings().put('document', document)

def slurper = new JsonSlurper()
// add the raw json to the Solr Document
def jsonStr = JsonOutput.toJson(data)
def context = [:]
document['raw_json_t'] =  jsonStr
document["record_type_s"] = recordType
document['_childDocuments_'] = []


JsonLdOptions options = new JsonLdOptions()
def compacted = JsonLdProcessor.compact(data, context, options);
if (compacted['@graph']) {
	compacted['@graph'].each { entry ->
		def type = entry['@type']
		def script = config['jsonld'].types[type];
		manager.getBindings().put('entry', entry)
		if (script) {
			// assumes groovy for now
			try {
				script.each { s ->
					engine.eval(new FileReader(s))
				}
			} catch (e) {
				logger.error("Failed to run: ${script}");
				logger.error(e)
			}
		} else {
			logger.info("No script configured for: ${entry['@id']} with type: ${type}, running default handler if configured, ignoring if not.");
			script = config['jsonld']['defaultHandlerScript']
			if (script) {
				try {
					engine.eval(new FileReader(script))
				} catch (e) {
					logger.error("Failed to run: ${script}");
					logger.error(e)
				}
			}
		}
	}
}
document["raw_compacted_t"] = JsonOutput.toJson(compacted)
document["date_updated_dt"] = new Date()
docList << [document: document, core: config['jsonld'].core]
logger.debug("JSON LD Parsed:")
logger.debug(docList.toString())
return docList
