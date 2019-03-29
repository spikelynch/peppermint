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
	def sw = new StringWriter()
	def pw = new PrintWriter(sw, true)

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
			throw e
		}
	} else {
		if (useDefaultHandler) {
			script = recordTypeConfig['defaultHandlerScript']
			if (script) {
			    logger.info("No script configured for type: ${type}, running default handler.");
				try {
					engine.eval(new FileReader(script))
				} catch (e) {
					logger.error("Failed to run: ${script}");
					logger.error(e)
					e.printStackTrace(pw)
					logger.error("Stack trace:  ")
					logger.error(sw.toString())
					throw e
				}
			} else {
			    logger.info("No script configured for type: ${type}. No default handler.");
			}
		} else {
			logger.info("No script configured for: ${type}, and not using default handler.")
		}
	}
}

def ensureSchemaOrgHttps(data) {
	def newContext = [:]
	data['@context'].each { key, val ->
		newContext[key] = val.replaceAll('http://schema.org', 'https://schema.org')
	}
	data['@context'] = newContext
}


boolean isCollectionOrArray(object) {
    [Collection, Object[]].any { it.isAssignableFrom(object.getClass()) }
}

def ensureIdsAreCleanAndShinyAndNiceAndWonderful(data) {
	def modified = []
	data.eachWithIndex {entry, idx ->
		def modEntry = [:]
		if (entry instanceof Map) {
			entry.each {key, val ->
				if (key == '@id') {
					modEntry['id_orig'] = val
					modEntry['@id'] = val.replaceAll(/\s/, '_')
				} else
				if (isCollectionOrArray(val)) {
					modEntry[key] = ensureIdsAreCleanAndShinyAndNiceAndWonderful(val)
				} else {
					modEntry[key] = val
				}
			}
		} else {
			modEntry = entry
		}

		modified[idx] = modEntry
	}
	return modified
}


// This looks for all facets with a 'relation' value, scans the
// graph for items of the given type in that relation to the
// root node, and lifts their value (specified by fieldName)
// into the rootNode. It returns the updated rootNode.

// for eg, with the following in the facets config:

// "Person": {
//   "relation": "creator",
//   "trim": true,
//   "fieldName": "name",
//   "field_suffix": "facetmulti"
// },

// the Solr document gets this:
//
// "Person": [ "Joe Blow", "Fred Nurks" ]

def resolveGraphLinks(facets, rootNode, graph) {
	def newRoot = rootNode.clone();
	facets.each { facetField, cf ->
		def relation = cf['relation'];
		if( relation ) {
			def links = findRelated(rootNode, relation, graph);
			links.each { l ->
				newRoot[facetField] = l[cf['fieldName']];
				logger.info("Lifted " + relation + ": " + l[cf['fieldName']]);
			}
		}
	} 
	return newRoot
}

def findRelated(rootNode, relation, graph) {
	if( rootNode[relation] && rootNode[relation] instanceof Collection ) {
		def ids = rootNode[relation].collect { it['@id'] };
		return graph.findAll { it['@id'] in ids }
	} else {
		return [];
	}
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
data['@graph'] = ensureIdsAreCleanAndShinyAndNiceAndWonderful(data['@graph'])


def slurper = new JsonSlurper()
// def jsonStr = JsonOutput.toJson(data['@graph'])
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
	def graph = compacted['@graph'];
	// find the root node of the graph...
	def rootNodeId = recordTypeConfig['rootNodeFieldContextId']
	def rootNodeVals = recordTypeConfig['rootNodeFieldValues'];
	logger.info("Using rootNodeId:" + rootNodeId)
	def rootNode = data['@graph'].find {
		return it[rootNodeId] instanceof Collection ? rootNodeVals.intersect(it[rootNodeId]).size() > 0 : rootNodeVals.contains(it[rootNodeId]) // it[rootNodeId] == 'data/' || it[rootNodeId]== './'
	}

	def rootResolved = resolveGraphLinks(recordTypeConfig['facets'], rootNode, graph);

	graph.each { entry ->
		if (entry['@id'] == rootNode['@id']) {
			processEntry(manager, engine, rootNode, 'rootNode', false)
		} else {
			def type = enforceSolrFieldNames(entry['@type']);
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

return docList
