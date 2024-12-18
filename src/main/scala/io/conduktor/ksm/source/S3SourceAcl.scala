package io.conduktor.ksm.source

import com.typesafe.config.Config
import io.conduktor.ksm.parser.AclParserRegistry
import io.conduktor.ksm.source
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{GetObjectRequest, S3Exception}

import java.io._
import java.time.Instant
import scala.util.{Failure, Success, Try}

class S3SourceAcl(parserRegistry: AclParserRegistry)
    extends SourceAcl(parserRegistry) {

  private val log = LoggerFactory.getLogger(classOf[S3SourceAcl])

  /**
    * Config Prefix for configuring this module
    */
  override val CONFIG_PREFIX: String = "s3"

  final val BUCKET_NAME = "bucketname"
  final val BUCKET_KEY = "objectkey"

  var lastModified: Instant = Instant.EPOCH
  var bucket: String = _
  var key: String = _
  var region: String = _

  def configure(bucketName: String, objectKey: String, regn: String): Unit = {
    bucket = bucketName
    key = objectKey
    region = regn
  }

  def s3Client(): S3Client =
    S3Client.builder().build

  /**
    * internal config definition for the module
    */
  override def configure(config: Config): Unit = {
    bucket = config.getString(BUCKET_NAME)
    key = config.getString(BUCKET_KEY)
  }

  /**
    * Refresh the current view on the external source of truth for Acl
    * Ideally this function is smart and does not pull the entire external Acl at every iteration
    * Return `None` if the Source Acls have not changed (usually using metadata).
    * Return `Some(x)` if the Acls have changed. `x` represents the parsing and parsing errors if any
    * Note: the first call to this function should never return `None`.
    *
    * Kafka Security Manager will not update Acls in Kafka until there are no errors in the result
    *
    * @return
    */
  override def refresh(): Option[ParsingContext] = {
    val s3 = s3Client()
    val s3Response = Try(
      s3.getObject(
        GetObjectRequest.builder()
          .bucket(bucket)
          .key(key)
          .ifModifiedSince(lastModified)
          .build()
      )
    )

    s3Response match {
      case Success(s3ObjectStream) =>
        var reader: BufferedReader = null
        try {
          reader = new BufferedReader(
            new InputStreamReader(s3ObjectStream)
          )
          lastModified = s3ObjectStream.response().lastModified()

          val content =
            Stream
              .continually(reader.readLine())
              .takeWhile(_ != null)
              .map(_.concat("\n"))
              .mkString
          Some(
            source.ParsingContext(
              parserRegistry.getParserByFilename(key),
              new BufferedReader(new StringReader(content))
            )
          )
        } finally {
          // no try-with-resources in Scala 2.12 :/
          if (reader != null) reader.close()
          s3ObjectStream.close()
        }
      case Failure(exception: S3Exception) if exception.statusCode() == 304 =>
        log.debug("S3 object not modified--skipping")
        None
      case Failure(error) =>
        log.error("Error fetching S3 object", error)
        None
    }
  }

  /**
    * Close all the necessary underlying objects or connections belonging to this instance
    */
  override def close(): Unit = {
    // S3 (HTTP)
  }
}
