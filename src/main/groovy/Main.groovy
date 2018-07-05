import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.impl.BearerAuthHandler
import io.vertx.core.logging.*
import io.vertx.core.http.*
import groovy.json.*
import java.nio.file.*;
import peppermint.*;

//-----------------------------------------
// START OF SCRIPT
//-----------------------------------------

def logger =  LoggerFactory.getLogger(this.getClass());
def rootDir = "/opt/peppermint"
if (!Files.exists(Paths.get(rootDir))) {
	rootDir = "./"
}
logger.info("Peppermint starting, using root directory: ${rootDir}")
def configPath = "${rootDir}/config.json";
if (!Files.exists(Paths.get(configPath))) {
	logger.error("Configuration file missing: ${configPath}");
	return;
}
logger.info("Using configuration: ${configPath}");
// load config
def jsonSlurper = new JsonSlurper()
def config = jsonSlurper.parseText(new File(configPath).text)
def server = vertx.createHttpServer()
def router = Router.router(vertx)
// install the default body handler
router.route().handler(BodyHandler.create())
router.route().handler(new BearerAuthHandler(new BearerAuthProvider(config.token)));
// build the routers based on configuration
config.routes.each { routeConfig ->
	router.route(HttpMethod[routeConfig.method], routeConfig.path).blockingHandler({ routingContext ->
		def recType = routingContext.request().getParam("recordType")
		def data = routingContext.getBodyAsJson();
		def response = routingContext.response()
		response.putHeader("content-type", "application/json")
		def binding = new Binding([recordType: recType, bodyData: data, response: response ]);
		def shell = new GroovyShell(binding)
		def output = [results:[]]
		data.records.each{ record -> 
			binding.setVariable("data", record)
			def outputEntry = [id: record.id, scripts:[:]] 
			routeConfig[recType].each { scriptPath ->
				if (!scriptPath.startsWith('/')) {
					scriptPath = "${rootDir}/${config.scriptSubDir}/${scriptPath}"
				}
				def scriptPathObj = Paths.get(scriptPath)
				if (!Files.exists(scriptPathObj)) {
					logger.error("Script not found for ${recType}: ${scriptPath}")
					logger.error("Please check your configuration.")
				}
				def success = true
				try {
					shell.evaluate(new File(scriptPath))
				} catch (e) {
					logger.error("Error running script for record type: ${recType}: ${scriptPath}")
					logger.error(e)
					success = false
				}
				outputEntry.scripts[scriptPathObj.getFileName()] = [success: success]
			}
			output.results << outputEntry
		}
		response.end(JsonOutput.toJson(output))
	})
}

server.requestHandler(router.&accept).listen(config.port)