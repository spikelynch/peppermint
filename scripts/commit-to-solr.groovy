/**
* This script will expects an array of maps. Each must contain the following:
*
* 'document' - the SolrInput document
* 'core' - the core for this document
*
* The map may contain the following:
*
* 'format' - the format parameter to append to the URL
*
*/
@Grapes([
	@Grab(group='org.apache.solr', module='solr-solrj', version='7.4.0')
])

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.client.solrj.*
import org.apache.solr.client.solrj.impl.*
import groovy.json.*
//-------------------------------------------------------
// Init, executed once to grab dependencies
//-------------------------------------------------------
try {
	if (initRun) {
		println "SOLR Committer, init okay."
		return
	}
} catch (e) {
	// swallowing
}
//-------------------------------------------------------
// Script Fns
//-------------------------------------------------------

//-------------------------------------------------------
// Start of Script
//-------------------------------------------------------
def getClient(clients, core, format) {
	def clientId = "${core}${format ? '_?format='+format : ''}"
	if (clients[clientId]) {
		return clients[clientId]
	}
	final String solrUrl = "${config.solr.baseUrl}/solr/${core}${format ? '?format='+format : ''}";
	clients[clientId] = new HttpSolrClient.Builder(solrUrl)
	  .withConnectionTimeout(10000)
	  .withSocketTimeout(60000)
	  .build();
	return clients[clientId]
}

def getSolrDoc(doc) {
	def solrDoc = new SolrInputDocument()
	doc.each { k, v ->
		if (k == '_childDocuments_') {
			v.each { c ->
				solrDoc.addChildDocument(getSolrDoc(c))
			}
		} else {
			if (v instanceof List || v instanceof Collection || v instanceof ArrayList) {
				v.each {
					solrDoc.addField(k, translateValue(it))
				}
			} else {
				solrDoc.addField(k, translateValue(v))
			}
		}
	}
	return solrDoc
}

def translateValue(v) {
	if (v instanceof Map) {
		return JsonOutput.toJson(v)
	}
	return v
}

def clients = [:]

logger.info("Committing to: ${config.solr.baseUrl}/solr")

scriptOutput.each { v ->
	def doc = getSolrDoc(v.document)
	//logger.info("Adding SOLR Doc:")
	//logger.info(doc.toString())
    getClient(clients, v.core.toString(), v['format']).add(doc)
  // getClient(clients, v.core.toString(), v['format']).add(v.document)
};
clients.each { clientId, client ->
	logger.info("Committing: ${clientId}");
	client.commit();
}
