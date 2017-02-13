# Introduction #
Realize the dynamic load of thesaurus and don't need to restart the service can achieve dynamic update thesaurus immediate effect, the project home is https://github.com/liang68/solr-repositories.git

# Feature #
 - based on IK Analyer 2012-FF Hotfix 1 
 - added support for Lucene 6.3.0 API

#Installation #

 - JDK8 

>  mvn clean install

 - JDK7

> mvn clean -Djavac.src.version=1.7 -Djavac.target.version=1.7 install

# Configuration #
### solrconfig.xml ###
    Add ik-analyzer-solr-6.3.0.jar   
    <lib dir="/home/solr/solr/dist/" regex="ik-analyzer-solr-\d.*\.jar" />

### schema.xml ###
    Add custom extensions class IKTokenizerFactory and use it(contains extended thesaurus dynamic loading)
    <fieldType name="text_ik" class="solr.TextField">   
      <analyzer type="index">
        <tokenizer class="org.wltea.analyzer.lucene.IKTokenizerFactory" useSmart="false" conf="ik.conf" />
        <filter class="solr.StopFilterFactory" ignoreCase="true" words="stopwords.txt" />
      </analyzer>
      <analyzer type="query">
        <tokenizer class="org.wltea.analyzer.lucene.IKTokenizerFactory" useSmart="true" conf="ik.conf" />
        <filter class="solr.StopFilterFactory" ignoreCase="true" words="stopwords.txt" />
      </analyzer>
    </fieldType>

or

    The default load IK thesaurus
    <fieldType name="text_ik" class="solr.TextField">   
      <analyzer type="index" useSmart="false" class="org.wltea.analyzer.lucene.IKAnalyzer"/>   
      <analyzer type="query" useSmart="true" class="org.wltea.analyzer.lucene.IKAnalyzer"/>   
    </fieldType>

# Compilation Error #
1. Question: The following error happened while running "mvn clean install"

> [ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.3:compile (default-compile) on project ik-analyzer-solr5: Fatal error compiling: invalid target release: 1.8 -> [Help 1]

Answer: Please check your JAVA_HOME setting. If JAVA_HOME setting exists, it may not be JAVA8.  


# Resources #
1. [Build IKAnalyzer With Solr 6.3.0](http://www.cnblogs.com/liang1101/articles/6395016.html)
    