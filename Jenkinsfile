SCM = [
        baseURL: 'https://github.com/mknezic/test-some-code.git',
		branches: '*/master',
		module: '.'
]

//pipelineTriggers([cron('*/1 * * * *')]), /*properties([pipelineTriggers([cron('H 23 * * *')])]) */
//pipelineTriggers([[$class: "ParameterizedTimerTrigger", parameterizedSpecification: "H 23 * * * %typeOfBuild=NIGHTLY"]]),
properties ([
        [$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '5']],
        disableConcurrentBuilds(),
        [$class: 'AuthorizationMatrixProperty',
		    blocksInheritance: true,
		    permissions : [
				'hudson.model.Item.Build:admin',
				'hudson.model.Item.Cancel:admin',
				'hudson.model.Item.Configure:admin',
				'hudson.model.Item.Create:admin',
				'hudson.model.Item.Delete:admin',
				'hudson.model.Item.Discover:admin',
				'hudson.model.Item.Move:admin',
				'hudson.model.Item.Read:admin',
				'hudson.model.Item.Read:admin',
				'hudson.model.Item.Workspace:admin',
				'hudson.model.Run.Delete:admin',
				'hudson.model.Run.Replay:admin',
				'hudson.model.Run.Update:admin'
			]
		],
		[$class: 'ParametersDefinitionProperty',
         parameterDefinitions: [
                 [$class: 'ChoiceParameterDefinition',
                  choices: 'DEFAULT\r\nNIGHTLY',
                  description: 'Type of build: DEFAULT or NIGHTLY build.',
                  name: 'typeOfBuild'],
                 [$class: 'BooleanParameterDefinition',
                  defaultValue: true,
                  description: 'send email notification?',
                  name: 'sendEmailNotification'],
                 [$class: 'BooleanParameterDefinition',
                  defaultValue: true,
                  description: 'clean out workspace / free space on build machine ?',
                  name: 'cleanWorkspace']
         ]
        ]
])

/* required tools for build*/
jdkTool = 'JDK 8'
mavenTool = 'Maven 3.3'
/* global maven settins file */
//mavenSettings = "$JENKINS_HOME/.m2/settings.xml"
sonarQubeRunnerTool = 'SQS 2.8'

def moduleCheckout() {
    /*checkout([$class: 'GitSCM', branches: [[name: SCM.branches]], 
				doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], 
				userRemoteConfigs: [[url: SCM.baseuRL]]])
	*/
	checkout changelog: false, poll: false, scm: [$class: 'GitSCM', branches: [[name: SCM.branches]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[url: SCM.baseURL]]]
}

def mvn(args, dirName) {
    if (dirName != null) {
        dir(dirName) {
            sh "JAVA_HOME=${tool jdkTool} ${tool mavenTool}/bin/mvn ${args}"
        }
    } else {
        sh "JAVA_HOME=${tool jdkTool} ${tool mavenTool}/bin/mvn ${args}"
    }
}

@Library('mk-utils') import static org.foo.Utilities.*

import groovy.json.JsonSlurperClassic

apiSonarRoot = "http://192.168.99.100:32770"

def getSonarJson(path) {
	sonarUrl = "${apiSonarRoot}${path}"
	echo "Reading URL ${sonarUrl} contents"
	sonarUrl.toURL().text
}

def getParsedResponse(responseJson) {
	println "got response ${responseJson}"
	new JsonSlurperClassic().parseText(responseJson)
}

def getNewViolatedResources(resKey, qualifier) {
	violatedResources = []
	qualifierParam = qualifier ? "&qualifiers=${qualifier}" : ""
	allResources = getParsedResponse(getSonarJson("/api/measures/component_tree?metricKeys=new_violations&additionalFields=periods${qualifierParam}&baseComponentKey=${resKey}"))
	println "all resources ${allResources}"
	println "all resources ${allResources.dump()}"
	println "components ${allResources.components}"
	println "components len ${allResources.components.size()}"
	println "components dump ${allResources.components.dump()}"
	println "with size ${allResources.components.size()}"
	if (allResources.components.size() > 0) {
		println "got some components"
		allResources.components.each { res ->
			println "Adding violated resource ${res.id} [${res.key}] for qualifier ${qualifier}"
			violatedResources << res.key
		}
	} else {
		println "no violated components, adding base component ${allResources.baseComponent.key}"
		violatedResources << allResources.baseComponent.key
	}
	println "found violated resources ${violatedResources}"
	violatedResources
}

def parseLineMetrics(resources, parser) {
	result = []

	if (resources?.size() == 1) {
		resources?.scm.each { scmLine ->
			result << [
				line: scmLine[0],
				author: scmLine[1],
				commitDate: parser(scmLine[2]),
				revision: scmLine[3]
			]
		}
	}

	result
}

