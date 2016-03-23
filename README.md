# Hub
Hub for preprocessing of questions for yodaQA
Hub is interlink between YodaQA-client and yodaQA. It's purpose is to get question from YodaQA-client, preprocess it, send it to yodaQA (or to other service, if it decides so), get answer and send answer back to web interface.

##Installation Instructions
Quick instructions for setting up, building and running:

  * You need Java 1.8 and Gradle
  * We assume that you cloned Hub and are now in the directory that contains this README.
  * ``gradlew build`` to build
  * ``gradlew run -PexecArgs="[Port to run] [Address of YodaQA] [Address of Lookup Service]"`` to run

####Example
YodaQA runs on ``http://localhost:4567/``. Lookup Service runs on ``http://localhost:5000/``. To run HUB on port 4568 (it must differ from yodaQA's port and Lookup Service port), run in it's root directory ``gradlew build`` and ``gradlew run -PexecArgs="4568 http://localhost:4567/ http://[::1]:5000/"``. To connect YodaQA-client to HUB add ``?e=http://localhost:4568/`` to the end of url.

####Traffic info
You can test traffic question topic detection by running ``gradlew run_traffic -PexecArgs="[Question] [Address of Lookup Service]"`. Output will be in the form of "topic<TAB>street name". This is used for testing mainly.
You need running Lookup Services https://github.com/brmson/label-lookup for this feature.

####Traffic info testing on dataset
You can test traffic on dataset https://docs.google.com/spreadsheets/d/1LAY6trroXwdL8OQVGbBym6EA4yPZEFdR9eHJRAOZIIY/edit?usp=sharing. This dataset contains question and it's topic with street names.
We usually have one street name (for questions concerning only single street), however there can be multiple for questions like "How to get from Evropská to Technická".
Download the spreadsheet as .tsv file. Start test by ``gradlew run_trafficTest -PexecArgs="[Location of .tsv file] [Address of Lookup Service]"`. It will evaluate
how many questions has correctly detected topic and street name.

##Dialog API
Dialog API expands YodaQA's API https://github.com/brmson/yodaqa/blob/master/doc/REST-API.md. Hub gets request from
web client and sends it further to YodaQA with minor changes. The list of changes follows.

####Artificial concepts
Artificial concepts are concepts, selected by users and not generated by YodaQA. Information about them are send in fields:
* numberOfConcepts - number of concepts generated in total
* fullLabel{i} - full label of selected concept of number {i} (replace '{i}' with number)
* pageID{i} - page id of selected concept of number {i} (replace '{i}' with number)

##Coreference resolution
Concepts of last n answers are used, when third person pronoun(he, she, it) is founded in question's text.
``MAX_QUESTIONS_TO_REMEMBER_CONCEPT`` constant is used as n. Default value is 5.

####Example
When the first question is "What book wrote J. R. R. Tolkien?", the generated concept is "J. R. R. Tolkien". The second
question "Where was he born?" contains "he", so concept from the first question will be used.

##Transformations
Hub can transform questions and answers also. We are using "age transform" currently.

####Example
Age transform changes question from "How old is someone?" to "When he was born?". We do it, because YodaQA has better
success with finding of birth date. Answers are transformed back by calculation difference between today's date and date in answer.
Transformed answers are showed to users.

##Traffic questions
Hub can help you to not get stuck in the traffic jam. You can ask him for actual traffic flow or traffic incidents in some street.
Prague is supported in current version only. We take data form Here. You need running Lookup Services https://github.com/brmson/label-lookup for this feature.
Connection address can be changed in the eu.ailao.hub.traffic.analyze.QuestionAnalyzer.class by changing the value of LABEL_LOOKUP_ADDRESS variable.
We use Lookup Services for detecting street in question.

####Example
You can ask "What is the traffic flow in the Wolkerova street?" or make command "Show me all traffic incidents in Wolkerova street!".
Hub detect's question or command concerning traffic and it will take data from Here instead of asking yodaQA.