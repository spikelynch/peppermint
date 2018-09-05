/**
 * This script expects the following on the binding:
 *
 *  document - the parent document
 *  docList - the list of maps that will be saved (see commit-to-solr.groovy)
 *  entry - the map containing the JSON entry
 *
 */
@Grapes([
	@Grab(group='org.apache.solr', module='solr-solrj', version='7.4.0')
])

import org.apache.solr.common.*;
import groovy.json.*;

// checking for init runs
try {
	if (initRun) {
		println "GeoJSON Parser, init okay."
		return
	}
} catch (e) {
	// swallowing
}
def lat = "${String.valueOf(entry['http://schema.org/longitude'])}"
def lng = "${String.valueOf(entry['http://schema.org/latitude'])}"
def output ='''
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "properties": {},
      "geometry": {
        "type": "Point",
        "coordinates": [
        ''' +lng+ ''',
        ''' +lat+ '''
        ]
      }
    }
  ]
}
'''
def doc = [:]
doc['id'] = entry['@id']
doc['geojson_s'] = output
document['_childDocuments_'] << doc