def getDatesByLine(resKey) {
	datesResource = getParsedResponse(getSonarJson("/api/sources/scm?commits_by_line=true&key=${resKey}"))
	parseLineMetrics(datesResource, { data -> getDateFromString(data) })
}

def getResource(resKey) {
	lines = []
	contents = getParsedResponse(getSonarJson("/api/sources/show?key=${resKey}"))

	if (contents?.sources?.size() > 0) {
		contents.sources.each { line ->
			lines << line
		}
	}

	lines
}

def getDateFromString(input) {
	//new Date().parse("yyyy-MM-dd'T'HH:mm:ssZ", input).clearTime()
	new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(input).clearTime()
}

def getResourceViolations(resKey) {
	violations = []
	today = new Date().clearTime()

	contents = getParsedResponse(getSonarJson("/api/issues/search?componentKeys=${resKey}&statuses=OPEN&createdInLast=100w&severities=BLOCKER,CRITICAL,MAJOR,MINOR"))
	contents.issues.each { violation ->
		createdAt = getDateFromString(violation.creationDate)

		//if (createdAt == today) {
			datesByLine = getDatesByLine(resKey)
			authorsByLine = datesByLine.findAll { entry -> entry.commitDate == today && entry.line == violation.line }.collect { entry -> entry.author }
			revisionsByLine = datesByLine.findAll { entry -> entry.commitDate == today && entry.line == violation.line }.collect { entry -> entry.revision }
			lineNumText = violation.line
			lineNumInt = lineNumText ? lineNumText.toInteger() : -1

			if (authorsByLine.size() > 0) {
				culprits = authorsByLine.unique()
			} else {
				culprits = []
				if (lineNumInt > -1)
					culprits << [violation.author]
			}
			
			culpritsText = culprits.size() > 0 ? culprits.join(" or ") : "Someone"
			
			if (revisionsByLine.size() > 0) {
				culpritRevisions = revisionsByLine.unique()
			} else {
				culpritRevisions = []
			}
			
			culpritRevisionssText = culpritRevisions.size() > 0 ? culpritRevisions.join(" or ") : "Unknown"

			violations << [
				key: violation.key,
				rule: violation.rule,
				message: violation.message,
				severity: violation.severity,
				line: lineNumInt,
				component: violation.component,
				author: culpritsText,
				revision: culpritRevisionssText
			]
		//}
	}

	violations
}


def getUserMappedViolations(projectKey) {
	violatedModules = []
	violatedFiles = []

	// Get violated projects.
	// Step 1. Read violated projects.
	violatedProjects = projectKey ? [projectKey] : getNewViolatedResources(null, "TRK")
	println "violated projects: ${violatedProjects}"

	// Get violated modules.
	// Step 2. Read violated modules for projects.
	violatedProjects.each { project ->
		println "checking project ${project}"
		violatedModules.addAll(getNewViolatedResources(project, "BRC"))
	}
	println "violated modules: ${violatedModules}"

	// Get violated files.
	// Step 3. Read violated files for modules.
	violatedModules.each { module ->
		println "checking module ${module}"
		violatedFiles.addAll(getNewViolatedResources(module, "FIL"))
	}
	println "violated files: ${violatedFiles}"

	// Step 4. Group received data per authors.
	allViolations = []
	violatedFiles.each { file ->
		println "checking file ${file}"
		fileViolations = getResourceViolations(file)
		fileContents = getResource(file)

		fileViolations.each { violation ->
			sourceFragment = null

			if (violation.line > -1) {
				sourceFragment = new java.util.HashMap() // [];  //[:]
				for (ln in violation.line - 3..violation.line + 3) {
					sourceFragment.put(ln, fileContents[ln - 1])
				}
			}

			allViolations << [violation: violation, resKey: file, source: sourceFragment]
		}
	}

	[allViolations.size(), violatedFiles.size(), allViolations.groupBy { it.violation.author }]
}

