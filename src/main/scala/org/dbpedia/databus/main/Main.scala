package org.dbpedia.databus.main

import better.files.File
import org.dbpedia.databus.filehandling.FileUtil
import org.dbpedia.databus.filehandling.converter.Converter
import org.dbpedia.databus.filehandling.downloader.Downloader
import org.dbpedia.databus.main.cli.CLIConf
import org.slf4j.LoggerFactory

object Main {

  def main(args: Array[String]) {

//    args.foreach(println(_))

    println("Welcome to DBPedia - Databus Client")

    val conf = new CLIConf(args)
    val cache_dir = File("./target/databus.tmp/cache_dir/")
    cache_dir.createDirectoryIfNotExists()

    // Take query as Source
    if (conf.query.isDefined) {

      //Test if query is a File or a Query
      val queryString: String = {
        if (File(conf.query()).exists()) Downloader.readQueryFile(File(conf.query()))
        else conf.query()
      }

      println("\n========================================================\n")
      println("DOWNLOAD TOOL:")

      val allSHAs = Downloader.downloadWithQuery(queryString, cache_dir, conf.overwrite())

      if(conf.compression() == "same" && conf.format() == "same"){
        allSHAs.foreach(
          sha => FileUtil.copyUnchangedFile(FileUtil.getFileWithSHA256(sha, cache_dir), cache_dir, File(conf.target()))
        )

        println("\n--------------------------------------------------------\n")
        println(s"Files have been downloaded to ${conf.target()}")
      }
      else{
        println("\n========================================================\n")
        println("CONVERSION TOOL - for queried files:\n")

        if(!isSupportedOutFormat(conf.format())) System.exit(1)

        allSHAs.foreach(
          sha => Converter.convertFile(FileUtil.getFileWithSHA256(sha, cache_dir), File(conf.target()), conf.format(), conf.compression())
        )
      }
    }

    // Take already existing files as source
    if (conf.source.isDefined) {
      val dataId_string = "dataid.ttl"

      println("\n========================================================\n")
      println("CONVERSION TOOL - for source files:\n")

      println(s"""convert file(s) from source:\n${conf.source()}\n\nto destination:\n${conf.target()}""")
      println("\n========================================================\n")

      if(!isSupportedOutFormat(conf.format())) System.exit(1)

      val source = File(conf.source())
      val destination_dir = File(conf.target())

      if (source.isDirectory) {
        val files = source.listRecursively.toSeq
        for (file <- files) {
          if (!file.isDirectory) {
            if (!file.name.equals(dataId_string)) {
              Converter.convertFile(file, destination_dir, conf.format(), conf.compression())
            }
          }
        }
      }
      else {
        Converter.convertFile(source, destination_dir, conf.format(), conf.compression())
      }
    }
  }

  def isSupportedOutFormat(format:String): Boolean ={
    if (format.matches("rdfxml|ttl|nt|jsonld|tsv|same")) true
    else {
      LoggerFactory.getLogger("File Format Logger").error(s"Input file format $format is not supported.")
      println(s"Output file format $format is not supported.")
      false
    }

  }

//  def iterateOverDir(source:File, function:(File,File,String,String)=>Unit):Unit={
//    val dataId_string = "dataid.ttl"
//
//    if (source.isDirectory) {
//      val files = source.listRecursively.toSeq
//      for (file <- files) {
//        if (!file.isDirectory) {
//          if (!file.name.equals(dataId_string)) {
//            function()
////            Converter.convertFile(file, destination_dir, conf.format(), conf.compression())
//          }
//        }
//        else if (file.name == "temp") { //Delete temp dir of previous failed run
//          file.delete()
//        }
//      }
//    }
//    else {
//      function()
//      function
////      Converter.convertFile(source, destination_dir, conf.format(), conf.compression())
//    }
//  }
}