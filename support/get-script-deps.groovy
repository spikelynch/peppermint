import groovy.json.*
import javax.script.*;
import java.nio.file.*;

def getEngineName(scriptPath) {
	if (scriptPath.endsWith('groovy')) {
		return 'groovy'
	}
	if (scriptPath.endsWith('js')) {
		return 'nashorn'
	}

}

def rootDir = "/opt/peppermint"
if (!Files.exists(Paths.get(rootDir))) {
	rootDir = "."
}
println("Peppermint Prep Script running, using root directory: ${rootDir}")
def configPath = "${rootDir}/config.json";
if (!Files.exists(Paths.get(configPath))) {
	println("Configuration file missing: ${configPath}");
	return;
}
println("Using configuration: ${configPath}");
// load config
def jsonSlurper = new JsonSlurper()
def config = jsonSlurper.parseText(new File(configPath).text)

def binding = new SimpleBindings([initRun: true])
def manager = new ScriptEngineManager()
manager.setBindings(binding);

config.routes.each { routeConfig ->
  routeConfig.each { entryName, entryValue ->
    if (entryName != "method" && entryName != "path") {
      if (entryValue.scripts) {
        println "Preparing '${entryName}' scripts..."
        entryValue.scripts.each { script ->
          def engine = manager.getEngineByName(this.getEngineName(script));
          def scriptPath = "${rootDir}/${config.scriptSubDir}/${script}"
          println "Processing: ${scriptPath}"
          if (!Files.exists(Paths.get(scriptPath))) {
  					println("Script not found for : ${script}")
  					println("Please check your configuration.")
  				} else {
            engine.eval(new FileReader(scriptPath))
          }
        }
      }
    }
  }
}
println "Script dependencies downloaded."
