/**
* (Re)creates the SOLR schemas from configuration
*
* expects a config object that has 'solr.definition' object with the ff. nested arrays:
*
*/
@Grapes([
  @Grab('io.github.http-builder-ng:http-builder-ng-core:1.0.3')
])
import static groovyx.net.http.HttpBuilder.configure
import static groovyx.net.http.ContentTypes.JSON
import groovyx.net.http.*
import static groovy.json.JsonOutput.prettyPrint
import groovy.json.*;

//-------------------------------------------------------
// Init, executed once to grab dependencies
//-------------------------------------------------------
try {
	if (initRun) {
		println "SOLR schema builder, init okay."
		return
	}
} catch (e) {
	// swallowing
}
//-------------------------------------------------------
// Script Fns
//-------------------------------------------------------
def generateCommandStr(commandStr, fieldArr) {
  def fieldStrArr = []
  fieldArr.each { entry ->
    fieldStrArr << "\"${commandStr}\": ${JsonOutput.toJson(entry)}"
  }
  return fieldStrArr
}

def replaceExisting(existingFields, mainArr, addArr, replaceArr, forceReplace, matchClosure = null, skipExists) {
  if (!matchClosure) {
    matchClosure = {entry ->
      return {
        it.name == entry.name
      }
    }
  }
  mainArr.each { entry ->
    def entryExist = existingFields.find matchClosure(entry)
    if (forceReplace) {
      if (entryExist) {
        replaceArr << entryExist
      }
      addArr << entry
    } else {
      if (entryExist) {
        if (!skipExists) {
          replaceArr << entry
        }
      } else {
        addArr << entry;
      }
    }
  }
}

def getSchema(core, schemaField) {
  def uri = "${config.solr.baseUrl}/solr/${core}/schema/${schemaField}?wt=json"
  return configure {
    request.uri = uri
    request.contentType = "application/json"
    // Unfortunately, SOLR returns Content-Type: "text/plain" for JSON responses, no amount of bargaining, i.e. '?wt=json' will convince it
    response.parser "text/plain", NativeHandlers.Parsers.&json
  }.get()
}

def postSchema(core, commandStr) {
  def uri = "${config.solr.baseUrl}/solr/${core}/schema?wt=json"
  return configure {
    request.uri = uri
    request.contentType = "application/json"
    request.body = commandStr
    // Unfortunately, SOLR returns Content-Type: "text/plain" for JSON responses, no amount of bargaining, i.e. '?wt=json' will convince it
    response.parser "text/plain", NativeHandlers.Parsers.&json
  }.post()
}

def processConfig(core, schemaField, responseFieldName, configArr, addCommandStr, replaceCommandStr, commandStrArr, forceReplace = false, matchClosureFn = null, skipExists = false) {
  // replace all existing fields...
  def replaceFieldArr = []
  def addFieldArr = []
  replaceExisting(getSchema(core, schemaField)[responseFieldName], configArr, addFieldArr, replaceFieldArr, forceReplace, matchClosureFn, skipExists)
  commandStrArr.addAll(generateCommandStr(replaceCommandStr, replaceFieldArr))
  commandStrArr.addAll(generateCommandStr(addCommandStr, addFieldArr))
}

def deleteIndexData(core) {
  def uri = "${config.solr.baseUrl}/solr/${core}/update?commit=true"
  return configure {
    request.uri = uri
    request.contentType = "text/xml"
    request.body = "<delete><query>*:*</query></delete>"
  }.get()
}

//-------------------------------------------------------
// Start of Script
//-------------------------------------------------------

logger.info("SOLR schema builder starting...")
if (config.solr.rebuildSchemaAlways) {
  logger.info("Rebuilding schema...deleting all existing data for each core.")
  def postDataStr = "{"
  config.solr.cores.each { core ->
    deleteIndexData(core)
    def commandStrArr = []
    def addField = config.solr.schema[core]['add-field']
    def addDynamicField = config.solr.schema[core]['add-dynamic-field']
    def addCopyField = config.solr.schema[core]['add-copy-field']
    if (addField && addField.size() > 0) {
      // Not replacing existing as fields might have dependent copyFields which will cause an error when replacing/deleting
      processConfig(core, 'fields', 'fields', addField, 'add-field', 'replace-field', commandStrArr, false, null, true)
    }
    if (addDynamicField && addDynamicField.size() > 0) {
      processConfig(core, 'dynamicfields', 'dynamicFields', addDynamicField, 'add-dynamic-field', 'replace-dynamic-field', commandStrArr)
    }
    if (addCopyField && addCopyField.size() > 0) {
      def matchClosureFn = { configEntry ->
        return { existingEntry ->
          existingEntry.source == configEntry.source && ( configEntry.dest.find { it == existingEntry.dest } )
        }
      }
      processConfig(core, 'copyfields', 'copyFields', addCopyField, 'add-copy-field', 'delete-copy-field', commandStrArr, false, matchClosureFn, true)
    }

    postDataStr = "${postDataStr}${commandStrArr.join(',')}}"
    logger.info("Command str:")
    logger.info(postDataStr)
    def schemaUpdateRes = postSchema(core, postDataStr)
    logger.info(schemaUpdateRes)
    if (schemaUpdateRes?.responseHeader?.status == 0) {
      logger.info("Updates for core: ${core}, successful!")
    }
  }
}
