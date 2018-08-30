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

println "Committing to: ${config.solr.baseUrl}/${config.solr.baseCore}"
final String solrUrl = config.solr.baseUrl;
def client = new HttpSolrClient.Builder(solrUrl)
  .withConnectionTimeout(10000)
  .withSocketTimeout(60000)
  .build();
def document = new SolrInputDocument();
scriptOutput.each { k,v ->
  document.addField(k, v);
};
println document.toString();
client.add(config.solr.baseCore, document);
client.commit(config.solr.baseCore);
