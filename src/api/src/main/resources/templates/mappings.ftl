# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
#NOTE: THIS IS A DEFAULT MAPPING SPECIFICATION THAT INCLUDES MAPPINGS FOR
#      COMMON ONTOLOGIES. USERS MIGHT WANT TO ADAPT THIS CONFIGURATION BY
#      COMMENTING/UNCOMMENTING AND/OR ADDING NEW MAPPINGS

# --- Define the Languages for all fields ---
# to restrict languages to be imported (for all fields)
#| @=null;en;de;fr;it

#NOTE: null is used to import labels with no specified language

# --- Define the Languages for all fields ---
# Uncomment to restrict indexing to a specific list of languages, otherwise all
# languages are indexed
#| @=null;en;de;fr;it

# --- RDF RDFS and OWL Mappings ---
# This configuration only index properties that are typically used to store
# instance data defined by such namespaces. This excludes ontology definitions

# NOTE that nearly all other ontologies are are using properties of these three
#      schemas, therefore it is strongly recommended to include such information!

rdf:type | d=entityhub:ref

rdfs:label 
rdfs:comment
rdf:dafLabel
rdfs:seeAlso | d=entityhub:ref


owl:sameAs | d=entityhub:ref
rdfs:domain | d=entityhub:ref
rdfs:range | d=entityhub:ref

http://dati.gov.it/onto/ann-voc/dafLabel
http://dati.gov.it/onto/ann-voc/dafId
http://dati.gov.it/onto/ann-voc/definedInOntology | d=entityhub:ref

#If one likes to also index ontologies one should add the following statements
#owl:*
#rdfs:*

# --- Dublin Core (DC) ---
# The default configuration imports all dc-terms data and copies values for the
# old dc-elements standard over to the according properties of the dc-terms
# standard.

# NOTE that a lot of other ontologies are also using DC for some of there data
#      therefore it is strongly recommended to include such information!

#mapping for all dc-terms properties
dc:*

# copy dc:title to rdfs:label
dc:title > rdfs:label

# deactivated by default, because such mappings are mapped to dc-terms
#dc-elements:*

# mappings for the dc-elements properties to the dc-terms
dc-elements:contributor > dc:contributor
dc-elements:coverage > dc:coverage
dc-elements:creator > dc:creator
dc-elements:date > dc:date
dc-elements:description > dc:description
dc-elements:format > dc:format
dc-elements:identifier > dc:identifier
dc-elements:language > dc:language
dc-elements:publisher > dc:publisher
dc-elements:relation > dc:relation
dc-elements:rights > dc:rights
dc-elements:source > dc:source
dc-elements:subject > dc:subject
dc-elements:title > dc:title
dc-elements:type > dc:type
#also use dc-elements:title as label
dc-elements:title > rdfs:label

# --- Social Networks (via foaf) ---
#The Friend of a Friend schema is often used to describe social relations between people
foaf:*

# copy the name of a person over to rdfs:label
foaf:name > rdfs:label

# additional data types checks
foaf:knows | d=entityhub:ref
foaf:made | d=entityhub:ref
foaf:maker | d=entityhub:ref
foaf:member | d=entityhub:ref
foaf:homepage | d=xsd:anyURI
foaf:depiction | d=xsd:anyURI
foaf:img | d=xsd:anyURI
foaf:logo | d=xsd:anyURI
#page about the entity
foaf:page | d=xsd:anyURI


# --- Schema.org --

# Defines an Ontology used by search engines (Google, Yahoo and Bing) for 
# indexing websites.

schema:*
# Copy all names of schema instances over to rdfs:label
schema:name > rdfs:label

# --- Simple Knowledge Organization System (SKOS) ---

# A common data model for sharing and linking knowledge organization systems 
# via the Semantic Web. Typically used to encode controlled vocabularies as
# a thesaurus  
skos:*

# copy all SKOS labels (preferred, alternative and hidden) over to rdfs:label
skos:prefLabel > rdfs:label
skos:altLabel > rdfs:label
skos:hiddenLabel > rdfs:label

# copy values of **Match relations to the according related, broader and narrower
skos:relatedMatch > skos:related
skos:broadMatch > skos:broader
skos:narrowMatch > skos:skos:narrower

#similar mappings for transitive variants are not contained, because transitive
#reasoning is not directly supported by the Entityhub.

# Some SKOS thesaurus do use "skos:transitiveBroader" and "skos:transitiveNarrower"
# however such properties are only intended to be used by reasoners to
# calculate transitive closures over broader/narrower hierarchies.
# see http://www.w3.org/TR/skos-reference/#L2413 for details
# to correct such cases we will copy transitive relations to their counterpart
skos:narrowerTransitive > skos:narrower
skos:broaderTransitive > skos:broader


# --- Semantically-Interlinked Online Communities (SIOC) ---

# An ontology for describing the information in online communities. 
# This information can be used to export information from online communities 
# and to link them together. The scope of the application areas that SIOC can 
# be used for includes (and is not limited to) weblogs, message boards, 
# mailing lists and chat channels.
sioc:*

# --- biographical information (bio)
# A vocabulary for describing biographical information about people, both living
# and dead. (see http://vocab.org/bio/0.1/)
bio:*

# --- Rich Site Summary (rss) ---
rss:*

# --- GoodRelations (gr) ---
# GoodRelations is a standardised vocabulary for product, price, and company data
gr:*

# --- Creative Commons Rights Expression Language (cc)
# The Creative Commons Rights Expression Language (CC REL) lets you describe 
# copyright licenses in RDF.
cc:*






