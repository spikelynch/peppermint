
import groovy.json.*;

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
