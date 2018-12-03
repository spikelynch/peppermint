/**
*  Initialises the PROAI Solr repository.
*
*  Some sections like metadata formats, sets, etc. should be moved to a Proai API rather than from a Peppermint script.
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
		println "OAI-PMH initer, init okay."
		return
	}
} catch (e) {
	// swallowing
}
//-------------------------------------------------------
// Script Fns
//-------------------------------------------------------
def getClient(clients, core) {
	def clientId = "${config.solr.baseUrl}_${core}"
	if (clients[clientId]) {
		return clients[clientId]
	}
	final String solrUrl = "${config.solr.baseUrl}/solr/${core}";
	clients[clientId] = new HttpSolrClient.Builder(solrUrl)
	  .withConnectionTimeout(10000)
	  .withSocketTimeout(60000)
	  .build();
	return clients[clientId]
}

def getInitOaiRecords(coreName) {
	def solrDocs = []
	config.solr.oaipmh[coreName].init.each { initRec ->
		def solrDoc = new SolrInputDocument()
		if (initRec.xml && initRec.xml instanceof String) {
			solrDoc.addField("xml_s", initRec.xml)
			solrDoc.addField("record_type_s", initRec.recordType)
			def id = initRec.id
			if (!id) {
				id = initRec.recordType
			}
		  solrDoc.addField("id", id)
			solrDocs << solrDoc
		} else {
			if (initRec.xml && initRec.xml instanceof List) {
				initRec.xml.each { initXml ->
					if (initXml instanceof String) {
						solrDoc.addField("xml_s", initXml.xml)
						solrDoc.addField("record_type_s", initRec.recordType)
					}
					solrDocs << solrDoc
					solrDoc = new SolrInputDocument()
				}
			} else if (initRec.entries){
				initRec.entries.each { entry ->
					solrDoc.addField("record_type_s", initRec.recordType)
					entry.each { entryField, entryVal ->
						solrDoc.addField(entryField, entryVal)
					}
					solrDocs << solrDoc
					solrDoc = new SolrInputDocument()
				}
			}
		}
	}
	return solrDocs
}

//-------------------------------------------------------
// Start of Script
//-------------------------------------------------------


def clients = [:]

logger.info "Preparing OAI-PMH SOLR repo in: ${config.solr.baseUrl}/solr"

if (config.solr.oaipmh.reuseCore) {
  config.solr.cores.each { core ->
    def client = getClient(clients, core)
		// loop through and get all the init records
		getInitOaiRecords(core).each { rec ->
			client.add(rec)
		}
  }
}

clients.each { clientId, client ->
	logger.info "Committing: ${clientId}"
	client.commit();
}
