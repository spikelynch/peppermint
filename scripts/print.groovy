
// checking for init runs
try {
	if (initRun) {
		println "JSON LD Parser, init okay."
		return
	}
} catch (e) {
	// swallowing
}
println "Peppermint printing: ${data}"
