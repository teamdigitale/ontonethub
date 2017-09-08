# OntoNetHub

OntoNetHub is a Web-based application meant to deal with the management of ontology networks.
This include the upload, deletion, storage, and indexing of an ontology part of a network.

### Requirements

OntoNetHub is designed as an extension of [`Apache Stanbol`](https://stanbol.apache.org/) and released as a Docker component. Hence, users need [`Docker`](https://docs.docker.com/) to build and run OntoNetHub.

### Building and Running

First run your Docker instance. Then type the following command on terminal from the root of the project (i.e. the folder `ontonethub`) for building the components:

```
docker-compose build
```

Finally, type the following command on terminal for running the OntoNetHub.

```
docker-compose up
```

After that OntoNetHub is available on your browser at [`http://localhost:8000/stanbol/ontonethub`](http://localhost:8000/stanbol/ontonethub).

### Usage
The following sections describe the functionalities provided by the OntoNetHub. More details about the usage of the OntoNetHub via its HTTP REST API can be found in the Swagger descriptor (i.e. `ontonethub.yaml`) included in this release.

##### Uploading an ontology
An ontology can be uploaded by perfoming a HTTP POST request to the path `/stanbol/ontonethub/ontology`. The upload triggers the indexing of ontological terms and the physical storage of the ontology itself. The indexing is performed by using `rdfs:label` and `rdfs:comment` annotations associated with the OWL entities part of the ontology. The index resulting from an indexing process in a Solr index. The OntoNetHub relies on the Stanbol EntityHub for manging everything associated with the indexing generation and idexing searching.
The following is an example of a `curl` request for uploading an ontology to the OntoNetHub.

```
curl -X POST \
  -H "Content-type: multipart/form-data; boundary=----WebKitFormBoundaryzeZR8KqAYJyI2jPL" \
  -H "Accept: application/json" \
  -F name=person \
  -F description="The ontology for representing persons." \
  -F baseUri=http://data.gov.it/ontologies/person \
  -F data=@person.rdf \
  http://localhost:8000/stanbol/ontonethub/ontology
``` 
The request above returns the following JSON.
```
{
  "monitoringService":"http:\/\/localhost:8000\/stanbol\/jobs\/tGEjuGTUi8b_Vm5OneRSkg",
  "ontologyId":"tGEjuGTUi8b_Vm5OneRSkg"
}
```
Where:
 - the `monitoringService` represent the resource to query for obtaining the status (i.e. finished, running, aborted) of the job associated with the upload of an ontology;
 - the `ontologyId` is the identifier of the ontology within the OntoNetHub.
 
The URL provided by the `monitoringService` can be queried via HTTP GET. The following is a curl example.

```
curl -H "Accept: application/json" http://localhost:8000/stanbol/jobs/tGEjuGTUi8b_Vm5OneRSkg
```
The request above returns the following JSON:
```
{
  "status": "finished",
  "outputLocation": "http://localhost:8000/stanbol/ontonethub/ontology/tGEjuGTUi8b_Vm5OneRSkg",
  "messages": [
    "You can remove this job using DELETE",
  ]
}
```

##### Accessing an ontology within the OntoNetHub
The `outputLocation`, part of the JSON returned by the job service, provides the URL to access the information about the specific ontology indexed. The following is a curl example
```
curl -H "Accept: application/json" http://localhost:8000/stanbol/ontonethub/ontology/tGEjuGTUi8b_Vm5OneRSkg
```
The output of the request above is the following:
```
{
  "id":"tGEjuGTUi8b_Vm5OneRSkg",
  "ontologySource":"http:\/\/localhost:8000\/stanbol\/\/ontonethub\/ontology\/tGEjuGTUi8b_Vm5OneRSkg\/source",
  "name":"person",
  "description":"The ontology for representing persons.",
  "ontologyIRI":"http:\/\/data.gov.it\/ontologies\/person",
  "owlClasses":66,
  "objectProperties":60,
  "datatypeProperties":9,
  "annotationProperties":22,
  "individuals":0,
  "importedOntologies":7
}
```
Where are reported metadata (i.e. name, description, and ontologyIRI) and statistics (i.e.
number of owlClasses, objectProperties, datatypeProperties, annotationProperties, individuals, and importedOntologies). The attribute ontology `ontologySource` provides the URL to access the OWL source of the ontology. For example, the following curl request return the [`JSON-LD`](https://json-ld.org/) serialisation of the person ontology.
```
curl -H "Accept: application/json-ld" http://localhost:8080/stanbol/ontonethub/ontology/tGEjuGTUi8b_Vm5OneRSkg/source
```

##### Deleting an ontology from the OntoNetHub
An ontology can be deleted from the OntoNetHub by perfoming an HTTP DELETE request to the resource representing the ontology within the OntoNetHub. The following is an example:
```
curl -X DELETE http://localhost:8080/stanbol/ontonethub/ontology/44HDRw9NEKK4gAfQprG_ZQ
```

##### Querying the OntoNetHub
It is possible to query the OntoNetHub for retrieving OWL entities from the ontologies managed by the OntoNetHub. The following is an example of query for searching all the OWL entities having an annotation (i.e. `rdfs:label` or `rdfs:comment`) that match the string `Persona` in Italian.
```
curl -X POST "http://localhost:8000/stanbol/ontonethub/ontologies/find" -H  "accept: application/json" -H  "content-type: application/x-www-form-urlencoded" -d "name=Persona&lang=it”
```
It is possilble to use wildcards (i.e. `*`) in queries. Hence, if we want to find all possible terms staring with the word `Pers` the example above is converted to the following:
```
curl -X POST "http://localhost:8000/stanbol/ontonethub/ontologies/find" -H  "accept: application/json" -H  "content-type: application/x-www-form-urlencoded" -d "name=Pers*&lang=it”
```
In order to query a specific ontology instead of the whole set of ontologies managed by the OntoNetHub the path of the requests has to be set to `http://localhost:8000/stanbol/ontonethub/ontology/{ontologyID}/find`, where `ontologyID` has to be replaced with a proper ontology identifier, e.g. 44HDRw9NEKK4gAfQprG_ZQ as used in previous examples.

### Compiling from source code
The OnteNetHub is released along with the source code, which is available in the folder `ontonethub-src`. The source code is written in Java and can be built by using [`Maven`](https://maven.apache.org/). The following command can be used for bulding the source code if executed by command line from the root of the `ontonethub-src` folder:
```
mvn clean install
```
Once the compilation process finishes the WAR application `stanbol.war` is available in the folder `ontonethub-src/ontonethub-war/target`. The `stanbol.war` can be deployed in any application server (e.g. [`Tomcat`](https://tomcat.apache.org/)). We remark that the docker component part of this release provides a Tomcat service.