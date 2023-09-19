# holidays
Simple React and Spring boot application

## Run application in simple fashion


### Run back-end app
```
mvn spring-boot:run
``` 

### Run front-end
#### Go to frontend root
``` 
cd src\main\frontend
``` 

#### Run a React application
```
npm start 
``` 



## Testing
### A quick run case
Originally, you start back-end (spring boot) and front-end (react) applications independently.
This case is more convenient for developing.
You can test the REST API on http://localhost:8080
and see UI on http://localhost:8080

### Other endpoints to test Spring app
app status: http://localhost:8080/actuator/health
H2 console: http://localhost:8080/h2

Note: The DB inits each time you run a back end app. All new records will be removed

### A more complex case
You can compile a single artifact (war file) which included back-end and front-end inside.
It can be useful to deliver changes - they will be delivered as a single file.

To do this we need to use a frontend-maven-plugin and maven-resources-plugin.
The first one is needed to inject NodeJS into artifact and deploy node modules inside it.
The second one is needed to copy frontend code into artifact.

The significant configuration is below.
frontend-maven-plugin (install Node and node modules):
```
		<configuration>
			<workingDirectory>${project.basedir}/src/main/frontend</workingDirectory>
			<installDirectory>${project.basedir}/src/main/frontend/node</installDirectory>
			<arguments>install</arguments> <!-- Specify the npm or yarn command to execute -->
		</configuration>
```
maven-resources-plugin (copy front-end code to artifact):
```
    <outputDirectory>${project.build.directory}/classes/static</outputDirectory>
    ...
    <resources>
				<resource>
					<directory>${project.basedir}/src/main/frontend/build</directory>
```

Other config is presented in pom.xml and its pretty standard.

### Build a project
There is a command to build a project
```
mvn clean install
```

Then you can run it
```
mvn spring-boot:run
```

### Testing  
After up and running you can test the app at
http://localhost:8080