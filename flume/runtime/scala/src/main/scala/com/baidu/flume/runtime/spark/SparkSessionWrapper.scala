package com.baidu.flume.runtime.spark

import java.io.File

import scala.collection.JavaConverters._
import scala.io.Source
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.SparkSession
import baidu.flume.Config.{PbJobConfig, PbKVConfig}
import com.baidu.flume.runtime.spark.impl.util.Utils

/**
  * Spark Session Wrapper, Singleton.
  *
  * This wrapper holds preparedArchivePath, applicationArchive and pbJobConfig, which should be set
  * before calling [[SparkSessionWrapper.getSparkContext()]] and set only once. Later assignments to
  * these variables have no effect on the sparkSession.
  *
  * @author Ye, Xianjin(yexianjin@baidu.com)
  */
object SparkSessionWrapper extends Logging {

  var preparedArchivePath: String = _
  var applicationArchive: String = _
  var pbJobConfig: PbJobConfig = _

  lazy private val spark: SparkSession = {
    val conf = generateSparkConf()
    SparkSession.builder().config(conf).getOrCreate()
  }

  private def bigflowApplicationConfPass(conf: SparkConf, pbJobConf: PbJobConfig): SparkConf = {
    for (item: PbKVConfig <- pbJobConf.getKvConfigList.asScala
         if item.hasKey && !item.getKey.toStringUtf8.startsWith("spark.")){ // use toStringUtf8 blindly
      val key = item.getKey.toStringUtf8
      log.info(s"Bigflow config key: $key")
      lazy val value = item.getValue.toStringUtf8
      key match {
        case hadoop if key.startsWith("hadoop.") => conf.set(hadoop, value)
        // todo: handle cache file and cache archive for all supported cluster managers
        case BigflowConstants.userCacheArchiveKey =>
          log.info(s"Got bigflow cache archive: $value")
          val cacheList = Utils.mergeCacheFileLists(conf.get("spark.yarn.dist.archives", ""), value)
          if (cacheList != null) {
            conf.set("spark.yarn.dist.archives", cacheList)
          }
        case BigflowConstants.userCacheFileKey =>
          log.info(s"Got bigflow cache file: $value")
          val cacheList = Utils.mergeCacheFileLists(conf.get("spark.yarn.dist.files", ""), value)
          if (cacheList != null) {
            conf.set("spark.yarn.dist.files", cacheList)
          }
        case _ if key.startsWith(BigflowConstants.confKeyPrefix) => conf.set(key, value)
        case _ =>
      }
    }
    conf
  }


  /**
    * Set BIGFLOW_PYTHON_EXCEPTION_TOFT_STYLE_PATH to executor's Env. To hide sensitive password,
    * the environment key is appended with "_WITH_PASSWORD".
    * @param conf SparkConf
    * @return SparkConf
    */
  private def setBigFlowExceptionPath(conf: SparkConf): SparkConf = {
    val scriptPath = new File(".", "prepare/set_exception_path.sh")
    if (scriptPath.exists()) {
      val toftStylePath = Utils.autoClose(Source.fromFile(scriptPath)) { fileSource =>
        fileSource.getLines()
          .filter(line => line.contains(BigflowConstants.exceptionPathEnvKey))
          .map(line => line.split("=", 2)(1))
          .toList
      }
      if (toftStylePath.isEmpty) {
        log.info(s"set_exception_path.sh doesn't contain ${BigflowConstants.exceptionPathEnvKey}")
      } else {
       conf.setExecutorEnv(BigflowConstants.exceptionPathEnvShadedKey, toftStylePath.head)
      }
    } else {
      log.info("set_exception_path.sh doesn't exist, skipping")
    }
    conf
  }

  private def generateSparkConf(): SparkConf = {
    val conf = new SparkConf()

    // TODO: set default java_home to java8-ee, which shall not be set by user.
    conf.setIfMissing("spark.executorEnv.JAVA_HOME", "$HADOOP_YARN_HOME/../java8-ee/")
    conf.setIfMissing("spark.appMasterEnv.JAVA_HOME", "$HADOOP_YARN_HOME/../java8-ee/")

    val mergedConf: SparkConf = bigflowApplicationConfPass(conf, pbJobConfig)
    val sparkYarnArchiveKey = "spark.yarn.dist.archives"
    val mergedArchives: String = Utils.mergeCacheFileLists(mergedConf.get(sparkYarnArchiveKey, ""),
      s"$preparedArchivePath#__bigflow_on_spark__",
      s"$applicationArchive#__bigflow_on_spark_application__")
    mergedConf.set(sparkYarnArchiveKey, mergedArchives)
    setBigFlowExceptionPath(mergedConf)
  }

  def getSparkContext(): SparkContext = {
    spark.sparkContext
  }

  def getSparkSession(): SparkSession = {
    spark
  }

}

