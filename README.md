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
curl -X POST "http://localhost:8000/stanbol/ontonethub/ontologies/find" -H  "accept: application/json" -H  "content-type: application/x-www-form-urlencoded" -d "name=codice fis*&lang=it”
```
The output returned by the find method is a JSON array, whose elements are the results mathing the search. An example of possible ouput is the following:
```
[
    {
        "score": 1.8298779,
        "ontology": {
            "id": "wPeZgTJZ5BJK6CDwHbh4rw",
            "label": [
                {
                    "value": "Organizzazioni (Pubbliche e Private)",
                    "lang": "it"
                }
            ],
            "comment": []
        },
        "universe": {
            "value": "COV-AP_IT.Organization.taxCode.Literal",
            "fingerprint": "COV-AP_IT.Organization.taxCode.Literal",
            "domain": {
                "id": "https://w3id.org/italia/onto/COV/Organization",
                "label": [
                    {
                        "value": "Organizzazione",
                        "lang": "it"
                    }
                ],
                "comment": [
                    {
                        "value": "Questa è la classe che rappresenta un'organizzazione, sia essa pubblica che privata, tipicamente registrata all'interno di un registro pubblico (e.g., indice della PA per le pubbliche amministrazioni, registro imprese per le organizzazioni private). Esempio \"Agenzia per l'Ialia Digitale\", \"Comune di Bologna\", \"TELECOM ITALIA SPA O TIM S.P.A.\"",
                        "lang": "it"
                    }
                ],
                "controlledVocabularies": []
            },
            "property": {
                "id": "https://w3id.org/italia/onto/COV/taxCode",
                "label": [
                    {
                        "value": "codice fiscale",
                        "lang": "it"
                    }
                ],
                "comment": [
                    {
                        "value": "Questa proprietà rappresenta il codice fiscale dell'organizzazione sia essa pubblica che privata. Nel caso di imprese private il codice fiscale e la partita iva, tranne eccezioni, sono sempre coincidenti per le società, e sempre differenti per le imprese individuali. Il numero \"Registro Imprese\", ovvero il numero di iscrizione attribuito dal Registro Imprese della Camera di Commercio per le organizzazioni private, è il Codice Fiscale; nel caso si tratti di impresa individuale sarà il Codice Fiscale del titolare.",
                        "lang": "it"
                    }
                ],
                "controlledVocabularies": []
            },
            "range": {
                "id": "http://www.w3.org/2000/01/rdf-schema#Literal",
                "label": [
                    {
                        "value": "Literal"
                    }
                ],
                "comment": [],
                "controlledVocabularies": []
            }
        },
        "contexts": [
            {
                "id": "https://w3id.org/italia/onto/CLV/CLV-AP_IT.Identifier.issuedBy.Organization",
                "label": "CLV-AP_IT.Identifier.issuedBy.Organization",
                "humanlabel": "'rilasciato' da associato Identificativo."
            }
        ]
    }
]
```

Where:
 - score: identifies the confidence returned querying the Solr index;
 - ontology: is the JSON object that provides information about the ontology where the result has been found. The structure of the object is the following:
   - id: the ID associated with the ontology internally to the OntoNetHub;
   - label: the array of ontology labels. The array contains JSON object that represent literals using the schema `{value: string, lang: string}`. Accordingly:
     - value: is the literal value;
     - lang: is the natural language for expressing the specific value.
   - comment: the array of ontology comments. The array contains JSON object that represent literals using the schema `{value: string, lang: string}` as for labels.
 - universe: provide the information that contextualises the result with repsect to the search query. Namely, a universe provide information about a the triple domain-property-range (aka subject-property-range) that mathces a possible term provided as input. The universe is a JSON object defined as follows:
   - value: the literal representation of the context, povided as ONTOLOGY.DOMAIN.PROPERTY.RANGE. An example of possible literal is "COV-AP_IT.Organization.taxCode.Literal", where COV-AP_IT idenfies the ontology, Organization is the domaim, taxCode is the property, and Literal is the range;
   - fingerprint: is the MD5 checksum of the context value;
   - domain: is the JSON object that provides information about the domain returned in the universe. The schema is `{id: string, label: object, comment: object}`, where:
     - id: is the URI that idenfies the domain class, e.g. https://w3id.org/italia/onto/COV/Organization ;
     - label and comment: are literal objects represented as for the ontology object.
     - controlledVocabularies: is the array of possible available controlled vocabularies that standardise the object of this type. Controlled vocabularies are returned as URIs provided as string, e.g. http://dati.gov.it/onto/controlledvocabulary/AccoStarRating;
     - contexts: the list of contextes associated with a specific object. A context is a JSON object with the schema `{id:string, label:string}`. Basically a context is a reference to a universe that holds for a specific concept when the latter is used either as domain or as range in the universe that characterises a search result.
   - property: is the JSON object that provides information about the property returned in the universe. The schema is `{id: string, label: object, comment: object, controlledVocabularies}` as previously defined;
  - range: is the JSON object that provides information about the range returned in the universe. The schema is `{id: string, label: object, comment: object, controlledVocabularies, contexts}` as previously defined.

In order to query a specific ontology instead of the whole set of ontologies managed by the OntoNetHub the path of the requests has to be set to `http://localhost:8000/stanbol/ontonethub/ontology/{ontologyID}/find`, where `ontologyID` has to be replaced with a proper ontology identifier, e.g. 44HDRw9NEKK4gAfQprG_ZQ as used in previous examples.

Furthermore, it is possible to retrieve the context associated with a certain ontology entity. By context we mean the RDF representation of an entity, which is:
 - the inferred domain of a property, in case the entity is the latter property is the object of the context we are looking for; 
 - the inferred domain of a property having a certain class as range, in case the latter class is the object of the context we are looking for.

The following is an example of a curl request that looks for the context for the entity http://dati.gov.it/onto/smapit/emailAddress.
```
curl -X GET -H "Accept: application/json" 'http://localhost:8080/stanbol/ontonethub/ontologies/context?id=http://dati.gov.it/onto/smapit/emailAddress&lang=it' 
```
In such a request the parameter `id` identifies the entity and the parameter `lang` allows to retrieve literals, which are associated with the found context and defined in a specific language.

### Compiling from source code
The OnteNetHub is released along with the source code, which is available in the folder `ontonethub-src`. The source code is written in Java and can be built by using [`Maven`](https://maven.apache.org/). The following command can be used for bulding the source code if executed by command line from the root of the `ontonethub-src` folder:
```
mvn clean install
```
Once the compilation process finishes the WAR application `stanbol.war` is available in the folder `ontonethub-src/ontonethub-war/target`. The `stanbol.war` can be deployed in any application server (e.g. [`Tomcat`](https://tomcat.apache.org/)). We remark that the docker component part of this release provides a Tomcat service.