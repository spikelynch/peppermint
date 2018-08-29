@Grapes([
	@Grab(group='com.github.jsonld-java', module='jsonld-java', version='0.12.0')
])
import java.util.*;
import java.io.*;
import groovy.json.*;
import com.github.jsonldjava.core.*;
import com.github.jsonldjava.utils.*;



def document = [:];
def slurper = new JsonSlurper()
// add the raw json to the Solr Document
def jsonStr = JsonOutput.toJson(data);
def context = [:]
document["raw_json"] = jsonStr;
document["record_type"] = recordType;
JsonLdOptions options = new JsonLdOptions()
def compacted = JsonLdProcessor.compact(data, context, options);

if (compacted['@graph']) {
	def childDocs = [];
	compacted['@graph'].each { entry ->
		def type = entry['@type']
		def script = config['jsonld'].types[type];
		def result = [id: entry['@id'], type: type ];
		if (script) {
			// assumes groovy for now
			try {
				manager.getBindings().put('entry', entry)
				result << engine.eval(new FileReader(script))
			} catch (e) {
				logger.error("Failed to run: ${script}");
				logger.error(e)
			}
		} else {
			logger.info("No script configured for: ${entry['@id']} with type: ${type}, storing as JSON.");
			result << entry;
		}
		childDocs << result;
	}
	document["_childDocuments_"] = childDocs;
}

document["raw_compacted"] = JsonOutput.toJson(compacted);
document["date_updated"] = new Date();
return document;
