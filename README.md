# Introduction #
Realize the dynamic load of thesaurus and don't need to restart the service can achieve dynamic update thesaurus immediate effect, the project home is https://github.com/liang68/solr-repositories.git

# Feature #
 - based on IK Analyer 2012-FF Hotfix 1 
 - added support for Lucene 6.3.0 API

#Installation #

 - JDK8 

>  mvn clean install

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
2. New way of remote calls automatically update thesaurus interval 

# Summary #
### Two kinds of method for automatic dynamic loading dictionary ###
 - 1. Configure the local external thesaurus update by a scalar
 - 2. By configuring the remote file to update thesaurus   (All the back of the class with the suffix Remote is corresponding to the realization of the way)

    更加详尽的解释说明请查看我的博客：http://www.cnblogs.com/liang1101/articles/6395016.html
    
    