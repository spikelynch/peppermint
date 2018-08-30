
import groovy.json.*;

// checking for init runs
try {
	if (initRun) {
		println "JSON LD Parser, init okay."
		return
	}
} catch (e) {
	// swallowing
}

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
        ''' +entry['http://schema.org/longitude']+ ''',
        ''' +entry['http://schema.org/latitude']+ '''
        ]
      }
    }
  ]
}
'''
return new JsonSlurper().parseText(output)
