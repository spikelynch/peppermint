/**
 * JSON-LD Parser for Solr.
 *
 * Works with commit-to-solr.groovy.
 */
@Grapes([
	@Grab(group='com.github.jsonld-java', module='jsonld-java', version='0.12.0')
])
import java.util.*;
import java.io.*;
import groovy.json.*;
import com.github.jsonldjava.core.*;
import com.github.jsonldjava.utils.*;

//-------------------------------------------------------
// Init, executed once to grab dependencies
//-------------------------------------------------------
try {
	if (initRun) {
		println "JSON LD Main Parser, init okay."
		return
	}
} catch (e) {
	// swallowing
}
//-------------------------------------------------------
// Script Fns
//-------------------------------------------------------

def processEntry(manager, engine, entry, type, useDefaultHandler) {
	def script = recordTypeConfig.types[type];
	manager.getBindings().put('entry', entry)
	manager.getBindings().put('entryType', type)
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
		if (useDefaultHandler) {
			logger.info("No script configured for: ${entry['@id']} with type: ${type}, running default handler if configured, ignoring if not.");
			script = recordTypeConfig['defaultHandlerScript']
			if (script) {
				try {
					engine.eval(new FileReader(script))
				} catch (e) {
					logger.error("Failed to run: ${script}");
					logger.error(e)
				}
			}
		} else {
			logger.error("No script configured for: ${type}, and not using default handler, ignoring.")
		}
	}
}

def ensureSchemaOrgHttps(data) {
	def newContext = [:]
	data['@context'].each {key, val ->
		newContext[key] = val.replaceAll('http://schema.org', 'https://schema.org')
	}
	data['@context'] = newContext
}
//-------------------------------------------------------
// Start of Script
//-------------------------------------------------------
def docList = []
def document = [:]
// put the document list
manager.getBindings().put('docList', docList)
manager.getBindings().put('document', document)
ensureSchemaOrgHttps(data)
def slurper = new JsonSlurper()
def jsonStr = JsonOutput.toJson(data)
def context = [:]
// document['raw_json_t'] =  jsonStr
recordTypeConfig = config['recordType'][recordType]
manager.getBindings().put('recordTypeConfig', recordTypeConfig)
document['record_type_s'] = recordType
document["record_format_s"] = recordTypeConfig['format']
document['_childDocuments_'] = []

JsonLdOptions options = new JsonLdOptions()
def compacted = JsonLdProcessor.compact(data, context, options);
if (compacted['@graph']) {
	// find the root node of the graph...
	def rootNodeId = recordTypeConfig['rootNodeFieldContextId']
	def rootNodeVals = recordTypeConfig['rootNodeFieldValues'];
	logger.info("Using rootNodeId:" + rootNodeId)
	def rootNode = data['@graph'].find {
		return it[rootNodeId] instanceof Collection ? rootNodeVals.intersect(it[rootNodeId]).size() > 0 : rootNodeVals.contains(it[rootNodeId]) // it[rootNodeId] == 'data/' || it[rootNodeId]== './'
	}
	compacted['@graph'].each { entry ->
		if (entry['@id'] == rootNode['@id']) {
			processEntry(manager, engine, entry, 'rootNode', false)
		} else {
			def type = entry['@type']
			if (type instanceof Collection) {
				type.each { t ->
					processEntry(manager, engine, entry, t, true)
				}
			} else {
				processEntry(manager, engine, entry, type, true)
			}
		}
	}
}
// document["raw_compacted_t"] = JsonOutput.toJson(compacted)
document["date_updated_dt"] = new Date()
docList << [document: document, core: recordTypeConfig.core]
// logger.info("JSON LD Parsed:")
// logger.info(JsonOutput.toJson(docList))
return docList