node {
	
    try {
		stage ('params') {
			echo "typeOfBuild=" + env.typeOfBuild
		}
        stage ('from lib') {
			sayHello()
			sayHello 'superbird'
			hello 'bla'
		}
        stage ('ekho') {
			sh 'echo bla'
		}
    	stage ('checkout') {
			moduleCheckout()
			sh "echo ${env.BRANCH_NAME}"
		}
    	stage ('do build') {
			mvn("-B -e -DskipTests -T1C clean install", SCM.module)
		}
		if (build.result == hudson.model.Result.SUCCESS) {
			stage ('do sonar') {
				def pom = readMavenPom file:'pom.xml'
				withSonarQubeEnv('sonar') {
					//sh "JAVA_HOME=${tool jdkTool} ${tool mavenTool}/bin/mvn sonar:sonar"
					sh "${tool sonarQubeRunnerTool}/bin/sonar-scanner -Dsonar.projectKey=" + pom.groupId + ":" + pom.artifactId + " -Dsonar.projectName='" + pom.name + "' -Dsonar.projectVersion=" + pom.version + " -Dsonar.language=java -Dsonar.sources=src -Dsonar.junit.reportsPath=target/surefire-reports"
				} // SonarQube taskId is automatically attached to the pipeline context
			
				//SonarRunnerBuilder nema interface za step builder pa nemože ovako
				/* def sonarBuilder = new hudson.plugins.sonar.SonarRunnerBuilder(
					project: null, 
						properties: 
							'sonar.projectKey=my:testproject\
							 sonar.projectName=my test project\
							 sonar.projectVersion=1.0\
							 sonar.sources=src\
							 sonar.junit.reportsPath=target/surefire-reports', 
						javaOpts: '', additionalArguments: '', jdk: 'JDK 8', task: ''
				);
				step sonarBuilder
				*/
				//nemože ni ovako jer ima databound nešto ali nema interface
				/*
					step([$class: 'SonarRunnerBuilder', project: null, 
						properties: 
							'sonar.projectKey=my:testproject\
							 sonar.projectName=my test project\
							 sonar.projectVersion=1.0\
							 sonar.sources=src\
							 sonar.junit.reportsPath=target/surefire-reports', 
						javaOpts: '', additionalArguments: '', jdk: 'JDK 8', task: '']);
				*/
				//al može ovako preko toola	
		/*    	
				sh "${tool sonarQubeRunnerTool}/bin/sonar-scanner -Dsonar.host.url=http://192.168.99.100:32770 -Dsonar.login=cc24edd931e26f9b6afa43da7dec8e34fc96e0a7 -Dsonar.projectKey=" + pom.groupId + ":" + pom.artifactId + " -Dsonar.projectName='" + pom.name + "' -Dsonar.projectVersion=" + pom.version + " -Dsonar.language=java -Dsonar.sources=src -Dsonar.junit.reportsPath=target/surefire-reports"
		  */  	
				//sh "${tool sonarQubeRunnerTool}/bin/sonar-scanner -Dsonar.host.url=http://192.168.99.100:32771 -Dsonar.login=0e56aba225734b26a5d4fce001bd7db440cf2671 -Dsonar.projectKey=my:testproject -Dsonar.projectName='my test project' -Dsonar.projectVersion=1.0 -Dsonar.language=java -Dsonar.sources=src -Dsonar.junit.reportsPath=target/surefire-reports"
				
				doBuild "something as a build name"
				currentBuild.result = 'SUCCESS'
			}
		} else {
		  echo "build not SUCCESSful, no sonar analysis"
		}
    } catch (err) {
        currentBuild.result = 'FAILURE'
        throw err
    }
}

// No need to occupy a node
if (build.result == hudson.model.Result.SUCCESS) {
	stage("Quality Gate") {
	  timeout(time: 1, unit: 'HOURS') { // Just in case something goes wrong, pipeline will be killed after a timeout
		def qg = waitForQualityGate() // Reuse taskId previously collected by withSonarQubeEnv
		if (qg.status != 'OK') {
		  error "Pipeline aborted due to quality gate failure: ${qg.status}"
		}
		//do report
		node {
			def pom = readMavenPom file:'pom.xml'
			withSonarQubeEnv('sonar') {
		//		sh "${tool sonarQubeRunnerTool}/bin/sonar-scanner -Dsonar.projectKey=" + pom.groupId + ":" + pom.artifactId + " -Dsonar.projectName='" + pom.name + "' -Dsonar.projectVersion=" + pom.version + " -Dsonar.language=java -Dsonar.sources=src -Dsonar.junit.reportsPath=target/surefire-reports"

				echo "sonar host:" + env.SONAR_HOST_URL
				echo "sonar auth:" + env.SONAR_AUTH_TOKEN
				//(violationsCount, filesCount, userMappedViolations) = getUserMappedViolations(pom.groupId)
				violationsInfo = getUserMappedViolations(pom.groupId + ":" + pom.artifactId)
				println "violations info ${violationsInfo}"
				violationsCount = violationsInfo[0]
				filesCount = violationsInfo[1]
				userMappedViolations = violationsInfo[2]
				if (violationsCount > 0 || filesCount > 0) {
					echo "has $violationsCount violations"
					echo "(violationsCount, filesCount, userMappedViolations) = ($violationsCount, $filesCount, $userMappedViolations)"
				} else {
					echo "no violations found"
				}
				def templateTxt = readFile('sonar-report-template.groovy')
				def binding = ["violationsCount": violationsCount, "filesCount":filesCount, "userMappedViolations":userMappedViolations]
				def engine = new groovy.text.SimpleTemplateEngine()
				def template = engine.createTemplate(templateTxt).make(binding)
				def resultText = template.toString()
				println "violations report: ${resultText}" 
			}
		}
	  }
	}
}