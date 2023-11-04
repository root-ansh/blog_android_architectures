# Android Architecture Experiments

This repo will have all the blogs/experments/samples on android architecture used. 

- [single module](/single_module) : this will have an understanding of basic mvvm clean architecture in a single module. we will cover the various decisions took around creating a repo,usecase,datasource,viewmodel,presentation,folders inside the presentation module , the choice of coroutines/concurrency, types of usecase, etc in this repo
  - todo use hashnode for this
  - todo replace single module project with hilt 2023
-  [multi module](/multi_module) : same as single module, but  splitted into multiple modules based on generic layers. this is relatively simpler architecture, but allows discussing module caching, module graphs and how to decrease build times.
-  [multi module](/multi_module) : same as single module, but  splitted into multiple modules based on business feature. this is somewhat tough, more verbose but quite popular as it mimics the "microservice" architecture from the backend world. we will associate this with business requirements and feature like "dynamic module install"
- 