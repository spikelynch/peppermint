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

// checking for init runs
try {
	if (initRun) {
		println "JSON LD Parser, init okay."
		return
	}
} catch (e) {
	// swallowing
}

def getClient(clients, core, format) {
	def clientId = "${core}_${format ? '?format='+format : ''}"
	if (clients[clientId]) {
		return clients[clientId]
	}
	final String solrUrl = "${config.solr.baseUrl}/${core}${format ? '?format='+format : ''}";
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
			solrDoc.addField(k, v)
		}
	}
	return solrDoc
}

def clients = [:]

logger.info "Committing to: ${config.solr.baseUrl}"

scriptOutput.each { v ->
	def doc = getSolrDoc(v.document)
	logger.info("Adding SOLR Doc:")
	logger.info(doc.toString())
  getClient(clients, v.core.toString(), v['format']).add(doc)
  // getClient(clients, v.core.toString(), v['format']).add(v.document)
};
clients.each { clientId, client ->
	logger.info "Committing: ${clientId}"
	client.commit();
}
