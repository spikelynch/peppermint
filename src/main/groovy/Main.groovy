import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.impl.BearerAuthHandler
import io.vertx.core.logging.*
import io.vertx.core.http.*
import groovy.json.*
import groovy.util.*
import java.nio.file.*;
import peppermint.*;
import javax.script.*;
import java.util.regex.*

def getEngineName(scriptPath) {
	if (scriptPath.endsWith('groovy')) {
		return 'groovy'
	}
	if (scriptPath.endsWith('js')) {
		return 'nashorn'
	}

}

//-----------------------------------------
// START OF Verticle
//-----------------------------------------

def logger =  LoggerFactory.getLogger(this.getClass());
def rootDir = "/opt/peppermint"
if (!Files.exists(Paths.get(rootDir))) {
	rootDir = "./"
}
logger.info("Peppermint starting....using root directory: ${rootDir}")
def configPath = "${rootDir}/config.json";
if (!Files.exists(Paths.get(configPath))) {
	logger.error("Configuration file missing: ${configPath}");
	return;
}
logger.info('''
------------------------------------------------------------------------------------------------------------------
.-------.     .-''-.  .-------. .-------.     .-''-.  .-------.    ,---.    ,---..-./`) ,---.   .--.,---------.
\\  _(`)_ \\  .'_ _   \\ \\  _(`)_ \\\\  _(`)_ \\  .'_ _   \\ |  _ _   \\   |    \\  /    |\\ .-.')|    \\  |  |\\          \\ 
| (_ o._)| / ( ` )   '| (_ o._)|| (_ o._)| / ( ` )   '| ( ' )  |   |  ,  \\/  ,  |/ `-' \\|  ,  \\ |  | `--.  ,---'
|  (_,_) /. (_ o _)  ||  (_,_) /|  (_,_) /. (_ o _)  ||(_ o _) /   |  |\\_   /|  | `-'`"`|  |\\_ \\|  |    |   \\
|   '-.-' |  (_,_)___||   '-.-' |   '-.-' |  (_,_)___|| (_,_).' __ |  _( )_/ |  | .---. |  _( )_\\  |    :_ _:
|   |     '  \\   .---.|   |     |   |     '  \\   .---.|  |\\ \\  |  || (_ o _) |  | |   | | (_ o _)  |    (_I_)
|   |      \\  `-'    /|   |     |   |      \\  `-'    /|  | \\ `'   /|  (_,_)  |  | |   | |  (_,_)\\  |   (_(=)_)
/   )       \\       / /   )     /   )       \\       / |  |  \\    / |  |      |  | |   | |  |    |  |    (_I_)
`---'        `'-..-'  `---'     `---'        `'-..-'  ''-'   `'-'  '--'      '--' '---' '--'    '--'    '---'

------------------------------------------------------------------------------------------------------------------
''')
logger.info("Using configuration: ${configPath}");
// Load up the environment variables
def configBinding = [:]
configBinding.putAll(System.getenv())
if (!configBinding['SOLR_HOST']) {
	configBinding['SOLR_HOST'] = 'localhost';
}
if (!configBinding['SOLR_PORT']) {
	configBinding['SOLR_PORT'] = '8983';
}
// replace the JSON with variables, of form $VARIABLE_NAME
def configText = new File(configPath).text;
configBinding.each { bindKey, bindVal ->
	configText = configText.replaceAll(Matcher.quoteReplacement("\$${bindKey}".toString()), bindVal);
}
// parse the configuration
def config = new JsonSlurper().parseText(configText)
def server = vertx.createHttpServer()
def router = Router.router(vertx)
// install the default body handler
router.route().handler(BodyHandler.create())
router.route().handler(new BearerAuthHandler(new BearerAuthProvider(config.token)));
// build the routers based on configuration
config.routes.each { routeConfig ->
	logger.info("Listening to route: ${routeConfig.method}:${routeConfig.path}")
	router.route(HttpMethod[routeConfig.method], routeConfig.path).blockingHandler({ routingContext ->
		def recType = routingContext.request().getParam("recordType")
		def data = routingContext.getBodyAsJson();
		def response = routingContext.response()
		response.putHeader("content-type", "application/json")
		def binding = new SimpleBindings([recordType: recType, bodyData: data, response: response, config:config, routeConfig: routeConfig, logger: logger])
		def manager = new ScriptEngineManager();
		manager.setBindings(binding);
		binding.put('manager', manager);
		def output = [results:[]]
		data.records.each{ record ->
			binding.put("data", record)
			binding.put("scriptOutput", record)
			def outputEntry = [id: record.id, scripts:[:]]
			routeConfig[recType].scripts.each { scriptPath ->
				def engine = manager.getEngineByName(this.getEngineName(scriptPath));
				binding.put('engine', engine);
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
					def scriptOutput = engine.eval(new FileReader(scriptPath))
					binding.put("scriptOutput", scriptOutput)
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
