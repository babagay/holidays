# holidays
Simple React and Spring boot application

## Run application in simple fashion


### Run back-end app
```
mvn spring-boot:run
``` 

Alterbatively, just start main()

### Run front-end
#### Go to frontend root
``` 
cd src\main\frontend
``` 

#### Run a React application and Hot Swap a React code
```
npm start 
```
check: http://localhost:3000

Note: You should check your changes on port 3000 if you develop UI.
      On port 8080 there is a compiled static code got from java artifact.
      Other variant - execute next commands (inside frontend folder; target folder must already exist)
```
npm run build 
Remove-Item -Recurse -Force ..\..\..\target\classes\static\*
Copy-Item -Recurse -Force build\* ..\..\..\target\classes\static\
```
or
```
npm run deploy
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

    url:jdbc:h2:file:~/data/maindb user:SA password:password
    Data stored in <currentUserFolder>/data/maindb.mv.db

Note: The DB inits each time you run a back end app. All new records will be removed
Note: tests using a real DB will fail due to DB file will be locked

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
This will execute the frontend-maven-plugin to install Node.js, npm, and build the React frontend. 
The maven-resources-plugin will then copy the React build output into the WEB-INF/classes/static directory of the WAR file.

Other config is presented in pom.xml and its pretty standard.

You can deploy the generated WAR file to your servlet container like Tomcat, Jetty, or any other compatible container.

### Build a project
There is a command to build a project
```
mvn clean install # full build including npm install and execution the all tests
    or 
mvn install "-Dskip.npm" "-DskipTests"
    or
mvn clean install -DskipFrontend "-Dmaven.test.skip=true" # skip npm install and tests
```

Then you can run it
```
mvn spring-boot:run
```

### Testing  
After up and running you can test the app at
http://localhost:8080

It is valid for case when you run 'mvn clean install' command

Note: in case of changes you need to run 'mvn clean install' again to see differences on UI

To develop frontend run UI with React (see command above) and open localhost:3000

To use Postman run App, authenticate using your GitHub account
Then open dev tools and copy JSESSIONID.
After that use it in the Postman's Cookie

Note: integration tests use basic auth

## TODO list 
* Implement tooltips on events to see the whole Title
* Implement custom useForm hook

## Storybook
Go to frontend directory and run the storybook server:
```
npm run storybook
```
Storybook is reachable at http://localhost:6006
There we can check the appearance of different views of components, e.g. HolidayModal. 

## Oauth2
After implementing authorization, thee is a bit tricky to test with Postman
Regular way looks like this
    In POstman go to Authorization tab
    Select Oauth 2.0
    Scroll down
    Click Get NEw Token button 
    Authenticate with Git
    (!) It doesnot work. So use the second way
    Authenticate with Git via UI
    Copy Cookie
    Insert the header in the Postman
    Switch off authentication (select No Auth in Authorization tab in Postman)
